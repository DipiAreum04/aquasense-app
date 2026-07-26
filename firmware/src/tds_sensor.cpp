#include "tds_sensor.h"

#define VREF 5.0
#define ADC_RES 16383.0
#define SAMPLE_COUNT 30

TdsSensor::TdsSensor(uint8_t pin) : _pin(pin) {}

int TdsSensor::getMedian(int* arr, int len) {
    int temp[len];
    memcpy(temp, arr, len * sizeof(int));

    for (int j = 0; j < len - 1; j++) {
        for (int i = 0; i < len - j - 1; i++) {
            if (temp[i] > temp[i + 1]) {
                int swap = temp[i];
                temp[i] = temp[i + 1];
                temp[i + 1] = swap;
            }
        }
    }

    if (len & 1)
        return temp[len / 2];
    else
        return (temp[len / 2] + temp[len / 2 - 1]) / 2;
}

float TdsSensor::readTdsPpm() {
    int buffer[SAMPLE_COUNT];

    for (int i = 0; i < SAMPLE_COUNT; i++) {
        buffer[i] = analogRead(_pin);
        delay(10);
    }

    int median = getMedian(buffer, SAMPLE_COUNT);
    float voltage = median * (VREF / ADC_RES);

    float tdsValue =
        (133.42 * voltage * voltage * voltage) -
        (255.86 * voltage * voltage) +
        (857.39 * voltage);

    tdsValue *= 0.5;  // DFRobot scaling factor

    return tdsValue;
}
