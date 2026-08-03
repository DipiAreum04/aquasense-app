#ifndef NTP_TIME_HPP
#define NTP_TIME_HPP

#include <WiFiUdp.h>

class NTPTime {
public:
    NTPTime();

    void begin();

    // Returns the current epoch time in seconds. Only hits the network every
    // SYNC_INTERVAL_MS; in between, the epoch is extrapolated from millis()
    // off the last successful sync. Returns 0 if no sync has ever succeeded.
    // Guaranteed to never decrease between calls, even if a resync finds the
    // local millis()-extrapolated clock was running fast: bucket accounting
    // does unsigned currentTime-vs-lastCommit math that would underflow into
    // a huge value on a backward step, so a correction is held flat instead
    // of stepping back, until real elapsed time catches back up to it.
    unsigned long getEpoch();

private:
    bool sync();

    WiFiUDP udp;
    const char* server = "pool.ntp.org";

    static const unsigned long SYNC_INTERVAL_MS = 1800000UL;  // 30 min, per pool.ntp.org's usage policy

    bool _hasSynced = false;
    unsigned long _baseEpoch = 0;
    unsigned long _baseMillis = 0;
    unsigned long _nextSyncMillis = 0;
    unsigned long _lastReportedEpoch = 0;
};

#endif
