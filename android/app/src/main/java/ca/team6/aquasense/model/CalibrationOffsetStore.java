package ca.team6.aquasense.model;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Device-local calibration offsets, scoped to the aquarium that owns each physical probe.
 *
 * <p>Settings &rarr; Sensor Calibration has the user measure the medium the probe is sitting in with
 * an instrument they trust, and stores what that instrument said minus what the board reported at
 * the same moment. So an offset is always {@code reference - raw}, and adding it back to a later raw
 * reading is what puts the app's numbers on the reference's scale.
 *
 * <p>The aquarium is part of the key because the offset describes one piece of hardware sitting in
 * one tank. A second aquarium has its own probes, which have drifted their own way, and correcting
 * its readings by this tank's numbers would be worse than not correcting them at all.
 *
 * <p>Applied on the way to the screen rather than written back to the database. The board keeps
 * publishing what its probe actually measures - that is the record of what the hardware said, and
 * rewriting it would leave nothing to re-derive a correction from when the user calibrates again.
 * Every offset is stored in the unit the database stores that sensor in, so temperature is degrees
 * Celsius here however Display &amp; Units happens to be set.
 *
 * <p>Water level is deliberately not correctable: it is a detector reporting whether water is at the
 * probe, and shifting a 0 or a 1 by a fraction says nothing about it.
 */
public final class CalibrationOffsetStore {
    private static final String PREFIX = "calibrationOffset.";
    private static final String CALIBRATED_AT_PREFIX = "calibratedAt.";

    private final Context context;
    private final SharedPreferenceHelper preferences;

    public CalibrationOffsetStore(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.preferences = SharedPreferenceHelper.getInstance(this.context);
    }

    /** Whether a correction means anything for this sensor. See the note on water level above. */
    public static boolean isCalibratable(@NonNull String sensorId) {
        return !DatabaseSchema.WATER_LEVEL_KEY.equals(sensorId);
    }

    public double get(@NonNull String aquariumId, @NonNull String sensorId) {
        if (!isCalibratable(sensorId)) return 0d;
        String stored = preferences.getString(key(aquariumId, sensorId), null);
        if (stored == null) return 0d;
        try {
            double value = Double.parseDouble(stored);
            return Double.isFinite(value) ? value : 0d;
        } catch (NumberFormatException ignored) {
            return 0d;
        }
    }

    public void set(@NonNull String aquariumId, @NonNull String sensorId, double offset) {
        if (!Double.isFinite(offset)) throw new IllegalArgumentException("Offset must be finite");
        if (!isCalibratable(sensorId)) return;
        preferences.setString(key(aquariumId, sensorId), Double.toString(offset));
        preferences.setLong(CALIBRATED_AT_PREFIX + aquariumId + "." + sensorId,
                System.currentTimeMillis());
    }

    public long calibratedAt(@NonNull String aquariumId, @NonNull String sensorId) {
        return preferences.getLong(
                CALIBRATED_AT_PREFIX + aquariumId + "." + sensorId, 0L);
    }

    /** Offline markers are protocol values, not measurements, and must never be shifted. */
    public double correct(@NonNull String aquariumId, @NonNull String sensorId, double rawValue) {
        return DatabaseSchema.isOffline(rawValue) ? rawValue
                : CalibrationMath.correct(rawValue, get(aquariumId, sensorId));
    }

    /**
     * Corrects a whole period's buckets, gap markers included - which is to say left alone, exactly
     * as {@link #correct} leaves them. Returns the list unchanged when there is nothing to correct
     * by, so the common case of an uncalibrated sensor copies nothing.
     */
    @NonNull
    public List<SensorReading> correctAll(@NonNull String aquariumId,
                                          @NonNull String sensorId,
                                          @NonNull List<SensorReading> buckets) {
        double offset = get(aquariumId, sensorId);
        if (offset == 0d) {
            return buckets;
        }

        List<SensorReading> corrected = new ArrayList<>(buckets.size());
        for (SensorReading bucket : buckets) {
            corrected.add(bucket.isOffline()
                    ? bucket
                    : new SensorReading(CalibrationMath.correct(bucket.getValue(), offset),
                            bucket.getTimestampSeconds()));
        }
        return corrected;
    }

    /**
     * How the offset reads on the calibration screen, already in the unit the user is shown that
     * sensor in and carrying its sign, or null when this aquarium's sensor has never been
     * calibrated.
     */
    @Nullable
    public String describe(@NonNull String aquariumId, @NonNull String sensorId) {
        double offset = get(aquariumId, sensorId);
        if (offset == 0d) {
            return null;
        }

        boolean fahrenheit = ReadingFormatter.isFahrenheit(context);
        double displayOffset = DatabaseSchema.TEMPERATURE_KEY.equals(sensorId)
                // A correction is a difference rather than a temperature, so it scales without the
                // 32 degree shift - see ReadingFormatter.toDisplayTemperatureDelta.
                ? ReadingFormatter.toDisplayTemperatureDelta(offset, fahrenheit)
                : offset;
        String unit = context.getString(
                ReadingFormatter.unitResIdFor(sensorId, fahrenheit)).trim();
        String number = (displayOffset > 0 ? "+" : "")
                + ReadingFormatter.formatValue(
                        sensorId, displayOffset, ReadingFormatter.isPrecise(context));
        return unit.isEmpty() ? number : number + " " + unit;
    }

    @NonNull
    private static String key(@NonNull String aquariumId, @NonNull String sensorId) {
        return PREFIX + aquariumId + "." + sensorId;
    }
}
