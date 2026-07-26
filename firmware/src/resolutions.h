#ifndef RESOLUTIONS_H
#define RESOLUTIONS_H

#include <Arduino.h>

struct Resolution {
    const char* name;
    unsigned long periodSeconds;

    unsigned long bucketIntervalSeconds() const {
        return periodSeconds / 100UL;
    }
};

static const Resolution RESOLUTIONS[] = {
    { "last_hour", 3600UL },
    { "last_day",  86400UL },
    { "last_week", 604800UL },
    { "last_month", 2592000UL },
    { "last_6_months", 15768000UL },
    { "last_year",  31536000UL },
};

static const int RESOLUTION_COUNT = sizeof(RESOLUTIONS) / sizeof(RESOLUTIONS[0]);

#endif
