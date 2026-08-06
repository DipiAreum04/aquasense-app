package ca.team6.aquasense.dashboard;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import java.text.DecimalFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.AquariumRepository;
import ca.team6.aquasense.model.Aquarium;
import ca.team6.aquasense.model.DatabaseSchema;
import ca.team6.aquasense.model.ProfileInputValidator;
import ca.team6.aquasense.model.ReadingFormatter;
import ca.team6.aquasense.model.SettingsRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;
import ca.team6.aquasense.model.ThresholdBand;
import ca.team6.aquasense.model.WaterType;
import ca.team6.aquasense.model.aquarium_templates.AquariumTemplate;
import ca.team6.aquasense.model.aquarium_templates.BuiltInTemplates;

/**
 * Dedicated fragment screen for configuring new aquarium profiles with template
 * selection.
 */
public class AddAquariumFragment extends Fragment {

    // Identifies the Custom row in templateChoices. Cannot collide with a template ID, since
    // BuiltInTemplates keys those on the water chemistry family.
    private static final String CUSTOM_CHOICE_ID = "custom";

    // Matches the template cards in Settings, so a bound reads the same on both screens.
    private static final DecimalFormat BOUND_FORMAT = new DecimalFormat("0.##");

    // How far a sensor card's title is dimmed when the chosen water type cannot be measured with
    // it. The explanation below the title keeps its own opacity so it stays readable.
    private static final float DISABLED_TITLE_ALPHA = 0.4f;

    private EditText etAquariumName;
    private LinearLayout advancedSettingsContent;
    private ImageView ivAdvancedSettingsChevron;
    private boolean advancedSettingsExpanded = false;
    private Button btnCreate;
    private boolean submitting;

    // Template ID (or CUSTOM_CHOICE_ID) to its row, in the order the rows are shown. Rebuilt with
    // the view, so it is cleared in onDestroyView rather than holding a dead hierarchy.
    private final Map<String, View> templateChoices = new LinkedHashMap<>();
    // Held as an ID rather than an AquariumTemplate so that Custom, which has no template, is a
    // value like any other and the selection survives the view being recreated on rotation.
    private String selectedChoiceId = BuiltInTemplates.getDefault().getId();

    private SwitchCompat switchWaterLevelAlert;
    private EditText etTemperatureMin, etTemperatureMax;
    private EditText etDissolvedSolidsMin, etDissolvedSolidsMax;
    private EditText etPhLevelMin, etPhLevelMax;
    private TextView tvDissolvedSolidsTitle;
    private View dissolvedSolidsThresholdInputs;
    private TextView tvDissolvedSolidsDisabledNote;
    private RadioGroup rgSensorHistoryRetention;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add_aquarium, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Toolbar toolbar = view.findViewById(R.id.toolbar_add_aquarium);
        NavigationUI.setupWithNavController(toolbar, Navigation.findNavController(view));

        etAquariumName = view.findViewById(R.id.etAquariumName);

        setupAdvancedSettingsToggle(view);
        setupSensorThresholdFields(view);
        setupTemperatureUnitToggle(view);
        setupTemplateChoices(view);

        btnCreate = view.findViewById(R.id.btnCreateAquarium);
        btnCreate.setOnClickListener(this::createAquarium);
    }

    private void createAquarium(@NonNull View clicked) {
        if (submitting) {
            return;
        }

        String name = etAquariumName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(getContext(), R.string.toast_enter_aquarium_name, Toast.LENGTH_SHORT).show();
            return;
        }
        if (ProfileInputValidator.isInvalidName(name)) {
            Toast.makeText(getContext(), R.string.toast_invalid_aquarium_name, Toast.LENGTH_SHORT).show();
            return;
        }

        AquariumRepository repository = AquariumRepository.getInstance(requireContext());
        // Reads the live list the repository keeps in sync with the database, so a tank added on
        // another device is still caught by the duplicate and limit checks below.
        List<Aquarium> existingAquariums = repository.getAquariums();

        for (Aquarium existing : existingAquariums) {
            if (existing.getName().equalsIgnoreCase(name)) {
                Toast.makeText(getContext(),
                        getString(R.string.toast_duplicate_aquarium_name, name),
                        Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (existingAquariums.size() >= AquariumRepository.MAX_AQUARIUMS_LIMIT) {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.dialog_limit_reached_title)
                    .setMessage(getString(
                            R.string.dialog_limit_reached_message, AquariumRepository.MAX_AQUARIUMS_LIMIT))
                    .setPositiveButton(R.string.action_ok, null)
                    .show();
            return;
        }

        // TODO: Persist the tank volume and notes, which have nowhere in the schema to go yet.
        setSubmitting(true);
        repository.addAquarium(name, selectedWaterType(), selectedTemplate(),
                new AquariumRepository.WriteCallback() {
                    @Override
                    public void onSuccess() {
                        if (!isAdded()) {
                            return;
                        }
                        setSubmitting(false);
                        Toast.makeText(getContext(),
                                getString(R.string.toast_created_aquarium, name),
                                Toast.LENGTH_SHORT).show();
                        // Pop back to Aquarium Selector / Dashboard, both of which pick the new
                        // aquarium up from their subscription rather than from this screen.
                        Navigation.findNavController(clicked).popBackStack();
                    }

                    @Override
                    public void onError() {
                        if (!isAdded()) {
                            return;
                        }
                        // Stay on the form so the user can retry without retyping everything.
                        setSubmitting(false);
                        Toast.makeText(getContext(), R.string.toast_create_aquarium_failed,
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // TODO: Build the custom aquarium template UI and logic
    /** The template the user picked, or null for Custom, which is not backed by one. */
    @Nullable
    private AquariumTemplate selectedTemplate() {
        return BuiltInTemplates.fromId(selectedChoiceId);
    }

    /**
     * The water type to store, taken from the selected template.
     *
     * <p>Custom has no template to read it from and the form offers no water type control of its
     * own, so it falls back to the default template's. That is the safer of the two: a freshwater
     * tank mislabelled as saltwater would be judged against marine thresholds.
     */
    @NonNull
    private WaterType selectedWaterType() {
        AquariumTemplate template = selectedTemplate();
        return template != null
                ? template.getWaterType()
                : BuiltInTemplates.getDefault().getWaterType();
    }

    // The write is a network round trip, so the button is latched until it resolves rather than
    // letting an impatient double-tap create the aquarium twice.
    private void setSubmitting(boolean value) {
        submitting = value;
        btnCreate.setEnabled(!value);
    }

    /**
     * Builds one row per built-in template, then a Custom row. Driven by
     * {@link BuiltInTemplates#all()} so a template added there shows up here with no edit to this
     * screen, and so the names, descriptions and water types always match the thresholds the
     * dashboard enforces.
     */
    private void setupTemplateChoices(View view) {
        LinearLayout container = view.findViewById(R.id.templateChoiceContainer);
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        templateChoices.clear();
        container.removeAllViews();
        for (AquariumTemplate template : BuiltInTemplates.all()) {
            container.addView(buildTemplateChoice(inflater, container, template));
        }
        container.addView(buildCustomChoice(inflater, container));

        // Re-checks whatever was selected before, which on a first build is the default template.
        showSelectedChoice();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        templateChoices.clear();
    }

    private View buildTemplateChoice(LayoutInflater inflater,
                                     ViewGroup container,
                                     @NonNull AquariumTemplate template) {
        View row = inflater.inflate(R.layout.item_template_choice, container, false);

        ((ImageView) row.findViewById(R.id.ivTemplateChoiceIcon))
                .setImageResource(template.getIconResId());
        ((TextView) row.findViewById(R.id.tvTemplateChoiceName)).setText(template.getNameResId());
        ((TextView) row.findViewById(R.id.tvTemplateChoiceDescription))
                .setText(template.getDescriptionResId());

        // The badge is the only place the screen tells the user which water type they are about
        // to store, and water_type is required by the schema.
        WaterType waterType = template.getWaterType();
        TextView badge = row.findViewById(R.id.tvTemplateChoiceWaterType);
        badge.setText(waterType.getLabelResId());
        badge.setBackgroundResource(waterType == WaterType.SALTWATER
                ? R.drawable.bg_water_badge_saltwater
                : R.drawable.bg_water_badge_freshwater);

        templateChoices.put(template.getId(), row);
        row.setOnClickListener(v -> selectChoice(template.getId()));
        return row;
    }

    private View buildCustomChoice(LayoutInflater inflater, ViewGroup container) {
        View row = inflater.inflate(R.layout.item_template_choice, container, false);

        ((ImageView) row.findViewById(R.id.ivTemplateChoiceIcon))
                .setImageResource(R.drawable.template_custom_24);
        ((TextView) row.findViewById(R.id.tvTemplateChoiceName)).setText(R.string.template_custom_title);
        ((TextView) row.findViewById(R.id.tvTemplateChoiceDescription))
                .setText(R.string.template_custom_desc);
        // Custom starts from no template, so there is no water type to advertise yet.
        row.findViewById(R.id.tvTemplateChoiceWaterType).setVisibility(View.GONE);

        templateChoices.put(CUSTOM_CHOICE_ID, row);
        row.setOnClickListener(v -> selectChoice(CUSTOM_CHOICE_ID));
        return row;
    }

    /** Handles a tap on a row. Takes a template ID, or {@link #CUSTOM_CHOICE_ID} for Custom. */
    private void selectChoice(@NonNull String choiceId) {
        selectedChoiceId = choiceId;
        showSelectedChoice();

        // Custom means the user fills the thresholds in themselves, so open the section that
        // holds them rather than making them hunt for it. Only on a tap: re-checking the rows
        // after a rotation should not reopen a section the user had collapsed.
        if (CUSTOM_CHOICE_ID.equals(choiceId) && !advancedSettingsExpanded) {
            toggleAdvancedSettings();
        }
    }

    private void showSelectedChoice() {
        for (Map.Entry<String, View> choice : templateChoices.entrySet()) {
            RadioButton radio = choice.getValue().findViewById(R.id.rbTemplateChoice);
            radio.setChecked(choice.getKey().equals(selectedChoiceId));
        }
        showTemplateThresholds();
    }

    /**
     * DS-4.4: Advanced settings (tank volume, notes) stay collapsed until the
     * user explicitly taps the header, regardless of which template is chosen.
     */
    private void setupAdvancedSettingsToggle(View view) {
        View advancedSettingsHeader = view.findViewById(R.id.advancedSettingsHeader);
        advancedSettingsContent = view.findViewById(R.id.advancedSettingsContent);
        ivAdvancedSettingsChevron = view.findViewById(R.id.ivAdvancedSettingsChevron);

        advancedSettingsHeader.setOnClickListener(v -> toggleAdvancedSettings());
        // The freshly inflated section is collapsed, so re-apply the state the user left it in.
        showAdvancedSettings();
    }

    private void toggleAdvancedSettings() {
        advancedSettingsExpanded = !advancedSettingsExpanded;
        showAdvancedSettings();
    }

    private void showAdvancedSettings() {
        advancedSettingsContent.setVisibility(advancedSettingsExpanded ? View.VISIBLE : View.GONE);
        ivAdvancedSettingsChevron.setRotation(advancedSettingsExpanded ? 180f : 0f);
    }

    private void setupSensorThresholdFields(View view) {
        switchWaterLevelAlert = view.findViewById(R.id.switchWaterLevelAlert);
        etTemperatureMin = view.findViewById(R.id.etTemperatureMin);
        etTemperatureMax = view.findViewById(R.id.etTemperatureMax);
        etDissolvedSolidsMin = view.findViewById(R.id.etDissolvedSolidsMin);
        etDissolvedSolidsMax = view.findViewById(R.id.etDissolvedSolidsMax);
        etPhLevelMin = view.findViewById(R.id.etPhLevelMin);
        etPhLevelMax = view.findViewById(R.id.etPhLevelMax);
        tvDissolvedSolidsTitle = view.findViewById(R.id.tvDissolvedSolidsTitle);
        dissolvedSolidsThresholdInputs = view.findViewById(R.id.dissolvedSolidsThresholdInputs);
        tvDissolvedSolidsDisabledNote = view.findViewById(R.id.tvDissolvedSolidsDisabledNote);
        rgSensorHistoryRetention = view.findViewById(R.id.rgSensorHistoryRetention);
    }

    /**
     * Fills the threshold fields with the selected template's safe range, so the user sees
     * the numbers the aquarium will be created with instead of a blank form.
     */
    
    private void showTemplateThresholds() {
        // Null for Custom, which clears the fields: it is defined by what the user types, and a
        // template's numbers sitting in the boxes would misrepresent that.
        AquariumTemplate template = selectedTemplate();

        showBand(etTemperatureMin, etTemperatureMax,
                template == null ? null : template.getThresholds(DatabaseSchema.TEMPERATURE_KEY),
                true);
        showBand(etPhLevelMin, etPhLevelMax,
                template == null ? null : template.getThresholds(DatabaseSchema.PH_LEVEL_KEY),
                false);
        showBand(etDissolvedSolidsMin, etDissolvedSolidsMax,
                template == null ? null : template.getThresholds(DatabaseSchema.DISSOLVED_SOLIDS_KEY),
                false);

        showDissolvedSolidsApplicable(template);
    }

    /**
     * Writes one band's safe range into a min / max pair, or empties both when the template has no
     * band for that sensor.
     *
     * @param isTemperature converts to whatever unit the toggle above the field is set to, since
     *     templates hold Celsius but the field is labelled with the user's unit.
     */
    private void showBand(EditText min, EditText max, @Nullable ThresholdBand band, boolean isTemperature) {
        if (band == null) {
            min.setText("");
            max.setText("");
            return;
        }
        double low = isTemperature
                ? ReadingFormatter.toDisplayTemperature(requireContext(), band.getSafeLow())
                : band.getSafeLow();
        double high = isTemperature
                ? ReadingFormatter.toDisplayTemperature(requireContext(), band.getSafeHigh())
                : band.getSafeHigh();
        min.setText(BOUND_FORMAT.format(low));
        max.setText(BOUND_FORMAT.format(high));
    }

    /**
     * Greys out the TDS card for a template whose water the probe cannot read, which today
     * is Saltwater only, and explains why in the template's own words.
     */
    private void showDissolvedSolidsApplicable(@Nullable AquariumTemplate template) {
        boolean applicable = template == null
                || template.isSensorApplicable(DatabaseSchema.DISSOLVED_SOLIDS_KEY);

        tvDissolvedSolidsTitle.setAlpha(applicable ? 1f : DISABLED_TITLE_ALPHA);
        dissolvedSolidsThresholdInputs.setVisibility(applicable ? View.VISIBLE : View.GONE);
        etDissolvedSolidsMin.setEnabled(applicable);
        etDissolvedSolidsMax.setEnabled(applicable);

        // Every template that disables a sensor carries a note explaining it
        int noteResId = template == null ? 0 : template.getDisabledNoteResId();
        if (applicable || noteResId == 0) {
            tvDissolvedSolidsDisabledNote.setVisibility(View.GONE);
            return;
        }
        tvDissolvedSolidsDisabledNote.setText(noteResId);
        tvDissolvedSolidsDisabledNote.setVisibility(View.VISIBLE);
    }

    /**
     * Mirrors DisplayUnitsFragment's Celsius/Fahrenheit RadioGroup, writing to the
     * same
     * app-wide KEY_TEMP_UNIT preference so this toggle stays in sync with Settings
     * > Display
     * & Units instead of introducing a second, competing unit setting.
     */
    private void setupTemperatureUnitToggle(View view) {
        TextView tvTemperatureTitle = view.findViewById(R.id.tvTemperatureTitle);
        RadioGroup rgTemperatureUnit = view.findViewById(R.id.rgTemperatureUnit);

        SharedPreferenceHelper prefs = SharedPreferenceHelper.getInstance(requireContext());
        if (prefs == null)
            return;

        String tempUnit = prefs.getString(SettingsRepository.KEY_TEMP_UNIT, "F");
        rgTemperatureUnit.check("C".equals(tempUnit) ? R.id.rbAdvancedCelsius : R.id.rbAdvancedFahrenheit);
        updateTemperatureTitle(tvTemperatureTitle, tempUnit);

        rgTemperatureUnit.setOnCheckedChangeListener((group, checkedId) -> {
            String unit = (checkedId == R.id.rbAdvancedCelsius) ? "C" : "F";
            prefs.updateField(SettingsRepository.KEY_TEMP_UNIT, unit);
            updateTemperatureTitle(tvTemperatureTitle, unit);
            showTemplateThresholds();
        });
    }

    private void updateTemperatureTitle(TextView tvTemperatureTitle, String tempUnit) {
        int unitResId = "C".equals(tempUnit) ? R.string.unit_celsius : R.string.unit_fahrenheit;
        tvTemperatureTitle.setText(getString(R.string.label_temperature_threshold, getString(unitResId)));
    }

}
