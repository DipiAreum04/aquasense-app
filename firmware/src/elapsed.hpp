#ifndef ELAPSED_HPP
#define ELAPSED_HPP

#include <Arduino.h>

inline unsigned long secondsSince(unsigned long now, unsigned long since) {
    return (now > since) ? (now - since) : 0;
}

#endif
