package ca.team6.aquasense.model.aquarium_sensors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Covers how a dashboard card grades a jump between samples: the spike rule itself, and what
 * status a spike shows as once the band has had its say.
 */
public class SpikeStatusTest {

    private static final double DELTA = 2.0;

    /** Consecutive samples, one second apart, which is what a live board produces. */
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

    /**
     * The dashboard re-applies a reading it already holds whenever any other sensor publishes. The
     * verdict has to survive that, or the spike clears a moment after it is raised.
     */
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

        // Settled next to the spiked value, so the card goes back to whatever the band says.
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

    /** The one warning a reading that is in range can earn, and the only case a jump changes. */
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

    /**
     * The rule end to end, over the readings a card actually sees: a jump inside the safe band
     * raises a warning, and the next reading that does not jump puts the card back to normal.
     */
    @Test
    public void aSpikeWarnsForOneReadingAndThenClears() {
        SpikeTracker tracker = new SpikeTracker();
        assertEquals(SensorStatus.NORMAL, statusOf(tracker, 25.0, 1L, SensorStatus.NORMAL));

        assertEquals(SensorStatus.WARNING, statusOf(tracker, 27.5, 2L, SensorStatus.NORMAL));

        assertEquals(SensorStatus.NORMAL, statusOf(tracker, 27.6, 3L, SensorStatus.NORMAL));
    }

    /** A jump out of the safe band is that band's business, and clears with it rather than early. */
    @Test
    public void aJumpOutOfRangeHoldsItsBandForAsLongAsTheReadingIsOutOfRange() {
        SpikeTracker tracker = new SpikeTracker();
        statusOf(tracker, 25.0, 1L, SensorStatus.NORMAL);

        assertEquals(SensorStatus.CRITICAL, statusOf(tracker, 31.0, 2L, SensorStatus.CRITICAL));

        // Settled well inside critical, so there is no jump left, but the band has not moved.
        assertEquals(SensorStatus.CRITICAL, statusOf(tracker, 31.1, 3L, SensorStatus.CRITICAL));
    }

    /** Grades one reading the way a card does: the tracker's verdict settled against the band. */
    private static SensorStatus statusOf(
            SpikeTracker tracker, double value, long timestamp, SensorStatus banded) {
        return AquariumSensor.gradedStatus(gradeNext(tracker, value, timestamp), banded);
    }
}
