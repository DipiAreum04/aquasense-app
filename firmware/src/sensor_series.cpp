#include "sensor_series.hpp"

SensorSeries::SensorSeries(SensorType t)
    : type(t), lastInstantValue(NAN), lastInstantTimestamp(0) {}

float SensorSeries::sanitizeValue(float value, SensorType type) const {
    switch (type) {

        case SENSOR_TEMPERATURE:
            if (value == -127 || value == 85 || isnan(value)) return -1000;
            return value;

        case SENSOR_WATER_LEVEL:
            // 0 means "no water detected", which is a normal reading, not an error.
            // Only a negative or NaN value counts as a broken sensor.
            if (value < 0 || isnan(value)) return -1;
            return value;

        case SENSOR_TDS:
            if (value <= 0 || isnan(value)) return -1;
            return value;

        case SENSOR_PH:
            if (value <= 0 || value > 14 || isnan(value)) return -1;
            return value;
    }
    return -1; 
}

void SensorSeries::updateInstant(float value, unsigned long timestamp) {
    lastInstantValue = sanitizeValue(value, type);
    lastInstantTimestamp = timestamp;
}



String SensorSeries::getInstantJson() const {
    String out = "{";
    out += "\"timestamp\":" + String(lastInstantTimestamp) + ",";
    out += "\"value\":" + String(lastInstantValue);
    out += "}";
    return out;
}

void SensorSeries::pushToResolution(int resolutionIndex, float value, unsigned long timestamp) {
    buckets[resolutionIndex].push(value, timestamp);
}

const BucketSeries& SensorSeries::getBucketSeries(int resolutionIndex) const {
    return buckets[resolutionIndex];
}