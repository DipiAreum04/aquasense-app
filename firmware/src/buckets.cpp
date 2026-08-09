#include "buckets.hpp"
#include "consts.hpp"
#include "elapsed.hpp"

Buckets::Buckets(
    const String& kind, unsigned long totalSpanSecs
) : _bucketSize(totalSpanSecs / RESOLUTION),
    _kind(kind)
{}

void Buckets::resume(const String& sensorId, unsigned long currentTime, long storedIndex) {
    _bucketIndex = (int) ((storedIndex + 1) % RESOLUTION);
    _lastCommit = currentTime;
    _valueCount = 0;
    _valueTotal = 0;
    _startupCommitted = false;
    _cached = false;
    _cachedValue = OFFLINE_VALUE;
    _pendingRequest = PENDING_NONE;
    _pendingCount = 0;
    _pendingDeadline = false;

    Serial.println(
        "BUCKET["+sensorId+"::"+_kind+"] of size "+String(_bucketSize)+
        " seconds resuming from index "+String(_bucketIndex)
    );
}

void Buckets::add(float value) {
    if (value != OFFLINE_VALUE) {
        _valueTotal += value;
        _valueCount++;
    }
}

bool Buckets::atDeadline(unsigned long currentTime) const {
    return secondsSince(currentTime, _lastCommit) >= _bucketSize;
}

float Buckets::currentValue() const {
    return (_valueCount > 0) ? (_valueTotal / _valueCount) : OFFLINE_VALUE;
}

int Buckets::pendingUpdates(unsigned long currentTime, BucketUpdate* out) {
    _pendingRequest = PENDING_NONE;
    _pendingCount = 0;
    _pendingDeadline = atDeadline(currentTime);

    if (owesStartupGap()) {
        _valueCount = 0;
        _valueTotal = 0;

        out[0].periodId = _kind.c_str();
        out[0].index = _bucketIndex;
        out[0].timestamp = currentTime;
        out[0].value = OFFLINE_VALUE;

        return setPending(PENDING_STARTUP_GAP, 1);
    }

    if (_cached) {
        return setPending(PENDING_FLUSH, cacheEntries(currentTime, out));
    }

    if (_pendingDeadline) {
        out[0].periodId = _kind.c_str();
        out[0].index = _bucketIndex;
        out[0].timestamp = currentTime;
        out[0].value = currentValue();

        return setPending(PENDING_COMMIT, 1);
    }

    return 0;
}

void Buckets::commitPending(const String& sensorId, unsigned long currentTime) {
    if (_pendingRequest == PENDING_NONE) {
        return;
    }

    for (int i = 0; i < _pendingCount; i++) {
        Serial.println(
            "BUCKET["+sensorId+"::"+_kind+"]["+String((_bucketIndex + i) % RESOLUTION)+
            "] Committed at "+String(currentTime)
        );
    }

    _bucketIndex = (_bucketIndex + _pendingCount) % RESOLUTION;

    if (_pendingRequest == PENDING_FLUSH) {
        _cached = false;
        _cachedValue = OFFLINE_VALUE;

        if (_pendingDeadline) {
            startOver(currentTime);
        }
    } else {
        if (_pendingRequest == PENDING_STARTUP_GAP) {
            _startupCommitted = true;
        }
        startOver(currentTime);
    }

    _pendingRequest = PENDING_NONE;
    _pendingCount = 0;
}

void Buckets::cachePending(unsigned long currentTime) {
    if (
        _pendingRequest != PENDING_NONE
        && _pendingRequest != PENDING_STARTUP_GAP
        && _pendingDeadline
    ) {
        cacheCurrent(currentTime);
    }

    _pendingRequest = PENDING_NONE;
    _pendingCount = 0;
}

void Buckets::cacheCurrent(unsigned long currentTime) {
    float value = currentValue();

    if (!_cached || _cachedValue == OFFLINE_VALUE) {
        _cachedValue = value;
    } else if (value != OFFLINE_VALUE) {
        _cachedValue = (_cachedValue + value) / 2;
    }

    _cached = true;
    startOver(currentTime);
}

void Buckets::startOver(unsigned long currentTime) {
    _lastCommit = currentTime;
    _valueCount = 0;
    _valueTotal = 0;
}

int Buckets::cacheEntries(unsigned long currentTime, BucketUpdate* out) const {
    float values[MAX_PENDING_UPDATES];
    int count = 0;

    values[count++] = OFFLINE_VALUE;
    values[count++] = _cachedValue;
    if (_pendingDeadline) {
        values[count++] = currentValue();
    }

    for (int offset = 0; offset < count; offset++) {
        unsigned long age = (unsigned long) (count - 1 - offset);

        out[offset].periodId = _kind.c_str();
        out[offset].index = (_bucketIndex + offset) % RESOLUTION;
        out[offset].timestamp = (currentTime > age) ? (currentTime - age) : 0;
        out[offset].value = values[offset];
    }

    return count;
}

int Buckets::setPending(PendingRequest request, int count) {
    _pendingRequest = request;
    _pendingCount = count;

    return count;
}
