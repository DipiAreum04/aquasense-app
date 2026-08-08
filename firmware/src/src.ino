#include <Arduino.h>
#include <WiFiS3.h>
#include <EEPROM.h>

#include "BLEWifiSetup.hpp"
#include "temp_sensor.hpp"
#include "ph_sensor.hpp"
#include "tds_sensor.hpp"
#include "water_level.hpp"
#include "wifi_firebase.hpp"
#include "ntp_time.hpp"
#include "pairing_data.hpp"
#include "pairing_store.hpp"
#include "telemetry_manager.hpp"

/* How long to wait for the router before calling an attempt failed. Generous, because
 * the cost of being wrong is wiping credentials that were fine.
 */
static const unsigned long WIFI_CONNECT_TIMEOUT_MS = 15000;

/* Consecutive failed reconnects before the board gives up on its stored credentials
 * and returns to pairing.
 *
 * Not 1. A router rebooting, or a hub carried out of range, must not cost the user
 * their pairing - but a network that has genuinely gone away for good has to be
 * escapable without a laptop and a USB cable.
 */
static const int MAX_WIFI_FAILURES = 5;

static const unsigned long TICK_INTERVAL_MS = 1000;
static const unsigned long RECONNECT_INTERVAL_MS = 5000;

/**
 * Joins a network and waits for it to come up.
 *
 * Deliberately does not service BLE while it waits, unlike the BLE-01 version of this
 * loop. By the time this runs the radio has already been handed to Wi-Fi, and there is
 * no BLE stack left to poll.
 */
static bool connectWifi(const String& ssid, const String& password) {
    Serial.print("Joining Wi-Fi network: ");
    Serial.println(ssid);

    WiFi.begin(ssid.c_str(), password.c_str());

    unsigned long start = millis();
    while (millis() - start < WIFI_CONNECT_TIMEOUT_MS) {
        if (WiFi.status() == WL_CONNECTED) {
            Serial.print("Connected. IP: ");
            Serial.println(WiFi.localIP());
            return true;
        }
        delay(200);
    }

    Serial.println("Wi-Fi connection failed.");
    return false;
}

/**
 * Wipes the stored pairing and restarts into advertising.
 *
 * A reset rather than bringing BLE back up in place: the ESP32-S3 has just been driven
 * as a Wi-Fi radio, and restarting is a far more reliable way to get a clean BLE stack
 * than tearing down and re-initialising one live. Nothing is lost by it - the app is
 * not holding a connection at this point, it is waiting on Firebase - which is exactly
 * why the same trick must NOT be used on commit, where it is.
 */
static void returnToPairing(const char* reason) {
    Serial.println(reason);
    Serial.println("Clearing the stored pairing and restarting to advertise.");
    PairingStore::clear();
    delay(200);
    NVIC_SystemReset();
}

void run() {
    TempSensor tempSensor(4);
    PhSensor   phSensor(A0, 1.82, 7.33, 0.001);
    TdsSensor  tdsSensor(A1);
    WaterLevel waterSensor(7);

    PairingData pairingData;
    WiFiFirebase cloud(
        pairingData.getDbUrl(),
        pairingData.getWebApiKey(),
        pairingData.getDeviceEmail(),
        pairingData.getDevicePassword()
    );
    TelemetryManager telemetry(cloud);

    BLEWifiSetup bleWifi;
    NTPTime ntp;

    Serial.begin(115200);
    delay(500); // cant lower it more than this
    EEPROM.begin();

    Serial.println("AquaSense hub starting...");

    cloud.setExpectedDeviceUid(String(pairingData.getDeviceUid()));

    /* The board is in exactly one of two modes and never both, because BLE and Wi-Fi
     * share one antenna on the R4 and cannot run at the same time.
     *
     *   online == false : advertising, waiting for the app to hand over credentials
     *   online == true  : on Wi-Fi, publishing telemetry
     */
    bool online = false;
    String ssid;
    String wifiPassword;
    String ownerUid;

    if (PairingStore::load(ssid, wifiPassword, ownerUid)) {
        // Already paired: skip BLE entirely, it is never started this boot.
        Serial.println("Found a stored pairing.");
        if (connectWifi(ssid, wifiPassword)) {
            cloud.setOwnerUid(ownerUid);
            online = true;
        } else {
            returnToPairing("Stored Wi-Fi credentials did not work.");
        }
    } else {
        Serial.println("No usable pairing stored.");
        if (!bleWifi.begin(pairingData.getDeviceUid())) {
            Serial.println("Cannot advertise, so this board cannot be paired. Restarting.");
            delay(2000);
            NVIC_SystemReset();
        }
    }

    tempSensor.begin();
    waterSensor.begin();
    analogReadResolution(14);

    if (online) {
        ntp.begin();
    }

    unsigned long lastTickMillis = millis();
    unsigned long lastReconnectMillis = 0;
    int wifiFailures = 0;

    while (true) {
        // ---- Pairing mode -------------------------------------------------------
        if (!online) {
            if (!bleWifi.poll()) {
                delay(50);
                continue;
            }

            /* A complete set was committed and BLE has already been torn down, so the
             * antenna is free. This is the one moment the user is stood there watching
             * the app's progress screen, so a wrong password is worth reporting fast.
             */
            ssid = bleWifi.ssid();
            wifiPassword = bleWifi.password();
            ownerUid = bleWifi.ownerUid();

            if (!connectWifi(ssid, wifiPassword)) {
                returnToPairing("The Wi-Fi credentials just received did not work.");
            }

            cloud.setOwnerUid(ownerUid);
            ntp.begin();
            online = true;
            lastTickMillis = millis();
            Serial.println("Paired and online. Publishing telemetry.");
            continue;
        }

        // ---- Online mode --------------------------------------------------------
        if (WiFi.status() != WL_CONNECTED) {
            if (millis() - lastReconnectMillis < RECONNECT_INTERVAL_MS) {
                continue;
            }
            lastReconnectMillis = millis();

            Serial.println("Wi-Fi lost. Reconnecting...");
            if (connectWifi(ssid, wifiPassword)) {
                wifiFailures = 0;
            } else if (++wifiFailures >= MAX_WIFI_FAILURES) {
                returnToPairing("Wi-Fi has been unreachable for several attempts.");
            }
            continue;
        }

        unsigned long nowMillis = millis();
        if (nowMillis - lastTickMillis < TICK_INTERVAL_MS) {
            continue;
        }
        lastTickMillis = nowMillis;

        unsigned long epoch = ntp.getEpoch();
        if (epoch == 0) {
            Serial.println("NTP time not yet available. Retrying...");
            continue;
        }

        float tempC           = tempSensor.readTemperatureC();
        float phValue         = phSensor.readPH(tempC);
        float tdsPpm          = tdsSensor.readTdsPpm();
        float waterLevelValue = waterSensor.isDetected() ? 1.0f : 0.0f;

        Serial.print("Epoch: ");
        Serial.print(epoch);
        Serial.print(" | Temp: ");
        Serial.print(tempC);
        Serial.print(" C");
        Serial.print(" | pH: ");
        Serial.print(phValue);
        Serial.print(" | TDS: ");
        Serial.print(tdsPpm);
        Serial.print(" | Water Level: ");
        Serial.println(waterLevelValue);

        if (!telemetry.tick(epoch, tempC, waterLevelValue, tdsPpm, phValue)) {
            Serial.println("Could not reach the database. Will retry on the next tick.");
        }
    }
}

void setup() {
    run();
}

void loop() {}
