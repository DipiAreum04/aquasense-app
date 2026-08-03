#ifndef WIFI_FIREBASE_H
#define WIFI_FIREBASE_H

#include <WiFiS3.h>
#include <ArduinoHttpClient.h>
#include <ArduinoJson.h>
#include "firebase_auth.hpp"

// One sensor's latest reading, as handed to commitInstants(). sensorId points at
// storage the caller owns (Periods::kind()), so it has to outlive the call.
struct SensorInstant {
    const char* sensorId;
    float value;
};

// One period's pending bucket write, as handed to commitBuckets(). periodId points
// at storage the caller owns (Buckets::kind()), so it has to outlive the call.
struct BucketCommit {
    const char* periodId;
    int index;
    unsigned long timestamp;
    float value;
};

class WiFiFirebase {
public:
    WiFiFirebase(const char* dbUrl, const char* apiKey, const char* email, const char* password);

    WiFiFirebase(const WiFiFirebase&) = delete;
    WiFiFirebase& operator=(const WiFiFirebase&) = delete;

    // Signs in (or refreshes, once the token is close to expiry) and holds on to
    // the new ID token. Must be called before any of the methods below; returns
    // false if sign-in/refresh fails.
    bool ensureFreshToken();

    // Reads everything one sensor needs to resume where it left off in a single
    // request: each period's bucket index, plus when the device was last online.
    //
    // outIndices is filled with one entry per periodIds entry, -1 where the period
    // has no buckets yet. outLastOnline is left untouched if the device has never
    // uploaded a reading. Both are only meaningful when this returns true.
    bool getSensorBootstrap(
        const String& sensorId,
        const String* periodIds,
        int periodCount,
        long* outIndices,
        unsigned long& outLastOnline
    );

    // Writes every period whose bucket has just closed in a single request, index
    // updates included. See setJsonMulti for why these are batched.
    bool commitBuckets(const String& sensorId, const BucketCommit* commits, int count);

    // Writes every sensor's last_instant in a single request. See setJsonMulti for
    // why these are batched rather than committed one sensor at a time.
    bool commitInstants(const SensorInstant* instants, int count, unsigned long timestamp);

private:
    String _dbHost;
    const char* _userId;
    String _deviceId;

    // One connection, held open across ticks so a TLS handshake is not paid per
    // request. _dbHost must stay declared above _http: HttpClient keeps the host as
    // a bare pointer, so it has to be built first and must outlive the client.
    WiFiSSLClient _tls;
    HttpClient _http;

    FirebaseAuth _auth;
    String _appliedIdToken;

    static String bucketKey(int index);
    static String hostFromUrl(const char* url);

    String telemetryDevicePath() const;
    String telemetrySensorPath(const String& sensorId) const;

    void finishResponse();
    bool getJsonFiltered(const String& path, const JsonDocument& filter, JsonDocument& out);
    bool setJsonMulti(const String& rootPath, const String& json);
    int patchJson(const String& rootPath, const String& json);
};

#endif
