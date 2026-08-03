package ca.team6.aquasense.model.aquarium_sensors;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.DatabaseSchema;
import ca.team6.aquasense.model.InfoSheetSection;

public class WaterLevelSensor extends AquariumSensor {
    public WaterLevelSensor() {
        super();
    }

    @Override
    public String getId() {
        return DatabaseSchema.WATER_LEVEL_KEY;
    }

    @Override
    public int getTitleIconResId() {
        return R.drawable.water_24px;
    }

    @Override
    public int getNameResId() {
        return R.string.water_level;
    }

    @Override
    public int getInfoSheetTitleResId() {
        return R.string.sensor_info_water_level_title;
    }

    @Override
    public int getInfoSheetDescResId() {
        return R.string.sensor_info_water_level_desc;
    }

    // TODO: PLACEHOLDERS NEED TO BE REPLACED.
    @Override
    public InfoSheetSection[] getInfoSheetSectionsForDisconnected() {
        return new InfoSheetSection[] {
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
        };
    }

    // TODO: PLACEHOLDERS NEED TO BE REPLACED.
    @Override
    public InfoSheetSection[] getInfoSheetSectionsForCritical() {
        return new InfoSheetSection[] {
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
        };
    }

    // TODO: PLACEHOLDERS NEED TO BE REPLACED.
    @Override
    public InfoSheetSection[] getInfoSheetSectionsForWarning() {
        return new InfoSheetSection[] {
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
        };
    }

    // TODO: PLACEHOLDERS NEED TO BE REPLACED.
    @Override
    public InfoSheetSection[] getInfoSheetSectionsForNormal() {
        return new InfoSheetSection[] {
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
        };
    }
}
