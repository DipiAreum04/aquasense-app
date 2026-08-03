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
    static const int SENSOR_COUNT = 4;

    void collect(Periods** out);
    void markUnsynced();

    WiFiFirebase& _firebase;

    // Epoch of the last reading that actually reached the database, or 0 if none
    // has since the last outage was noticed. Doubles as the latch that stops that
    // outage being noticed twice - see tick().
    unsigned long _lastUploadEpoch = 0;

    // Per sensor, not one shared flag. A sensor that has already bootstrapped must
    // never do so a second time - see tick() for what that costs.
    bool _synced[SENSOR_COUNT] = { false, false, false, false };

    Periods _periodsTemperature;
    Periods _periodsWaterLevel;
    Periods _periodsDissolvedSolids;
    Periods _periodsPhLevel;
};

#endif
