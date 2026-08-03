#include "pairing_data.hpp"

PairingData::PairingData() {}

const char* PairingData::getDbUrl() const {
    return dbUrl;
}

const char* PairingData::getWebApiKey() const {
    return webApiKey;
}

const char* PairingData::getDeviceEmail() const {
    return deviceEmail;
}

const char* PairingData::getDevicePassword() const {
    return devicePassword;
}
