#ifndef PAIRING_DATA_HPP
#define PAIRING_DATA_HPP

#include <Arduino.h>

class PairingData {
public:
    PairingData();
    const char* getDbUrl() const;
    const char* getAquariumId() const;

private:
    const char* dbUrl = "https://aams-c2c68-default-rtdb.firebaseio.com";
    const char* aquariumId = "8DiVHQV9CNTkffiT4u9JDHObVrE2";  //  device's ID hard coded
};

#endif
