#include "temp_sensor.hpp"

TempSensor::TempSensor(uint8_t pin) : _pin(pin) {
    _oneWire = new OneWire(_pin);
    _sensors = new DallasTemperature(_oneWire);
}

void TempSensor::begin() {
    _sensors->begin();
}

float TempSensor::readTemperatureC() {
    _sensors->requestTemperatures();
    float t = _sensors->getTempCByIndex(0);

    if (t == DEVICE_DISCONNECTED_C) {
        return NAN;
    }

    return t;
}
