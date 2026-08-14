package ca.team6.aquasense.aquarium.sensors;

import android.content.Context;

import ca.team6.aquasense.R;
import ca.team6.aquasense.firebase.DatabaseSchema;
import ca.team6.aquasense.aquarium.InfoSheetSection;

public class DissolvedSolidsSensor extends AquariumSensor {
    public DissolvedSolidsSensor() {
        super();
        this.setUnitResId(R.string.unit_parts_per_million);
    }

    @Override
    public String getId() {
        return DatabaseSchema.DISSOLVED_SOLIDS_KEY;
    }

    @Override
    public int getTitleIconResId() {
        return R.drawable.total_dissolved_solids_24px;
    }

    @Override
    public int getNameResId() {
        return R.string.dissolved_solids;
    }

    @Override
    public int getInfoSheetTitleResId() {
        return R.string.sensor_info_dissolved_solids_title;
    }

    @Override
    public int getInfoSheetAboutResId() {
        return R.string.sensor_info_dissolved_solids_about;
    }

    @Override
    public InfoSheetSection[] getInfoSheetSectionsForDisconnected(Context context) {
        return new InfoSheetSection[] {
                section(context, R.string.sensor_info_section_reconnect,
                        context.getString(R.string.sensor_info_dissolved_solids_disconnected_1),
                        context.getString(R.string.sensor_info_dissolved_solids_disconnected_2),
                        context.getString(R.string.sensor_info_dissolved_solids_disconnected_3),
                        context.getString(R.string.sensor_info_dissolved_solids_disconnected_4))
        };
    }

    @Override
    public InfoSheetSection[] getInfoSheetSectionsForCritical(Context context) {
        return new InfoSheetSection[] {
                section(context, R.string.sensor_info_section_act,
                        context.getString(R.string.sensor_info_dissolved_solids_critical_1),
                        context.getString(R.string.sensor_info_dissolved_solids_critical_2),
                        context.getString(R.string.sensor_info_dissolved_solids_critical_3))
        };
    }

    @Override
    public InfoSheetSection[] getInfoSheetSectionsForWarning(Context context) {
        return new InfoSheetSection[] {
                section(context, R.string.sensor_info_section_todo,
                        context.getString(R.string.sensor_info_dissolved_solids_warning_1),
                        context.getString(R.string.sensor_info_dissolved_solids_warning_2),
                        context.getString(R.string.sensor_info_dissolved_solids_warning_3))
        };
    }

    @Override
    public InfoSheetSection[] getInfoSheetSectionsForNormal(Context context) {
        return new InfoSheetSection[] {
                section(context, R.string.sensor_info_section_keep,
                        context.getString(R.string.sensor_info_dissolved_solids_normal_1),
                        context.getString(R.string.sensor_info_dissolved_solids_normal_2),
                        context.getString(R.string.sensor_info_dissolved_solids_normal_3))
        };
    }
}
