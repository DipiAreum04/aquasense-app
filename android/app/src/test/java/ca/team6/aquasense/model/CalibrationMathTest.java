package ca.team6.aquasense.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

public class CalibrationMathTest {
    @Test
    public void average_usesEverySample() {
        assertEquals(23.5, CalibrationMath.average(Arrays.asList(22d, 23d, 24d, 25d)), 0d);
    }

    @Test
    public void offset_movesRawAverageToReference() {
        double average = 23.5;
        double offset = CalibrationMath.offset(25d, average);
        assertEquals(1.5, offset, 0d);
        assertEquals(25d, CalibrationMath.correct(average, offset), 0d);
    }

    @Test
    public void average_rejectsEmptySamplingWindow() {
        assertThrows(IllegalArgumentException.class,
                () -> CalibrationMath.average(Collections.emptyList()));
    }

    @Test
    public void parseOperatingPoint_acceptsUpToOneDecimalPlace() {
        assertEquals(25d, CalibrationMath.parseOperatingPoint("25"), 1e-9);
        assertEquals(25.5d, CalibrationMath.parseOperatingPoint("25.5"), 1e-9);
        assertEquals(-3.2d, CalibrationMath.parseOperatingPoint("-3.2"), 1e-9);
        assertEquals(0.5d, CalibrationMath.parseOperatingPoint("0.5"), 1e-9);
        assertEquals(7d, CalibrationMath.parseOperatingPoint("  7 "), 1e-9);
    }

    @Test
    public void parseOperatingPoint_acceptsACommaSeparator() {
        assertEquals(25.5d, CalibrationMath.parseOperatingPoint("25,5"), 1e-9);
    }

    @Test
    public void parseOperatingPoint_rejectsPrecisionTheProcedureCannotDeliver() {
        assertTrue(Double.isNaN(CalibrationMath.parseOperatingPoint("25.55")));
        assertTrue(Double.isNaN(CalibrationMath.parseOperatingPoint("25.")));
        assertTrue(Double.isNaN(CalibrationMath.parseOperatingPoint(".5")));
    }

    @Test
    public void parseOperatingPoint_rejectsWhatIsNotANumber() {
        assertTrue(Double.isNaN(CalibrationMath.parseOperatingPoint("")));
        assertTrue(Double.isNaN(CalibrationMath.parseOperatingPoint("abc")));
        assertTrue(Double.isNaN(CalibrationMath.parseOperatingPoint("1e3")));
        assertTrue(Double.isNaN(CalibrationMath.parseOperatingPoint(null)));
    }

    @Test
    public void isWithinSafeRange_acceptsTheBandInclusiveOfItsEdges() {
        assertTrue(CalibrationMath.isWithinSafeRange(25d, 24d, 26d));
        assertTrue(CalibrationMath.isWithinSafeRange(24d, 24d, 26d));
        assertTrue(CalibrationMath.isWithinSafeRange(26d, 24d, 26d));
    }

    @Test
    public void isWithinSafeRange_rejectsOutsideTheBandAndNonValues() {
        assertFalse(CalibrationMath.isWithinSafeRange(23d, 24d, 26d));
        assertFalse(CalibrationMath.isWithinSafeRange(27d, 24d, 26d));
        assertFalse(CalibrationMath.isWithinSafeRange(Double.NaN, 24d, 26d));
    }

    @Test
    public void percentError_measuresTheOffsetAgainstTheBandWidth() {
        assertEquals(25d, CalibrationMath.percentError(0.5d, 24d, 26d), 1e-9);
        assertEquals(25d, CalibrationMath.percentError(-0.5d, 24d, 26d), 1e-9);
        assertEquals(0.2d, CalibrationMath.percentError(0.5d, 150d, 400d), 1e-9);
    }

    @Test
    public void percentError_hasNoAnswerForABandWithNoWidth() {
        assertTrue(Double.isNaN(CalibrationMath.percentError(0.5d, 7d, 7d)));
    }

    @Test
    public void offset_calibratesAgainstTheSampledAverage() {
        double target = 25d;
        double average = CalibrationMath.average(Arrays.asList(24.4d, 24.6d, 24.5d));
        double offset = CalibrationMath.offset(target, average);

        assertEquals(0.5d, offset, 1e-9);
        assertEquals(target, CalibrationMath.correct(average, offset), 1e-9);
    }
}
