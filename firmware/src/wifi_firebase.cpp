#include "wifi_firebase.h"

WiFiFirebase::WiFiFirebase(const char* dbUrl)
    : dbUrl(dbUrl), fb(dbUrl) {}

void WiFiFirebase::begin() {
    if (WiFi.status() == WL_CONNECTED) {
        Serial.println("Firebase ready (WiFi connected).");
    }
    else {
        Serial.println("Firebase begin called but WiFi NOT connected.");
    }
}

bool WiFiFirebase::sendJSON(const char* path, const String& json) {
    if (WiFi.status() != WL_CONNECTED) {
        Serial.println("sendJSON: WiFi not connected.");
        return false;
    }

    int code = fb.setJson(path, json);
    return (code == StatusCode::OK);
}

