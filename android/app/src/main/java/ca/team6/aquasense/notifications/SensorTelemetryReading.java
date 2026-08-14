package ca.team6.aquasense.notifications;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;

import ca.team6.aquasense.model.DatabaseSchema;

final class SensorTelemetryReading {

    final long timestamp;

    /** Graded and reported: the sample with this aquarium's calibration correction on it. */
    final double value;

    /**
     * Remembered: the sample exactly as the board published it.
     *
     * <p>The dedupe store is a record of which samples have been handled, and it outlives any one
     * offset - recalibrating would otherwise make every stored value describe a reading that was
     * never taken, and spike deltas measured against it would be off by the change in offset.
     */
    final double rawValue;

    private SensorTelemetryReading(long timestamp, double value) {
        this(timestamp, value, value);
    }

    private SensorTelemetryReading(long timestamp, double value, double rawValue) {
        this.timestamp = timestamp;
        this.value = value;
        this.rawValue = rawValue;
    }

    /**
     * The same sample with its value replaced, for putting the sensor's calibration correction on
     * it before it is graded. Held apart from the read above so what came off the wire and what the
     * app makes of it stay two separate things.
     */
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
