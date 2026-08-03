#ifndef FIREBASE_AUTH_HPP
#define FIREBASE_AUTH_HPP

#include <Arduino.h>

/**
 * Signs the device in against the Firebase Identity Toolkit REST API and keeps
 * the resulting ID token fresh. ID tokens expire after 1h, so callers should
 * call ensureFreshToken() periodically (it is a no-op until the token is close
 * to expiring).
 */
class FirebaseAuth {
public:
    FirebaseAuth(const char* apiKey, const char* email, const char* password);

    // Signs in if we've never signed in, or re-signs in if the current token is
    // close to expiring. Returns false if a required sign-in attempt fails, or if
    // one failed recently enough that this is still backing off from it.
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

    // Backoff after a failed sign-in. _backoffMs is 0 whenever no backoff is in
    // effect, which is what lets the first attempt after a success go straight out.
    unsigned long _retryAtMillis = 0;
    unsigned long _backoffMs = 0;

    bool _signedInOnce = false;
};

#endif
