#include "pairing_data.hpp"

PairingData::PairingData() {}

const char* PairingData::getDbUrl() const {
    return dbUrl;
}

const char* PairingData::getAquariumId() const {
    return aquariumId;
}
