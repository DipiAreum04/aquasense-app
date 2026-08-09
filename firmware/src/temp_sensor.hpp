#pragma once
#include <OneWire.h>
#include <DallasTemperature.h>

class TempSensor {
public:
    TempSensor(uint8_t pin);
    void begin();
    float readTemperatureC();

private:
    uint8_t _pin;
    OneWire* _oneWire;
    DallasTemperature* _sensors;
};
