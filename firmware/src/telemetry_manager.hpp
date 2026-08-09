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
        bool linkUp,
        float temperature,
        float waterLevel,
        float dissolvedSolids,
        float phLevel
    );

private:
    static const int SENSOR_COUNT = 4;
    static const int MAX_PENDING_UPDATES = SENSOR_COUNT * Periods::MAX_PENDING_UPDATES;

    void collect(Periods** out);
    bool resumeSensors(Periods** sensors, unsigned long currentTime);
    bool allResumed() const;
    bool commit(
        Periods** sensors,
        const float* readings,
        unsigned long commitTime,
        bool online
    );

    WiFiFirebase& _firebase;

    bool _resumed[SENSOR_COUNT] = { false, false, false, false };

    BucketUpdate _updates[MAX_PENDING_UPDATES];

    Periods _periodsTemperature;
    Periods _periodsWaterLevel;
    Periods _periodsDissolvedSolids;
    Periods _periodsPhLevel;
};

#endif
