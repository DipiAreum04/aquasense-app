package ca.team6.aquasense.model.aquarium_sensors;

import android.content.Context;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.DatabaseSchema;
import ca.team6.aquasense.model.InfoSheetSection;

public class PhLevelSensor extends AquariumSensor {
    public PhLevelSensor() {
        super();
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
    public int getInfoSheetAboutResId() {
        return R.string.sensor_info_ph_level_about;
    }

    @Override
    public InfoSheetSection[] getInfoSheetSectionsForDisconnected(Context context) {
        return new InfoSheetSection[] {
                section(context, R.string.sensor_info_section_reconnect,
                        context.getString(R.string.sensor_info_ph_level_disconnected_1),
                        context.getString(R.string.sensor_info_ph_level_disconnected_2),
                        context.getString(R.string.sensor_info_ph_level_disconnected_3),
                        context.getString(R.string.sensor_info_ph_level_disconnected_4))
        };
    }

    @Override
    public InfoSheetSection[] getInfoSheetSectionsForCritical(Context context) {
        return new InfoSheetSection[] {
                section(context, R.string.sensor_info_section_act,
                        context.getString(R.string.sensor_info_ph_level_critical_1),
                        context.getString(R.string.sensor_info_ph_level_critical_2),
                        context.getString(R.string.sensor_info_ph_level_critical_3))
        };
    }

    @Override
    public InfoSheetSection[] getInfoSheetSectionsForWarning(Context context) {
        return new InfoSheetSection[] {
                section(context, R.string.sensor_info_section_todo,
                        context.getString(R.string.sensor_info_ph_level_warning_1),
                        context.getString(R.string.sensor_info_ph_level_warning_2),
                        context.getString(R.string.sensor_info_ph_level_warning_3))
        };
    }

    @Override
    public InfoSheetSection[] getInfoSheetSectionsForNormal(Context context) {
        return new InfoSheetSection[] {
                section(context, R.string.sensor_info_section_keep,
                        context.getString(R.string.sensor_info_ph_level_normal_1),
                        context.getString(R.string.sensor_info_ph_level_normal_2),
                        context.getString(R.string.sensor_info_ph_level_normal_3))
        };
    }
}
