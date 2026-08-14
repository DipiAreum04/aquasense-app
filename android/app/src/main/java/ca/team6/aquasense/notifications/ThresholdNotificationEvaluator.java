package ca.team6.aquasense.notifications;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import ca.team6.aquasense.firebase.DatabaseSchema;
import ca.team6.aquasense.aquarium.ThresholdBand;
import ca.team6.aquasense.aquarium.sensors.SensorStatus;

public final class ThresholdNotificationEvaluator {

    private ThresholdNotificationEvaluator() {}

    public static List<ThresholdViolation> evaluateSensor(
            String aquariumId,
            String sensorId,
            @Nullable ThresholdBand band,
            double spikeDelta,
            double value,
            @Nullable Double previousValue
    ) {
        if (isNotAMeasurement(value)) {
            return Collections.emptyList();
        }

        ThresholdViolation breach = evaluateBand(aquariumId, sensorId, band, value);
        if (breach != null) {
            return Collections.singletonList(breach);
        }

        ThresholdViolation spike =
                evaluateSpike(aquariumId, sensorId, spikeDelta, value, previousValue);

        return spike == null ? Collections.emptyList() : Collections.singletonList(spike);
    }

    @Nullable
    private static ThresholdViolation evaluateBand(
            String aquariumId,
            String sensorId,
            @Nullable ThresholdBand band,
            double value
    ) {
        if (DatabaseSchema.WATER_LEVEL_KEY.equals(sensorId)) {
            return value < midpointOfWaterLevelStates()
                    ? ThresholdViolation.threshold(
                            aquariumId, sensorId, SensorStatus.CRITICAL, value, null)
                    : null;
        }

        if (band == null) {
            return null;
        }

        SensorStatus status = band.statusFor(value);
        if (status == SensorStatus.NORMAL) {
            return null;
        }
        return ThresholdViolation.threshold(aquariumId, sensorId, status, value, band);
    }

    @Nullable
    private static ThresholdViolation evaluateSpike(
            String aquariumId,
            String sensorId,
            double spikeDelta,
            double value,
            @Nullable Double previousValue
    ) {
        if (previousValue == null || Double.isNaN(spikeDelta)) {
            return null;
        }
        if (isNotAMeasurement(previousValue)) {
            return null;
        }
        if (Math.abs(value - previousValue) < spikeDelta) {
            return null;
        }
        return ThresholdViolation.spike(aquariumId, sensorId, value, previousValue, spikeDelta);
    }

    static boolean isNotAMeasurement(double value) {
        return Double.isNaN(value) || DatabaseSchema.isOffline(value);
    }

    private static double midpointOfWaterLevelStates() {
        return (SensorThresholds.WATER_LEVEL_LOW + SensorThresholds.WATER_LEVEL_DETECTED) / 2.0;
    }
}
