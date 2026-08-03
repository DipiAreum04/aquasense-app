#include "ntp_time.hpp"

NTPTime::NTPTime() {}

void NTPTime::begin() {
    udp.begin(2390);
}

unsigned long NTPTime::getEpoch() {
    unsigned long now = millis();

    if (!_hasSynced || (long) (now - _nextSyncMillis) >= 0) {
        sync();
        now = millis();
    }

    if (!_hasSynced) {
        return 0;
    }

    unsigned long computed = _baseEpoch + (now - _baseMillis) / 1000UL;
    if (computed < _lastReportedEpoch) {
        computed = _lastReportedEpoch;
    }
    _lastReportedEpoch = computed;
    return computed;
}

/**
 * Blocking NTP round trip. On success, updates the millis()-anchored baseline
 * that getEpoch() extrapolates from between syncs. On failure, the previous
 * baseline (if any) is left in place rather than being discarded, and the next
 * attempt is still pushed out by SYNC_INTERVAL_MS so a dropped reply doesn't
 * turn into a retry-every-tick hammering of the pool.
 */
bool NTPTime::sync() {
    const int packetSize = 48;
    byte buffer[packetSize];

    memset(buffer, 0, packetSize);
    buffer[0] = 0b11100011;

    udp.beginPacket(server, 123);
    udp.write(buffer, packetSize);
    udp.endPacket();

    bool synced = false;
    unsigned long start = millis();
    while (millis() - start < 2000) {
        int size = udp.parsePacket();
        if (size >= packetSize) {
            udp.read(buffer, packetSize);

            unsigned long secsSince1900 =
                (unsigned long) buffer[40] << 24 |
                (unsigned long) buffer[41] << 16 |
                (unsigned long) buffer[42] << 8 |
                (unsigned long) buffer[43];

            _baseEpoch = secsSince1900 - 2208988800UL;
            _baseMillis = millis();
            _hasSynced = true;
            synced = true;
            break;
        }
    }

    _nextSyncMillis = millis() + SYNC_INTERVAL_MS;
    return synced;
}
