package ca.team6.aquasense.pairing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Which route the user took into pairing: first installation or adding another aquarium.
 * This is used to determine whether the toolbar should offer an up arrow, and also to
 * determine what to do after pairing is complete.
 */
public enum PairingEntryMode {

    FIRST_RUN,

    ADD_AQUARIUM;

    public boolean canNavigateUp() {
        return this == ADD_AQUARIUM;
    }

    @NonNull
    public static PairingEntryMode fromName(@Nullable String name) {
        if (name != null) {
            for (PairingEntryMode mode : values()) {
                if (mode.name().equals(name)) {
                    return mode;
                }
            }
        }
        return ADD_AQUARIUM;
    }
}
