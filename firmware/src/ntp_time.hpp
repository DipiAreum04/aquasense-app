#ifndef NTP_TIME_HPP
#define NTP_TIME_HPP

#include <WiFiUdp.h>

class NTPTime {
public:
    NTPTime();

    void begin();
    unsigned long getEpoch();

private:
    WiFiUDP udp;
    const char* server = "pool.ntp.org";
};

#endif
