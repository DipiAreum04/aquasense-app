package ca.team6.aquasense.model.aquarium_sensors;

import android.content.Context;

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
     * The sensor sits on the outside of the glass and reads through it, reporting 1 while it senses
     * water at its own height and 0 once it does not. A zero is therefore not a low reading to be
     * measured against a band: it is the water line already past the point the sensor is stuck at,
     * which is critical whether or not this aquarium configures a water level band.
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
    public int getInfoSheetAboutResId() {
        return R.string.sensor_info_water_level_about;
    }

    /**
     * This sensor has two states rather than a range, so the shared note, which places a reading
     * against the aquarium's band, has nothing to say about it. This one says whether there is
     * water at the sensor's height instead.
     */
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

    /**
     * The card grades this sensor by whether it senses water rather than against a band, so listing
     * bands would name numbers the reading is never measured against.
     */
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

    /**
     * Only reachable when this aquarium has a band configured for a sensor that reports 0 or 1, so
     * the copy points at the thresholds rather than at the tank.
     */
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
