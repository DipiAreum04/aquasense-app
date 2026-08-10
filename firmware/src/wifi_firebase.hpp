#ifndef WIFI_FIREBASE_H
#define WIFI_FIREBASE_H

#include <WiFiS3.h>
#include <ArduinoHttpClient.h>
#include <ArduinoJson.h>
#include "firebase_auth.hpp"

struct SensorInstant {
    const char* sensorId;
    float value;
};

struct BucketUpdate {
    const char* sensorId;
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

    void setOwnerUid(const String& ownerUid);

    void setExpectedDeviceUid(const String& deviceUid);

    bool ensureFreshToken();
    
    bool permissionRevoked() const;

    bool getSensorBootstrap(
        const String& sensorId,
        const String* periodIds,
        int periodCount,
        long* outIndices
    );

    bool commitTick(
        const SensorInstant* instants,
        int instantCount,
        const BucketUpdate* buckets,
        int bucketCount,
        unsigned long timestamp
    );

    static int pathCount(
        int instantCount,
        const BucketUpdate* buckets,
        int bucketCount
    );

private:
    String _dbHost;
    String _userId;
    String _deviceId;
    String _expectedDeviceId;

    bool _deviceIdChecked = false;

    WiFiSSLClient _tls;
    HttpClient _http;

    FirebaseAuth _auth;
    String _appliedIdToken;

    int _deniedStreak = 0;

    static String bucketKey(int index);
    static String hostFromUrl(const char* url);
    static bool endsPeriodGroup(const BucketUpdate* buckets, int index, int count);
    static void appendReading(
        String& json,
        bool& first,
        const String& path,
        unsigned long timestamp,
        float value
    );

    String telemetryDevicePath() const;
    String telemetrySensorPath(const String& sensorId) const;

    void finishResponse();
    void noteResponse(int statusOrError);
    bool getJsonFiltered(const String& path, const JsonDocument& filter, JsonDocument& out);
    bool setJsonMulti(const String& rootPath, const String& json);
    int patchJson(const String& rootPath, const String& json);
};

#endif
