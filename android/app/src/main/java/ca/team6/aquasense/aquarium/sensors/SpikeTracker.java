package ca.team6.aquasense.aquarium.sensors;

import androidx.annotation.Nullable;

final class SpikeTracker {

    @Nullable
    private Double baseline;
    private long gradedTimestampSeconds = Long.MIN_VALUE;
    private boolean spiking;

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

    void pause() {
        this.spiking = false;
    }

    void reset() {
        this.baseline = null;
        this.gradedTimestampSeconds = Long.MIN_VALUE;
        this.spiking = false;
    }
}
