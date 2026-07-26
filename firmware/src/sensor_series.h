#ifndef SENSOR_SERIES_H
#define SENSOR_SERIES_H

#include <Arduino.h>
#include "bucket_series.h"
#include "resolutions.h"

class SensorSeries {
public:
    SensorSeries();

    void updateInstant(float value, unsigned long timestamp);

    // Builds: {"timestamp":<epoch>,"value":<float>}
    String getInstantJson() const;

    void pushToResolution(int resolutionIndex, float value, unsigned long timestamp);
    const BucketSeries& getBucketSeries(int resolutionIndex) const;

private:
    float lastInstantValue;
    unsigned long lastInstantTimestamp;
    BucketSeries buckets[RESOLUTION_COUNT];
};

#endif
