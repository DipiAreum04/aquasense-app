package ca.team6.aquasense.model;

import androidx.annotation.Nullable;

/**
 * The water chemistry family an aquarium belongs to.
 *
 * <p>This is a property of the water itself, not of the threshold numbers, so it survives the user
 * editing an aquarium's bands.
 */
public enum WaterType {

    FRESHWATER("freshwater"),
    SALTWATER("saltwater");

    private final String key;

    WaterType(String key) {
        this.key = key;
    }

    /** The value stored in the database. Matches the {@code water_type} enum in schema.json. */
    public String getKey() {
        return this.key;
    }

    // Parses a stored water type from the database.
    @Nullable
    public static WaterType fromKey(@Nullable String key) {
        for (WaterType waterType : values()) {
            if (waterType.key.equals(key)) {
                return waterType;
            }
        }
        return null;
    }
}
