#include "bucket_series.h"

BucketSeries::BucketSeries() : filledCount(0), writeIndex(0), lastPushTimestamp(0) {
    for (int i = 0; i < BUCKET_COUNT; i++) values[i] = NAN;
}

void BucketSeries::push(float value, unsigned long timestamp) {
    values[writeIndex] = value;
    writeIndex = (writeIndex + 1) % BUCKET_COUNT;
    if (filledCount < BUCKET_COUNT) filledCount++;
    lastPushTimestamp = timestamp;
}

String BucketSeries::toJsonObject() const {
    String valuesArray = "[";
    for (int i = 0; i < filledCount; i++) {
        valuesArray += isnan(values[i]) ? "null" : String(values[i]);
        if (i < filledCount - 1) valuesArray += ",";
    }
    valuesArray += "]";

    String out = "{";
    out += "\"values\":" + valuesArray + ",";
    out += "\"timestamp\":" + String(lastPushTimestamp) + ",";
    out += "\"index\":" + String(writeIndex);
    out += "}";
    return out;
}