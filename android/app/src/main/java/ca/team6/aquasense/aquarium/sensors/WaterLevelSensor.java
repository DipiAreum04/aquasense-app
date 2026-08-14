package ca.team6.aquasense.aquarium.sensors;

import android.content.Context;

import androidx.annotation.Nullable;

import ca.team6.aquasense.R;
import ca.team6.aquasense.firebase.DatabaseSchema;
import ca.team6.aquasense.aquarium.InfoSheetSection;
import ca.team6.aquasense.aquarium.ThresholdBand;

public class WaterLevelSensor extends AquariumSensor {

    public static final double HIGH_THRESHOLD = 0.5d;

    public WaterLevelSensor() {
        super();
    }

    @Override
    public String getId() {
        return DatabaseSchema.WATER_LEVEL_KEY;
    }

    @Override
    public SensorStatus statusFor(double value, @Nullable ThresholdBand thresholdBand) {
        if (value < HIGH_THRESHOLD) {
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
    public int getInfoSheetAboutResId() {
        return R.string.sensor_info_water_level_about;
    }

    @Override
    public int getInfoSheetStatusNoteResId() {
        switch (this.status) {
            case DISCONNECTED:
                return R.string.sensor_info_note_disconnected;
            case CRITICAL:
                return R.string.sensor_info_note_water_level_low;
            case WARNING:
                return R.string.sensor_info_note_water_level_warning;
            default:
                return R.string.sensor_info_note_water_level_safe;
        }
    }

    @Nullable
    @Override
    public String[] getInfoSheetBandTexts(Context context) {
        return null;
    }

    @Override
    public InfoSheetSection[] getInfoSheetSectionsForDisconnected(Context context) {
        return new InfoSheetSection[] {
                section(context, R.string.sensor_info_section_reconnect,
                        context.getString(R.string.sensor_info_water_level_disconnected_1),
                        context.getString(R.string.sensor_info_water_level_disconnected_2),
                        context.getString(R.string.sensor_info_water_level_disconnected_3),
                        context.getString(R.string.sensor_info_water_level_disconnected_4))
        };
    }

    @Override
    public InfoSheetSection[] getInfoSheetSectionsForCritical(Context context) {
        return new InfoSheetSection[] {
                section(context, R.string.sensor_info_section_act,
                        context.getString(R.string.sensor_info_water_level_critical_1),
                        context.getString(R.string.sensor_info_water_level_critical_2),
                        context.getString(R.string.sensor_info_water_level_critical_3))
        };
    }

    @Override
    public InfoSheetSection[] getInfoSheetSectionsForWarning(Context context) {
        return new InfoSheetSection[] {
                section(context, R.string.sensor_info_section_todo,
                        context.getString(R.string.sensor_info_water_level_warning_1),
                        context.getString(R.string.sensor_info_water_level_warning_2),
                        context.getString(R.string.sensor_info_water_level_warning_3))
        };
    }

    @Override
    public InfoSheetSection[] getInfoSheetSectionsForNormal(Context context) {
        return new InfoSheetSection[] {
                section(context, R.string.sensor_info_section_keep,
                        context.getString(R.string.sensor_info_water_level_normal_1),
                        context.getString(R.string.sensor_info_water_level_normal_2),
                        context.getString(R.string.sensor_info_water_level_normal_3))
        };
    }
}
