#ifndef BLE_WIFI_SETUP_HPP
#define BLE_WIFI_SETUP_HPP

#include <Arduino.h>
#include <ArduinoBLE.h>
#include <WiFiS3.h>
#include <EEPROM.h>

#define SSID_ADDR 0
#define PASS_ADDR 64

class BLEWifiSetup {
public:
    BLEWifiSetup();
    void begin();
    void poll();
    bool tryConnectStored();

private:
    BLEService wifiService{ "12345678-1234-5678-1234-56789abcdef0" };

    BLEStringCharacteristic ssidChar{
        "12345678-1234-5678-1234-56789abcdef1",
        BLEWrite,
        32
    };

    BLEStringCharacteristic passChar{
        "12345678-1234-5678-1234-56789abcdef2",
        BLEWrite,
        64
    };

    void saveCredentials(const String& ssid, const String& pass);
    void loadCredentials(String& ssid, String& pass);
};

#endif
