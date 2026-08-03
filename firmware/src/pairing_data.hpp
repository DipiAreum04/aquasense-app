#ifndef PAIRING_DATA_HPP
#define PAIRING_DATA_HPP

#include <Arduino.h>

class PairingData {
public:
    PairingData();
    const char* getDbUrl() const;
    const char* getWebApiKey() const;
    const char* getDeviceEmail() const;
    const char* getDevicePassword() const;

private:
    const char* dbUrl = "https://aams-c2c68-default-rtdb.firebaseio.com";

    // TODO: placeholders until per-device Firebase credentials are wired up through
    // BLE provisioning (see WiFiFirebase's sprint 3 TODO). These must be replaced
    // before shipping - they will not authenticate against the real project.
    const char* webApiKey = "AIzaSyA3pu6pUkaqZldvXNvD5_Mh8ehcFsCz2jU";
    const char* deviceEmail = "dev-0000000001@aquasense.ca";
    const char* devicePassword = "Patty-Fuchka01#";
};

#endif
