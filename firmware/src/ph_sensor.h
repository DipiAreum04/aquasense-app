#pragma once
#include <Arduino.h>

class PhSensor {
public:
    PhSensor(uint8_t pin, float neutralVoltage, float acidSlope, float tempCoefficient);
    float readPH(float tempC);

private:
    uint8_t _pin;
    float _neutralVoltage;
    float _acidSlope;
    float _tempCoefficient;
};

