#ifndef BUCKETS_HPP
#define BUCKETS_HPP

#include <Arduino.h>
#include "wifi_firebase.hpp"

class Buckets {
public:
    Buckets(const String& kind, unsigned long totalSpanSecs);

    bool sync(WiFiFirebase& firebase, const String& sensorKind, unsigned long currentTime);

    void add(float value);

    bool tryCommit(
        WiFiFirebase& firebase, const String& sensorKind, unsigned long currentTime, bool force = false
    );

private:
    unsigned long _bucketSize;
    int _numberOfBuckets;
    unsigned long _lastCommit = 0;
    String _kind;
    int _valueCount = 0;
    float _valueTotal = 0;
    int _bucketIndex = 0;
};

#endif
