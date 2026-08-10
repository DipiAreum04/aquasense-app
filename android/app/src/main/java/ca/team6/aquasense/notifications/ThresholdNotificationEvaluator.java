package ca.team6.aquasense.notifications;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.List;

import ca.team6.aquasense.model.DatabaseSchema;
import ca.team6.aquasense.model.ThresholdBand;
import ca.team6.aquasense.model.aquarium_sensors.SensorStatus;

public final class ThresholdNotificationEvaluator {

    private ThresholdNotificationEvaluator() {}

    /**
     * Grades one reading of one sensor.
     *
     * @return what this reading is worth alerting on: the band it breached, or the jump it made
     *     from inside the band, or nothing at all. Never both, since a reading that is out of
     *     range is not a spike.
     */
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
            // A reading out of range is reported as the breach it is. The jump that carried it
            // there adds nothing the breach does not already say, and alerting on both would
            // notify one reading twice, so a spike is only ever raised from inside the band.
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
        // Guards against a sentinel that reached the caller's cache before this check existed.
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
