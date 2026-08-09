#include "ph_sensor.hpp"

PhSensor::PhSensor(uint8_t pin, float neutralVoltage, float acidSlope, float tempCoefficient)
    : _pin(pin), _neutralVoltage(neutralVoltage), _acidSlope(acidSlope), _tempCoefficient(tempCoefficient) {}

float PhSensor::readPH(float tempC) {
    int raw = analogRead(_pin);
    float voltage = raw * (5.0 / 16383.0);

    if (isnan(tempC)) {
        float pH = 7 + (voltage - _neutralVoltage) * _acidSlope;
        return pH;
    }

    float compensatedSlope = _acidSlope * (1 + _tempCoefficient * (tempC - 27.0));
    float pH = 7 + (voltage - _neutralVoltage) * compensatedSlope;

    return pH;
}
