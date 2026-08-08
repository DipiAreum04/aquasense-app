#ifndef BUCKETS_HPP
#define BUCKETS_HPP

#include <Arduino.h>
#include "wifi_firebase.hpp"

class Buckets {
public:
    Buckets(const String& kind, unsigned long totalSpanSecs);

    const String& kind() const { return _kind; }

    // Picks up from the index the database last recorded. Makes no request; the
    // index is read once per sensor by Periods::sync. markingGap says whether this
    // period is about to write a gap marker, which is the only case where the bucket
    // clock is restarted.
    void resume(unsigned long currentTime, long storedIndex, bool markingGap);

    // Whether this period still owes the gap marker every period writes once per
    // boot, before it begins filling buckets of its own. Unconditional: it does not
    // consult the clock or what the database already holds.
    bool owesBootMarker() const { return !_markedBoot; }

    // Whether the device was away for at least a whole bucket, and so owes this
    // period a gap marker.
    bool gapElapsed(unsigned long currentTime, unsigned long lastOnline) const;

    void add(float value);

    // Throws away the samples collected so far. Used when the window this bucket
    // covers turns out to span an outage, so its average describes neither side.
    void discardPending();

    // A commit split into its three steps, so Periods can gather every period's
    // pending write into one request: decide, describe, then advance once the
    // write has actually landed.
    bool needsCommit(unsigned long currentTime, bool force) const;
    void describeCommit(BucketCommit& out, unsigned long currentTime) const;
    void onCommitted(unsigned long currentTime);

private:
    unsigned long _bucketSize;
    int _numberOfBuckets;
    unsigned long _lastCommit = 0;
    bool _markedBoot = false;
    String _kind;
    int _valueCount = 0;
    float _valueTotal = 0;
    int _bucketIndex = 0;
};

#endif
