package ca.team6.aquasense.settings;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import com.google.android.material.tabs.TabLayout;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.Aquarium;
import ca.team6.aquasense.model.ReadingFormatter;
import ca.team6.aquasense.model.ThresholdBand;
import ca.team6.aquasense.model.WaterType;
import ca.team6.aquasense.model.aquarium_templates.AquariumTemplate;
import ca.team6.aquasense.model.aquarium_templates.BuiltInTemplates;
import ca.team6.aquasense.notifications.SensorThresholds;
import ca.team6.aquasense.ui.InputFieldError;

public final class ThresholdEditor {

    public interface Listener {
        void onEditorChanged();
    }

    private static final DecimalFormat FIELD_FORMAT = new DecimalFormat("0.##");

    private static final String STATE_SENSOR_IDS = "thresholdEditor:sensorIds";
    private static final String STATE_SELECTED = "thresholdEditor:selected";
    private static final String STATE_VALUES_PREFIX = "thresholdEditor:values:";
    private static final String STATE_SAVED_PREFIX = "thresholdEditor:saved:";

    private enum Level {
        WARNING_LOW(
                ThresholdForm.Field.WARNING_LOW,
                R.string.threshold_level_warning_low,
                R.string.threshold_desc_warning_low,
                R.string.threshold_badge_critical_below,
                R.color.status_red,
                R.color.status_red_soft,
                R.drawable.ic_arrow_down_24px,
                0),
        SAFE_LOW(
                ThresholdForm.Field.SAFE_LOW,
                R.string.threshold_level_safe_low,
                R.string.threshold_desc_safe_low,
                R.string.threshold_badge_warning_below,
                R.color.status_orange,
                R.color.status_orange_soft,
                R.drawable.ic_arrow_down_24px,
                R.string.threshold_error_above_warning_low),
        SAFE_HIGH(
                ThresholdForm.Field.SAFE_HIGH,
                R.string.threshold_level_safe_high,
                R.string.threshold_desc_safe_high,
                R.string.threshold_badge_warning_above,
                R.color.status_orange,
                R.color.status_orange_soft,
                R.drawable.ic_arrow_up_24px,
                R.string.threshold_error_above_safe_low),
        WARNING_HIGH(
                ThresholdForm.Field.WARNING_HIGH,
                R.string.threshold_level_warning_high,
                R.string.threshold_desc_warning_high,
                R.string.threshold_badge_critical_above,
                R.color.status_red,
                R.color.status_red_soft,
                R.drawable.ic_arrow_up_24px,
                R.string.threshold_error_above_safe_high),
        SPIKE(
                ThresholdForm.Field.SPIKE,
                R.string.threshold_level_spike,
                R.string.threshold_desc_spike,
                0,
                R.color.accent,
                0,
                R.drawable.trending_up_24px,
                0);

        final ThresholdForm.Field field;
        @StringRes final int titleResId;
        @StringRes final int descriptionResId;
        @StringRes final int badgeResId;
        @ColorRes final int accentColorResId;
        @ColorRes final int badgeFillColorResId;
        @DrawableRes final int iconResId;
        @StringRes final int outOfOrderResId;

        Level(ThresholdForm.Field field,
              @StringRes int titleResId,
              @StringRes int descriptionResId,
              @StringRes int badgeResId,
              @ColorRes int accentColorResId,
              @ColorRes int badgeFillColorResId,
              @DrawableRes int iconResId,
              @StringRes int outOfOrderResId) {
            this.field = field;
            this.titleResId = titleResId;
            this.descriptionResId = descriptionResId;
            this.badgeResId = badgeResId;
            this.accentColorResId = accentColorResId;
            this.badgeFillColorResId = badgeFillColorResId;
            this.iconResId = iconResId;
            this.outOfOrderResId = outOfOrderResId;
        }
    }

    private static final class LevelCard {
        final ImageView icon;
        final TextView title;
        final TextView badge;
        final TextView description;
        final View box;
        final TextView prefix;
        final EditText value;
        final TextView unit;
        final TextView error;

        LevelCard(View root) {
            this.icon = root.findViewById(R.id.ivThresholdLevelIcon);
            this.title = root.findViewById(R.id.tvThresholdLevelTitle);
            this.badge = root.findViewById(R.id.tvThresholdLevelBadge);
            this.description = root.findViewById(R.id.tvThresholdLevelDescription);
            this.box = root.findViewById(R.id.boxThresholdLevelValue);
            this.prefix = root.findViewById(R.id.tvThresholdLevelPrefix);
            this.value = root.findViewById(R.id.etThresholdLevelValue);
            this.unit = root.findViewById(R.id.tvThresholdLevelUnit);
            this.error = root.findViewById(R.id.tvThresholdLevelError);
        }
    }

    private static final class Draft {
        final ThresholdForm.Units units;
        final String[] values = new String[Level.values().length];
        final String[] saved = new String[Level.values().length];
        final boolean[] revealed = new boolean[Level.values().length];

        ThresholdForm.Result result;
        @Nullable
        ThresholdBand lastValidBand;

        boolean showingFallback;

        Draft(ThresholdForm.Units units) {
            this.units = units;
        }

        boolean isEdited() {
            return !Arrays.equals(this.values, this.saved);
        }

        boolean isDirty() {
            return this.showingFallback || this.isEdited();
        }

        void markSaved() {
            System.arraycopy(this.values, 0, this.saved, 0, this.values.length);
            this.showingFallback = false;
        }
    }

    public static final class Snapshot {

        private final Map<String, ThresholdBand> thresholds;
        private final Map<String, Double> spikeDeltas;
        private final List<Draft> captured;

        private Snapshot(Map<String, ThresholdBand> thresholds,
                         Map<String, Double> spikeDeltas,
                         List<Draft> captured) {
            this.thresholds = Collections.unmodifiableMap(thresholds);
            this.spikeDeltas = Collections.unmodifiableMap(spikeDeltas);
            this.captured = captured;
        }

        @NonNull
        public Map<String, ThresholdBand> getThresholds() {
            return this.thresholds;
        }

        @NonNull
        public Map<String, Double> getSpikeDeltas() {
            return this.spikeDeltas;
        }

        public boolean isEmpty() {
            return this.thresholds.isEmpty() && this.spikeDeltas.isEmpty();
        }

        public void markSaved() {
            for (Draft draft : this.captured) {
                draft.markSaved();
            }
        }
    }

    private final Context context;
    private final boolean fahrenheit;
    @Nullable
    private final Listener listener;

    private final TabLayout tabs;
    private final TextView tvOverallRangeTitle;
    private final View overallRangeBar;
    private final TextView tvOverallRangeLow;
    private final TextView tvOverallRangeSafe;
    private final TextView tvOverallRangeHigh;
    private final TextView tvBlocked;

    private final List<LevelCard> cards = new ArrayList<>();
    private final List<String> sensorIds = new ArrayList<>();
    private final Map<String, Draft> drafts = new LinkedHashMap<>();
    @Nullable
    private String selectedSensorId;

    private boolean binding;

    public ThresholdEditor(@NonNull View editorRoot, @Nullable Listener listener) {
        this.context = editorRoot.getContext();
        this.fahrenheit = ReadingFormatter.isFahrenheit(this.context);
        this.listener = listener;

        this.tabs = editorRoot.findViewById(R.id.tabsWaterParameterSensor);
        this.tvOverallRangeTitle = editorRoot.findViewById(R.id.tvOverallRangeTitle);
        this.overallRangeBar = editorRoot.findViewById(R.id.overallRangeBar);
        this.tvOverallRangeLow = editorRoot.findViewById(R.id.tvOverallRangeLow);
        this.tvOverallRangeSafe = editorRoot.findViewById(R.id.tvOverallRangeSafe);
        this.tvOverallRangeHigh = editorRoot.findViewById(R.id.tvOverallRangeHigh);
        this.tvBlocked = editorRoot.findViewById(R.id.tvWaterParametersBlocked);

        this.inflateLevelCards(
                LayoutInflater.from(this.context),
                editorRoot.findViewById(R.id.containerThresholdLevels),
                editorRoot.findViewById(R.id.containerSpikeLevel));
    }

    public void showAquarium(@NonNull Aquarium aquarium) {
        AquariumTemplate fallback =
                BuiltInTemplates.forWaterType(WaterType.fromKey(aquarium.getWaterType()));

        this.reset();
        for (String sensorId : ThresholdForm.CONFIGURABLE_SENSOR_IDS) {
            if (!aquarium.isSensorApplicable(sensorId)) {
                continue;
            }
            this.sensorIds.add(sensorId);
            this.drafts.put(sensorId, this.draftFor(aquarium, fallback, sensorId));
        }
        this.buildTabs();
    }

    public void showTemplate(@NonNull AquariumTemplate template) {
        this.reset();
        for (String sensorId : ThresholdForm.CONFIGURABLE_SENSOR_IDS) {
            if (!template.isSensorApplicable(sensorId)) {
                continue;
            }
            this.sensorIds.add(sensorId);
            this.drafts.put(sensorId, this.draftFor(null, template, sensorId));
        }
        this.buildTabs();
    }

    public void clear() {
        this.reset();
        this.buildTabs();
        this.showBlockingTab();
    }

    @NonNull
    public List<String> getSensorIds() {
        return Collections.unmodifiableList(this.sensorIds);
    }

    public boolean hasSensors() {
        return !this.sensorIds.isEmpty();
    }

    public boolean isValid() {
        return this.firstInvalidSensorId() == null;
    }

    public boolean isDirty() {
        for (Draft draft : this.drafts.values()) {
            if (draft.isDirty()) {
                return true;
            }
        }
        return false;
    }

    public boolean hasUnsavedEdits() {
        for (Draft draft : this.drafts.values()) {
            if (draft.isEdited()) {
                return true;
            }
        }
        return false;
    }

    @Nullable
    public String firstInvalidSensorId() {
        for (Map.Entry<String, Draft> entry : this.drafts.entrySet()) {
            if (!entry.getValue().result.isValid()) {
                return entry.getKey();
            }
        }
        return null;
    }

    @NonNull
    public Snapshot snapshotAll() {
        return this.snapshot(false);
    }

    @NonNull
    public Snapshot snapshotDirty() {
        return this.snapshot(true);
    }

    @NonNull
    private Snapshot snapshot(boolean dirtyOnly) {
        Map<String, ThresholdBand> bands = new LinkedHashMap<>();
        Map<String, Double> spikeDeltas = new LinkedHashMap<>();
        List<Draft> captured = new ArrayList<>();
        if (!this.isValid()) {
            return new Snapshot(bands, spikeDeltas, captured);
        }

        for (Map.Entry<String, Draft> entry : this.drafts.entrySet()) {
            Draft draft = entry.getValue();
            ThresholdBand band = draft.result.getBand();
            if (band == null || (dirtyOnly && !draft.isDirty())) {
                continue;
            }
            bands.put(entry.getKey(), band);
            spikeDeltas.put(entry.getKey(), draft.result.getSpikeDelta());
            captured.add(draft);
        }
        return new Snapshot(bands, spikeDeltas, captured);
    }

    public void showBlockingTab() {
        String firstInvalid = this.firstInvalidSensorId();
        if (firstInvalid == null || firstInvalid.equals(this.selectedSensorId)) {
            this.tvBlocked.setVisibility(View.GONE);
            return;
        }
        this.tvBlocked.setText(this.context.getString(R.string.threshold_blocked_by_tab,
                this.context.getString(ReadingFormatter.nameResIdFor(firstInvalid))));
        this.tvBlocked.setVisibility(View.VISIBLE);
    }

    public void saveState(@NonNull Bundle outState) {
        outState.putStringArrayList(STATE_SENSOR_IDS, new ArrayList<>(this.sensorIds));
        outState.putString(STATE_SELECTED, this.selectedSensorId);
        for (Map.Entry<String, Draft> entry : this.drafts.entrySet()) {
            Draft draft = entry.getValue();
            outState.putStringArray(STATE_VALUES_PREFIX + entry.getKey(), draft.values);
            outState.putStringArray(STATE_SAVED_PREFIX + entry.getKey(), draft.saved);
        }
    }

    public void restoreState(@Nullable Bundle savedState) {
        if (savedState == null) {
            return;
        }
        List<String> sensors = savedState.getStringArrayList(STATE_SENSOR_IDS);
        if (sensors == null || this.sensorIds.isEmpty() || !sensors.equals(this.sensorIds)) {
            return;
        }

        for (Map.Entry<String, Draft> entry : this.drafts.entrySet()) {
            Draft draft = entry.getValue();
            String[] values = savedState.getStringArray(STATE_VALUES_PREFIX + entry.getKey());
            String[] saved = savedState.getStringArray(STATE_SAVED_PREFIX + entry.getKey());
            if (values == null || saved == null
                    || values.length != draft.values.length
                    || saved.length != draft.saved.length) {
                continue;
            }
            System.arraycopy(values, 0, draft.values, 0, values.length);
            System.arraycopy(saved, 0, draft.saved, 0, saved.length);
            draft.result = validate(draft);
        }

        String selected = savedState.getString(STATE_SELECTED);
        int index = selected == null ? -1 : this.sensorIds.indexOf(selected);
        TabLayout.Tab tab = index < 0 ? null : this.tabs.getTabAt(index);
        if (tab != null) {
            tab.select();
        } else {
            this.bindSensor(this.sensorIds.get(0));
        }
    }

    private void reset() {
        this.sensorIds.clear();
        this.drafts.clear();
        this.selectedSensorId = null;
    }

    private void inflateLevelCards(LayoutInflater inflater,
                                   ViewGroup boundsContainer,
                                   ViewGroup spikeContainer) {
        for (Level level : Level.values()) {
            ViewGroup container = level == Level.SPIKE ? spikeContainer : boundsContainer;
            View card = inflater.inflate(R.layout.item_threshold_level, container, false);
            LevelCard views = new LevelCard(card);
            this.bindLevelChrome(views, level);
            this.watchField(views, level);
            container.addView(card);
            this.cards.add(views);
        }
    }

    private void bindLevelChrome(LevelCard card, Level level) {
        int accent = ContextCompat.getColor(this.context, level.accentColorResId);

        card.icon.setImageResource(level.iconResId);
        ImageViewCompat.setImageTintList(card.icon, ColorStateList.valueOf(accent));
        card.title.setText(level.titleResId);

        if (level.badgeResId == 0) {
            card.badge.setVisibility(View.GONE);
        } else {
            card.badge.setText(level.badgeResId);
            card.badge.setTextColor(accent);
            card.badge.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this.context, level.badgeFillColorResId)));
        }

        if (level == Level.SPIKE) {
            card.prefix.setText(R.string.threshold_spike_prefix);
            card.prefix.setVisibility(View.VISIBLE);
        }
    }

    private void watchField(LevelCard card, Level level) {
        card.value.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                onFieldEdited();
            }
        });

        card.value.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus || this.selectedSensorId == null) {
                return;
            }
            Draft draft = this.drafts.get(this.selectedSensorId);
            if (draft == null) {
                return;
            }
            draft.revealed[level.ordinal()] = true;
            this.showProblems(draft);
        });
    }

    private Draft draftFor(@Nullable Aquarium aquarium,
                           @NonNull AquariumTemplate template,
                           @NonNull String sensorId) {
        ThresholdForm.Units units = ThresholdForm.unitsFor(sensorId, this.fahrenheit);
        Draft draft = new Draft(units);

        ThresholdBand stored = aquarium == null ? null : aquarium.thresholdFor(sensorId);
        ThresholdBand band = stored == null ? template.getThresholds(sensorId) : stored;

        if (band != null) {
            draft.values[Level.WARNING_LOW.ordinal()] = format(units.toDisplay(band.getWarnLow()));
            draft.values[Level.SAFE_LOW.ordinal()] = format(units.toDisplay(band.getSafeLow()));
            draft.values[Level.SAFE_HIGH.ordinal()] = format(units.toDisplay(band.getSafeHigh()));
            draft.values[Level.WARNING_HIGH.ordinal()] = format(units.toDisplay(band.getWarnHigh()));
            draft.lastValidBand = band;
        } else {
            Arrays.fill(draft.values, "");
        }

        double spikeDelta = SensorThresholds.resolveSpikeDelta(aquarium, sensorId);
        draft.values[Level.SPIKE.ordinal()] = Double.isNaN(spikeDelta)
                ? ""
                : format(units.deltaToDisplay(spikeDelta));

        draft.markSaved();
        draft.showingFallback = aquarium == null
                || stored == null
                || aquarium.spikeDeltaFor(sensorId) == null;
        draft.result = validate(draft);
        return draft;
    }

    private void buildTabs() {
        this.tabs.clearOnTabSelectedListeners();
        this.tabs.removeAllTabs();
        if (this.sensorIds.isEmpty()) {
            return;
        }

        for (String sensorId : this.sensorIds) {
            TabLayout.Tab tab = this.tabs.newTab();
            tab.setText(ReadingFormatter.nameResIdFor(sensorId));
            tab.setTag(sensorId);
            this.tabs.addTab(tab);
        }

        this.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                Object sensorId = tab.getTag();
                if (sensorId instanceof String) {
                    bindSensor((String) sensorId);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        this.bindSensor(this.sensorIds.get(0));
    }

    private void bindSensor(String sensorId) {
        Draft draft = this.drafts.get(sensorId);
        if (draft == null) {
            return;
        }
        this.selectedSensorId = sensorId;

        String sensorName = this.context.getString(ReadingFormatter.nameResIdFor(sensorId));
        String unit = this.context
                .getString(ReadingFormatter.unitResIdFor(sensorId, this.fahrenheit)).trim();

        this.tvOverallRangeTitle.setText(unit.isEmpty()
                ? this.context.getString(R.string.threshold_overall_range)
                : this.context.getString(R.string.template_parameter_with_unit,
                        this.context.getString(R.string.threshold_overall_range), unit));

        this.binding = true;
        for (Level level : Level.values()) {
            LevelCard card = this.cards.get(level.ordinal());
            card.description.setText(this.context.getString(level.descriptionResId, sensorName));
            card.unit.setText(unit);
            card.value.setText(draft.values[level.ordinal()]);
        }
        this.binding = false;

        this.showProblems(draft);
        this.showOverallRange(draft);
        this.notifyChanged();
    }

    private void onFieldEdited() {
        if (this.binding || this.selectedSensorId == null) {
            return;
        }
        Draft draft = this.drafts.get(this.selectedSensorId);
        if (draft == null) {
            return;
        }

        for (Level level : Level.values()) {
            draft.values[level.ordinal()] =
                    this.cards.get(level.ordinal()).value.getText().toString();
        }
        draft.result = validate(draft);

        this.showProblems(draft);
        this.showOverallRange(draft);
        this.notifyChanged();
    }

    private void notifyChanged() {
        this.showBlockingTab();
        if (this.listener != null) {
            this.listener.onEditorChanged();
        }
    }

    private static ThresholdForm.Result validate(Draft draft) {
        return ThresholdForm.validate(
                draft.values[Level.WARNING_LOW.ordinal()],
                draft.values[Level.SAFE_LOW.ordinal()],
                draft.values[Level.SAFE_HIGH.ordinal()],
                draft.values[Level.WARNING_HIGH.ordinal()],
                draft.values[Level.SPIKE.ordinal()],
                draft.units);
    }

    private void showProblems(Draft draft) {
        for (Level level : Level.values()) {
            LevelCard card = this.cards.get(level.ordinal());
            ThresholdForm.Problem problem = draft.result.problemFor(level.field);

            if (problem == null) {
                draft.revealed[level.ordinal()] = false;
            }
            if (problem == null || !draft.revealed[level.ordinal()]) {
                InputFieldError.set(card.box, InputFieldError.State.NEUTRAL);
                card.error.setVisibility(View.GONE);
                continue;
            }

            InputFieldError.set(card.box, InputFieldError.State.ERROR);
            card.error.setText(messageFor(level, problem));
            card.error.setVisibility(View.VISIBLE);
        }
    }

    @StringRes
    private static int messageFor(Level level, ThresholdForm.Problem problem) {
        switch (problem) {
            case NOT_ABOVE_PREVIOUS:
                return level.outOfOrderResId != 0
                        ? level.outOfOrderResId
                        : R.string.threshold_error_number;
            case NOT_POSITIVE:
                return R.string.threshold_error_spike_positive;
            default:
                return R.string.threshold_error_number;
        }
    }

    private void showOverallRange(Draft draft) {
        ThresholdBand band = draft.result.isValid() ? draft.result.getBand() : draft.lastValidBand;
        if (band == null) {
            this.tvOverallRangeLow.setText("");
            this.tvOverallRangeSafe.setText("");
            this.tvOverallRangeHigh.setText("");
            return;
        }
        draft.lastValidBand = band;

        ThresholdBandBar.apply(this.overallRangeBar, band);

        this.tvOverallRangeLow.setText(this.context.getString(R.string.template_bound_below,
                format(draft.units.toDisplay(band.getWarnLow()))));
        this.tvOverallRangeSafe.setText(this.context.getString(R.string.template_band_range,
                format(draft.units.toDisplay(band.getSafeLow())),
                format(draft.units.toDisplay(band.getSafeHigh()))));
        this.tvOverallRangeHigh.setText(this.context.getString(R.string.template_bound_above,
                format(draft.units.toDisplay(band.getWarnHigh()))));
    }

    private static String format(double value) {
        return FIELD_FORMAT.format(value);
    }
}
