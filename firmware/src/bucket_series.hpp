#ifndef BUCKET_SERIES_HPP
#define BUCKET_SERIES_HPP

#include <Arduino.h>


class BucketSeries {
public:
    static const int BUCKET_COUNT = 100;

    BucketSeries();

    void push(float value, unsigned long timestamp);

    // Builds: {"values":[...],"timestamp":<epoch>,"index":<int>}
    String toJsonObjectConst() const;

private:
    float values[BUCKET_COUNT];
    int filledCount;
    int writeIndex;
    unsigned long lastPushTimestamp;
};

#endif
