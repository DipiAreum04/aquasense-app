#ifndef ELAPSED_HPP
#define ELAPSED_HPP

#include <Arduino.h>

/**
 * Seconds from `since` up to `now`, or 0 if `now` has not reached `since` yet.
 *
 * The epoch times compared in this firmware come from two sources that can disagree:
 * the NTP clock, and timestamps read back out of the database. A reference can
 * legitimately sit ahead of the clock, and an NTP correction can step the clock back
 * under a reference that was sound when it was taken. Subtracting unsigned values in
 * that order wraps to something near ULONG_MAX, which every elapsed-time test here
 * would read as an enormous outage and act on - fabricating gap markers and closing
 * buckets that are nowhere near full.
 *
 * Reporting 0 instead makes a clock that has gone backwards hold everything still
 * until it catches up, which is recoverable in a way that writing invented data is not.
 *
 * Not for millis(). Unsigned subtraction is the correct idiom there, because millis
 * only ever wraps forwards, and wrapping it through here would break it.
 *
 * @param now The current epoch time in seconds.
 * @param since The earlier epoch time to measure from.
 * @return The seconds between the two, or 0 if since is in the future.
 */
inline unsigned long secondsSince(unsigned long now, unsigned long since) {
    return (now > since) ? (now - since) : 0;
}

#endif
