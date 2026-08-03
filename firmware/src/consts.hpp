#ifndef CONSTS_HPP
#define CONSTS_HPP

static const int RESOLUTION = 100;
static const float OFFLINE_VALUE = -2147483648.0f;

/* The shortest span any period buckets over: last_1h, divided into RESOLUTION
 * buckets. An upload gap briefer than this cannot have cost any period a bucket,
 * which makes it the point below which an outage is not worth reacting to.
 * Tracks Periods' last_1h span.
 */
static const unsigned long SHORTEST_BUCKET_SECS = 3600 / RESOLUTION;

/* How long any HTTP exchange may wait, applied to both the response and the reads
 * that follow it. Neither library default suits this firmware: ArduinoHttpClient
 * waits 30s for a response, which is long enough to starve bleWifi.poll() in the
 * main loop, while Stream gives up on a read after 1s, which is short enough to
 * abandon a slow reply part-way through its body. Every request sets both to this.
 */
static const unsigned long HTTP_TIMEOUT_MS = 10000;

#endif
