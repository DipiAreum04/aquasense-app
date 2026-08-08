#include "periods.hpp"

Periods::Periods(const String& kind)
    : _kind(kind),
      _buckets1h("last_1h", 3600),
      _buckets1d("last_1d", 86400),
      _buckets1w("last_1w", 604800),
      _buckets1m("last_1m", 2592000),
      _buckets6m("last_6m", 15768000),
      _buckets1y("last_1y", 31536000) {}

/**
 * Synchronizes the periods with the database. Every period's bucket index arrives
 * in one read, rather than each period round-tripping for its own, so the whole
 * sensor costs a single request before any gap-marking commits.
 *
 * The first sync after boot marks a gap in all six periods unconditionally, so a
 * freshly started board writes six markers per sensor before it fills anything.
 * Later syncs mark only the periods the outage actually cost a bucket.
 *
 * @param firebase The WiFiFirebase instance to use for database operations.
 * @param currentTime The current epoch time in seconds.
 * @return true if the operation was successful, false if the bootstrap read or a
 *   gap-marking write failed. The next tick retries.
 */
bool Periods::sync(WiFiFirebase& firebase, unsigned long currentTime) {
    Buckets* periods[PERIOD_COUNT];
    collect(periods);

    String periodIds[PERIOD_COUNT];
    for (int i = 0; i < PERIOD_COUNT; i++) {
        periodIds[i] = periods[i]->kind();
    }

    long indices[PERIOD_COUNT];
    unsigned long lastOnline = currentTime;
    if (!firebase.getSensorBootstrap(_kind, periodIds, PERIOD_COUNT, indices, lastOnline)) {
        return false;
    }

    bool force[PERIOD_COUNT];
    for (int i = 0; i < PERIOD_COUNT; i++) {
        /* Every period marks a gap on the first sync after boot, whatever the database
         * already holds for it and however brief the downtime was. The device cannot
         * vouch for the window its previous run left open, so the marker closes that
         * window off and the buckets that follow start from a known point rather than
         * averaging across the break.
         *
         * It is also the only thing that ever creates the long periods' nodes. A
         * last_1y bucket spans 3.65 days, so a period that waits for one to close on
         * its own needs an unbroken run of that length before it appears in the
         * database at all - and gapElapsed cannot bootstrap it either, since a device
         * that has never been away that long is not owed a gap by that measure.
         *
         * Decided before resume(), which needs to know: a period that is not marking
         * a gap must keep both its pending samples and its place in the current
         * bucket. Neither test reads anything resume() sets, so nothing here depends
         * on resume() having run.
         */
        force[i] = periods[i]->owesBootMarker() || periods[i]->gapElapsed(currentTime, lastOnline);

        periods[i]->resume(currentTime, indices[i], force[i]);

        /* Anything still pending was sampled before the outage. Averaging it into
         * the marker would report the gap as though readings had been taken across
         * it, so it goes. Only the periods actually marking a gap are cleared: a
         * short outage that last_1y rightly ignores must not cost last_1y the days
         * of samples it has been gathering.
         *
         * This is also what makes the boot marker read OFFLINE_VALUE by construction
         * rather than by luck. Nothing has been accumulated that early in the tick,
         * but describeCommit reports an average the moment a single sample exists,
         * and a marker carrying one is not a marker.
         */
        if (force[i]) {
            periods[i]->discardPending();
        }
    }

    return commit(firebase, currentTime, force);
}

/**
 * Private helper which gathers the periods into an array so they can be walked
 * rather than spelled out one by one.
 *
 * @param out An array of PERIOD_COUNT pointers to fill in.
 */
void Periods::collect(Buckets** out) {
    out[0] = &_buckets1h;
    out[1] = &_buckets1d;
    out[2] = &_buckets1w;
    out[3] = &_buckets1m;
    out[4] = &_buckets6m;
    out[5] = &_buckets1y;
}

/**
 * Private helper which writes every period that reports a commit due. The whole
 * sensor's worth goes up as one request, so a device marking a gap in all six
 * periods at once costs one round trip rather than twelve.
 *
 * @param firebase The WiFiFirebase instance to use for database operations.
 * @param commitTime The current epoch time in seconds.
 * @param force One flag per period, forcing that period's commit.
 * @return true if the operation was successful, false if a database write failed.
 *   Failures are left for the next tick to retry rather than retried in place.
 */
bool Periods::commit(WiFiFirebase& firebase, unsigned long commitTime, const bool* force) {
    Buckets* periods[PERIOD_COUNT];
    collect(periods);

    BucketCommit commits[PERIOD_COUNT];
    Buckets* pending[PERIOD_COUNT];
    int count = 0;

    for (int i = 0; i < PERIOD_COUNT; i++) {
        if (!periods[i]->needsCommit(commitTime, force[i])) {
            continue;
        }
        periods[i]->describeCommit(commits[count], commitTime);
        pending[count] = periods[i];
        count++;
    }

    if (count == 0) {
        return true;
    }

    if (!firebase.commitBuckets(_kind, commits, count)) {
        return false;
    }

    // Only advance once the write has landed, so a failure retries the same slot.
    for (int i = 0; i < count; i++) {
        pending[i]->onCommitted(commitTime);
    }

    return true;
}

/**
 * Accounts for a new sensor value in every period's running average. Touches no
 * database: the reading itself is uploaded by TelemetryManager, which batches all
 * four sensors' instants into one request.
 *
 * @param value The new sensor value to account for.
 */
void Periods::accumulate(float value) {
    _buckets1h.add(value);
    _buckets1d.add(value);
    _buckets1w.add(value);
    _buckets1m.add(value);
    _buckets6m.add(value);
    _buckets1y.add(value);
}

/**
 * Commits whichever periods have reached the end of their current bucket. Most
 * ticks this writes nothing, since the shortest bucket spans 36 seconds.
 *
 * @param firebase The WiFiFirebase instance to use for database operations.
 * @param commitTime The current epoch time in seconds.
 * @return true if the operation was successful, false if a database write failed.
 *   Failures are left for the next tick to retry rather than retried in place.
 */
bool Periods::commitBuckets(WiFiFirebase& firebase, unsigned long commitTime) {
    bool force[PERIOD_COUNT] = { false, false, false, false, false, false };
    return commit(firebase, commitTime, force);
}
