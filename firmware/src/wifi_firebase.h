#ifndef WIFI_FIREBASE_H
#define WIFI_FIREBASE_H

#include <WiFiS3.h>
#include <Firebase.h>
#include "status_code.h"

class WiFiFirebase {
public:
    WiFiFirebase(const char* dbUrl);

    void begin();
    bool sendJSON(const char* path, const String& json);
  

private:
    const char* dbUrl;
    Firebase fb;
};

#endif