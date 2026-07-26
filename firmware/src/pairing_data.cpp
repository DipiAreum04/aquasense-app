#include "pairing_data.h"

PairingData::PairingData() {}

const char* PairingData::getDbUrl() const {
    return dbUrl;
}

const char* PairingData::getAquariumId() const {
    return aquariumId;
}