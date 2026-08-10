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

    /**
     * The {@code NewAquariumConfig} to write once pairing succeeds: the name, the water type, and
     * every threshold and spike delta the form settled on.
     */
    public static final String AQUARIUM_CONFIG = "aquariumConfig";

    /**
     * Which template the values came from, for the summary page to name.
     *
     * <p>Carried beside the configuration rather than inside it, because it is not part of what
     * gets stored: {@code database/rules.json} has no field for a template, so it is a source of
     * starting numbers at creation time and a label afterwards.
     */
    public static final String TEMPLATE_ID = "templateId";

    private SetupArgs() {
    }
}
