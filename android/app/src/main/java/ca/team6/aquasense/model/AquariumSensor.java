package ca.team6.aquasense.model;

import ca.team6.aquasense.R;

public class AquariumSensor {
    // TODO: INFO TEXT NEEDS TO BE DYNAMIC (STATUS-SPECIFIC).
    // TODO: NEEDS REFACTORING. THIS IS BETTER SUITED WITH SUBCLASSES THAT OVERRIDE A BASE.

    public static final AquariumSensor LIQUID_LEVEL = new AquariumSensor(
            R.drawable.water_24px,
            R.string.liquid_level,
            R.string.unit_dimensionless,
            R.string.sensor_info_liquid_level_title,
            R.string.sensor_info_liquid_level_desc,
            new InfoSheetSection[] {
                    new InfoSheetSection(
                            R.string.sensor_info_example_title,
                            new int[] {
                                R.string.sensor_info_example_point,
                                R.string.sensor_info_example_point,
                                R.string.sensor_info_example_point
                            }
                    ),
                    new InfoSheetSection(
                            R.string.sensor_info_example_title,
                            new int[] {
                                    R.string.sensor_info_example_point,
                                    R.string.sensor_info_example_point,
                                    R.string.sensor_info_example_point
                            }
                    )
            }
    );

    public static final AquariumSensor TEMPERATURE = new AquariumSensor(
            R.drawable.thermometer_24px,
            R.string.temperature,
            R.string.unit_celsius,
            R.string.sensor_info_temperature_title,
            R.string.sensor_info_temperature_desc,
            new InfoSheetSection[] {
                    new InfoSheetSection(
                            R.string.sensor_info_example_title,
                            new int[] {
                                    R.string.sensor_info_example_point,
                                    R.string.sensor_info_example_point,
                                    R.string.sensor_info_example_point
                            }
                    ),
                    new InfoSheetSection(
                            R.string.sensor_info_example_title,
                            new int[] {
                                    R.string.sensor_info_example_point,
                                    R.string.sensor_info_example_point,
                                    R.string.sensor_info_example_point
                            }
                    )
            }
    );

    public static final AquariumSensor DISSOLVED_SOLIDS = new AquariumSensor(
            R.drawable.total_dissolved_solids_24px,
            R.string.dissolved_solids,
            R.string.unit_parts_per_million,
            R.string.sensor_info_dissolved_solids_title,
            R.string.sensor_info_dissolved_solids_desc,
            new InfoSheetSection[] {
                    new InfoSheetSection(
                            R.string.sensor_info_example_title,
                            new int[] {
                                    R.string.sensor_info_example_point,
                                    R.string.sensor_info_example_point,
                                    R.string.sensor_info_example_point
                            }
                    ),
                    new InfoSheetSection(
                            R.string.sensor_info_example_title,
                            new int[] {
                                    R.string.sensor_info_example_point,
                                    R.string.sensor_info_example_point,
                                    R.string.sensor_info_example_point
                            }
                    )
            }
    );

    public static final AquariumSensor PH_LEVEL = new AquariumSensor(
            R.drawable.water_ph_24px,
            R.string.ph_level,
            R.string.unit_dimensionless,
            R.string.sensor_info_ph_level_title,
            R.string.sensor_info_ph_level_desc,
            new InfoSheetSection[] {
                    new InfoSheetSection(
                            R.string.sensor_info_example_title,
                            new int[] {
                                    R.string.sensor_info_example_point,
                                    R.string.sensor_info_example_point,
                                    R.string.sensor_info_example_point
                            }
                    ),
                    new InfoSheetSection(
                            R.string.sensor_info_example_title,
                            new int[] {
                                    R.string.sensor_info_example_point,
                                    R.string.sensor_info_example_point,
                                    R.string.sensor_info_example_point
                            }
                    )
            }
    );

    public final int titleIconResourceId;
    public final int nameResourceId;
    /** Mutable so Display & Units can switch temperature between °C and °F. */
    public int unitResourceId;
    public int statusIconResourceId;
    public int statusTextResourceId;
    public int statusColorResourceId;
    public String value;

    public final int infoTitle;
    public final int infoDesc;
    public final InfoSheetSection[] infoSections;

    private AquariumSensor(
            int titleIconResourceId,
            int nameResourceId,
            int unitResourceId,
            int infoTitle,
            int infoDesc,
            InfoSheetSection[] infoSections
    ) {
        this.titleIconResourceId = titleIconResourceId;
        this.nameResourceId = nameResourceId;
        this.unitResourceId = unitResourceId;
        this.statusIconResourceId = R.drawable.gray_circle_24;
        this.statusTextResourceId = R.string.offline;
        this.statusColorResourceId = R.color.status_gray;
        this.value = "-";
        this.infoTitle = infoTitle;
        this.infoDesc = infoDesc;
        this.infoSections = infoSections;
    }
}
