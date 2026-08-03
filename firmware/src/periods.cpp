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
 * Synchronizes the periods with the database, retrieving the current bucket index and timestamp for each period.
 *
 * @param firebase The WiFiFirebase instance to use for database operations.
 * @param currentTime The current epoch time in seconds.
 * @return true if the operation was successful, false otherwise if any operation fails all 3 attempts.
 */
bool Periods::sync(WiFiFirebase& firebase, unsigned long currentTime) {
    return _buckets1h.sync(firebase, _kind, currentTime)
        && _buckets1d.sync(firebase, _kind, currentTime)
        && _buckets1w.sync(firebase, _kind, currentTime)
        && _buckets1m.sync(firebase, _kind, currentTime)
        && _buckets6m.sync(firebase, _kind, currentTime)
        && _buckets1y.sync(firebase, _kind, currentTime);
}

/**
 * Accounts for a new sensor value, committing it to the database if necessary.
 *
 * @param firebase The WiFiFirebase instance to use for database operations.
 * @param value The new sensor value to account for.
 * @param commitTime The current epoch time in seconds.
 * @return true if the operation was successful, false otherwise if any operation fails all 3 attempts.
 */
bool Periods::account(WiFiFirebase& firebase, float value, unsigned long commitTime) {
    _buckets1h.add(value);
    _buckets1d.add(value);
    _buckets1w.add(value);
    _buckets1m.add(value);
    _buckets6m.add(value);
    _buckets1y.add(value);

    if (!firebase.commitInstant(_kind, commitTime, value)) {
        return false;
    }

    bool ok = _buckets1h.tryCommit(firebase, _kind, commitTime);
    ok = _buckets1d.tryCommit(firebase, _kind, commitTime) && ok;
    ok = _buckets1w.tryCommit(firebase, _kind, commitTime) && ok;
    ok = _buckets1m.tryCommit(firebase, _kind, commitTime) && ok;
    ok = _buckets6m.tryCommit(firebase, _kind, commitTime) && ok;
    ok = _buckets1y.tryCommit(firebase, _kind, commitTime) && ok;
    return ok;
}
