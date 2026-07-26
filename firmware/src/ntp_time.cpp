#include "ntp_time.h"

NTPTime::NTPTime() {}

void NTPTime::begin() {
    udp.begin(2390);
}

unsigned long NTPTime::getEpoch() {
    const int packetSize = 48;
    byte buffer[packetSize];

    memset(buffer, 0, packetSize);
    buffer[0] = 0b11100011;

    udp.beginPacket(server, 123);
    udp.write(buffer, packetSize);
    udp.endPacket();

    unsigned long start = millis();
    while (millis() - start < 2000) {
        int size = udp.parsePacket();
        if (size >= packetSize) {
            udp.read(buffer, packetSize);

            unsigned long secsSince1900 =
                (unsigned long)buffer[40] << 24 |
                (unsigned long)buffer[41] << 16 |
                (unsigned long)buffer[42] << 8 |
                (unsigned long)buffer[43];

            return secsSince1900 - 2208988800UL;
        }
    }

    return 0;
}
