package ca.team6.aquasense.model;

public final class SensorReading {

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

    public long getTimestampSeconds() {
        return timestampSeconds;
    }

    public boolean isOffline() {
        return ((int) value) == OFFLINE_VALUE;
    }

    public long ageSeconds(long nowSeconds) {
        return nowSeconds - timestampSeconds;
    }
}
