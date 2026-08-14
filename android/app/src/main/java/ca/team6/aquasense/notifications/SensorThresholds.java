package ca.team6.aquasense.notifications;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ca.team6.aquasense.aquarium.Aquarium;
import ca.team6.aquasense.firebase.DatabaseSchema;

public final class SensorThresholds {

    public static final double DEFAULT_TEMPERATURE_SPIKE_C = 2.0;
    public static final double DEFAULT_PH_SPIKE = 0.5;
    public static final double DEFAULT_TDS_SPIKE_PPM = 50.0;

    public static final double WATER_LEVEL_DETECTED = 1.0;
    public static final double WATER_LEVEL_LOW = 0.0;

    public static final long NOTIFICATION_COOLDOWN_MS = 15 * 60 * 1000L;

    public static final String HUB_DEDUPE_KEY = "__hub__";

    public static final long HUB_SILENCE_TIMEOUT_SECONDS = 15 * 60;

    public static final long HUB_CHECK_INTERVAL_MS = 60 * 1000L;

    private SensorThresholds() {}

    public static double resolveSpikeDelta(@Nullable Aquarium aquarium, @NonNull String sensorId) {
        Double configured = aquarium == null ? null : aquarium.spikeDeltaFor(sensorId);
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
