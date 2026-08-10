package ca.team6.aquasense.notifications;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ca.team6.aquasense.model.Aquarium;
import ca.team6.aquasense.model.DatabaseSchema;

public final class SensorThresholds {

    public static final double DEFAULT_TEMPERATURE_SPIKE_C = 2.0;
    public static final double DEFAULT_PH_SPIKE = 0.5;
    public static final double DEFAULT_TDS_SPIKE_PPM = 50.0;

    public static final double WATER_LEVEL_DETECTED = 1.0;
    public static final double WATER_LEVEL_LOW = 0.0;

    /**
     * How long one condition stays quiet after alerting. Held per sensor <em>and</em> status, so a
     * warning that turns critical still gets through while the same reading repeating does not.
     */
    public static final long NOTIFICATION_COOLDOWN_MS = 15 * 60 * 1000L;

    public static final String HUB_DEDUPE_KEY = "__hub__";

    public static final long HUB_SILENCE_TIMEOUT_SECONDS = 15 * 60;

    public static final long HUB_CHECK_INTERVAL_MS = 60 * 1000L;

    private SensorThresholds() {}

    /**
     * How far one sensor of one aquarium has to move between samples to count as a spike.
     *
     * <p>Read from {@code /{uid}/aquariums/{id}/spike_deltas/{sensor}}. The constants below are a
     * fallback for an aquarium that has no delta stored, not the normal source: whenever the
     * database has a value for that aquarium and sensor, that value wins.
     *
     * <p>Both callers hold an {@link Aquarium} kept current by a live listener on
     * {@code /{uid}/aquariums}, so a delta edited in the database takes effect on the next reading
     * without restarting the app.
     *
     * @return the delta, or {@link Double#NaN} when this sensor is not a ranged measurement and so
     *     cannot spike at all.
     */
    public static double resolveSpikeDelta(@Nullable Aquarium aquarium, @NonNull String sensorId) {
        Double configured = aquarium == null ? null : aquarium.spikeDeltaFor(sensorId);
        // The schema requires a positive delta. A zero or negative one that reached the database
        // anyway would make every reading a spike, so it is treated as unset rather than obeyed.
        if (configured != null && configured > 0d) {
            return configured;
        }
        return defaultSpikeDelta(sensorId);
    }

    public static double defaultSpikeDelta(String sensorId) {
        switch (sensorId) {
            case DatabaseSchema.TEMPERATURE_KEY:
                return DEFAULT_TEMPERATURE_SPIKE_C;
            case DatabaseSchema.PH_LEVEL_KEY:
                return DEFAULT_PH_SPIKE;
            case DatabaseSchema.DISSOLVED_SOLIDS_KEY:
                return DEFAULT_TDS_SPIKE_PPM;
            default:
                return Double.NaN;
        }
    }
}
