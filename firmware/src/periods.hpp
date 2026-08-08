#ifndef PERIODS_HPP
#define PERIODS_HPP

#include <Arduino.h>
#include "buckets.hpp"
#include "wifi_firebase.hpp"

class Periods {
public:
    Periods(const String& kind);

    bool sync(WiFiFirebase& firebase, unsigned long currentTime);

    const String& kind() const { return _kind; }

    void accumulate(float value);

    bool commitBuckets(WiFiFirebase& firebase, unsigned long commitTime);

private:
    static const int PERIOD_COUNT = 6;

    void collect(Buckets** out);

    // Writes whichever periods report a commit due, all in one request. force
    // carries one flag per period, for the gap markers sync() owes on boot and
    // after an outage.
    bool commit(WiFiFirebase& firebase, unsigned long commitTime, const bool* force);

    String _kind;
    Buckets _buckets1h;
    Buckets _buckets1d;
    Buckets _buckets1w;
    Buckets _buckets1m;
    Buckets _buckets6m;
    Buckets _buckets1y;
};

#endif
