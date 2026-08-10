package ca.team6.aquasense.model.aquarium_templates;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.DatabaseSchema;
import ca.team6.aquasense.model.ThresholdBand;
import ca.team6.aquasense.model.WaterType;

/**
 * This class defines the four built-in aquarium templates (SETTINGS-04).
 *
 * <p>The threshold values defined here are the single source of truth for what "safe"/"warning"/"critical"
 * means on the dashboard. Temperatures are in Celsius, TDS is in ppm. Water level is a boolean sensor 
 * and is not included in the thresholds. Moreover, only the saltwater template disables the TDS sensor 
 * as saltwater has a very high TDS range that cannot be read by our TDS sensor.
 *
 */
public final class BuiltInTemplates {

    public static final String FRESHWATER_ID = "freshwater";
    public static final String COLDWATER_ID = "coldwater";
    public static final String HARDWATER_ID = "hardwater";
    public static final String SALTWATER_ID = "saltwater";

    public static final AquariumTemplate FRESHWATER = new AquariumTemplate(
            FRESHWATER_ID,
            R.string.template_freshwater,
            R.string.template_freshwater_desc,
            R.array.template_freshwater_species,
            R.drawable.template_freshwater,
            R.color.template_accent_freshwater,
            WaterType.FRESHWATER,
            bands()
                    .temperature(22, 24, 27, 29)
                    .phLevel(6.5, 6.8, 7.6, 7.8)
                    .dissolvedSolids(50, 100, 300, 450)
                    .build(),
            Collections.emptySet(),
            0
    );

    public static final AquariumTemplate COLDWATER = new AquariumTemplate(
            COLDWATER_ID,
            R.string.template_coldwater,
            R.string.template_coldwater_desc,
            R.array.template_coldwater_species,
            R.drawable.template_coldwater,
            R.color.template_accent_coldwater,
            WaterType.FRESHWATER,
            bands()
                    .temperature(15, 17.5, 22, 24)
                    .phLevel(6, 7, 7.8, 8.4)
                    .dissolvedSolids(100, 150, 350, 500)
                    .build(),
            Collections.emptySet(),
            0
    );

    public static final AquariumTemplate HARDWATER = new AquariumTemplate(
            HARDWATER_ID,
            R.string.template_hardwater,
            R.string.template_hardwater_desc,
            R.array.template_hardwater_species,
            R.drawable.template_hardwater,
            R.color.template_accent_hardwater,
            WaterType.FRESHWATER,
            bands()
                    .temperature(22, 24, 28, 30)
                    .phLevel(7.5, 7.8, 8.6, 9)
                    .dissolvedSolids(220, 300, 450, 570)
                    .build(),
            Collections.emptySet(),
            0
    );

    public static final AquariumTemplate SALTWATER = new AquariumTemplate(
            SALTWATER_ID,
            R.string.template_saltwater,
            R.string.template_saltwater_desc,
            R.array.template_saltwater_species,
            R.drawable.template_saltwater,
            R.color.template_accent_saltwater,
            WaterType.SALTWATER,
            // The TDS sensor cannot read saltwater, so it is disabled for this template.
            bands()
                    .temperature(22, 24, 26, 28)
                    .phLevel(7.8, 8.1, 8.4, 8.6)
                    .build(),
            Set.of(DatabaseSchema.DISSOLVED_SOLIDS_KEY),
            R.string.template_tds_disabled_saltwater
    );

    private static final List<AquariumTemplate> ALL = List.of(
            FRESHWATER,
            COLDWATER,
            HARDWATER,
            SALTWATER
    );

    private BuiltInTemplates() {}

    /** All built-in templates, in the order they are displayed to the user. */
    public static List<AquariumTemplate> all() {
        return ALL;
    }

    /**
     * Looks up a template by its persisted ID.
     */
    @Nullable
    public static AquariumTemplate fromId(@Nullable String id) {
        for (AquariumTemplate template : ALL) {
            if (template.getId().equals(id)) {
                return template;
            }
        }
        return null;
    }

    /** The template a new aquarium starts on is freshwater as it is the most common tank type. */
    public static AquariumTemplate getDefault() {
        return FRESHWATER;
    }

    /**
     * The template to fall back on for an aquarium that stores a water type but no thresholds of
     * its own. The schema only records {@code water_type}, not which template it came from, so an
     * aquarium created before templates existed can only be resolved this far.
     */
    public static AquariumTemplate forWaterType(@Nullable WaterType waterType) {
        return waterType == WaterType.SALTWATER ? SALTWATER : FRESHWATER;
    }

    /** Starts a threshold set for a template. */
    private static Bands bands() {
        return new Bands();
    }

    /**
     * Small builder for each template's thresholds.
     * One line per sensor, named with its key and the four bounds in the same order as the
     * {@link ThresholdBand} constructor parameters (warnLow, safeLow, safeHigh, warnHigh).
     */
    private static final class Bands {

        private final Map<String, ThresholdBand> map = new LinkedHashMap<>();

        Bands temperature(double warnLow, double safeLow, double safeHigh, double warnHigh) {
            return this.put(DatabaseSchema.TEMPERATURE_KEY, warnLow, safeLow, safeHigh, warnHigh);
        }

        Bands phLevel(double warnLow, double safeLow, double safeHigh, double warnHigh) {
            return this.put(DatabaseSchema.PH_LEVEL_KEY, warnLow, safeLow, safeHigh, warnHigh);
        }

        Bands dissolvedSolids(double warnLow, double safeLow, double safeHigh, double warnHigh) {
            return this.put(
                    DatabaseSchema.DISSOLVED_SOLIDS_KEY, warnLow, safeLow, safeHigh, warnHigh);
        }

        Map<String, ThresholdBand> build() {
            return this.map;
        }

        private Bands put(
                String sensorId,
                double warnLow,
                double safeLow,
                double safeHigh,
                double warnHigh
        ) {
            this.map.put(
                    sensorId, new ThresholdBand(warnLow, safeLow, safeHigh, warnHigh));
            return this;
        }
    }
}
