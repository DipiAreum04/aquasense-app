#include "boot_counter.hpp"
#include <EEPROM.h>

uint8_t BootCounter::_count = 0;
bool BootCounter::_settled = false;

void BootCounter::begin() {
    uint8_t stored = 0;

    if (EEPROM.read(MARKER_ADDR) == MARKER) {
        stored = EEPROM.read(COUNT_ADDR);

        if (stored >= TRIGGER_COUNT) {
            stored = 0;
        }
    } else {
        EEPROM.write(MARKER_ADDR, MARKER);
    }

    _count = stored + 1;
    EEPROM.write(COUNT_ADDR, _count);
}

bool BootCounter::factoryResetRequested() {
    return _count >= TRIGGER_COUNT;
}

void BootCounter::clear() {
    _count = 0;
    EEPROM.write(COUNT_ADDR, 0);
}

void BootCounter::settle() {
    if (_settled || millis() < SETTLE_MS) {
        return;
    }

    _settled = true;
    if (_count != 0) {
        clear();
    }
}
