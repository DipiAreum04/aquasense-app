#include "firebase_auth.hpp"
#include <ArduinoHttpClient.h>
#include <WiFiS3.h>
#include <ArduinoJson.h>

#define HTTP_CODE_OK 200

namespace {
    const char* SIGN_IN_HOST = "identitytoolkit.googleapis.com";
    const uint16_t SIGN_IN_PORT = 443;
    const unsigned long DEFAULT_LIFETIME_MS = 3600000UL;  // Google ID tokens are valid for 1h
    const unsigned long REFRESH_MARGIN_MS = 300000UL;     // refresh 5 min before expiry
}

FirebaseAuth::FirebaseAuth(const char* apiKey, const char* email, const char* password)
    : _apiKey(apiKey), _email(email), _password(password) {}

bool FirebaseAuth::ensureFreshToken() {
    if (_signedInOnce && (long) (millis() - _refreshAtMillis) < 0) {
        return true;
    }
    return signIn();
}

/**
 * Signs in with identitytoolkit's signInWithPassword endpoint. Uses ArduinoHttpClient
 * rather than a raw WiFiSSLClient because Google's response is chunk-encoded, and parses
 * the response straight off the stream with an ArduinoJson filter (rather than buffering
 * the ~2KB body into a String first) since the idToken alone is ~1KB and we only have
 * 32KB of SRAM to work with.
 */
bool FirebaseAuth::signIn() {
    WiFiSSLClient tlsClient;
    HttpClient http(tlsClient, SIGN_IN_HOST, SIGN_IN_PORT);

    String path = String("/v1/accounts:signInWithPassword?key=") + _apiKey;
    String body = String("{\"email\":\"") + _email +
        "\",\"password\":\"" + _password +
        "\",\"returnSecureToken\":true}";

    http.beginRequest();
    http.post(path);
    http.sendHeader("Content-Type", "application/json");
    http.sendHeader("Content-Length", body.length());
    http.beginBody();
    http.print(body);
    http.endRequest();

    int status = http.responseStatusCode();
    if (status != HTTP_CODE_OK) {
        Serial.print("Firebase sign-in failed, HTTP ");
        Serial.println(status);
        Serial.println(http.responseBody());
        http.stop();
        return false;
    }
    http.skipResponseHeaders();

    JsonDocument filter;
    filter["idToken"] = true;
    filter["localId"] = true;
    filter["expiresIn"] = true;

    JsonDocument doc;
    DeserializationError err = deserializeJson(doc, http, DeserializationOption::Filter(filter));
    http.stop();

    if (err) {
        Serial.print("Firebase sign-in response parse failed: ");
        Serial.println(err.c_str());
        return false;
    }

    const char* idToken = doc["idToken"];
    const char* localId = doc["localId"];
    if (idToken == nullptr || localId == nullptr) {
        Serial.println("Firebase sign-in response missing idToken/localId");
        return false;
    }

    _idToken = idToken;
    _localId = localId;

    unsigned long lifetimeMs = (unsigned long) (doc["expiresIn"] | 3600) * 1000UL;
    unsigned long margin = lifetimeMs < REFRESH_MARGIN_MS ? lifetimeMs : REFRESH_MARGIN_MS;
    _refreshAtMillis = millis() + lifetimeMs - margin;
    _signedInOnce = true;

    return true;
}
