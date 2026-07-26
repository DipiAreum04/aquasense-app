#ifndef TELEMETRY_MANAGER_H
#define TELEMETRY_MANAGER_H

#include <Arduino.h>
#include "sensor_series.h"
#include "resolutions.h"
#include "wifi_firebase.h"

class TelemetryManager {
public:
    TelemetryManager(WiFiFirebase& firebase, const char* aquariumId);

    void tick(unsigned long epoch,
        float temperature,
        float waterLevel,
        float dissolvedSolids,
        float phLevel);

private:
    struct SensorSpec {
        const char* name;
        SensorSeries* series;
    };

    WiFiFirebase& _firebase;
    const char* _aquariumId;

    SensorSeries temperatureSeries;
    SensorSeries waterLevelSeries;
    SensorSeries dissolvedSolidsSeries;
    SensorSeries phLevelSeries;

    unsigned long elapsedSeconds;

    void uploadInstant(const char* sensorName, const String& instantJson);
    void uploadResolution(const char* sensorName, const Resolution& res, const BucketSeries& bucket);
};

#endif
