package ca.team6.aquasense.setup;

/**
 * The aquarium details the setup wizard carries from the form that collects them to the pages
 * that need them afterwards.
 *
 * <p>Nothing is written to the database until the hub reports the UID the aquarium is keyed by,
 * so these travel as navigation arguments across three destinations rather than being read back
 * from a repository. One set of key names, because each screen forwards the same bundle on.
 */
public final class SetupArgs {

    public static final String AQUARIUM_NAME = "aquariumName";
    public static final String WATER_TYPE = "waterType";
    /** Null for a Custom aquarium, which is not backed by a template. */
    public static final String TEMPLATE_ID = "templateId";

    private SetupArgs() {
    }
}
