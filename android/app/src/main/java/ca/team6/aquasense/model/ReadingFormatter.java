package ca.team6.aquasense.model;

import android.content.Context;

import java.util.Locale;

/**
 * Formats numeric sensor readings for display using the Display &amp; Units reading-precision
 * preference.
 *
 * <p>Decimal places are chosen per sensor rather than globally since a single "2 decimals everywhere"
 * setting would render pH as {@code 7.18} but dissolved solids as {@code 342.00}, which claims
 * precision the probe does not have.
 */
public final class ReadingFormatter {

    private ReadingFormatter() {}

    // Decimal places per sensor ID, indexed [standard, precise].
    // Water level is a float switch reporting 0 or 1, so it never gains decimals.
    private static int decimalsFor(String sensorId, boolean precise) {
        switch (sensorId) {
            case DatabaseSchema.TEMPERATURE_KEY:
            case DatabaseSchema.PH_LEVEL_KEY:
                return precise ? 2 : 1;
            case DatabaseSchema.DISSOLVED_SOLIDS_KEY:
                return precise ? 1 : 0;
            case DatabaseSchema.WATER_LEVEL_KEY:
                return 0;
            default:
                ScopedLogger.error("Unknown sensor ID " + sensorId + ", defaulting to 1 decimal.");
                return 1;
        }
    }

    public static boolean isPrecise(Context context) {
        SharedPreferenceHelper prefs = SharedPreferenceHelper.getInstance(context);
        if (prefs == null) {
            return false;
        }
        return SettingsRepository.PRECISION_PRECISE.equals(
                prefs.getString(SettingsRepository.KEY_READING_PRECISION,
                        SettingsRepository.PRECISION_STANDARD));
    }

    /**
     * Formats a raw sensor reading for the dashboard card and chart labels.
     *
     * @param sensorId one of the {@code AquariumSensor.getId()} values
     * @param value    the raw reading as stored in Realtime Database
     */
    // The precision is chosen at runtime, so the format string cannot be a literal. The IDE
    // cannot evaluate it statically and reports it as malformed; "%." + 2 + "f" is just "%.2f".
    @SuppressWarnings("MalformedFormatString")
    public static String format(Context context, String sensorId, double value) {
        int decimals = decimalsFor(sensorId, isPrecise(context));
        return String.format(Locale.getDefault(), "%." + decimals + "f", value);
    }
}
