package ca.team6.aquasense.notifications;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import ca.team6.aquasense.firebase.DatabaseSchema;
import ca.team6.aquasense.aquarium.ThresholdBand;

public class ThresholdViolationKeyTest {

    private static final String AQUARIUM = "tank-1";
    private static final ThresholdBand TEMPERATURE_BAND = new ThresholdBand(22, 24, 27, 29);

    private static ThresholdViolation temperatureAt(double value) {
        return ThresholdNotificationEvaluator.evaluateSensor(
                AQUARIUM, DatabaseSchema.TEMPERATURE_KEY, TEMPERATURE_BAND,
                Double.NaN, value, null).get(0);
    }

    @Test
    public void sameSensorAtSameStatusSharesACooldownWindow() {
        assertEquals(
                temperatureAt(28.0).cooldownKey(),
                temperatureAt(28.5).cooldownKey());
    }

    @Test
    public void warningTurningCriticalOpensItsOwnCooldownWindow() {
        assertNotEquals(
                temperatureAt(28.0).cooldownKey(),
                temperatureAt(30.0).cooldownKey());
    }

    @Test
    public void warningTurningCriticalReplacesTheNotificationAlreadyInTheShade() {
        assertEquals(
                temperatureAt(28.0).notificationId(),
                temperatureAt(30.0).notificationId());
    }

    @Test
    public void differentSensorsNeverShareACooldownWindow() {
        ThresholdViolation temperature = temperatureAt(30.0);
        ThresholdViolation hub = ThresholdViolation.hubDisconnected(AQUARIUM);

        assertNotEquals(temperature.cooldownKey(), hub.cooldownKey());
    }

    @Test
    public void aSpikeIsSeparateFromTheBandBreachOnTheSameSensor() {
        ThresholdViolation breach = temperatureAt(30.0);
        ThresholdViolation spike = ThresholdViolation.spike(
                AQUARIUM, DatabaseSchema.TEMPERATURE_KEY, 30.0, 25.0, 2.0);

        assertNotEquals(breach.cooldownKey(), spike.cooldownKey());
        assertNotEquals(breach.notificationId(), spike.notificationId());
    }
}
