package ca.team6.aquasense.firebase;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class DatabaseSchema {

    private DatabaseSchema() {}

    public static final String ACCOUNT_KEY = "account";
    public static final String AQUARIUMS_KEY = "aquariums";
    public static final String TELEMETRY_KEY = "telemetry";

    public static final String NAME_KEY = "name";
    public static final String EMAIL_KEY = "email";

    public static final String WATER_TYPE_KEY = "water_type";
    public static final String THRESHOLDS_KEY = "thresholds";
    public static final String SPIKE_DELTAS_KEY = "spike_deltas";

    public static final String WATER_TYPE_FRESHWATER = "freshwater";
    public static final String WATER_TYPE_SALTWATER = "saltwater";

    public static final String WARN_LOW_KEY = "warn_low";
    public static final String SAFE_LOW_KEY = "safe_low";
    public static final String SAFE_HIGH_KEY = "safe_high";
    public static final String WARN_HIGH_KEY = "warn_high";

    public static final String TEMPERATURE_KEY = "temperature";
    public static final String WATER_LEVEL_KEY = "water_level";
    public static final String DISSOLVED_SOLIDS_KEY = "dissolved_solids";
    public static final String PH_LEVEL_KEY = "ph_level";

    public static final List<String> SENSOR_IDS = Collections.unmodifiableList(Arrays.asList(
            TEMPERATURE_KEY,
            WATER_LEVEL_KEY,
            DISSOLVED_SOLIDS_KEY,
            PH_LEVEL_KEY));

    public static final String LAST_INSTANT_KEY = "last_instant";
    public static final String LAST_1H_KEY = "last_1h";
    public static final String LAST_1D_KEY = "last_1d";
    public static final String LAST_1W_KEY = "last_1w";
    public static final String LAST_1M_KEY = "last_1m";
    public static final String LAST_6M_KEY = "last_6m";
    public static final String LAST_1Y_KEY = "last_1y";

    public static final String TIMESTAMP_KEY = "timestamp";
    public static final String VALUE_KEY = "value";

    public static final double OFFLINE_SENTINEL = Integer.MIN_VALUE;

    public static boolean isOffline(double value) {
        return value == OFFLINE_SENTINEL;
    }

    public static final String INDEX_KEY = "index";
    public static final String BUCKETS_KEY = "buckets";
    public static final String BUCKET_PREFIX = "B";
    public static final int BUCKET_COUNT = 100;

    public static String bucketKey(int slot) {
        return String.format(Locale.ROOT, BUCKET_PREFIX + "%02d", slot);
    }
}
