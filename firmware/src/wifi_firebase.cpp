#include "wifi_firebase.hpp"
#include <ArduinoJson.h>
#include "consts.hpp"

#define HTTP_CODE_OK 200

/* TODO: The user ID is hardcoded to one test account for the demo, so every board
 * uploads into that same node. Wire this to the real signed-in user (through
 * PairingData / BLE provisioning) in sprint 3.
 */
WiFiFirebase::WiFiFirebase(const char* dbUrl, const char* apiKey, const char* email, const char* password)
    : dbUrl(dbUrl),
      _userId("6pkWpLZO4BYFl2j4OQivkPupXuB3"),
      _auth(apiKey, email, password) {}

WiFiFirebase::~WiFiFirebase() {
    delete fb;
}

/**
 * Signs in (or refreshes) via _auth and, whenever the ID token changes, rebuilds
 * the underlying Firebase client with it. The Firebase library only accepts the
 * auth token through its constructor (no setter), so the client has to be
 * recreated on every refresh instead of updated in place.
 */
bool WiFiFirebase::ensureFreshToken() {
    if (!_auth.ensureFreshToken()) {
        return false;
    }

    if (fb == nullptr || _appliedIdToken != _auth.idToken()) {
        delete fb;
        fb = new Firebase(dbUrl, _auth.idToken());
        _appliedIdToken = _auth.idToken();
        _deviceId = _auth.localId();
    }

    return true;
}

/**
 * Private helper method called by other WiFiFirebase API methods to set a JSON
 * string at a given Firebase path. Retries up to maxAttempts times on failure.
 *
 * @param path The Firebase path to set.
 * @param json The JSON string to set.
 * @param maxAttempts How many attempts to make before giving up. getCurrentBucketInfo
 *   passes 1 here: its retry loop happens once per tick already (via the outer
 *   TelemetryManager re-attempting sync() every tick), so adding delay()-based
 *   retries underneath it would just block the main loop - and bleWifi.poll() with
 *   it - for several extra seconds per tick while the DB is unreachable.
 * @return true if the operation was successful within maxAttempts, false otherwise.
 */
bool WiFiFirebase::setJson(const String& path, const String& json, int maxAttempts) {
    if (fb == nullptr) {
        return false;
    }
    for (int attempts = 0; attempts < maxAttempts; attempts++) {
        int code = fb->setJson(path, json);
        if (code == HTTP_CODE_OK) {
            return true;
        }
        bool willRetry = attempts < maxAttempts - 1;
        Serial.print("HTTP "+String(code)+": Failed to send JSON to "+path);
        Serial.println(willRetry ? " Retrying in 1 second..." : "");
        if (willRetry) {
            delay(1000);
        }
    }
    return false;
}

/**
 * Private helper method called by other WiFiFirebase API methods to get a JSON
 * string from a given Firebase path. Retries up to maxAttempts times on failure.
 *
 * @param path The Firebase path to get.
 * @param out The string to store the retrieved JSON.
 * @param maxAttempts How many attempts to make before giving up. See setJson's doc
 *   for why getCurrentBucketInfo passes 1 here.
 * @return true if the operation was successful within maxAttempts, false otherwise.
 */
bool WiFiFirebase::getJson(const String& path, String& out, int maxAttempts) {
    if (fb == nullptr) {
        return false;
    }
    for (int attempts = 0; attempts < maxAttempts; attempts++) {
        int code = fb->getJson(path, out);
        if (code == HTTP_CODE_OK) {
            return true;
        }
        bool willRetry = attempts < maxAttempts - 1;
        Serial.print("HTTP "+String(code)+": Failed to read JSON from "+path);
        Serial.println(willRetry ? " Retrying in 1 second..." : "");
        if (willRetry) {
            delay(1000);
        }
    }
    return false;
}

/**
 * Private helper method which constructs a bucket key corresponding to the given
 * bucket index.
 *
 * @param index The index of the bucket.
 * @return The bucket key.
 */
String WiFiFirebase::bucketKey(int index) {
    String padded = String(index);
    int width = String(RESOLUTION - 1).length();
    while (padded.length() < width) {
        padded = "0" + padded;
    }
    return "B" + padded;
}

/**
 * Private helper method which constructs the telemetry path for a given sensor.
 *
 * @param sensorId The sensor id.
 * @return The telemetry path for the given sensor.
 */
String WiFiFirebase::telemetrySensorPath(const String& sensorId) const {
    return String(_userId) + "/telemetry/" + _deviceId + "/" + sensorId;
}

/**
 * Private helper method which constructs the telemetry instant path for a given sensor.
 *
 * @param sensorId The sensor id.
 * @return The telemetry instant path.
 */
String WiFiFirebase::telemetrySensorInstantPath(const String& sensorId) const {
    return telemetrySensorPath(sensorId) + "/last_instant";
}

/**
 * Private helper method which constructs the telemetry node path for a given sensor and period.
 *
 * @param sensorId The sensor id.
 * @param periodId The period id.
 * @return The telemetry node path.
 */
String WiFiFirebase::telemetrySensorPeriodPath(const String& sensorId, const String& periodId) const {
    return telemetrySensorPath(sensorId) + "/" + periodId;
}

/**
 * Private helper method which constructs the telemetry index path for a given sensor and period.
 *
 * @param sensorId The sensor id.
 * @param periodId The period id.
 * @return The telemetry index path.
 */
String WiFiFirebase::telemetrySensorPeriodIndexPath(const String& sensorId, const String& periodId) const {
    return telemetrySensorPeriodPath(sensorId, periodId) + "/index";
}

/**
 * Private helper method which constructs the telemetry bucket path for a given sensor, period, and index.
 *
 * @param sensorId The sensor id.
 * @param periodId The period id.
 * @param index The bucket index.
 * @return The telemetry bucket path.
 */
String WiFiFirebase::telemetrySensorPeriodBucketPath(const String& sensorId, const String& periodId, int index) const {
    return telemetrySensorPeriodPath(sensorId, periodId) + "/buckets/" + bucketKey(index);
}

/**
 * Private helper method which constructs the telemetry bucket timestamp path for a given sensor, period, and index.
 *
 * @param sensorId The sensor id.
 * @param periodId The period id.
 * @param index The bucket index.
 * @return The telemetry bucket timestamp path.
 */
String WiFiFirebase::telemetrySensorPeriodBucketTimestampPath(const String& sensorId, const String& periodId, int index) const {
    return telemetrySensorPeriodBucketPath(sensorId, periodId, index) + "/timestamp";
}

/**
 * Gets the current bucket index and timestamp for a given sensor and period from the database.
 *
 * @param sensorId The sensor id.
 * @param periodId The period id.
 * @param outIndex The output parameter to store the retrieved index. Will not be modified if no index is found.
 * @param outTimestamp The output parameter to store the retrieved timestamp of the current bucket. Will not be modified if no timestamp is found.
 * @return true if the operation was successful within 3 attempts, false otherwise.
 */
bool WiFiFirebase::getCurrentBucketInfo(
    const String& sensorId, const String& periodId, long& outIndex, unsigned long& outTimestamp
) {
    String indexPath = telemetrySensorPeriodIndexPath(sensorId, periodId);

    String indexBody;
    if (!getJson(indexPath, indexBody, 1)) {
        return false;
    }

    indexBody.trim();
    if (indexBody.length() == 0 || indexBody == "null") {
        return true;
    } else {
        outIndex = indexBody.toInt();
    }

    String timestampPath = telemetrySensorPeriodBucketTimestampPath(sensorId, periodId, outIndex);

    String timestampBody;
    if (!getJson(timestampPath, timestampBody, 1)) {
        return false;
    }

    timestampBody.trim();
    if (!(timestampBody.length() == 0 || timestampBody == "null")) {
        outTimestamp = (unsigned long) timestampBody.toInt();
    }

    return true;
}

bool WiFiFirebase::commitBucket(
    const String& sensorKind, const String& kind, int index, unsigned long timestamp, float value
) {
    JsonDocument doc;
    doc["timestamp"] = timestamp;
    doc["value"] = serialized(String(value, 7));
    String json;
    serializeJson(doc, json);

    bool ok = true;
    ok = ok && setJson(telemetrySensorPeriodBucketPath(sensorKind, kind, index), json);
    ok = ok && setJson(telemetrySensorPeriodIndexPath(sensorKind, kind), String(index));
    return ok;
}

bool WiFiFirebase::commitInstant(const String& sensorKind, unsigned long timestamp, float value) {
    JsonDocument doc;
    doc["timestamp"] = timestamp;
    doc["value"] = serialized(String(value, 7));
    String json;
    serializeJson(doc, json);

    return setJson(telemetrySensorInstantPath(sensorKind), json);
}
