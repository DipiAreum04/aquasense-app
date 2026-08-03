#ifndef SENSOR_SANITIZER_HPP
#define SENSOR_SANITIZER_HPP

#include <Arduino.h>

enum SensorType {
    SENSOR_TEMPERATURE,
    SENSOR_WATER_LEVEL,
    SENSOR_TDS,
    SENSOR_PH
};

float sanitizeSensorValue(SensorType type, float value);

#endif
