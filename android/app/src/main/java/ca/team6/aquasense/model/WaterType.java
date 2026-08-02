package ca.team6.aquasense.model;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import ca.team6.aquasense.R;

/**
 * The water chemistry family an aquarium belongs to.
 *
 * <p>This is a property of the water itself, not of the threshold numbers, so it survives the user
 * editing an aquarium's bands.
 */
public enum WaterType {

    FRESHWATER("freshwater", R.string.water_type_freshwater),
    SALTWATER("saltwater", R.string.water_type_saltwater);

    private final String key;
    @StringRes private final int labelResId;

    WaterType(String key, @StringRes int labelResId) {
        this.key = key;
        this.labelResId = labelResId;
    }

    /** The value stored in the database. Matches the {@code water_type} enum in schema.json. */
    public String getKey() {
        return this.key;
    }

    /** Display name for this water type. Never write this to the database, use {@link #getKey()}. */
    @StringRes
    public int getLabelResId() {
        return this.labelResId;
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
