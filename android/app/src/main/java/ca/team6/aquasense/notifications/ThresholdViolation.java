package ca.team6.aquasense.notifications;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ca.team6.aquasense.aquarium.ThresholdBand;
import ca.team6.aquasense.aquarium.sensors.SensorStatus;

public final class ThresholdViolation {

    public enum ViolationKind {
        THRESHOLD,
        SPIKE,
        SENSOR_OFFLINE,
        HUB_DISCONNECTED
    }

    public final String aquariumId;
    public final String sensorId;
    public final ViolationKind kind;
    public final SensorStatus severity;
    public final double value;
    public final double previousValue;
    @Nullable
    public final ThresholdBand band;
    public final double spikeDelta;

    private ThresholdViolation(
            String aquariumId,
            String sensorId,
            ViolationKind kind,
            SensorStatus severity,
            double value,
            double previousValue,
            @Nullable ThresholdBand band,
            double spikeDelta
    ) {
        this.aquariumId = aquariumId;
        this.sensorId = sensorId;
        this.kind = kind;
        this.severity = severity;
        this.value = value;
        this.previousValue = previousValue;
        this.band = band;
        this.spikeDelta = spikeDelta;
    }

    static ThresholdViolation threshold(
            String aquariumId,
            String sensorId,
            SensorStatus severity,
            double value,
            @Nullable ThresholdBand band
    ) {
        return new ThresholdViolation(
                aquariumId, sensorId, ViolationKind.THRESHOLD, severity,
                value, Double.NaN, band, Double.NaN);
    }

    static ThresholdViolation spike(
            String aquariumId,
            String sensorId,
            double value,
            double previousValue,
            double spikeDelta
    ) {
        return new ThresholdViolation(
                aquariumId, sensorId, ViolationKind.SPIKE, SensorStatus.WARNING,
                value, previousValue, null, spikeDelta);
    }

    static ThresholdViolation sensorOffline(String aquariumId, String sensorId) {
        return new ThresholdViolation(
                aquariumId, sensorId, ViolationKind.SENSOR_OFFLINE, SensorStatus.DISCONNECTED,
                Double.NaN, Double.NaN, null, Double.NaN);
    }

    static ThresholdViolation hubDisconnected(String aquariumId) {
        return new ThresholdViolation(
                aquariumId, SensorThresholds.HUB_DEDUPE_KEY,
                ViolationKind.HUB_DISCONNECTED, SensorStatus.DISCONNECTED,
                Double.NaN, Double.NaN, null, Double.NaN);
    }

    @NonNull
    public String dedupeKey() {
        return aquariumId + ":" + sensorId + ":" + kind.name();
    }

    @NonNull
    public String cooldownKey() {
        return dedupeKey() + ":" + severity.name();
    }

    public int notificationId() {
        return dedupeKey().hashCode();
    }

    public boolean isCritical() {
        return severity == SensorStatus.CRITICAL
                || kind == ViolationKind.SENSOR_OFFLINE
                || kind == ViolationKind.HUB_DISCONNECTED;
    }
}
