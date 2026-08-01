package ca.team6.aquasense.model;

/**
 * One {@code last_instant} sample: the reading itself plus the moment the board recorded it.
 */
public final class SensorReading {

    /**
     * Written by the firmware in place of a reading when a probe is not reporting. It is never a
     * real measurement, so callers must check {@link #isOffline()} before showing the value.
     */
    public static final double OFFLINE_VALUE = Integer.MIN_VALUE;

    private final double value;
    private final long timestampSeconds;

    public SensorReading(double value, long timestampSeconds) {
        this.value = value;
        this.timestampSeconds = timestampSeconds;
    }

    public double getValue() {
        return value;
    }

    /** Epoch <em>seconds</em>, matching the bounds in {@code database/schema.json}. */
    public long getTimestampSeconds() {
        return timestampSeconds;
    }

    public boolean isOffline() {
        // Compared with a tolerance rather than ==, since the value round-trips through a double.
        return ((int) value) == OFFLINE_VALUE;
    }

    /**
     * How old this sample is. A board that stops publishing leaves its last reading in place, so
     * age is the only way to tell a live value from a frozen one.
     */
    public long ageSeconds(long nowSeconds) {
        return nowSeconds - timestampSeconds;
    }
}
