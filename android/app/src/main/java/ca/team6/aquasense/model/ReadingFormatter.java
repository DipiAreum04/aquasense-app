package ca.team6.aquasense.model;

import android.content.Context;

import java.util.Locale;

import ca.team6.aquasense.R;

/**
 * Formats numeric sensor readings for display using the Display &amp; Units preferences.
 *
 * <p>Decimal places are chosen per sensor rather than globally since a single "2 decimals everywhere"
 * setting would render pH as {@code 7.18} but dissolved solids as {@code 342.00}, which claims
 * precision the sensor does not have.
 */
public final class ReadingFormatter {

    private ReadingFormatter() {}

    // Decimal places per sensor ID, indexed [standard, precise].
    // Water level is absent because it is a float switch shown as LOW / SAFE rather than a number,
    // so format() answers it before reaching here.
    private static int decimalsFor(String sensorId, boolean precise) {
        switch (sensorId) {
            case DatabaseSchema.TEMPERATURE_KEY:
            case DatabaseSchema.PH_LEVEL_KEY:
                return precise ? 2 : 1;
            case DatabaseSchema.DISSOLVED_SOLIDS_KEY:
                return precise ? 1 : 0;
            default:
                ScopedLogger.error("Unknown sensor ID " + sensorId + ", defaulting to 1 decimal.");
                return 1;
        }
    }

    // Converts a Celsius temperature to the unit the user reads the app in.
    public static double toDisplayTemperature(Context context, double celsius) {
        return isCelsius(context) ? celsius : celsius * 9 / 5 + 32;
    }

    /** Whether readings are shown in Celsius, so a caller can label an axis in the same unit. */
    public static boolean isCelsius(Context context) {
        SharedPreferenceHelper prefs = SharedPreferenceHelper.getInstance(context);
        if (prefs == null) {
            return "C".equals(new AppSettings().tempUnit);
        }
        return "C".equals(prefs.getString(
                SettingsRepository.KEY_TEMP_UNIT, new AppSettings().tempUnit));
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
     * <p>Water level comes back as a word ({@code LOW} / {@code SAFE}); every other sensor as a number.
     *
     * @param sensorId one of the {@code AquariumSensor.getId()} values
     * @param value    the raw reading as stored in Realtime Database
     */
    // The precision is chosen at runtime, so the format string cannot be a literal. The IDE
    // cannot evaluate it statically and reports it as malformed; "%." + 2 + "f" is just "%.2f".
    @SuppressWarnings("MalformedFormatString")
    public static String format(Context context, String sensorId, double value) {
        // The float switch reports 1 when it still senses water and 0 once the level drops below it,
        // which reads as a state rather than a quantity. Compared against a midpoint because the
        // value arrives from the database as a double.
        if (DatabaseSchema.WATER_LEVEL_KEY.equals(sensorId)) {
            return context.getString(value >= 0.5 ? R.string.water_level_safe : R.string.water_level_low);
        }

        int decimals = decimalsFor(sensorId, isPrecise(context));
        double displayValue = DatabaseSchema.TEMPERATURE_KEY.equals(sensorId)
                ? toDisplayTemperature(context, value)
                : value;
        return String.format(Locale.getDefault(), "%." + decimals + "f", displayValue);
    }
}
