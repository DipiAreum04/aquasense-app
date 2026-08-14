package ca.team6.aquasense.notifications;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class QuietHoursTest {

    private static int at(int hour, int minute) {
        return hour * 60 + minute;
    }

    @Test
    public void windowWithinOneDayIncludesItsStartAndExcludesItsEnd() {
        assertTrue(QuietHours.isActiveAt("09:00", "17:00", at(9, 0)));
        assertTrue(QuietHours.isActiveAt("09:00", "17:00", at(12, 30)));
        assertFalse(QuietHours.isActiveAt("09:00", "17:00", at(17, 0)));
        assertFalse(QuietHours.isActiveAt("09:00", "17:00", at(8, 59)));
    }

    @Test
    public void defaultWindowWrapsPastMidnight() {
        assertTrue(QuietHours.isActiveAt("22:00", "07:00", at(23, 30)));
        assertTrue(QuietHours.isActiveAt("22:00", "07:00", at(2, 0)));
        assertTrue(QuietHours.isActiveAt("22:00", "07:00", at(22, 0)));
        assertFalse(QuietHours.isActiveAt("22:00", "07:00", at(7, 0)));
        assertFalse(QuietHours.isActiveAt("22:00", "07:00", at(12, 0)));
    }

    @Test
    public void zeroLengthWindowMutesNothing() {
        assertFalse(QuietHours.isActiveAt("22:00", "22:00", at(22, 0)));
        assertFalse(QuietHours.isActiveAt("22:00", "22:00", at(3, 0)));
    }

    @Test
    public void unreadableBoundsMuteNothing() {
        assertFalse(QuietHours.isActiveAt(null, "07:00", at(23, 0)));
        assertFalse(QuietHours.isActiveAt("22:00", null, at(23, 0)));
        assertFalse(QuietHours.isActiveAt("10pm", "07:00", at(23, 0)));
        assertFalse(QuietHours.isActiveAt("22:00", "7", at(23, 0)));
        assertFalse(QuietHours.isActiveAt("25:00", "07:00", at(23, 0)));
        assertFalse(QuietHours.isActiveAt("22:75", "07:00", at(23, 0)));
    }

    @Test
    public void parsesMinutesSinceMidnight() {
        assertEquals(0, QuietHours.parseMinutes("00:00"));
        assertEquals(at(22, 30), QuietHours.parseMinutes("22:30"));
        assertEquals(at(23, 59), QuietHours.parseMinutes("23:59"));
        assertEquals(-1, QuietHours.parseMinutes("nonsense"));
    }
}
