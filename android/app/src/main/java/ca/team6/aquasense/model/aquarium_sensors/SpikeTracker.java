package ca.team6.aquasense.model.aquarium_sensors;

import androidx.annotation.Nullable;

/**
 * Decides whether a sensor's latest sample jumped far enough from the one before it to count as a
 * spike.
 *
 * <p>Holds no Android types, so the rule is exercised on the JVM rather than only on a device.
 */
final class SpikeTracker {

    /** The previous measurement. Null until this sensor has reported once. */
    @Nullable
    private Double baseline;
    /** Timestamp of the sample {@link #spiking} was decided for, so it is decided only once. */
    private long gradedTimestampSeconds = Long.MIN_VALUE;
    private boolean spiking;

    /**
     * Grades one measurement and moves the baseline on.
     *
     * <p>The verdict is cached against {@code timestampSeconds}, because the dashboard re-applies
     * the reading it already holds every time any other sensor publishes. Re-grading the same
     * sample would compare its value against itself, find no jump, and clear the spike a moment
     * after it was raised.
     *
     * @param spikeDelta how far the value must have moved to spike; {@link Double#NaN} for a sensor
     *     that cannot spike, which never grades as one.
     * @return whether this sample is a spike.
     */
    boolean grade(double value, long timestampSeconds, double spikeDelta) {
        if (timestampSeconds == this.gradedTimestampSeconds) {
            return this.spiking;
        }
        this.spiking = this.baseline != null
                && !Double.isNaN(spikeDelta)
                && Math.abs(value - this.baseline) >= spikeDelta;
        this.baseline = value;
        this.gradedTimestampSeconds = timestampSeconds;
        return this.spiking;
    }

    /**
     * Reports that there is no live measurement to grade. The baseline survives, so a probe that
     * drops out or falls behind for one sample does not lose the history behind it.
     */
    void pause() {
        this.spiking = false;
    }

    /**
     * Drops the history entirely, for when the samples now arriving are from a different source --
     * another aquarium, or a released subscription. Without this the tank being left behind would
     * be the baseline the next tank's first reading is judged against.
     */
    void reset() {
        this.baseline = null;
        this.gradedTimestampSeconds = Long.MIN_VALUE;
        this.spiking = false;
    }
}
