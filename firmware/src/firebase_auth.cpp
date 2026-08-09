#include "firebase_auth.hpp"
#include <ArduinoHttpClient.h>
#include <WiFiS3.h>
#include <ArduinoJson.h>
#include "consts.hpp"

#define HTTP_CODE_OK 200

namespace {
    const char* SIGN_IN_HOST = "identitytoolkit.googleapis.com";
    const uint16_t SIGN_IN_PORT = 443;
    const unsigned long DEFAULT_LIFETIME_MS = 3600000UL;
    const unsigned long REFRESH_MARGIN_MS = 300000UL;

    const unsigned long RETRY_BACKOFF_MIN_MS = 1000UL;
    const unsigned long RETRY_BACKOFF_MAX_MS = 60000UL;
}

FirebaseAuth::FirebaseAuth(const char* apiKey, const char* email, const char* password)
    : _apiKey(apiKey), _email(email), _password(password) {}

bool FirebaseAuth::ensureFreshToken() {
    if (_signedInOnce && (long) (millis() - _refreshAtMillis) < 0) {
        return true;
    }

    if (_backoffMs != 0 && (long) (millis() - _retryAtMillis) < 0) {
        return false;
    }

    if (signIn()) {
        _backoffMs = 0;
        return true;
    }

    unsigned long next = (_backoffMs == 0) ? RETRY_BACKOFF_MIN_MS : _backoffMs * 2;
    if (next > RETRY_BACKOFF_MAX_MS) {
        next = RETRY_BACKOFF_MAX_MS;
    }
    _backoffMs = next;
    _retryAtMillis = millis() + _backoffMs;

    Serial.println("Firebase sign-in retry in "+String(_backoffMs / 1000)+"s");
    return false;
}

bool FirebaseAuth::signIn() {
    WiFiSSLClient tlsClient;
    HttpClient http(tlsClient, SIGN_IN_HOST, SIGN_IN_PORT);
    http.setHttpResponseTimeout(HTTP_TIMEOUT_MS);
    http.setTimeout(HTTP_TIMEOUT_MS);

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
