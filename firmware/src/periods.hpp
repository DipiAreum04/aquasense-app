#ifndef PERIODS_HPP
#define PERIODS_HPP

#include <Arduino.h>
#include "buckets.hpp"
#include "wifi_firebase.hpp"

class Periods {
public:
    static const int PERIOD_COUNT = 6;
    static const int MAX_PENDING_UPDATES = PERIOD_COUNT * Buckets::MAX_PENDING_UPDATES;

    Periods(const String& kind);

    const String& kind() const { return _kind; }

    bool resume(WiFiFirebase& firebase, unsigned long currentTime);

    void account(float value);

    int pendingUpdates(unsigned long currentTime, BucketUpdate* out, int capacity);
    void commitPending(unsigned long currentTime);
    void cachePending(unsigned long currentTime);

private:
    void collect(Buckets** out);

    String _kind;
    Buckets _buckets1h;
    Buckets _buckets1d;
    Buckets _buckets1w;
    Buckets _buckets1m;
    Buckets _buckets6m;
    Buckets _buckets1y;
};

#endif
