#include "sensor_sanitizer.hpp"
#include "consts.hpp"

float sanitizeSensorValue(SensorType type, float value) {
    switch (type) {
        case SENSOR_TEMPERATURE:
            if (value == -127 || value == 85 || isnan(value)) return OFFLINE_VALUE;
            return value;

        case SENSOR_WATER_LEVEL:
            if (value < 0 || isnan(value)) return OFFLINE_VALUE;
            return value;

        case SENSOR_TDS:
            if (value < 0 || isnan(value)) return OFFLINE_VALUE;
            return value;

        case SENSOR_PH:
            if (value <= 0 || value > 14 || isnan(value)) return OFFLINE_VALUE;
            return value;
    }
    return OFFLINE_VALUE;
}
