#ifndef BUCKETS_HPP
#define BUCKETS_HPP

#include <Arduino.h>
#include "consts.hpp"
#include "wifi_firebase.hpp"

class Buckets {
public:
    static const int MAX_PENDING_UPDATES = 3;

    Buckets(const String& kind, unsigned long totalSpanSecs);

    const String& kind() const { return _kind; }

    void resume(const String& sensorId, unsigned long currentTime, long storedIndex);

    void add(float value);

    int pendingUpdates(unsigned long currentTime, BucketUpdate* out);

    void commitPending(const String& sensorId, unsigned long currentTime);

    void cachePending(unsigned long currentTime);

private:
    enum PendingRequest {
        PENDING_NONE,
        PENDING_STARTUP_GAP,
        PENDING_FLUSH,
        PENDING_COMMIT
    };

    bool owesStartupGap() const { return !_startupCommitted; }

    bool atDeadline(unsigned long currentTime) const;
    float currentValue() const;

    void cacheCurrent(unsigned long currentTime);

    void startOver(unsigned long currentTime);
    int cacheEntries(unsigned long currentTime, BucketUpdate* out) const;
    int setPending(PendingRequest request, int count);

    unsigned long _bucketSize;
    unsigned long _lastCommit = 0;
    String _kind;

    int _valueCount = 0;
    float _valueTotal = 0;
    int _bucketIndex = 0;

    bool _startupCommitted = false;
    bool _cached = false;
    float _cachedValue = OFFLINE_VALUE;

    PendingRequest _pendingRequest = PENDING_NONE;
    int _pendingCount = 0;
    bool _pendingDeadline = false;
};

#endif
