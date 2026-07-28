package ca.team6.aquasense.model;

/**
 * This class contains all the Realtime Database keys used in the app.
 *
 * <p>These mirror {@code database/schema.json} exactly.
 *
 * <p>Tree shape:
 * <pre>
 * /{uid}/account/{name,email}
 * /{uid}/aquariums/{aquariumId}
 * /{uid}/telemetry/{aquariumId}/{sensor}/last_instant/{timestamp,value}
 * /{uid}/telemetry/{aquariumId}/{sensor}/{period}/{index,timestamp,values/B0..B99}
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

    // Sensor nodes under /{uid}/telemetry/{aquariumId}. These double as the sensor IDs the
    // dashboard uses, so AquariumSensor.getId() returns these same values.
    public static final String TEMPERATURE_KEY = "temperature";
    public static final String WATER_LEVEL_KEY = "water_level";
    public static final String DISSOLVED_SOLIDS_KEY = "dissolved_solids";
    public static final String PH_LEVEL_KEY = "ph_level";

    // Period nodes under a sensor.
    public static final String LAST_INSTANT_KEY = "last_instant";
    public static final String LAST_1H_KEY = "last_1h";
    public static final String LAST_1D_KEY = "last_1d";
    public static final String LAST_1W_KEY = "last_1w";
    public static final String LAST_1M_KEY = "last_1m";
    public static final String LAST_6M_KEY = "last_6m";
    public static final String LAST_1Y_KEY = "last_1y";

    // Fields of a last_instant node.
    public static final String TIMESTAMP_KEY = "timestamp";
    public static final String VALUE_KEY = "value";

    // Fields of a period node. `index` is the ring-buffer write cursor and `values` holds the
    // buckets B0..B(BUCKET_COUNT - 1).
    public static final String INDEX_KEY = "index";
    public static final String VALUES_KEY = "values";
    public static final String BUCKET_PREFIX = "B";
    public static final int BUCKET_COUNT = 100;

    /** Bucket child key for a slot in a period's {@code values} node, e.g. {@code B7}. */
    public static String bucketKey(int slot) {
        return BUCKET_PREFIX + slot;
    }
}
