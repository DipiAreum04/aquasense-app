package ca.team6.aquasense.notifications;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

import ca.team6.aquasense.model.DatabaseSchema;
import ca.team6.aquasense.model.ThresholdBand;
import ca.team6.aquasense.model.aquarium_sensors.SensorStatus;

/**
 * Covers the NF-1.1 alert rules. The evaluator takes no Android or Firebase types, so these run on
 * the JVM without a device.
 */
public class ThresholdNotificationEvaluatorTest {

    private static final String AQUARIUM = "tank-1";

    /** Freshwater temperature band: critical below 22 / above 29, safe 24 to 27. */
    private static final ThresholdBand TEMPERATURE_BAND = new ThresholdBand(22, 24, 27, 29);
    private static final ThresholdBand PH_BAND = new ThresholdBand(6.5, 6.8, 7.6, 7.8);

    private static List<ThresholdViolation> evaluateTemperature(double value, Double previous) {
        return ThresholdNotificationEvaluator.evaluateSensor(
                AQUARIUM, DatabaseSchema.TEMPERATURE_KEY, TEMPERATURE_BAND,
                SensorThresholds.DEFAULT_TEMPERATURE_SPIKE_C, value, previous);
    }

    @Test
    public void readingInsideSafeBandRaisesNothing() {
        assertTrue(evaluateTemperature(25.0, null).isEmpty());
    }

    @Test
    public void readingBetweenSafeAndWarnBoundsIsAWarning() {
        List<ThresholdViolation> violations = evaluateTemperature(28.0, null);

        assertEquals(1, violations.size());
        ThresholdViolation violation = violations.get(0);
        assertEquals(ThresholdViolation.ViolationKind.THRESHOLD, violation.kind);
        assertEquals(SensorStatus.WARNING, violation.severity);
        assertFalse(violation.isCritical());
    }

    @Test
    public void readingBeyondWarnBoundIsCritical() {
        List<ThresholdViolation> violations = evaluateTemperature(31.0, null);

        assertEquals(1, violations.size());
        assertEquals(SensorStatus.CRITICAL, violations.get(0).severity);
        assertTrue(violations.get(0).isCritical());
    }

    @Test
    public void jumpAtLeastTheSpikeDeltaIsReported() {
        List<ThresholdViolation> violations = evaluateTemperature(26.5, 24.0);

        assertEquals(1, violations.size());
        ThresholdViolation violation = violations.get(0);
        assertEquals(ThresholdViolation.ViolationKind.SPIKE, violation.kind);
        // A spike inside the safe band is advisory, so quiet hours may still hold it back.
        assertEquals(SensorStatus.WARNING, violation.severity);
    }

    @Test
    public void jumpSmallerThanTheSpikeDeltaIsNotASpike() {
        assertTrue(evaluateTemperature(25.0, 24.0).isEmpty());
    }

    /**
     * A jump only counts as a spike while the reading it lands on is otherwise in range. Once the
     * reading is out of range the breach is the alert, and the jump is not a second one.
     */
    @Test
    public void aJumpThatBreachesTheBandIsReportedAsTheBreachAlone() {
        List<ThresholdViolation> violations = evaluateTemperature(31.0, 24.0);

        assertEquals(1, violations.size());
        ThresholdViolation violation = violations.get(0);
        assertEquals(ThresholdViolation.ViolationKind.THRESHOLD, violation.kind);
        assertEquals(SensorStatus.CRITICAL, violation.severity);
    }

    @Test
    public void aJumpThatOnlyReachesTheWarningBandIsAlsoTheBreachAlone() {
        List<ThresholdViolation> violations = evaluateTemperature(28.0, 24.0);

        assertEquals(1, violations.size());
        ThresholdViolation violation = violations.get(0);
        assertEquals(ThresholdViolation.ViolationKind.THRESHOLD, violation.kind);
        assertEquals(SensorStatus.WARNING, violation.severity);
    }

    @Test
    public void firstReadingHasNoPredecessorSoCannotSpike() {
        assertTrue(evaluateTemperature(24.0, null).isEmpty());
    }

    // --- Offline sentinel: the bug that made a disconnected probe alert three times. ---

    @Test
    public void offlineSentinelIsNotTreatedAsAReading() {
        assertTrue(evaluateTemperature(DatabaseSchema.OFFLINE_SENTINEL, 24.0).isEmpty());
    }

    @Test
    public void offlineSentinelIsTheSameMarkerForEverySensor() {
        assertTrue(ThresholdNotificationEvaluator.evaluateSensor(
                AQUARIUM, DatabaseSchema.PH_LEVEL_KEY, PH_BAND,
                SensorThresholds.DEFAULT_PH_SPIKE,
                DatabaseSchema.OFFLINE_SENTINEL, 7.0).isEmpty());
        assertTrue(ThresholdNotificationEvaluator.evaluateSensor(
                AQUARIUM, DatabaseSchema.WATER_LEVEL_KEY, null, Double.NaN,
                DatabaseSchema.OFFLINE_SENTINEL, null).isEmpty());
    }

    @Test
    public void recoveryFromOfflineDoesNotReadAsASpike() {
        // The caller keeps the last real value rather than the sentinel, but a sentinel that
        // reached the cache must not turn into a huge false spike either.
        List<ThresholdViolation> violations =
                evaluateTemperature(24.0, DatabaseSchema.OFFLINE_SENTINEL);

        assertTrue(violations.isEmpty());
    }

    @Test
    public void anExtremeButRealReadingStillAlerts() {
        // Only the sentinel means "no reading". A genuine extreme is exactly what the user needs
        // to hear about, so it must not be filtered out for looking implausible.
        List<ThresholdViolation> violations = ThresholdNotificationEvaluator.evaluateSensor(
                AQUARIUM, DatabaseSchema.PH_LEVEL_KEY, PH_BAND,
                SensorThresholds.DEFAULT_PH_SPIKE, 0.5, null);

        assertEquals(1, violations.size());
        assertTrue(violations.get(0).isCritical());
    }

    // --- Water level is a float switch, not a ranged measurement. ---

    @Test
    public void noWaterDetectedIsCritical() {
        List<ThresholdViolation> violations = ThresholdNotificationEvaluator.evaluateSensor(
                AQUARIUM, DatabaseSchema.WATER_LEVEL_KEY, null,
                Double.NaN, SensorThresholds.WATER_LEVEL_LOW, null);

        assertEquals(1, violations.size());
        assertTrue(violations.get(0).isCritical());
    }

    @Test
    public void waterDetectedRaisesNothing() {
        List<ThresholdViolation> violations = ThresholdNotificationEvaluator.evaluateSensor(
                AQUARIUM, DatabaseSchema.WATER_LEVEL_KEY, null,
                Double.NaN, SensorThresholds.WATER_LEVEL_DETECTED, null);

        assertTrue(violations.isEmpty());
    }

    // --- Configuration edges. ---

    @Test
    public void sensorWithNoBandIsUnmonitoredRatherThanAlwaysSafe() {
        List<ThresholdViolation> violations = ThresholdNotificationEvaluator.evaluateSensor(
                AQUARIUM, DatabaseSchema.DISSOLVED_SOLIDS_KEY, null,
                SensorThresholds.DEFAULT_TDS_SPIKE_PPM, 9999.0, null);

        // Out of any plausible range, but with no band configured there is nothing to breach.
        assertTrue(violations.isEmpty());
    }

    @Test
    public void sensorWithNoSpikeDeltaStillChecksItsBand() {
        List<ThresholdViolation> violations = ThresholdNotificationEvaluator.evaluateSensor(
                AQUARIUM, DatabaseSchema.TEMPERATURE_KEY, TEMPERATURE_BAND,
                Double.NaN, 31.0, 24.0);

        assertEquals(1, violations.size());
        assertEquals(ThresholdViolation.ViolationKind.THRESHOLD, violations.get(0).kind);
    }

    @Test
    public void dedupeKeySeparatesSensorsAndViolationKinds() {
        ThresholdViolation temperature = evaluateTemperature(31.0, null).get(0);
        ThresholdViolation spike = evaluateTemperature(26.5, 24.0).get(0);
        List<ThresholdViolation> ph = ThresholdNotificationEvaluator.evaluateSensor(
                AQUARIUM, DatabaseSchema.PH_LEVEL_KEY, PH_BAND,
                SensorThresholds.DEFAULT_PH_SPIKE, 9.0, null);

        assertFalse(temperature.dedupeKey().equals(spike.dedupeKey()));
        assertFalse(temperature.dedupeKey().equals(ph.get(0).dedupeKey()));
    }

    @Test
    public void sameOngoingConditionKeepsTheSameDedupeKey() {
        // Two consecutive out-of-range samples are one alert, which is what stops the monitor
        // re-notifying on every reading.
        String first = evaluateTemperature(31.0, null).get(0).dedupeKey();
        String second = evaluateTemperature(32.0, null).get(0).dedupeKey();

        assertEquals(first, second);
    }
}
