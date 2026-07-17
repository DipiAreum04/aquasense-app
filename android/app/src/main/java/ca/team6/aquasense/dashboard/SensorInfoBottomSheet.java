package ca.team6.aquasense.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.aquarium_sensors.AquariumSensor;
import ca.team6.aquasense.model.InfoSheetSection;
import ca.team6.aquasense.model.ScopedLogger;

public class SensorInfoBottomSheet extends BottomSheetDialogFragment {
    private static final String ICON_KEY = "iconResourceId";
    private static final String TITLE_KEY = "titleResourceId";
    private static final String DESCRIPTION_KEY = "descResourceId";
    private static final String SECTIONS_KEY = "sectionsResourceId";

    public SensorInfoBottomSheet() {}

    public static SensorInfoBottomSheet from(AquariumSensor sensor) {
        SensorInfoBottomSheet fragment = new SensorInfoBottomSheet();

        Bundle args = new Bundle();
        args.putInt(ICON_KEY, sensor.getTitleIconResId());
        args.putInt(TITLE_KEY, sensor.getInfoSheetTitleResId());
        args.putInt(DESCRIPTION_KEY, sensor.getInfoSheetDescResId());
        args.putByteArray(
                SECTIONS_KEY,
                InfoSheetSection.serialize(sensor.getInfoSheetSections())
        );
        fragment.setArguments(args);

        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(
            LayoutInflater inflater,
            ViewGroup container,
            Bundle savedInstanceState
    ) {
        View view = inflater.inflate(
                R.layout.sensor_info,
                container,
                false
        );

        Bundle arguments = getArguments();
        if (arguments == null) {
            ScopedLogger.error("Bundle arguments are null.");
            return null;
        }

        ImageView iconView = view.findViewById(R.id.sensorInfoIcon);
        TextView titleView = view.findViewById(R.id.sensorInfoTitle);
        TextView descriptionView = view.findViewById(R.id.sensorInfoDescription);
        LinearLayout bodyView = view.findViewById(R.id.sensorInfoBody);

        iconView.setImageResource(arguments.getInt(ICON_KEY));
        titleView.setText(arguments.getInt(TITLE_KEY));
        descriptionView.setText(arguments.getInt(DESCRIPTION_KEY));

        bodyView.removeAllViews();
        InfoSheetSection[] infoSheetSections = InfoSheetSection.deserialize(
                arguments.getByteArray(SECTIONS_KEY),
                InfoSheetSection[].class
        );

        if (infoSheetSections == null) {
            return view;
        }

        for (InfoSheetSection section : infoSheetSections) {
            section.applyTo(this, bodyView);
        }

        return view;
    }
}
