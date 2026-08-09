#include "periods.hpp"

Periods::Periods(const String& kind)
    : _kind(kind),
      _buckets1h("last_1h", 3600),
      _buckets1d("last_1d", 86400),
      _buckets1w("last_1w", 604800),
      _buckets1m("last_1m", 2592000),
      _buckets6m("last_6m", 15768000),
      _buckets1y("last_1y", 31536000) {}

void Periods::collect(Buckets** out) {
    out[0] = &_buckets1h;
    out[1] = &_buckets1d;
    out[2] = &_buckets1w;
    out[3] = &_buckets1m;
    out[4] = &_buckets6m;
    out[5] = &_buckets1y;
}

bool Periods::resume(WiFiFirebase& firebase, unsigned long currentTime) {
    Buckets* periods[PERIOD_COUNT];
    collect(periods);

    String periodIds[PERIOD_COUNT];
    for (int i = 0; i < PERIOD_COUNT; i++) {
        periodIds[i] = periods[i]->kind();
    }

    long indices[PERIOD_COUNT];
    if (!firebase.getSensorBootstrap(_kind, periodIds, PERIOD_COUNT, indices)) {
        return false;
    }

    for (int i = 0; i < PERIOD_COUNT; i++) {
        periods[i]->resume(_kind, currentTime, indices[i]);
    }

    return true;
}

void Periods::account(float value) {
    Buckets* periods[PERIOD_COUNT];
    collect(periods);

    for (int i = 0; i < PERIOD_COUNT; i++) {
        periods[i]->add(value);
    }
}

int Periods::pendingUpdates(unsigned long currentTime, BucketUpdate* out, int capacity) {
    Buckets* periods[PERIOD_COUNT];
    collect(periods);

    int count = 0;
    for (int i = 0; i < PERIOD_COUNT; i++) {
        if (capacity - count < Buckets::MAX_PENDING_UPDATES) {
            break;
        }
        count += periods[i]->pendingUpdates(currentTime, out + count);
    }

    for (int i = 0; i < count; i++) {
        out[i].sensorId = _kind.c_str();
    }

    return count;
}

void Periods::commitPending(unsigned long currentTime) {
    Buckets* periods[PERIOD_COUNT];
    collect(periods);

    for (int i = 0; i < PERIOD_COUNT; i++) {
        periods[i]->commitPending(_kind, currentTime);
    }
}

void Periods::cachePending(unsigned long currentTime) {
    Buckets* periods[PERIOD_COUNT];
    collect(periods);

    for (int i = 0; i < PERIOD_COUNT; i++) {
        periods[i]->cachePending(currentTime);
    }
}
