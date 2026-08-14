package ca.team6.aquasense.model;

import androidx.annotation.NonNull;

import ca.team6.aquasense.notifications.SensorThresholds;
import ca.team6.aquasense.notifications.ThresholdViolation;

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
