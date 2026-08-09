#include "wifi_firebase.hpp"
#include <ArduinoHttpClient.h>
#include <ArduinoJson.h>
#include "consts.hpp"

#define HTTP_CODE_OK 200

namespace {
    const uint16_t HTTPS_PORT = 443;
}

WiFiFirebase::WiFiFirebase(const char* dbUrl, const char* apiKey, const char* email, const char* password)
    : _dbHost(hostFromUrl(dbUrl)),
      _http(_tls, _dbHost.c_str(), HTTPS_PORT),
      _auth(apiKey, email, password) {
    _http.connectionKeepAlive();

    _http.setTimeout(HTTP_TIMEOUT_MS);
}

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

    if (!_http.endOfBodyReached()) {
        _http.stop();
    }
}

bool WiFiFirebase::ensureFreshToken() {
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

bool WiFiFirebase::getJsonFiltered(const String& path, const JsonDocument& filter, JsonDocument& out) {
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
        _http.stop();
        if (result != HTTP_CODE_OK) {
            Serial.println("HTTP "+String(result)+": Failed to read JSON from "+path);
        }
    }
    return ok;
}

int WiFiFirebase::patchJson(const String& rootPath, const String& json) {
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
        _http.skipResponseHeaders();
        finishResponse();
    } else {
        _http.stop();
    }

    return result;
}

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

String WiFiFirebase::bucketKey(int index) {
    String padded = String(index);
    int width = String(RESOLUTION - 1).length();
    while (padded.length() < width) {
        padded = "0" + padded;
    }
    return "B" + padded;
}

String WiFiFirebase::telemetryDevicePath() const {
    return _userId + "/telemetry/" + _deviceId;
}

String WiFiFirebase::telemetrySensorPath(const String& sensorId) const {
    return telemetryDevicePath() + "/" + sensorId;
}

bool WiFiFirebase::getSensorBootstrap(
    const String& sensorId,
    const String* periodIds,
    int periodCount,
    long* outIndices
) {
    if (_appliedIdToken.length() == 0) {
        return false;
    }

    JsonDocument filter;
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

    return true;
}

void WiFiFirebase::appendReading(
    String& json,
    bool& first,
    const String& path,
    unsigned long timestamp,
    float value
) {
    if (!first) {
        json += ',';
    }
    first = false;

    json += '"';
    json += path;
    json += "\":{\"timestamp\":";
    json += timestamp;
    json += ",\"value\":";
    json += String(value, 7);
    json += '}';
}

bool WiFiFirebase::endsPeriodGroup(const BucketUpdate* buckets, int index, int count) {
    return (index + 1 == count)
        || strcmp(buckets[index + 1].sensorId, buckets[index].sensorId) != 0
        || strcmp(buckets[index + 1].periodId, buckets[index].periodId) != 0;
}

int WiFiFirebase::pathCount(
    int instantCount,
    const BucketUpdate* buckets,
    int bucketCount
) {
    int paths = instantCount + bucketCount;
    for (int i = 0; i < bucketCount; i++) {
        if (endsPeriodGroup(buckets, i, bucketCount)) {
            paths++;
        }
    }

    return paths;
}

bool WiFiFirebase::commitTick(
    const SensorInstant* instants,
    int instantCount,
    const BucketUpdate* buckets,
    int bucketCount,
    unsigned long timestamp
) {
    if (instantCount <= 0 && bucketCount <= 0) {
        return true;
    }

    String json;
    json.reserve(96 * instantCount + 144 * bucketCount + 2);
    json = "{";
    bool first = true;

    for (int i = 0; i < instantCount; i++) {
        appendReading(
            json,
            first,
            String(instants[i].sensorId) + "/last_instant",
            timestamp,
            instants[i].value
        );
    }

    for (int i = 0; i < bucketCount; i++) {
        String prefix = String(buckets[i].sensorId) + "/" + buckets[i].periodId;

        appendReading(
            json,
            first,
            prefix + "/buckets/" + bucketKey(buckets[i].index),
            buckets[i].timestamp,
            buckets[i].value
        );

        if (endsPeriodGroup(buckets, i, bucketCount)) {
            if (!first) {
                json += ',';
            }
            json += '"';
            json += prefix;
            json += "/index\":";
            json += buckets[i].index;
        }
    }

    json += '}';

    return setJsonMulti(telemetryDevicePath(), json);
}
