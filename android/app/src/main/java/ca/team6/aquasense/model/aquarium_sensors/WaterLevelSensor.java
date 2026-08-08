package ca.team6.aquasense.model.aquarium_sensors;

import androidx.annotation.Nullable;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.DatabaseSchema;
import ca.team6.aquasense.model.InfoSheetSection;
import ca.team6.aquasense.model.ThresholdBand;

public class WaterLevelSensor extends AquariumSensor {
    public WaterLevelSensor() {
        super();
    }

    @Override
    public String getId() {
        return DatabaseSchema.WATER_LEVEL_KEY;
    }

    /**
     * The float switch reports 1 while it is submerged and 0 once it is not, so a zero is not a
     * low reading to be measured against a band: it is the tank at the point where the switch has
     * surfaced, which is critical whether or not this aquarium configures a water level band.
     */
    @Override
    public SensorStatus statusFor(double value, @Nullable ThresholdBand thresholdBand) {
        if (value <= 0d) {
            return SensorStatus.CRITICAL;
        }
        return super.statusFor(value, thresholdBand);
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
