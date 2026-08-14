package ca.team6.aquasense.notifications;

import ca.team6.aquasense.aquarium.sensors.SensorStatus;

public class NotificationLogEntry {
    public final long localId;
    public final String aquariumId;
    public final SensorType sensorType;
    public final NotificationTrigger trigger;
    public final SensorStatus severity;
    public final long timestamp;

    public NotificationLogEntry(
            String aquariumId,
            SensorType sensorType,
            NotificationTrigger trigger,
            SensorStatus severity,
            long timestamp
    ) {
        this(-1, aquariumId, sensorType, trigger, severity, timestamp);
    }

    public NotificationLogEntry(
            long localId,
            String aquariumId,
            SensorType sensorType,
            NotificationTrigger trigger,
            SensorStatus severity,
            long timestamp
    ) {
        this.localId = localId;
        this.aquariumId = aquariumId;
        this.sensorType = sensorType;
        this.trigger = trigger;
        this.severity = severity;
        this.timestamp = timestamp;
    }
}
