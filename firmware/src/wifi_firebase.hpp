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

    /* The Firebase UID of the user who owns this tank, handed over BLE during
     * pairing and reloaded from EEPROM on every later boot.
     *
     * Telemetry lives under the owner's subtree, so this is half of every write path
     * this class builds. It used to be a hardcoded literal, which only ever worked for
     * one specific account. Must be set before any of the methods below; they refuse
     * to run without it rather than writing to a path with an empty first segment.
     */
    void setOwnerUid(const String& ownerUid);

    /* The UID this board is expected to authenticate as, from PairingData.
     *
     * Optional, and used only to catch a mismatch: the app claims the aquarium under
     * the UID it read over BLE, while writes here are authorised against whatever
     * Firebase actually signs this board in as. If those two diverge the security
     * rules reject every write and nothing reports why, so ensureFreshToken() compares
     * them once and complains on the Serial console.
     */
    void setExpectedDeviceUid(const String& deviceUid);

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
    String _userId;
    String _deviceId;
    String _expectedDeviceId;

    // Latches so a mismatch is reported once at sign-in rather than on every hourly
    // token refresh for the life of the board.
    bool _deviceIdChecked = false;

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
