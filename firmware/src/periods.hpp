#ifndef PERIODS_HPP
#define PERIODS_HPP

#include <Arduino.h>
#include "buckets.hpp"
#include "wifi_firebase.hpp"

class Periods {
public:
    Periods(const String& kind);

    bool sync(WiFiFirebase& firebase, unsigned long currentTime);

    bool account(WiFiFirebase& firebase, float value, unsigned long commitTime);

private:
    String _kind;
    Buckets _buckets1h;
    Buckets _buckets1d;
    Buckets _buckets1w;
    Buckets _buckets1m;
    Buckets _buckets6m;
    Buckets _buckets1y;
};

#endif
