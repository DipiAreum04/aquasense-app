package ca.team6.aquasense.model.aquarium_sensors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SpikeStatusTest {

    private static final double DELTA = 2.0;

    private static boolean gradeNext(SpikeTracker tracker, double value, long timestamp) {
        return tracker.grade(value, timestamp, DELTA);
    }

    @Test
    public void firstSampleIsNeverASpike() {
        assertFalse(gradeNext(new SpikeTracker(), 25.0, 1L));
    }

    @Test
    public void jumpOfAtLeastTheDeltaIsASpike() {
        SpikeTracker tracker = new SpikeTracker();
        gradeNext(tracker, 25.0, 1L);

        assertTrue(gradeNext(tracker, 27.0, 2L));
    }

    @Test
    public void jumpSmallerThanTheDeltaIsNot() {
        SpikeTracker tracker = new SpikeTracker();
        gradeNext(tracker, 25.0, 1L);

        assertFalse(gradeNext(tracker, 26.9, 2L));
    }

    @Test
    public void aDropCountsTheSameAsARise() {
        SpikeTracker tracker = new SpikeTracker();
        gradeNext(tracker, 25.0, 1L);

        assertTrue(gradeNext(tracker, 22.0, 2L));
    }

    @Test
    public void reapplyingTheSameSampleKeepsTheVerdict() {
        SpikeTracker tracker = new SpikeTracker();
        gradeNext(tracker, 25.0, 1L);
        assertTrue(gradeNext(tracker, 27.0, 2L));

        assertTrue(gradeNext(tracker, 27.0, 2L));
        assertTrue(gradeNext(tracker, 27.0, 2L));
    }

    @Test
    public void theSampleAfterASpikeIsGradedAfresh() {
        SpikeTracker tracker = new SpikeTracker();
        gradeNext(tracker, 25.0, 1L);
        assertTrue(gradeNext(tracker, 27.0, 2L));

        assertFalse(gradeNext(tracker, 27.5, 3L));
    }

    @Test
    public void aSensorThatCannotSpikeNeverDoes() {
        SpikeTracker tracker = new SpikeTracker();
        tracker.grade(0.0, 1L, Double.NaN);

        assertFalse(tracker.grade(1.0, 2L, Double.NaN));
    }

    @Test
    public void pauseKeepsTheBaselineSoOneMissedSampleDoesNotLoseHistory() {
        SpikeTracker tracker = new SpikeTracker();
        gradeNext(tracker, 25.0, 1L);
        tracker.pause();

        assertTrue(gradeNext(tracker, 28.0, 2L));
    }

    @Test
    public void resetDropsTheBaselineSoTheNextAquariumStartsClean() {
        SpikeTracker tracker = new SpikeTracker();
        gradeNext(tracker, 25.0, 1L);
        tracker.reset();

        assertFalse(gradeNext(tracker, 40.0, 2L));
    }

    @Test
    public void aSpikeThatLandsInRangeShowsAsWarning() {
        assertEquals(
                SensorStatus.WARNING,
                AquariumSensor.gradedStatus(true, SensorStatus.NORMAL));
    }

    @Test
    public void aJumpIntoTheWarningBandIsJustThatBand() {
        assertEquals(
                SensorStatus.WARNING,
                AquariumSensor.gradedStatus(true, SensorStatus.WARNING));
    }

    @Test
    public void aJumpIntoTheCriticalBandIsJustThatBand() {
        assertEquals(
                SensorStatus.CRITICAL,
                AquariumSensor.gradedStatus(true, SensorStatus.CRITICAL));
    }

    @Test
    public void withoutAJumpTheBandIsTheWholeStatus() {
        assertEquals(
                SensorStatus.NORMAL,
                AquariumSensor.gradedStatus(false, SensorStatus.NORMAL));
        assertEquals(
                SensorStatus.CRITICAL,
                AquariumSensor.gradedStatus(false, SensorStatus.CRITICAL));
    }

    @Test
    public void aSpikeWarnsForOneReadingAndThenClears() {
        SpikeTracker tracker = new SpikeTracker();
        assertEquals(SensorStatus.NORMAL, statusOf(tracker, 25.0, 1L, SensorStatus.NORMAL));

        assertEquals(SensorStatus.WARNING, statusOf(tracker, 27.5, 2L, SensorStatus.NORMAL));

        assertEquals(SensorStatus.NORMAL, statusOf(tracker, 27.6, 3L, SensorStatus.NORMAL));
    }

    @Test
    public void aJumpOutOfRangeHoldsItsBandForAsLongAsTheReadingIsOutOfRange() {
        SpikeTracker tracker = new SpikeTracker();
        statusOf(tracker, 25.0, 1L, SensorStatus.NORMAL);

        assertEquals(SensorStatus.CRITICAL, statusOf(tracker, 31.0, 2L, SensorStatus.CRITICAL));

        assertEquals(SensorStatus.CRITICAL, statusOf(tracker, 31.1, 3L, SensorStatus.CRITICAL));
    }

    private static SensorStatus statusOf(
            SpikeTracker tracker, double value, long timestamp, SensorStatus banded) {
        return AquariumSensor.gradedStatus(gradeNext(tracker, value, timestamp), banded);
    }
}
