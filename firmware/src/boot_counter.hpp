#ifndef BOOT_COUNTER_HPP
#define BOOT_COUNTER_HPP

#include <Arduino.h>
#include "pairing_store.hpp"

class BootCounter {
public:
    static constexpr uint8_t TRIGGER_COUNT = 10;

    static constexpr unsigned long SETTLE_MS = 10000;

    static void begin();

    static bool factoryResetRequested();

    static void clear();

    static void settle();

private:
    static constexpr uint8_t MARKER = 0x42;

    static constexpr int MARKER_ADDR = PairingStore::RECORD_SIZE;
    static constexpr int COUNT_ADDR = PairingStore::RECORD_SIZE + 1;

    static uint8_t _count;
    static bool _settled;
};

#endif
