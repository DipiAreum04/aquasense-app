package ca.team6.aquasense.dashboard;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.aquarium_sensors.AquariumSensor;
import ca.team6.aquasense.model.aquarium_sensors.SensorStatus;
import ca.team6.aquasense.model.InfoSheetSection;
import ca.team6.aquasense.model.ScopedLogger;

public class SensorInfoBottomSheet extends BottomSheetDialogFragment {
    private static final String ICON_KEY = "iconResourceId";
    private static final String TITLE_KEY = "titleResourceId";
    private static final String NAME_KEY = "nameResourceId";
    private static final String ABOUT_KEY = "aboutResourceId";
    private static final String NOTE_KEY = "noteResourceId";
    private static final String VALUE_KEY = "value";
    private static final String UNIT_KEY = "unitResourceId";
    private static final String VALUE_COLOR_KEY = "valueColorResourceId";
    private static final String STATUS_ICON_KEY = "statusIconResourceId";
    private static final String STATUS_TEXT_KEY = "statusTextResourceId";
    private static final String STATUS_COLOR_KEY = "statusColorResourceId";
    private static final String PILL_COLOR_KEY = "pillColorResourceId";
    private static final String BANDS_KEY = "bandTexts";
    private static final String ACTIVE_BAND_KEY = "activeBand";
    private static final String SECTIONS_KEY = "sectionsResourceId";
    private static final String ABOUT_TITLE_KEY = "aboutTitleResourceId";
    private static final String HAS_READING_KEY = "hasReading";

    private static final int SAFE_BAND = 0;
    private static final int WARNING_BAND = 1;
    private static final int CRITICAL_BAND = 2;
    private static final int NO_ACTIVE_BAND = -1;

    private static final float MAX_HEIGHT_FRACTION = 0.85f;
    private static final float ACTIVE_BAND_STROKE_DP = 1.5f;

    public SensorInfoBottomSheet() {}

    public static SensorInfoBottomSheet from(Context context, AquariumSensor sensor) {
        SensorInfoBottomSheet fragment = new SensorInfoBottomSheet();

        Bundle args = new Bundle();
        args.putInt(ICON_KEY, sensor.getTitleIconResId());
        args.putInt(TITLE_KEY, sensor.getInfoSheetTitleResId());
        args.putInt(NAME_KEY, sensor.getNameResId());
        args.putInt(ABOUT_KEY, sensor.getInfoSheetAboutResId());
        args.putInt(NOTE_KEY, sensor.getInfoSheetStatusNoteResId());
        args.putString(VALUE_KEY, sensor.getValue());
        args.putInt(UNIT_KEY, sensor.getUnitResId());
        args.putInt(VALUE_COLOR_KEY, sensor.getValueColorResId());
        args.putInt(STATUS_ICON_KEY, sensor.getStatusIconResId());
        args.putInt(STATUS_TEXT_KEY, sensor.getStatusTextResId());
        args.putInt(STATUS_COLOR_KEY, sensor.getStatusColorResId());
        args.putInt(PILL_COLOR_KEY, sensor.getStatusPillBackgroundColorResId());
        args.putStringArray(BANDS_KEY, sensor.getInfoSheetBandTexts(context));
        args.putInt(ACTIVE_BAND_KEY, activeBand(sensor.getSensorStatus()));
        args.putByteArray(
                SECTIONS_KEY,
                InfoSheetSection.serialize(sensor.getInfoSheetSections(context))
        );
        args.putBoolean(HAS_READING_KEY, true);
        fragment.setArguments(args);

        return fragment;
    }

    public static SensorInfoBottomSheet explaining(@DrawableRes int iconResId,
                                                   @StringRes int titleResId,
                                                   @StringRes int aboutTitleResId,
                                                   @StringRes int aboutResId,
                                                   InfoSheetSection... sections) {
        SensorInfoBottomSheet fragment = new SensorInfoBottomSheet();

        Bundle args = new Bundle();
        args.putInt(ICON_KEY, iconResId);
        args.putInt(TITLE_KEY, titleResId);
        args.putInt(ABOUT_TITLE_KEY, aboutTitleResId);
        args.putInt(ABOUT_KEY, aboutResId);
        args.putInt(STATUS_COLOR_KEY, R.color.accent);
        args.putBoolean(HAS_READING_KEY, false);
        args.putByteArray(SECTIONS_KEY, InfoSheetSection.serialize(sections));
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

        Context context = view.getContext();
        boolean hasReading = arguments.getBoolean(HAS_READING_KEY, true);
        int statusColor = ContextCompat.getColor(context, arguments.getInt(STATUS_COLOR_KEY));

        ImageView iconView = view.findViewById(R.id.sensorInfoIcon);
        iconView.setImageResource(arguments.getInt(ICON_KEY));
        iconView.setImageTintList(ColorStateList.valueOf(statusColor));

        TextView titleView = view.findViewById(R.id.sensorInfoTitle);
        titleView.setText(arguments.getInt(TITLE_KEY));

        View statusPill = view.findViewById(R.id.sensorInfoStatusPill);
        View readingBlock = view.findViewById(R.id.sensorInfoReadingBlock);
        statusPill.setVisibility(hasReading ? View.VISIBLE : View.GONE);
        readingBlock.setVisibility(hasReading ? View.VISIBLE : View.GONE);

        TextView aboutTitleView = view.findViewById(R.id.sensorInfoAboutTitle);
        if (hasReading) {
            String name = context.getString(arguments.getInt(NAME_KEY));

            ((ImageView) view.findViewById(R.id.sensorInfoStatusIcon))
                    .setImageResource(arguments.getInt(STATUS_ICON_KEY));
            TextView statusTextView = view.findViewById(R.id.sensorInfoStatusText);
            statusTextView.setText(arguments.getInt(STATUS_TEXT_KEY));
            statusTextView.setTextColor(statusColor);
            statusPill.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(context, arguments.getInt(PILL_COLOR_KEY))));

            int valueColor = ContextCompat.getColor(context, arguments.getInt(VALUE_COLOR_KEY));
            TextView valueView = view.findViewById(R.id.sensorInfoValue);
            valueView.setText(arguments.getString(VALUE_KEY));
            valueView.setTextColor(valueColor);
            TextView unitView = view.findViewById(R.id.sensorInfoUnit);
            unitView.setText(arguments.getInt(UNIT_KEY));
            unitView.setTextColor(valueColor);

            ((TextView) view.findViewById(R.id.sensorInfoReadingLabel))
                    .setText(context.getString(R.string.sensor_info_current, name));
            ((TextView) view.findViewById(R.id.sensorInfoStatusNote))
                    .setText(arguments.getInt(NOTE_KEY));
            aboutTitleView.setText(context.getString(R.string.sensor_info_about, name));
        } else {
            aboutTitleView.setText(arguments.getInt(ABOUT_TITLE_KEY));
            ((ViewGroup.MarginLayoutParams) aboutTitleView.getLayoutParams()).topMargin = 0;
        }

        ((TextView) view.findViewById(R.id.sensorInfoDescription))
                .setText(arguments.getInt(ABOUT_KEY));

        this.bindBands(
                view,
                arguments.getStringArray(BANDS_KEY),
                arguments.getInt(ACTIVE_BAND_KEY, NO_ACTIVE_BAND)
        );

        view.findViewById(R.id.sensorInfoClose).setOnClickListener(v -> dismiss());
        view.findViewById(R.id.sensorInfoDismiss).setOnClickListener(v -> dismiss());

        LinearLayout bodyView = view.findViewById(R.id.sensorInfoBody);
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

    private static int activeBand(SensorStatus status) {
        switch (status) {
            case NORMAL:
                return SAFE_BAND;
            case WARNING:
                return WARNING_BAND;
            case CRITICAL:
                return CRITICAL_BAND;
            default:
                return NO_ACTIVE_BAND;
        }
    }

    private void bindBands(View view, @Nullable String[] bands, int activeBand) {
        View ranges = view.findViewById(R.id.sensorInfoRanges);
        if (bands == null) {
            ranges.setVisibility(View.GONE);
            return;
        }

        this.bindBandRow(view.findViewById(R.id.sensorInfoRangeSafe),
                R.drawable.blue_circle_24, R.string.sensor_info_band_safe,
                R.color.status_blue, R.color.status_blue_soft,
                bands[SAFE_BAND], activeBand == SAFE_BAND);
        this.bindBandRow(view.findViewById(R.id.sensorInfoRangeWarning),
                R.drawable.orange_circle_24, R.string.sensor_info_band_warning,
                R.color.status_orange, R.color.status_orange_soft,
                bands[WARNING_BAND], activeBand == WARNING_BAND);
        this.bindBandRow(view.findViewById(R.id.sensorInfoRangeCritical),
                R.drawable.red_circle_24, R.string.sensor_info_band_critical,
                R.color.status_red, R.color.status_red_soft,
                bands[CRITICAL_BAND], activeBand == CRITICAL_BAND);
    }

    private void bindBandRow(
            View row,
            @DrawableRes int dotResId,
            @StringRes int labelResId,
            @ColorRes int colorResId,
            @ColorRes int fillColorResId,
            String range,
            boolean active
    ) {
        Context context = row.getContext();
        int color = ContextCompat.getColor(context, colorResId);

        GradientDrawable background = new GradientDrawable();
        background.setCornerRadius(
                context.getResources().getDimension(R.dimen.sensor_info_row_corner_radius));
        background.setColor(ContextCompat.getColor(context, fillColorResId));
        if (active) {
            background.setStroke(Math.round(
                    ACTIVE_BAND_STROKE_DP * context.getResources().getDisplayMetrics().density),
                    color);
        }
        row.setBackground(background);

        ((ImageView) row.findViewById(R.id.rangeDot)).setImageResource(dotResId);
        ((TextView) row.findViewById(R.id.rangeLabel)).setText(labelResId);
        TextView rangeValue = row.findViewById(R.id.rangeValue);
        rangeValue.setText(range);
        rangeValue.setTextColor(color);
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        if (!(dialog instanceof BottomSheetDialog)) {
            return;
        }

        View sheet = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (sheet == null) {
            return;
        }

        BottomSheetBehavior<View> behavior = BottomSheetBehavior.from(sheet);
        behavior.setShouldRemoveExpandedCorners(false);
        behavior.setMaxHeight(Math.round(
                getResources().getDisplayMetrics().heightPixels * MAX_HEIGHT_FRACTION));
        behavior.setSkipCollapsed(true);
        behavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }
}
