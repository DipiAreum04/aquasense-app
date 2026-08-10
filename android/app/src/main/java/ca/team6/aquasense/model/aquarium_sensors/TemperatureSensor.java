package ca.team6.aquasense.model.aquarium_sensors;

import android.content.Context;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.DatabaseSchema;
import ca.team6.aquasense.model.InfoSheetSection;

public class TemperatureSensor extends AquariumSensor {
    public TemperatureSensor() {
        super();
        this.setUnitResId(R.string.unit_celsius);
    }

    @Override
    public String getId() {
        return DatabaseSchema.TEMPERATURE_KEY;
    }

    @Override
    public int getTitleIconResId() {
        return R.drawable.thermometer_24px;
    }

    @Override
    public int getNameResId() {
        return R.string.temperature;
    }

    @Override
    public int getInfoSheetTitleResId() {
        return R.string.sensor_info_temperature_title;
    }

    @Override
    public int getInfoSheetAboutResId() {
        return R.string.sensor_info_temperature_about;
    }

    @Override
    public InfoSheetSection[] getInfoSheetSectionsForDisconnected(Context context) {
        return new InfoSheetSection[] {
                section(context, R.string.sensor_info_section_reconnect,
                        context.getString(R.string.sensor_info_temperature_disconnected_1),
                        context.getString(R.string.sensor_info_temperature_disconnected_2),
                        context.getString(R.string.sensor_info_temperature_disconnected_3),
                        context.getString(R.string.sensor_info_temperature_disconnected_4))
        };
    }

    @Override
    public InfoSheetSection[] getInfoSheetSectionsForCritical(Context context) {
        return new InfoSheetSection[] {
                section(context, R.string.sensor_info_section_act,
                        context.getString(R.string.sensor_info_temperature_critical_1),
                        context.getString(R.string.sensor_info_temperature_critical_2),
                        context.getString(R.string.sensor_info_temperature_critical_3))
        };
    }

    @Override
    public InfoSheetSection[] getInfoSheetSectionsForWarning(Context context) {
        return new InfoSheetSection[] {
                section(context, R.string.sensor_info_section_todo,
                        context.getString(R.string.sensor_info_temperature_warning_1),
                        context.getString(R.string.sensor_info_temperature_warning_2),
                        context.getString(R.string.sensor_info_temperature_warning_3))
        };
    }

    @Override
    public InfoSheetSection[] getInfoSheetSectionsForNormal(Context context) {
        return new InfoSheetSection[] {
                section(context, R.string.sensor_info_section_keep,
                        context.getString(R.string.sensor_info_temperature_normal_1),
                        context.getString(R.string.sensor_info_temperature_normal_2),
                        context.getString(R.string.sensor_info_temperature_normal_3))
        };
    }
}
