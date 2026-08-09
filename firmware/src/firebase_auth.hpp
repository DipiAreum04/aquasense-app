#ifndef FIREBASE_AUTH_HPP
#define FIREBASE_AUTH_HPP

#include <Arduino.h>

class FirebaseAuth {
public:
    FirebaseAuth(const char* apiKey, const char* email, const char* password);

    bool ensureFreshToken();

    const char* idToken() const { return _idToken.c_str(); }
    const char* localId() const { return _localId.c_str(); }

private:
    bool signIn();

    const char* _apiKey;
    const char* _email;
    const char* _password;

    String _idToken;
    String _localId;
    unsigned long _refreshAtMillis = 0;

    unsigned long _retryAtMillis = 0;
    unsigned long _backoffMs = 0;

    bool _signedInOnce = false;
};

#endif
