package ca.team6.aquasense.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ca.team6.aquasense.model.DatabaseSchema;
import ca.team6.aquasense.model.ThresholdBand;

/**
 * Covers what the Water Parameters screen will and will not let the user save.
 *
 * <p>The ordering rules are the load-bearing part. {@code database/rules.json} requires
 * {@code warn_low < safe_low < safe_high < warn_high} and rejects the write otherwise, so a form
 * that accepted anything looser would hand the user a save button that fails at the server with
 * nothing useful to say. These tests are that contract restated on the client.
 */
public class ThresholdFormTest {

    private static final double PRECISION = 1e-9;

    private static ThresholdForm.Result freshwaterTemperature(
            String warningLow, String safeLow, String safeHigh, String warningHigh) {
        return ThresholdForm.validate(
                warningLow, safeLow, safeHigh, warningHigh, "2", ThresholdForm.IDENTITY);
    }

    @Test
    public void wellOrderedBoundsBecomeABand() {
        ThresholdForm.Result result = freshwaterTemperature("22", "24", "27", "29");

        assertTrue(result.isValid());
        ThresholdBand band = result.getBand();
        assertNotNull(band);
        assertEquals(22, band.getWarnLow(), PRECISION);
        assertEquals(24, band.getSafeLow(), PRECISION);
        assertEquals(27, band.getSafeHigh(), PRECISION);
        assertEquals(29, band.getWarnHigh(), PRECISION);
        assertEquals(2, result.getSpikeDelta(), PRECISION);
    }

    @Test
    public void aBoundBelowTheOneBeforeItIsBlamedOnItself() {
        ThresholdForm.Result result = freshwaterTemperature("22", "24", "23", "29");

        assertFalse(result.isValid());
        assertNull(result.getBand());
        assertEquals(
                ThresholdForm.Problem.NOT_ABOVE_PREVIOUS,
                result.problemFor(ThresholdForm.Field.SAFE_HIGH));
        // The bound it crossed is where the user put it; only the one that moved is wrong.
        assertNull(result.problemFor(ThresholdForm.Field.SAFE_LOW));
    }

    /**
     * Two bounds meeting is what "a range left uncovered" looks like in a four-bound model: there
     * is no value that reads as a warning on the low side, so that band exists in the form and
     * nowhere else.
     */
    @Test
    public void twoBoundsMeetingLeavesABandWithNoRoomAndIsRejected() {
        ThresholdForm.Result result = freshwaterTemperature("24", "24", "27", "29");

        assertFalse(result.isValid());
        assertEquals(
                ThresholdForm.Problem.NOT_ABOVE_PREVIOUS,
                result.problemFor(ThresholdForm.Field.SAFE_LOW));
    }

    @Test
    public void aBlankBoundIsReportedWithoutBlamingItsNeighbours() {
        ThresholdForm.Result result = freshwaterTemperature("22", "", "27", "29");

        assertFalse(result.isValid());
        assertEquals(
                ThresholdForm.Problem.NOT_A_NUMBER,
                result.problemFor(ThresholdForm.Field.SAFE_LOW));
        // Nothing is known about where the missing bound belongs, so nothing is said about the
        // bounds either side of it.
        assertNull(result.problemFor(ThresholdForm.Field.WARNING_LOW));
        assertNull(result.problemFor(ThresholdForm.Field.SAFE_HIGH));
    }

    @Test
    public void textThatIsNotANumberIsRejected() {
        ThresholdForm.Result result = freshwaterTemperature("22", "-", "27", "29");

        assertFalse(result.isValid());
        assertEquals(
                ThresholdForm.Problem.NOT_A_NUMBER,
                result.problemFor(ThresholdForm.Field.SAFE_LOW));
    }

    @Test
    public void everyBoundOutOfPlaceIsReportedAtOnce() {
        ThresholdForm.Result result = freshwaterTemperature("29", "27", "24", "22");

        assertFalse(result.isValid());
        assertEquals(3, result.getProblems().size());
    }

    @Test
    public void aSpikeOfZeroIsRejectedBecauseEveryReadingWouldBeOne() {
        ThresholdForm.Result result = ThresholdForm.validate(
                "22", "24", "27", "29", "0", ThresholdForm.IDENTITY);

        assertFalse(result.isValid());
        assertEquals(
                ThresholdForm.Problem.NOT_POSITIVE,
                result.problemFor(ThresholdForm.Field.SPIKE));
    }

    @Test
    public void aNegativeSpikeIsRejected() {
        ThresholdForm.Result result = ThresholdForm.validate(
                "22", "24", "27", "29", "-1", ThresholdForm.IDENTITY);

        assertEquals(
                ThresholdForm.Problem.NOT_POSITIVE,
                result.problemFor(ThresholdForm.Field.SPIKE));
    }

    @Test
    public void aBlankSpikeIsRejectedRatherThanTreatedAsNoSpikeChecking() {
        ThresholdForm.Result result = ThresholdForm.validate(
                "22", "24", "27", "29", "", ThresholdForm.IDENTITY);

        assertFalse(result.isValid());
        assertEquals(
                ThresholdForm.Problem.NOT_A_NUMBER,
                result.problemFor(ThresholdForm.Field.SPIKE));
    }

    /** A comma-decimal locale formats the prefilled value that way, so it has to read back. */
    @Test
    public void aCommaDecimalSeparatorParses() {
        ThresholdForm.Result result = freshwaterTemperature("22,5", "24", "27", "29");

        assertTrue(result.isValid());
        assertNotNull(result.getBand());
        assertEquals(22.5, result.getBand().getWarnLow(), PRECISION);
    }

    @Test
    public void aTemperatureTypedInFahrenheitIsStoredInCelsius() {
        ThresholdForm.Units units =
                ThresholdForm.unitsFor(DatabaseSchema.TEMPERATURE_KEY, true);

        // The Fahrenheit rendering of the freshwater template's 22/24/27/29 °C band.
        ThresholdForm.Result result =
                ThresholdForm.validate("71.6", "75.2", "80.6", "84.2", "3.6", units);

        assertTrue(result.isValid());
        ThresholdBand band = result.getBand();
        assertNotNull(band);
        assertEquals(22, band.getWarnLow(), 1e-6);
        assertEquals(24, band.getSafeLow(), 1e-6);
        assertEquals(27, band.getSafeHigh(), 1e-6);
        assertEquals(29, band.getWarnHigh(), 1e-6);
        // A spike is a difference, so it scales without picking up the 32 degree offset.
        assertEquals(2, result.getSpikeDelta(), 1e-6);
    }

    @Test
    public void aFahrenheitRoundTripReturnsTheSameCelsiusBound() {
        ThresholdForm.Units units =
                ThresholdForm.unitsFor(DatabaseSchema.TEMPERATURE_KEY, true);

        assertEquals(22.0, units.toStorage(units.toDisplay(22.0)), 1e-9);
        assertEquals(2.0, units.deltaToStorage(units.deltaToDisplay(2.0)), 1e-9);
    }

    /** Every sensor but temperature reads the same number however the app is configured. */
    @Test
    public void aSensorWithOneUnitIsNeverConverted() {
        ThresholdForm.Units units = ThresholdForm.unitsFor(DatabaseSchema.PH_LEVEL_KEY, true);

        assertEquals(ThresholdForm.IDENTITY, units);
    }

    @Test
    public void celsiusIsNeverConvertedEither() {
        ThresholdForm.Units units =
                ThresholdForm.unitsFor(DatabaseSchema.TEMPERATURE_KEY, false);

        assertEquals(ThresholdForm.IDENTITY, units);
    }
}
