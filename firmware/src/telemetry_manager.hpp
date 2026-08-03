#ifndef TELEMETRY_MANAGER_HPP
#define TELEMETRY_MANAGER_HPP

#include <Arduino.h>
#include "periods.hpp"
#include "wifi_firebase.hpp"

class TelemetryManager {
public:
    TelemetryManager(WiFiFirebase& firebase);

    bool tick(
        unsigned long epoch,
        float temperature,
        float waterLevel,
        float dissolvedSolids,
        float phLevel
    );

private:
    WiFiFirebase& _firebase;
    bool _synced = false;

    Periods _periodsTemperature;
    Periods _periodsWaterLevel;
    Periods _periodsDissolvedSolids;
    Periods _periodsPhLevel;
};

#endif
