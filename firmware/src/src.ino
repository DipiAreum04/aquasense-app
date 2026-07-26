#include <Arduino.h>
#include <WiFiS3.h>

#include "BLEWifiSetup.h"
#include "temp_sensor.h"
#include "ph_sensor.h"
#include "tds_sensor.h"
#include "water_level.h"
#include "wifi_firebase.h"
#include "ntp_time.h"
#include "pairing_data.h"
#include "telemetry_manager.h"

TempSensor tempSensor(4);
PhSensor   phSensor(A0, 2.535, -5.70, 0.03);
TdsSensor  tdsSensor(A1);
WaterLevel waterSensor(7);

BLEWifiSetup bleWifi;
bool provisioningMode = false;

PairingData pairingData;
WiFiFirebase cloud(pairingData.getDbUrl());
TelemetryManager telemetry(cloud, pairingData.getAquariumId());

NTPTime ntp;

unsigned long lastTickMillis = 0;
const unsigned long TICK_INTERVAL_MS = 1000;


void setup() {
    Serial.begin(115200);
    delay(500); //cant lower it more than this

    Serial.println("Starting MULTI-SENSOR + Epoch + Firebase telemetry (BLE WiFi provisioning)...");

    bleWifi.begin();

    if (bleWifi.tryConnectStored()) {
        Serial.println("WiFi already configured via BLE.");
        provisioningMode = false;
    } else {
        Serial.println("Waiting for BLE WiFi credentials (AquaSense_Setup in LightBlue)...");
        provisioningMode = true;
    }

    tempSensor.begin();
    waterSensor.begin();
    analogReadResolution(14);

    ntp.begin();

    if (!provisioningMode && WiFi.status() == WL_CONNECTED) {
        cloud.begin();
    }
}


void loop() {
    bleWifi.poll();

    if (provisioningMode) {
        delay(200);//cant lower it more than this
        return;
    }

    if (WiFi.status() != WL_CONNECTED) {
        Serial.println("WiFi lost. Trying reconnect via stored BLE credentials...");

        if (bleWifi.tryConnectStored()) {
            Serial.println("WiFi reconnected.");
            cloud.begin();
        } else {
            Serial.println("WiFi reconnect failed. Still waiting for valid credentials.");
            delay(1000); //need to give time to try snd reconnect to wifi
            return;
        }
    }

    unsigned long nowMillis = millis();
    if (nowMillis - lastTickMillis < TICK_INTERVAL_MS) {
        return;
    }
    lastTickMillis = nowMillis;

    unsigned long epoch = ntp.getEpoch();

    float tempC          = tempSensor.readTemperatureC();
    float phValue        = phSensor.readPH(tempC);
    float tdsPpm          = tdsSensor.readTdsPpm();
    bool  waterDetected   = waterSensor.isDetected();
    float waterLevelValue = waterDetected ? 1.0f : 0.0f;

    Serial.print("Epoch: ");
    Serial.print(epoch);
    Serial.print(" | Temp: ");
    Serial.print(tempC);
    Serial.print(" C");
    Serial.print(" | pH: ");
    Serial.print(phValue);
    Serial.print(" | TDS: ");
    Serial.print(tdsPpm);
    Serial.print(" | Water Level Anomaly: ");
    Serial.println(waterDetected ? "DETECTED" : "NONE");

    telemetry.tick(epoch, tempC, waterLevelValue, tdsPpm, phValue);
}