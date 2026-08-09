#ifndef NTP_TIME_HPP
#define NTP_TIME_HPP

#include <WiFiUdp.h>

class NTPTime {
public:
    NTPTime();

    void begin();

    bool hasSynced() const { return _hasSynced; }

    unsigned long getEpoch();

private:
    bool sync();

    WiFiUDP udp;
    const char* server = "pool.ntp.org";

    static const unsigned long SYNC_INTERVAL_MS = 1800000UL;

    bool _hasSynced = false;
    unsigned long _baseEpoch = 0;
    unsigned long _baseMillis = 0;
    unsigned long _nextSyncMillis = 0;
    unsigned long _lastReportedEpoch = 0;
};

#endif
