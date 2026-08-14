package ca.team6.aquasense.notifications;

import androidx.annotation.Nullable;

import java.util.Calendar;

public final class QuietHours {

    private QuietHours() {}

    public static boolean isActiveNow(@Nullable String start, @Nullable String end) {
        Calendar now = Calendar.getInstance();
        int minutesNow = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        return isActiveAt(start, end, minutesNow);
    }

    static boolean isActiveAt(@Nullable String start, @Nullable String end, int minutesNow) {
        int startMinutes = parseMinutes(start);
        int endMinutes = parseMinutes(end);
        if (startMinutes < 0 || endMinutes < 0) {
            return false;
        }
        if (startMinutes == endMinutes) {
            return false;
        }
        if (startMinutes < endMinutes) {
            return minutesNow >= startMinutes && minutesNow < endMinutes;
        }
        return minutesNow >= startMinutes || minutesNow < endMinutes;
    }

    static int parseMinutes(@Nullable String time) {
        if (time == null) {
            return -1;
        }
        String[] parts = time.split(":");
        if (parts.length != 2) {
            return -1;
        }
        try {
            int hours = Integer.parseInt(parts[0].trim(), 10);
            int minutes = Integer.parseInt(parts[1].trim(), 10);
            if (hours < 0 || hours > 23 || minutes < 0 || minutes > 59) {
                return -1;
            }
            return hours * 60 + minutes;
        } catch (NumberFormatException e) {
            return -1;
        }
    }
}
