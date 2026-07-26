#include "sensor_series.h"

SensorSeries::SensorSeries() : lastInstantValue(NAN), lastInstantTimestamp(0) {}

void SensorSeries::updateInstant(float value, unsigned long timestamp) {
    lastInstantValue = value;
    lastInstantTimestamp = timestamp;
}

String SensorSeries::getInstantJson() const {
    String out = "{";
    out += "\"timestamp\":" + String(lastInstantTimestamp) + ",";
    out += "\"value\":" + (isnan(lastInstantValue) ? String("null") : String(lastInstantValue));
    out += "}";
    return out;
}

void SensorSeries::pushToResolution(int resolutionIndex, float value, unsigned long timestamp) {
    buckets[resolutionIndex].push(value, timestamp);
}

const BucketSeries& SensorSeries::getBucketSeries(int resolutionIndex) const {
    return buckets[resolutionIndex];
}