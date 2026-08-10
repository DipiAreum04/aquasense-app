package ca.team6.aquasense.notifications;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;

import ca.team6.aquasense.model.DatabaseSchema;

final class SensorTelemetryReading {

    final long timestamp;
    final double value;

    private SensorTelemetryReading(long timestamp, double value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    @Nullable
    static SensorTelemetryReading fromInstantSnapshot(@NonNull DataSnapshot instantSnapshot) {
        if (!instantSnapshot.exists()) {
            return null;
        }
        Long timestamp = instantSnapshot.child(DatabaseSchema.TIMESTAMP_KEY).getValue(Long.class);
        Double value = readDouble(instantSnapshot, DatabaseSchema.VALUE_KEY);
        if (timestamp == null || value == null) {
            return null;
        }
        return new SensorTelemetryReading(timestamp, value);
    }

    @Nullable
    private static Double readDouble(@NonNull DataSnapshot snapshot, @NonNull String key) {
        Object value = snapshot.child(key).getValue();
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        return null;
    }
}
