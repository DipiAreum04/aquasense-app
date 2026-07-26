#ifndef BUCKET_SERIES_H
#define BUCKET_SERIES_H

#include <Arduino.h>

class BucketSeries {
public:
    static const int BUCKET_COUNT = 100;

    BucketSeries();

    void push(float value, unsigned long timestamp);

    // Builds: {"values":[...],"timestamp":<epoch>,"index":<int>}
    String toJsonObject() const;

private:
    float values[BUCKET_COUNT];
    int filledCount;
    int writeIndex;
    unsigned long lastPushTimestamp;
};

#endif
