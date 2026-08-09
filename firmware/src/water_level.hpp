#pragma once
#include <Arduino.h>

class WaterLevel {
public:
    WaterLevel(uint8_t pin);
    void begin();
    bool isDetected();

private:
    uint8_t _pin;
};
