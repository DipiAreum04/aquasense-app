#ifndef RESOLUTIONS_HPP
#define RESOLUTIONS_HPP

#include <Arduino.h>

struct Resolution {
    const char* name;
    unsigned long periodSeconds;

    unsigned long bucketIntervalSeconds() const {
        return periodSeconds / 100UL;
    }
};
//hi
static const Resolution RESOLUTIONS[] = {
    { "last_1h", 3600UL },
    { "last_1d", 86400UL },
    { "last_1w", 604800UL },
    { "last_1m", 2592000UL },
    { "last_6m", 15768000UL },
    { "last_1y", 31536000UL },
};
static const int RESOLUTION_COUNT = sizeof(RESOLUTIONS) / sizeof(RESOLUTIONS[0]);

#endif
