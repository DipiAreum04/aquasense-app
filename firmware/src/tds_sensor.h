#pragma once
#include <Arduino.h>

class TdsSensor {
public:
    TdsSensor(uint8_t pin);
    float readTdsPpm();

private:
    uint8_t _pin;
    int getMedian(int* arr, int len);
};

