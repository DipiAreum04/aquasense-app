#include "buckets.hpp"
#include "consts.hpp"

Buckets::Buckets(
    const String& kind, unsigned long totalSpanSecs
) : _bucketSize(totalSpanSecs / RESOLUTION),
    _numberOfBuckets(RESOLUTION),
    _kind(kind)
{}

/**
 * Synchronizes the buckets with the database, retrieving the current bucket index and timestamp.
 *
 * @param firebase The WiFiFirebase instance to use for database operations.
 * @param sensorKind The kind of sensor (e.g., "temperature", "water_level").
 * @param currentTime The current epoch time in seconds.
 * @return true if the operation was successful, false otherwise if any operation fails all 3 attempts.
 */
bool Buckets::sync(WiFiFirebase& firebase, const String& sensorKind, unsigned long currentTime) {
    _lastCommit = currentTime;

    long currentIndex = -1;
    unsigned long lastOnline = currentTime;

    if (!firebase.getCurrentBucketInfo(sensorKind, _kind, currentIndex, lastOnline)) {
        return false;
    }

    _bucketIndex = (int) ((currentIndex + 1) % _numberOfBuckets);

    return tryCommit(firebase, sensorKind, currentTime, currentTime - lastOnline >= _bucketSize);
}

/**
 * Adds a new value to the current bucket, updating the total and count.
 *
 * @param value The new sensor value to add.
 */
void Buckets::add(float value) {
    if (value != OFFLINE_VALUE) {
        _valueTotal += value;
        _valueCount++;
    }
}

/**
 * Attempts to commit the current bucket to the database if the bucket is full or if forced.
 *
 * @param firebase The WiFiFirebase instance to use for committing.
 * @param sensorKind The kind of sensor (e.g., "temperature", "water_level").
 * @param currentTime The current epoch time in seconds.
 * @param force If true, forces a commit regardless of whether the bucket is full.
 * @return true if the commit was successful, false if any operations fails all 3 attempts.
 */
bool Buckets::tryCommit(
    WiFiFirebase& firebase, const String& sensorKind, unsigned long currentTime, bool force
) {
    if (!force && currentTime - _lastCommit < _bucketSize) {
        return true;
    }

    float value = (_valueCount > 0) ? (_valueTotal / _valueCount) : OFFLINE_VALUE;
    if (!firebase.commitBucket(sensorKind, _kind, _bucketIndex, currentTime, value)) {
        return false;
    }

    _bucketIndex = (_bucketIndex + 1) % _numberOfBuckets;
    _lastCommit = currentTime;
    _valueCount = 0;
    _valueTotal = 0;

    return true;
}
