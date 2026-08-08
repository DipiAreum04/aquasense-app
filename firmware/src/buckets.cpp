#include "buckets.hpp"
#include "consts.hpp"
#include "elapsed.hpp"

Buckets::Buckets(
    const String& kind, unsigned long totalSpanSecs
) : _bucketSize(totalSpanSecs / RESOLUTION),
    _numberOfBuckets(RESOLUTION),
    _kind(kind)
{}

/**
 * Resumes from where the database left off, writing into the slot after the last
 * one it recorded.
 *
 * The bucket clock is restarted only when this period is about to mark a gap. A sync
 * is no longer the once-per-boot event it was: any stranding of a bucket's length or
 * more sends every period back through here, and restarting all six clocks each time
 * would mean a period whose bucket outlasts the interval between outages never
 * reaches its boundary at all. On flaky WiFi that silently retires the long periods -
 * last_1d needs only one outage every 14 minutes to stop recording, last_1w one every
 * 1.7 hours.
 *
 * No separate first-resume case is needed to get the clock started: the first resume
 * after boot always marks a gap, so _lastCommit is always set here before any bucket
 * can close.
 *
 * The index needs no such guard: a period that did not commit did not move the index
 * either, so recomputing it from the database yields what it already held.
 *
 * @param currentTime The current epoch time in seconds.
 * @param storedIndex The last bucket index the database holds, or -1 if it holds none.
 * @param markingGap Whether this period is about to write a gap marker.
 */
void Buckets::resume(unsigned long currentTime, long storedIndex, bool markingGap) {
    _bucketIndex = (int) ((storedIndex + 1) % _numberOfBuckets);

    if (markingGap) {
        _lastCommit = currentTime;
    }
}

/**
 * Whether the device has been away long enough for this period to have missed a
 * bucket, and so owes one gap marker. One marker covers the whole outage however
 * long it ran: the bucket's timestamp is what says when it ended.
 *
 * @param currentTime The current epoch time in seconds.
 * @param lastOnline The epoch time the device last uploaded a reading.
 * @return true if a gap marker is owed.
 */
bool Buckets::gapElapsed(unsigned long currentTime, unsigned long lastOnline) const {
    return secondsSince(currentTime, lastOnline) >= _bucketSize;
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
 * Discards the samples gathered so far, so that the next commit reports
 * OFFLINE_VALUE rather than an average of readings taken before an outage.
 */
void Buckets::discardPending() {
    _valueCount = 0;
    _valueTotal = 0;
}

/**
 * Whether the current bucket has closed and is due to be written.
 *
 * @param currentTime The current epoch time in seconds.
 * @param force If true, reports due regardless of whether the bucket is full.
 * @return true if this period has a bucket to commit.
 */
bool Buckets::needsCommit(unsigned long currentTime, bool force) const {
    return force || secondsSince(currentTime, _lastCommit) >= _bucketSize;
}

/**
 * Describes the bucket this period would write, without writing it. A period that
 * took no readings reports OFFLINE_VALUE, which is what marks a gap.
 *
 * @param out The commit to fill in.
 * @param currentTime The current epoch time in seconds.
 */
void Buckets::describeCommit(BucketCommit& out, unsigned long currentTime) const {
    out.periodId = _kind.c_str();
    out.index = _bucketIndex;
    out.timestamp = currentTime;
    out.value = (_valueCount > 0) ? (_valueTotal / _valueCount) : OFFLINE_VALUE;
}

/**
 * Advances to the next bucket. Only call once the write has actually landed, so a
 * failed commit is retried into the same slot rather than skipping it.
 *
 * A landed write is also what discharges the once-per-boot gap marker. Clearing that
 * flag any earlier - when the marker is described, or when sync() decides it is owed -
 * would drop the marker altogether on a sync whose commit then failed, which is the
 * one case the retry exists for. The first write any period lands after boot is that
 * marker: TelemetryManager::tick accumulates and commits nothing until every sensor
 * has synced.
 *
 * @param currentTime The current epoch time in seconds.
 */
void Buckets::onCommitted(unsigned long currentTime) {
    _bucketIndex = (_bucketIndex + 1) % _numberOfBuckets;
    _lastCommit = currentTime;
    _valueCount = 0;
    _valueTotal = 0;
    _markedBoot = true;
}
