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
    const char* getDeviceUid() const;

private:
    const char* dbUrl = "https://aams-c2c68-default-rtdb.firebaseio.com";

    const char* webApiKey = "AIzaSyA3pu6pUkaqZldvXNvD5_Mh8ehcFsCz2jU";
    const char* deviceEmail = "dev-0000000001@aquasense.ca";
    const char* devicePassword = "Patty-Fuchka01#";

    const char* deviceUid = "8DiVHQV9CNTkffiT4u9JDHObVrE2";
};

#endif
