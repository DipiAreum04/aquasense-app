package ca.team6.aquasense.model.aquarium_sensors;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.DatabaseSchema;
import ca.team6.aquasense.model.InfoSheetSection;

public class PhLevelSensor extends AquariumSensor {
    public PhLevelSensor() {
        super();
    }

    // TODO: STATUS SHOULD BE MADE DYNAMIC
    @Override
    protected void updateSensorStatus() {
        this.status = SensorStatus.OFFLINE;
    }

    @Override
    public String getId() {
        return DatabaseSchema.PH_LEVEL_KEY;
    }

    @Override
    public int getTitleIconResId() {
        return R.drawable.water_ph_24px;
    }

    @Override
    public int getNameResId() {
        return R.string.ph_level;
    }

    @Override
    public int getInfoSheetTitleResId() {
        return R.string.sensor_info_ph_level_title;
    }

    @Override
    public int getInfoSheetDescResId() {
        return R.string.sensor_info_ph_level_desc;
    }

    // TODO: PLACEHOLDERS NEED TO BE REPLACED.
    @Override
    public InfoSheetSection[] getInfoSheetSectionsForOffline() {
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
    public InfoSheetSection[] getInfoSheetSectionsForNominal() {
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
