package ca.team6.aquasense.model;

import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import ca.team6.aquasense.R;

public enum WaterType {

    FRESHWATER("freshwater", R.string.water_type_freshwater),
    SALTWATER("saltwater", R.string.water_type_saltwater);

    private final String key;
    @StringRes private final int labelResId;

    WaterType(String key, @StringRes int labelResId) {
        this.key = key;
        this.labelResId = labelResId;
    }

    public String getKey() {
        return this.key;
    }

    @StringRes
    public int getLabelResId() {
        return this.labelResId;
    }

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
