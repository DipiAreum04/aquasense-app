package ca.team6.aquasense.model;

import androidx.annotation.NonNull;

import ca.team6.aquasense.notifications.SensorThresholds;
import ca.team6.aquasense.notifications.ThresholdViolation;

/**
 * Maps a delivered threshold alert into a {@link NotificationLogEntry} for the history screen.
 */
public final class NotificationLogMapper {

    private NotificationLogMapper() {}

    @NonNull
    public static NotificationLogEntry fromViolation(@NonNull ThresholdViolation violation) {
        return new NotificationLogEntry(
                violation.aquariumId,
                sensorTypeFor(violation.sensorId),
                triggerFor(violation),
                violation.severity,
                System.currentTimeMillis());
    }

    /**
     * Anything that is not one of the four sensors is recorded against the hub, the hub's own
     * dedupe key included. Falling back to a real sensor would file board-level alerts under a
     * reading the user can go and look at, which is what {@link SensorType#HUB} exists to stop.
     */
    @NonNull
    private static SensorType sensorTypeFor(@NonNull String sensorId) {
        switch (sensorId) {
            case DatabaseSchema.TEMPERATURE_KEY:
                return SensorType.TEMPERATURE;
            case DatabaseSchema.DISSOLVED_SOLIDS_KEY:
                return SensorType.DISSOLVED_SOLIDS;
            case DatabaseSchema.PH_LEVEL_KEY:
                return SensorType.PH_LEVEL;
            case DatabaseSchema.WATER_LEVEL_KEY:
                return SensorType.WATER_LEVEL;
            case SensorThresholds.HUB_DEDUPE_KEY:
            default:
                return SensorType.HUB;
        }
    }

    @NonNull
    private static NotificationTrigger triggerFor(@NonNull ThresholdViolation violation) {
        switch (violation.kind) {
            case SENSOR_OFFLINE:
            case HUB_DISCONNECTED:
                return NotificationTrigger.OFFLINE;
            case SPIKE:
            case THRESHOLD:
            default:
                return NotificationTrigger.THRESHOLD;
        }
    }
}
