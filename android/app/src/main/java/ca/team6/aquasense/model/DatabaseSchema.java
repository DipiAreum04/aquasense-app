package ca.team6.aquasense.model;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * This class contains all the Realtime Database keys used in the app.
 *
 * <p>These mirror {@code database/schema.json} exactly.
 *
 * <p>Tree shape:
 * <pre>
 * /{uid}/account/{name,email}
 * /{uid}/aquariums/{aquariumId}/{name,water_type,thresholds,spike_deltas}
 * /{uid}/telemetry/{aquariumId}/{sensor}/last_instant/{timestamp,value}
 * /{uid}/telemetry/{aquariumId}/{sensor}/{period}/{index,buckets/B00..B99/{timestamp,value}}
 * </pre>
 */
public final class DatabaseSchema {

    private DatabaseSchema() {}

    // Children of /{uid}. The UID itself is the root key, so there is no constant for it.
    public static final String ACCOUNT_KEY = "account";
    public static final String AQUARIUMS_KEY = "aquariums";
    public static final String TELEMETRY_KEY = "telemetry";

    // /{uid}/account
    public static final String NAME_KEY = "name";
    public static final String EMAIL_KEY = "email";

    // /{uid}/aquariums/{aquariumId}. Only name and water_type are required by the schema;
    // thresholds and spike_deltas are optional, so the app must supply defaults when absent.
    public static final String WATER_TYPE_KEY = "water_type";
    public static final String THRESHOLDS_KEY = "thresholds";
    public static final String SPIKE_DELTAS_KEY = "spike_deltas";

    // Permitted water_type values.
    public static final String WATER_TYPE_FRESHWATER = "freshwater";
    public static final String WATER_TYPE_SALTWATER = "saltwater";

    // Fields of a thresholds band under /{uid}/aquariums/{aquariumId}/thresholds/{sensor}.
    // All four are required together, so a sensor is either fully configured or absent.
    public static final String WARN_LOW_KEY = "warn_low";
    public static final String SAFE_LOW_KEY = "safe_low";
    public static final String SAFE_HIGH_KEY = "safe_high";
    public static final String WARN_HIGH_KEY = "warn_high";

    // Sensor nodes under /{uid}/telemetry/{aquariumId}
    public static final String TEMPERATURE_KEY = "temperature";
    public static final String WATER_LEVEL_KEY = "water_level";
    public static final String DISSOLVED_SOLIDS_KEY = "dissolved_solids";
    public static final String PH_LEVEL_KEY = "ph_level";

    // Every sensor the schema requires a board to publish, so a screen can subscribe to each one
    // by name instead of watching their parent and discovering them. Watching the parent also
    // pulls down every period's buckets, which is far more data than any caller of this wants.
    public static final List<String> SENSOR_IDS = Collections.unmodifiableList(Arrays.asList(
            TEMPERATURE_KEY,
            WATER_LEVEL_KEY,
            DISSOLVED_SOLIDS_KEY,
            PH_LEVEL_KEY));

    // Period nodes under a sensor.
    public static final String LAST_INSTANT_KEY = "last_instant";
    public static final String LAST_1H_KEY = "last_1h";
    public static final String LAST_1D_KEY = "last_1d";
    public static final String LAST_1W_KEY = "last_1w";
    public static final String LAST_1M_KEY = "last_1m";
    public static final String LAST_6M_KEY = "last_6m";
    public static final String LAST_1Y_KEY = "last_1y";

    // Fields of a last_instant node, and of every bucket inside a period. Each bucket carries its
    // own timestamp, so a period has no timestamp of its own.
    public static final String TIMESTAMP_KEY = "timestamp";
    public static final String VALUE_KEY = "value";

    // This value expresses when a sensor is offline or its reading failed.
    public static final double OFFLINE_SENTINEL = Integer.MIN_VALUE;

    // True when a telemetry value is the offline sentinel rather than a measurement.
    public static boolean isOffline(double value) {
        return value == OFFLINE_SENTINEL;
    }

    // Fields of a period node. `index` is the ring-buffer write cursor pointing at the most
    // recently committed bucket, and `buckets` holds B0..B(BUCKET_COUNT - 1).
    public static final String INDEX_KEY = "index";
    public static final String BUCKETS_KEY = "buckets";
    public static final String BUCKET_PREFIX = "B";
    public static final int BUCKET_COUNT = 100;

    /**
     * Bucket child key for a slot in a period's {@code buckets} node, e.g. {@code B07}.
     *
     */
    public static String bucketKey(int slot) {
        return String.format(Locale.ROOT, BUCKET_PREFIX + "%02d", slot);
    }
}
