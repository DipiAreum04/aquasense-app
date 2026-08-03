#ifndef WIFI_FIREBASE_H
#define WIFI_FIREBASE_H

#include <WiFiS3.h>
#include <Firebase.h>
#include "firebase_auth.hpp"

class WiFiFirebase {
public:
    WiFiFirebase(const char* dbUrl, const char* apiKey, const char* email, const char* password);
    ~WiFiFirebase();

    WiFiFirebase(const WiFiFirebase&) = delete;
    WiFiFirebase& operator=(const WiFiFirebase&) = delete;

    // Signs in (or refreshes, once the token is close to expiry) and rebuilds the
    // underlying Firebase client against the new ID token. Must be called before
    // any of the methods below; returns false if sign-in/refresh fails.
    bool ensureFreshToken();

    bool getCurrentBucketInfo(
        const String& sensorId, const String& periodId, long& outIndex, unsigned long& outTimestamp
    );

    bool commitBucket(
        const String& sensorId, const String& periodId, int index, unsigned long timestamp, float value
    );

    bool commitInstant(const String& sensorId, unsigned long timestamp, float value);

private:
    const char* dbUrl;
    const char* _userId;
    String _deviceId;

    FirebaseAuth _auth;
    String _appliedIdToken;
    Firebase* fb = nullptr;

    static String bucketKey(int index);

    String telemetrySensorPath(const String& sensorId) const;
    String telemetrySensorInstantPath(const String& sensorId) const;
    String telemetrySensorPeriodPath(const String& sensorId, const String& periodId) const;
    String telemetrySensorPeriodIndexPath(const String& sensorId, const String& periodId) const;
    String telemetrySensorPeriodBucketPath(const String& sensorId, const String& periodId, int index) const;
    String telemetrySensorPeriodBucketTimestampPath(const String& sensorId, const String& periodId, int index) const;

    bool getJson(const String& path, String& out, int maxAttempts = 3);
    bool setJson(const String& path, const String& json, int maxAttempts = 3);
};

#endif
