package ca.team6.aquasense.settings;

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
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.tabs.TabLayout;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ca.team6.aquasense.R;
import ca.team6.aquasense.analytics.AquariumDropdown;
import ca.team6.aquasense.model.Aquarium;
import ca.team6.aquasense.model.AquariumRepository;
import ca.team6.aquasense.model.ReadingFormatter;
import ca.team6.aquasense.model.ThresholdBand;
import ca.team6.aquasense.model.WaterType;
import ca.team6.aquasense.model.aquarium_templates.AquariumTemplate;
import ca.team6.aquasense.model.aquarium_templates.BuiltInTemplates;
import ca.team6.aquasense.notifications.SensorThresholds;
import ca.team6.aquasense.ui.InputFieldError;

/**
 * SETTINGS-07 - lets the user set the thresholds their aquarium is judged against.
 *
 * <p>The numbers edited here are the same ones the dashboard colours its cards by, the analytics
 * page counts in-range readings against and the background monitor raises alerts from, so nothing
 * is stored locally: a save writes straight to {@code /{uid}/aquariums/{id}} and the live
 * subscription carries it back to every one of those screens.
 *
 * <p>One sensor is edited at a time. Its three tabs share a save, so what is typed on each survives
 * flipping between them and one press writes every sensor that changed. Switching <em>aquarium</em>
 * is the opposite: the form is rebuilt from the database, and anything unsaved is offered up for
 * discarding rather than carried across to a tank it was not typed for.
 */
public class WaterParametersFragment extends Fragment {

    // Two decimals is past what any of these sensors resolve, so this only ever trims trailing
    // zeroes - it is here to keep a converted Fahrenheit bound from arriving as 71.60000000000001.
    private static final DecimalFormat FIELD_FORMAT = new DecimalFormat("0.##");

    private static final float DISABLED_BUTTON_ALPHA = 0.5f;

    /**
     * The five inputs, in the order they are stacked. Each is the same layout with different words,
     * so what changes between them lives here rather than in five near-identical blocks of code.
     *
     * <p>The four bounds run low to high, matching the bar above them, which reads left to right.
     * Each is named after the band it opens - the same name the database column carries - while its
     * badge names what crossing it outward costs. That badge is where critical is declared: it is
     * never typed in, only implied by where the two warning bounds sit.
     *
     * <p>Colours run red, orange, orange, red from top to bottom, which is the bar's own ramp read
     * from the outside in.
     */
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
        /**
         * A distance rather than a place on the scale, and the only input that is not a band edge.
         * It says so three ways at once: its own section, the app's accent instead of a severity
         * colour, and no badge, because there is no band to cross and so nothing for one to name.
         */
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
        /** Names the band crossing this bound outward puts you in. Zero for the spike. */
        @StringRes final int badgeResId;
        @ColorRes final int accentColorResId;
        @ColorRes final int badgeFillColorResId;
        @DrawableRes final int iconResId;
        /**
         * What to say when this bound is not strictly above the one below it, named after that
         * bound so the message points at the field to compare it with. Zero for the two inputs
         * with nothing beneath them: Warning Low is the floor, and a spike is a width.
         */
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

    /** The views of one inflated card, held so binding a tab does not search the tree again. */
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

    /**
     * One sensor's in-progress edit. Holds what is typed rather than what it parses to, so a field
     * halfway through being retyped survives a tab change intact.
     */
    private static final class Draft {
        final ThresholdForm.Units units;
        final String[] values = new String[Level.values().length];
        /** What the database holds, formatted the same way, so an edit is a string comparison. */
        final String[] saved = new String[Level.values().length];
        /**
         * Whether each field's problem has been revealed. A field is not marked wrong while it is
         * still being typed into - only once the user leaves it.
         */
        final boolean[] revealed = new boolean[Level.values().length];

        ThresholdForm.Result result;
        /**
         * The last set of bounds that made sense, so the bar keeps its shape while a field is
         * momentarily blank mid-edit instead of collapsing and springing back.
         */
        @Nullable
        ThresholdBand lastValidBand;

        /**
         * True while some of what is shown was filled in from a fallback rather than read from the
         * database. What is on screen is then not what the aquarium is actually being graded
         * against, so the save has to be live even before anything is touched - otherwise the one
         * thing this screen exists to fix would be the one thing it cannot.
         */
        boolean showingFallback;

        Draft(ThresholdForm.Units units) {
            this.units = units;
        }

        /** Whether the user themselves changed anything, which is what is at risk of being lost. */
        boolean isEdited() {
            return !Arrays.equals(this.values, this.saved);
        }

        /** Whether there is anything worth writing, which a fallback counts towards and an edit is. */
        boolean isDirty() {
            return this.showingFallback || this.isEdited();
        }

        void markSaved() {
            System.arraycopy(this.values, 0, this.saved, 0, this.values.length);
            this.showingFallback = false;
        }
    }

    private AquariumRepository aquariums;
    @Nullable
    private AquariumRepository.AquariumsObserver observer;
    @Nullable
    private Aquarium aquarium;
    private boolean fahrenheit;

    private final List<String> sensorIds = new ArrayList<>();
    private final Map<String, Draft> drafts = new LinkedHashMap<>();
    @Nullable
    private String selectedSensorId;

    private final List<LevelCard> cards = new ArrayList<>();

    private ImageView ivAquariumIcon;
    private TextView tvAquariumName;
    private TextView tvEmpty;
    private View form;
    private TabLayout tabs;
    private TextView tvOverallRangeTitle;
    private View overallRangeBar;
    private TextView tvOverallRangeLow;
    private TextView tvOverallRangeSafe;
    private TextView tvOverallRangeHigh;
    private View cardTdsNote;
    private TextView tvTdsNote;
    private TextView tvBlocked;
    private View btnSave;

    /** True while fields are being filled from a draft, so the watchers ignore their own writes. */
    private boolean binding;
    private boolean saving;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_water_parameters, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.aquariums = AquariumRepository.getInstance(requireContext());
        this.fahrenheit = ReadingFormatter.isFahrenheit(requireContext());

        this.ivAquariumIcon = view.findViewById(R.id.waterParametersAquariumIcon);
        this.tvAquariumName = view.findViewById(R.id.waterParametersAquariumName);
        this.tvEmpty = view.findViewById(R.id.tvWaterParametersEmpty);
        this.form = view.findViewById(R.id.containerWaterParametersForm);
        this.tabs = view.findViewById(R.id.tabsWaterParameterSensor);
        this.tvOverallRangeTitle = view.findViewById(R.id.tvOverallRangeTitle);
        this.overallRangeBar = view.findViewById(R.id.overallRangeBar);
        this.tvOverallRangeLow = view.findViewById(R.id.tvOverallRangeLow);
        this.tvOverallRangeSafe = view.findViewById(R.id.tvOverallRangeSafe);
        this.tvOverallRangeHigh = view.findViewById(R.id.tvOverallRangeHigh);
        this.cardTdsNote = view.findViewById(R.id.cardWaterParametersTdsNote);
        this.tvTdsNote = view.findViewById(R.id.tvWaterParametersTdsNote);
        this.tvBlocked = view.findViewById(R.id.tvWaterParametersBlocked);
        this.btnSave = view.findViewById(R.id.btnSaveThresholds);

        this.inflateLevelCards(
                LayoutInflater.from(view.getContext()),
                view.findViewById(R.id.containerThresholdLevels),
                view.findViewById(R.id.containerSpikeLevel));

        this.setUpAquariumSelector(view);
        this.btnSave.setOnClickListener(v -> this.save());

        // Fires immediately with whatever the cache holds, and again when the first snapshot lands
        // for a user who opened settings straight after signing in.
        this.observer = list -> this.bindFirstAquarium();
        this.aquariums.addObserver(this.observer);
    }

    @Override
    public void onDestroyView() {
        this.detachObserver();
        this.cards.clear();
        super.onDestroyView();
    }

    private void inflateLevelCards(LayoutInflater inflater,
                                   ViewGroup boundsContainer,
                                   ViewGroup spikeContainer) {
        for (Level level : Level.values()) {
            ViewGroup container =
                    level == Level.SPIKE ? spikeContainer : boundsContainer;
            View card = inflater.inflate(R.layout.item_threshold_level, container, false);
            LevelCard views = new LevelCard(card);
            this.bindLevelChrome(views, level);
            this.watchField(views, level);
            container.addView(card);
            this.cards.add(views);
        }
    }

    /** Fills in everything about a card that depends on the level rather than the sensor. */
    private void bindLevelChrome(LevelCard card, Level level) {
        int accent = ContextCompat.getColor(requireContext(), level.accentColorResId);

        card.icon.setImageResource(level.iconResId);
        ImageViewCompat.setImageTintList(card.icon, ColorStateList.valueOf(accent));
        card.title.setText(level.titleResId);

        if (level.badgeResId == 0) {
            card.badge.setVisibility(View.GONE);
        } else {
            card.badge.setText(level.badgeResId);
            card.badge.setTextColor(accent);
            card.badge.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(requireContext(), level.badgeFillColorResId)));
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

        // Leaving a field is the moment its problem becomes worth pointing out; while the caret is
        // still in it the user is mid-thought and a half-typed number is not yet wrong.
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

    /**
     * The same selector the analytics page carries, over the same active aquarium, so the choice
     * made here is the one the dashboard is already showing.
     *
     * <p>The list is read when the card is tapped rather than held from here, so a tank added or
     * renamed on another device is in it without this screen having to be told.
     */
    private void setUpAquariumSelector(View root) {
        View card = root.findViewById(R.id.waterParametersAquariumSelector);
        AquariumDropdown dropdown = new AquariumDropdown(
                card, root.findViewById(R.id.waterParametersAquariumChevron), this::selectAquarium);

        card.setOnClickListener(v -> dropdown.show(
                this.aquariums.getAquariums(), this.aquariums.getActiveAquariumId()));
    }

    /**
     * Moves the screen, and the app, onto another aquarium.
     *
     * <p>Nothing typed is carried across. Thresholds belong to one tank, so values entered for
     * another are not a starting point for this one, they are the wrong numbers - but they are also
     * the user's work, so it is their call whether to lose them.
     */
    private void selectAquarium(@NonNull Aquarium picked) {
        if (this.aquarium != null && picked.getId().equals(this.aquarium.getId())) {
            return;
        }
        if (this.hasUnsavedEdits()) {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.threshold_discard_title)
                    .setMessage(R.string.threshold_discard_message)
                    .setPositiveButton(R.string.threshold_discard_confirm,
                            (dialog, which) -> switchTo(picked))
                    .setNegativeButton(android.R.string.cancel, null)
                    .show();
            return;
        }
        this.switchTo(picked);
    }

    private void switchTo(@NonNull Aquarium picked) {
        this.aquariums.setActiveAquariumId(picked.getId());
        this.showAquarium(picked);
    }

    private boolean hasUnsavedEdits() {
        for (Draft draft : this.drafts.values()) {
            if (draft.isEdited()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Binds the active aquarium the first time one is available, and stops listening once it has.
     * A later snapshot - including the one the user's own save produces - must not reach in and
     * overwrite fields they are still editing. From then on the selector drives which aquarium is
     * shown.
     */
    private void bindFirstAquarium() {
        if (this.aquarium != null || this.tvAquariumName == null) {
            return;
        }

        Aquarium active = this.aquariums.getActiveAquarium();
        if (active == null) {
            // Either the first snapshot has not arrived, or this user genuinely has no aquarium.
            // Only the second is worth saying out loud; the first resolves itself.
            this.showNoAquarium(this.aquariums.isLoaded());
            return;
        }

        this.detachObserver();
        this.showAquarium(active);
    }

    /** Rebuilds the whole form for one aquarium, from the database and nothing else. */
    private void showAquarium(@NonNull Aquarium active) {
        List<String> applicable = new ArrayList<>();
        for (String sensorId : ThresholdForm.CONFIGURABLE_SENSOR_IDS) {
            if (active.isSensorApplicable(sensorId)) {
                applicable.add(sensorId);
            }
        }

        this.aquarium = active;
        this.showAquariumCard(active);

        // Cleared before the check below, not after it, so a switch cannot leave the previous
        // aquarium's drafts behind for the save to write against this one.
        this.sensorIds.clear();
        this.drafts.clear();
        this.selectedSensorId = null;

        if (applicable.isEmpty()) {
            this.showNoAquarium(true);
            return;
        }

        this.sensorIds.addAll(applicable);
        for (String sensorId : applicable) {
            this.drafts.put(sensorId, this.draftFor(active, sensorId));
        }

        this.tvEmpty.setVisibility(View.GONE);
        this.form.setVisibility(View.VISIBLE);

        // Only the saltwater template carries one, for the TDS sensor it cannot read.
        int disabledNoteResId = active.getSensorDisabledNoteResId();
        boolean explainMissingTab = disabledNoteResId != 0
                && applicable.size() < ThresholdForm.CONFIGURABLE_SENSOR_IDS.size();
        if (explainMissingTab) {
            this.tvTdsNote.setText(disabledNoteResId);
        }
        this.cardTdsNote.setVisibility(explainMissingTab ? View.VISIBLE : View.GONE);

        this.buildTabs();
    }

    /** The selector card's own contents: the aquarium's name, and its icon by water type. */
    private void showAquariumCard(@Nullable Aquarium active) {
        if (active == null) {
            this.tvAquariumName.setText(this.aquariums.isLoaded()
                    ? R.string.no_aquariums
                    : R.string.loading_aquariums);
        } else {
            this.tvAquariumName.setText(active.getName());
        }

        boolean saltwater = active != null
                && WaterType.fromKey(active.getWaterType()) == WaterType.SALTWATER;
        this.ivAquariumIcon.setImageResource(
                saltwater ? R.drawable.aquarium_saltwater : R.drawable.aquarium_freshwater);
    }

    private void showNoAquarium(boolean explain) {
        this.showAquariumCard(null);
        this.tvEmpty.setVisibility(explain ? View.VISIBLE : View.GONE);
        this.form.setVisibility(View.GONE);
        // Hidden explicitly rather than with the form, which it sits outside of: it explains a tab
        // of the aquarium being shown, and there is no longer one being shown.
        this.cardTdsNote.setVisibility(View.GONE);
    }

    /**
     * Starts one sensor's draft from what the database holds, falling back to the template its
     * water type resolves to. An aquarium can be missing a band - the schema makes thresholds
     * optional - and opening the screen on blank fields would ask the user to invent numbers the
     * app already has a sensible answer for.
     */
    private Draft draftFor(Aquarium aquarium, String sensorId) {
        ThresholdForm.Units units = ThresholdForm.unitsFor(sensorId, this.fahrenheit);
        Draft draft = new Draft(units);

        ThresholdBand band = aquarium.thresholdFor(sensorId);
        if (band == null) {
            AquariumTemplate template =
                    BuiltInTemplates.forWaterType(WaterType.fromKey(aquarium.getWaterType()));
            band = template.getThresholds(sensorId);
        }

        if (band != null) {
            draft.values[Level.WARNING_LOW.ordinal()] = format(units.toDisplay(band.getWarnLow()));
            draft.values[Level.SAFE_LOW.ordinal()] = format(units.toDisplay(band.getSafeLow()));
            draft.values[Level.SAFE_HIGH.ordinal()] = format(units.toDisplay(band.getSafeHigh()));
            draft.values[Level.WARNING_HIGH.ordinal()] = format(units.toDisplay(band.getWarnHigh()));
            draft.lastValidBand = band;
        } else {
            Arrays.fill(draft.values, "");
        }

        // resolveSpikeDelta answers with a built-in default for an aquarium that stores none, so
        // this field too is only showing the stored value when the aquarium actually has one.
        double spikeDelta = SensorThresholds.resolveSpikeDelta(aquarium, sensorId);
        draft.values[Level.SPIKE.ordinal()] = Double.isNaN(spikeDelta)
                ? ""
                : format(units.deltaToDisplay(spikeDelta));

        draft.markSaved();
        // Declared after markSaved, which clears the flag along with the baseline. Anything filled
        // in from a fallback is not what the aquarium is being graded against, so the save stays
        // live before the user has touched a thing.
        draft.showingFallback = aquarium.thresholdFor(sensorId) == null
                || aquarium.spikeDeltaFor(sensorId) == null;
        draft.result = validate(draft);
        return draft;
    }

    private void buildTabs() {
        this.tabs.clearOnTabSelectedListeners();
        this.tabs.removeAllTabs();

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

        // The listener is attached after the tabs, so the first one's selection has already
        // happened silently and its cards have to be filled in by hand.
        this.bindSensor(this.sensorIds.get(0));
    }

    /** Swaps the five cards over to another sensor's draft. */
    private void bindSensor(String sensorId) {
        Draft draft = this.drafts.get(sensorId);
        if (draft == null) {
            return;
        }
        this.selectedSensorId = sensorId;

        String sensorName = getString(ReadingFormatter.nameResIdFor(sensorId));
        String unit = getString(ReadingFormatter.unitResIdFor(sensorId, this.fahrenheit)).trim();

        this.tvOverallRangeTitle.setText(unit.isEmpty()
                ? getString(R.string.threshold_overall_range)
                : getString(R.string.template_parameter_with_unit,
                        getString(R.string.threshold_overall_range), unit));

        this.binding = true;
        for (Level level : Level.values()) {
            LevelCard card = this.cards.get(level.ordinal());
            card.description.setText(getString(level.descriptionResId, sensorName));
            card.unit.setText(unit);
            card.value.setText(draft.values[level.ordinal()]);
        }
        this.binding = false;

        this.showProblems(draft);
        this.showOverallRange(draft);
        this.updateSaveState();
    }

    /** Pulls the five fields into the current draft and regrades it, on every keystroke. */
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
        this.updateSaveState();
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

    /**
     * Outlines and captions the fields whose problems have been revealed. A problem that clears
     * takes its outline with it immediately, whether or not the user has left the field: being
     * told a field is fixed the moment it is fixed is never unwelcome.
     */
    private void showProblems(Draft draft) {
        for (Level level : Level.values()) {
            LevelCard card = this.cards.get(level.ordinal());
            ThresholdForm.Problem problem = draft.result.problemFor(level.field);

            if (problem == null) {
                draft.revealed[level.ordinal()] = false;
            }
            // Anything not being pointed out is cleared rather than left alone, because these five
            // cards are reused across the tabs: leaving a card as it was would carry the previous
            // sensor's red outline over onto a field that is perfectly fine.
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
                // Zero for the two levels with nothing below them, which the validator never
                // reports out of order; falling through keeps a caption on screen either way.
                return level.outOfOrderResId != 0
                        ? level.outOfOrderResId
                        : R.string.threshold_error_number;
            case NOT_POSITIVE:
                return R.string.threshold_error_spike_positive;
            default:
                return R.string.threshold_error_number;
        }
    }

    /** Redraws the bar and its three labels from the bounds as they currently stand. */
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

        this.tvOverallRangeLow.setText(getString(R.string.template_bound_below,
                format(draft.units.toDisplay(band.getWarnLow()))));
        this.tvOverallRangeSafe.setText(getString(R.string.template_band_range,
                format(draft.units.toDisplay(band.getSafeLow())),
                format(draft.units.toDisplay(band.getSafeHigh()))));
        this.tvOverallRangeHigh.setText(getString(R.string.template_bound_above,
                format(draft.units.toDisplay(band.getWarnHigh()))));
    }

    /**
     * Enables the save only when there is something to save and every sensor's draft holds
     * together - including the ones on tabs that are not on screen, which is why a blocked save
     * says which tab is holding it up rather than leaving the button inexplicably dead.
     */
    private void updateSaveState() {
        boolean anyDirty = false;
        String firstInvalid = null;

        for (Map.Entry<String, Draft> entry : this.drafts.entrySet()) {
            Draft draft = entry.getValue();
            if (draft.isDirty()) {
                anyDirty = true;
            }
            if (firstInvalid == null && !draft.result.isValid()) {
                firstInvalid = entry.getKey();
            }
        }

        boolean enabled = anyDirty && firstInvalid == null && !this.saving;
        this.btnSave.setEnabled(enabled);
        this.btnSave.setAlpha(enabled ? 1f : DISABLED_BUTTON_ALPHA);

        if (firstInvalid != null && !firstInvalid.equals(this.selectedSensorId)) {
            this.tvBlocked.setText(getString(R.string.threshold_blocked_by_tab,
                    getString(ReadingFormatter.nameResIdFor(firstInvalid))));
            this.tvBlocked.setVisibility(View.VISIBLE);
        } else {
            this.tvBlocked.setVisibility(View.GONE);
        }
    }

    /**
     * Writes every sensor whose draft differs from the database, in one update, so a screen where
     * two tabs were edited cannot half-save.
     */
    private void save() {
        if (this.aquarium == null || this.saving) {
            return;
        }

        Map<String, ThresholdBand> bands = new LinkedHashMap<>();
        Map<String, Double> spikeDeltas = new LinkedHashMap<>();
        List<Draft> writing = new ArrayList<>();

        for (Map.Entry<String, Draft> entry : this.drafts.entrySet()) {
            Draft draft = entry.getValue();
            ThresholdBand band = draft.result.getBand();
            if (!draft.isDirty() || band == null) {
                continue;
            }
            bands.put(entry.getKey(), band);
            spikeDeltas.put(entry.getKey(), draft.result.getSpikeDelta());
            writing.add(draft);
        }
        if (writing.isEmpty()) {
            return;
        }

        this.saving = true;
        this.updateSaveState();

        this.aquariums.saveSensorThresholds(this.aquarium, bands, spikeDeltas,
                new AquariumRepository.WriteCallback() {
                    @Override
                    public void onSuccess() {
                        if (getView() == null) {
                            return;
                        }
                        for (Draft draft : writing) {
                            draft.markSaved();
                        }
                        saving = false;
                        updateSaveState();
                        toast(R.string.threshold_saved);
                    }

                    @Override
                    public void onError() {
                        if (getView() == null) {
                            return;
                        }
                        saving = false;
                        updateSaveState();
                        toast(R.string.threshold_save_failed);
                    }
                });
    }

    private void detachObserver() {
        if (this.observer != null) {
            this.aquariums.removeObserver(this.observer);
            this.observer = null;
        }
    }

    private void toast(@StringRes int messageResId) {
        Toast.makeText(requireContext().getApplicationContext(), messageResId, Toast.LENGTH_SHORT)
                .show();
    }

    private static String format(double value) {
        return FIELD_FORMAT.format(value);
    }
}
