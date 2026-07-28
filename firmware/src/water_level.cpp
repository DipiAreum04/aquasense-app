#include "water_level.hpp"

WaterLevel::WaterLevel(uint8_t pin) : _pin(pin) {}

void WaterLevel::begin() {
    pinMode(_pin, INPUT_PULLUP);
}

bool WaterLevel::isDetected() {
    return digitalRead(_pin) == LOW;
}
