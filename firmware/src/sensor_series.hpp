#ifndef SENSOR_SERIES_HPP
#define SENSOR_SERIES_HPP

#include <Arduino.h>
#include "bucket_series.hpp"
#include "resolutions.hpp"

enum SensorType {
    SENSOR_TEMPERATURE,
    SENSOR_WATER_LEVEL,
    SENSOR_TDS,
    SENSOR_PH
};

class SensorSeries {
public:
    SensorSeries(SensorType type);

    void updateInstant(float value, unsigned long timestamp);

    // Builds: {"timestamp":<epoch>,"value":<float>}
    String getInstantJson() const;

    void pushToResolution(int resolutionIndex, float value, unsigned long timestamp);
    const BucketSeries& getBucketSeries(int resolutionIndex) const;

private:

    float sanitizeValue(float value, SensorType type) const;
    SensorType type;
    float lastInstantValue;
    unsigned long lastInstantTimestamp;
    BucketSeries buckets[RESOLUTION_COUNT];
};

#endif
