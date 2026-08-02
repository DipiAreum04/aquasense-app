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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.AquariumRepository;
import ca.team6.aquasense.model.Aquarium;
import ca.team6.aquasense.model.ProfileInputValidator;
import ca.team6.aquasense.model.SettingsRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;
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

        // Advanced settings first: selecting Custom expands that section, so it has to exist by
        // the time the rows are built.
        setupAdvancedSettingsToggle(view);
        setupTemplateChoices(view);
        setupSensorThresholdFields(view);
        setupTemperatureUnitToggle(view);

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

        // TODO: Persist the chosen template's ID and its thresholds, plus the tank volume, notes
        setSubmitting(true);
        repository.addAquarium(name, selectedWaterType(),
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

    /**
     * The water type to store, taken from the selected template.
     *
     * <p>Custom has no template to read it from and the form offers no water type control of its
     * own, so it falls back to the default template's. That is the safer of the two: a freshwater
     * tank mislabelled as saltwater would be judged against marine thresholds.
     */
    @NonNull
    private WaterType selectedWaterType() {
        // Resolves to null for CUSTOM_CHOICE_ID, which is not a template.
        AquariumTemplate template = BuiltInTemplates.fromId(selectedChoiceId);
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

    /** Leaves exactly one row checked, the one named by {@link #selectedChoiceId}. */
    private void showSelectedChoice() {
        for (Map.Entry<String, View> choice : templateChoices.entrySet()) {
            RadioButton radio = choice.getValue().findViewById(R.id.rbTemplateChoice);
            radio.setChecked(choice.getKey().equals(selectedChoiceId));
        }
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
        rgSensorHistoryRetention = view.findViewById(R.id.rgSensorHistoryRetention);
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
        });
    }

    private void updateTemperatureTitle(TextView tvTemperatureTitle, String tempUnit) {
        int unitResId = "C".equals(tempUnit) ? R.string.unit_celsius : R.string.unit_fahrenheit;
        tvTemperatureTitle.setText(getString(R.string.label_temperature_threshold, getString(unitResId)));
    }

}
