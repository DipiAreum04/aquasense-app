#include "bucket_series.hpp"

BucketSeries::BucketSeries() : filledCount(0), writeIndex(0), lastPushTimestamp(0) {
    for (int i = 0; i < BUCKET_COUNT; i++) values[i] = NAN;
}

void BucketSeries::push(float value, unsigned long timestamp) {
    values[writeIndex] = value;
    lastPushTimestamp = timestamp;

    if (filledCount < BUCKET_COUNT) {
        filledCount++;
    }

    writeIndex = (writeIndex + 1) % BUCKET_COUNT;
}

String BucketSeries::toJsonObjectConst() const {
    String out = "{";

    out += "\"index\":" + String(writeIndex) + ",";
    out += "\"timestamp\":" + String(lastPushTimestamp) + ",";
    out += "\"values\":{";

    for (int i = 0; i < filledCount; i++) {
        out += "\"B" + String(i) + "\":";
        out += isnan(values[i]) ? "-1" : String(values[i]);

        if (i < filledCount - 1) {
            out += ",";
        }
    }

    out += "}"; // end values
    out += "}"; // end root object

    return out;
}
