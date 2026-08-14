package ca.team6.aquasense.notifications;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;

import ca.team6.aquasense.firebase.DatabaseSchema;

final class SensorTelemetryReading {

    final long timestamp;

    final double value;

    final double rawValue;

    private SensorTelemetryReading(long timestamp, double value) {
        this(timestamp, value, value);
    }

    private SensorTelemetryReading(long timestamp, double value, double rawValue) {
        this.timestamp = timestamp;
        this.value = value;
        this.rawValue = rawValue;
    }

    @NonNull
    SensorTelemetryReading withValue(double correctedValue) {
        return new SensorTelemetryReading(this.timestamp, correctedValue, this.rawValue);
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
