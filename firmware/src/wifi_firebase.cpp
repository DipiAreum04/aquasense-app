#include "wifi_firebase.hpp"
#include <ArduinoHttpClient.h>
#include <ArduinoJson.h>
#include "consts.hpp"

#define HTTP_CODE_OK 200

namespace {
    const uint16_t HTTPS_PORT = 443;
}

/* The owner UID is deliberately not set here. It identifies the human user whose
 * subtree this board writes into, which is not knowable at construction: it arrives
 * from the app over BLE during pairing, and from EEPROM on every boot after that.
 * setOwnerUid() supplies it, and nothing here will write until it has.
 */
WiFiFirebase::WiFiFirebase(const char* dbUrl, const char* apiKey, const char* email, const char* password)
    : _dbHost(hostFromUrl(dbUrl)),
      _http(_tls, _dbHost.c_str(), HTTPS_PORT),
      _auth(apiKey, email, password) {
    /* Reuse the connection instead of handshaking per request. HttpClient reconnects
     * on its own whenever the socket has gone away, so nothing here has to track
     * whether one is currently open. Unlike the response timeout, this flag survives
     * stop(), so setting it once is enough.
     */
    _http.connectionKeepAlive();

    // Stream reads default to a 1s timeout, which a 30KB body can outlast mid-transfer.
    _http.setTimeout(HTTP_TIMEOUT_MS);
}

/**
 * Private helper method which consumes whatever is left of the response body, so
 * that the socket is lined up at the start of the next reply, and reports whether
 * the connection can be trusted for another request.
 *
 * Only a reply that declared a Content-Length can be finished with certainty.
 * ArduinoHttpClient cannot detect the end of a chunk-encoded body - endOfBodyReached()
 * is hardwired to false without a Content-Length, and the library's own responseBody()
 * gives up by waiting for a read timeout - so rather than stall a tick guessing where
 * the body ended, those replies close the connection and the next request opens a
 * fresh one. That is exactly what every request did before keep-alive, so the worst
 * case here is the old behaviour rather than a regression.
 */
void WiFiFirebase::finishResponse() {
    if (_http.contentLength() < 0) {
        _http.stop();
        return;
    }

    unsigned long start = millis();
    while (!_http.endOfBodyReached() && millis() - start < HTTP_TIMEOUT_MS) {
        if (_http.available()) {
            _http.read();
        } else if (_http.connected()) {
            delay(1);
        } else {
            break;
        }
    }

    // Stalled partway through the body: what is left would be read as the next
    // reply's status line, so the socket cannot be handed on.
    if (!_http.endOfBodyReached()) {
        _http.stop();
    }
}

/**
 * Signs in (or refreshes) via _auth and keeps the token and device id the requests
 * below are built from. Every request carries the token in its query string, so
 * there is no client to rebuild when it changes - only these two copies.
 */
bool WiFiFirebase::ensureFreshToken() {
    /* Refused rather than defaulted. With no owner UID every path below would start
     * with a bare slash, and those writes are rejected by the security rules - which
     * would surface as a board that signs in happily and then silently publishes
     * nothing, the hardest failure here to trace back to its cause.
     */
    if (_userId.length() == 0) {
        Serial.println("No owner UID set - the board has not been paired. Refusing to publish.");
        return false;
    }

    if (!_auth.ensureFreshToken()) {
        return false;
    }

    if (_appliedIdToken != _auth.idToken()) {
        _appliedIdToken = _auth.idToken();
        _deviceId = _auth.localId();
    }

    /* The app claimed the aquarium under the UID it read over BLE, while writes here
     * are authorised against whatever Firebase signed this board in as. Those are two
     * separate sources for one value, and if they disagree the rules reject every
     * write while auth, Wi-Fi and the sensors all look perfectly healthy.
     *
     * Checked once, on the first sign-in, rather than on every hourly refresh.
     */
    if (!_deviceIdChecked && _expectedDeviceId.length() > 0) {
        _deviceIdChecked = true;
        if (_deviceId != _expectedDeviceId) {
            Serial.println("WARNING: this board's Firebase UID does not match the flashed one.");
            Serial.println("  flashed (PairingData): " + _expectedDeviceId);
            Serial.println("  Firebase sign-in:      " + _deviceId);
            Serial.println("  Telemetry will be rejected until these agree.");
        }
    }

    return true;
}

void WiFiFirebase::setOwnerUid(const String& ownerUid) {
    _userId = ownerUid;
}

void WiFiFirebase::setExpectedDeviceUid(const String& deviceUid) {
    _expectedDeviceId = deviceUid;
}

/**
 * Private helper method which reads a node and keeps only the fields named by
 * filter, parsing straight off the socket rather than buffering the response.
 *
 * A telemetry node carries every one of its buckets - up to 100 per period, so
 * roughly 30KB for a whole sensor - which is more than the board's 32KB of SRAM.
 * ArduinoJson's filter lets that stream past while only the handful of fields we
 * asked for are ever materialised, so the cost is transfer time rather than
 * memory. Reading the body into a String first would not fit.
 *
 * Makes a single attempt: TelemetryManager re-runs sync() every tick until it
 * succeeds, so retrying here as well would just block the main loop - and
 * bleWifi.poll() with it - while the DB is unreachable.
 *
 * @param path The Firebase path to read.
 * @param filter An ArduinoJson filter naming the fields to keep.
 * @param out The document to parse the filtered response into.
 * @return true if the node was read and parsed, false otherwise.
 */
bool WiFiFirebase::getJsonFiltered(const String& path, const JsonDocument& filter, JsonDocument& out) {
    // stop() puts this back to the 30s default, so it is re-applied per request
    // rather than set once alongside connectionKeepAlive().
    _http.setHttpResponseTimeout(HTTP_TIMEOUT_MS);

    bool ok = false;
    int result = _http.get("/" + path + ".json?auth=" + _appliedIdToken);
    if (result == HTTP_SUCCESS) {
        result = _http.responseStatusCode();
        if (result == HTTP_CODE_OK) {
            _http.skipResponseHeaders();
            DeserializationError err = deserializeJson(out, _http, DeserializationOption::Filter(filter));
            ok = !err;
            if (err) {
                Serial.print("Failed to parse JSON from "+path+": ");
                Serial.println(err.c_str());
            }
        }
    }

    if (ok) {
        finishResponse();
    } else {
        // A failed exchange leaves the socket at an unknown offset; drop it so the
        // next request cannot read this reply's leftovers as its own.
        _http.stop();
        if (result != HTTP_CODE_OK) {
            Serial.println("HTTP "+String(result)+": Failed to read JSON from "+path);
        }
    }
    return ok;
}

/**
 * Private helper method which builds one multi-location PATCH request against the
 * database and returns its HTTP status (or one of ArduinoHttpClient's negative
 * error codes if the request never got that far).
 *
 * PATCH is how the REST API expresses a multi-location update: every key in the
 * body is a path relative to rootPath, and the database fans them out server-side.
 *
 * @param rootPath The path the relative keys in json are resolved against.
 * @param json A JSON object whose keys are paths relative to rootPath.
 * @return The HTTP status code, or a negative HttpClient error code.
 */
int WiFiFirebase::patchJson(const String& rootPath, const String& json) {
    // stop() puts this back to the 30s default, so it is re-applied per request
    // rather than set once alongside connectionKeepAlive().
    _http.setHttpResponseTimeout(HTTP_TIMEOUT_MS);

    String path = "/" + rootPath + ".json?auth=" + _appliedIdToken;

    _http.beginRequest();
    int result = _http.patch(path);
    if (result == HTTP_SUCCESS) {
        _http.sendHeader("Content-Type", "application/json");
        _http.sendHeader("Content-Length", (int) json.length());
        _http.beginBody();
        _http.print(json);
        _http.endRequest();
        result = _http.responseStatusCode();
    }

    if (result == HTTP_CODE_OK) {
        // The body is the patched data echoed back. It is of no use to us, but it
        // has to be read off the socket before the socket can carry anything else.
        _http.skipResponseHeaders();
        finishResponse();
    } else {
        // A failed exchange leaves the socket at an unknown offset; drop it so the
        // next request cannot read this reply's leftovers as its own.
        _http.stop();
    }

    return result;
}

/**
 * Private helper method which writes several locations at once through a single
 * PATCH, the REST equivalent of a multi-location update: the keys of json are
 * paths relative to rootPath and the database fans them out server-side.
 *
 * One PATCH costs one TCP+TLS handshake, and on the R4's crypto hardware that
 * handshake dominates the cost of a request - so batching n writes here is close
 * to an n-fold saving over n setJson calls.
 *
 * Makes a single attempt, for the same reason getJsonFiltered does: the tick is
 * already the retry loop, and it comes round every second. Retrying here instead
 * would hold the main loop for a multiple of the request timeout with no
 * bleWifi.poll() in between, to arrive at the same place a second later.
 *
 * @param rootPath The path the relative keys in json are resolved against.
 * @param json A JSON object whose keys are paths relative to rootPath.
 * @return true if the update was accepted, false otherwise.
 */
bool WiFiFirebase::setJsonMulti(const String& rootPath, const String& json) {
    if (_appliedIdToken.length() == 0) {
        return false;
    }

    int code = patchJson(rootPath, json);
    if (code == HTTP_CODE_OK) {
        return true;
    }

    Serial.println("HTTP "+String(code)+": Failed to patch JSON at "+rootPath);
    return false;
}

/**
 * Private helper method which strips the scheme and any path off a database URL,
 * leaving the bare host that ArduinoHttpClient expects.
 *
 * @param url The database URL.
 * @return The host portion of the URL.
 */
String WiFiFirebase::hostFromUrl(const char* url) {
    String host(url);

    int schemeEnd = host.indexOf("://");
    if (schemeEnd >= 0) {
        host.remove(0, schemeEnd + 3);
    }

    int pathStart = host.indexOf('/');
    if (pathStart >= 0) {
        host.remove(pathStart);
    }

    return host;
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
 * Private helper method which constructs the telemetry path for this device. This
 * is the node every sensor hangs off, and so the root the batched instant update
 * is patched against.
 *
 * @return The telemetry path for this device.
 */
String WiFiFirebase::telemetryDevicePath() const {
    return _userId + "/telemetry/" + _deviceId;
}

/**
 * Private helper method which constructs the telemetry path for a given sensor.
 *
 * @param sensorId The sensor id.
 * @return The telemetry path for the given sensor.
 */
String WiFiFirebase::telemetrySensorPath(const String& sensorId) const {
    return telemetryDevicePath() + "/" + sensorId;
}

/**
 * Reads one sensor's whole telemetry node and pulls out what the periods need to
 * resume: every bucket index, and when the device last uploaded a reading.
 *
 * This is one request per sensor. Asking each period for its index and its current
 * bucket's timestamp separately meant up to twelve, and since a handshake dominates
 * the cost of a request on this board that bootstrap took longer than the first
 * minute and a half of uptime.
 *
 * The offline gap is measured from last_instant rather than from each period's
 * newest bucket. Firebase orders children lexicographically, so a period's buckets
 * arrive before its index and a single streaming pass cannot tell which bucket the
 * index points at - and keeping all of them is the memory problem getJsonFiltered
 * exists to avoid. last_instant is written every tick, so it answers "when was this
 * device last online" directly.
 *
 * @param sensorId The sensor id.
 * @param periodIds The period ids to read indices for.
 * @param periodCount How many period ids periodIds holds.
 * @param outIndices Filled with one index per period, -1 where a period has no buckets yet.
 * @param outLastOnline The epoch time of the last uploaded reading. Left untouched
 *   if the device has never uploaded one.
 * @return true if the node was read and parsed, false otherwise.
 */
bool WiFiFirebase::getSensorBootstrap(
    const String& sensorId,
    const String* periodIds,
    int periodCount,
    long* outIndices,
    unsigned long& outLastOnline
) {
    if (_appliedIdToken.length() == 0) {
        return false;
    }

    JsonDocument filter;
    filter["last_instant"]["timestamp"] = true;
    for (int i = 0; i < periodCount; i++) {
        filter[periodIds[i]]["index"] = true;
    }

    JsonDocument doc;
    if (!getJsonFiltered(telemetrySensorPath(sensorId), filter, doc)) {
        return false;
    }

    for (int i = 0; i < periodCount; i++) {
        outIndices[i] = doc[periodIds[i]]["index"] | (long) -1;
    }

    unsigned long lastOnline = doc["last_instant"]["timestamp"] | (unsigned long) 0;
    if (lastOnline != 0) {
        outLastOnline = lastOnline;
    }

    return true;
}

/**
 * Commits every closed bucket for one sensor in one request. Each period
 * contributes two locations - the bucket itself and the period's index cursor -
 * and all of them ride the same PATCH.
 *
 * A device coming back from a long outage marks a gap in all six periods at once,
 * which used to be twelve requests per sensor. The index is written alongside its
 * bucket rather than after it, so the two can no longer end up disagreeing because
 * the second write failed.
 *
 * @param sensorId The sensor id.
 * @param commits The bucket writes to commit.
 * @param count How many writes commits holds.
 * @return true if the update was accepted, false otherwise.
 *   The update is atomic, so a false here means no bucket was written.
 */
bool WiFiFirebase::commitBuckets(const String& sensorId, const BucketCommit* commits, int count) {
    if (count <= 0) {
        return true;
    }

    JsonDocument doc;
    for (int i = 0; i < count; i++) {
        String periodPath = String(commits[i].periodId);

        JsonObject bucket = doc[periodPath + "/buckets/" + bucketKey(commits[i].index)].to<JsonObject>();
        bucket["timestamp"] = commits[i].timestamp;
        bucket["value"] = serialized(String(commits[i].value, 7));

        doc[periodPath + "/index"] = commits[i].index;
    }
    String json;
    serializeJson(doc, json);

    return setJsonMulti(telemetrySensorPath(sensorId), json);
}

/**
 * Commits every sensor's latest reading in one request, rather than one request
 * per sensor. Each entry lands at <sensor>/last_instant exactly as a per-sensor
 * write would; only the transport changes.
 *
 * @param instants The readings to commit.
 * @param count How many readings instants holds.
 * @param timestamp The epoch time in seconds the readings were taken at.
 * @return true if the update was accepted, false otherwise.
 *   The update is atomic, so a false here means no sensor was written.
 */
bool WiFiFirebase::commitInstants(const SensorInstant* instants, int count, unsigned long timestamp) {
    if (count <= 0) {
        return true;
    }

    JsonDocument doc;
    for (int i = 0; i < count; i++) {
        JsonObject instant = doc[String(instants[i].sensorId) + "/last_instant"].to<JsonObject>();
        instant["timestamp"] = timestamp;
        instant["value"] = serialized(String(instants[i].value, 7));
    }
    String json;
    serializeJson(doc, json);

    return setJsonMulti(telemetryDevicePath(), json);
}
