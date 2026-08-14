package ca.team6.aquasense.notifications;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

import ca.team6.aquasense.firebase.DatabaseSchema;
import ca.team6.aquasense.aquarium.ThresholdBand;
import ca.team6.aquasense.aquarium.sensors.SensorStatus;

public class ThresholdNotificationEvaluatorTest {

    private static final String AQUARIUM = "tank-1";

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
        assertEquals(SensorStatus.WARNING, violation.severity);
    }

    @Test
    public void jumpSmallerThanTheSpikeDeltaIsNotASpike() {
        assertTrue(evaluateTemperature(25.0, 24.0).isEmpty());
    }

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
        List<ThresholdViolation> violations =
                evaluateTemperature(24.0, DatabaseSchema.OFFLINE_SENTINEL);

        assertTrue(violations.isEmpty());
    }

    @Test
    public void anExtremeButRealReadingStillAlerts() {
        List<ThresholdViolation> violations = ThresholdNotificationEvaluator.evaluateSensor(
                AQUARIUM, DatabaseSchema.PH_LEVEL_KEY, PH_BAND,
                SensorThresholds.DEFAULT_PH_SPIKE, 0.5, null);

        assertEquals(1, violations.size());
        assertTrue(violations.get(0).isCritical());
    }


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


    @Test
    public void sensorWithNoBandIsUnmonitoredRatherThanAlwaysSafe() {
        List<ThresholdViolation> violations = ThresholdNotificationEvaluator.evaluateSensor(
                AQUARIUM, DatabaseSchema.DISSOLVED_SOLIDS_KEY, null,
                SensorThresholds.DEFAULT_TDS_SPIKE_PPM, 9999.0, null);

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
        String first = evaluateTemperature(31.0, null).get(0).dedupeKey();
        String second = evaluateTemperature(32.0, null).get(0).dedupeKey();

        assertEquals(first, second);
    }
}
