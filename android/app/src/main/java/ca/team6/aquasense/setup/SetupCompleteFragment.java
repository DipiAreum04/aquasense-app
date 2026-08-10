package ca.team6.aquasense.setup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import ca.team6.aquasense.R;
import ca.team6.aquasense.auth.AuthNavigator;
import ca.team6.aquasense.model.NewAquariumConfig;
import ca.team6.aquasense.model.SettingsRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;
import ca.team6.aquasense.model.WaterType;
import ca.team6.aquasense.model.aquarium_templates.AquariumTemplate;
import ca.team6.aquasense.model.aquarium_templates.BuiltInTemplates;
import ca.team6.aquasense.ui.RevealSequence;

/**
 * Closing page of the first-installation wizard (SETTINGS-03): confirms what was set up, offers the
 * two app-wide display choices, and hands the user to the dashboard.
 *
 * <p>Only reached once the hub is paired, which is also the point at which the aquarium was
 * written to the database. Everything shown here is echoed back from the wizard's own arguments
 * rather than read from the database, so the page does not sit blank waiting for a snapshot.
 *
 * <p>The display preferences are here rather than on the add-aquarium form because they are
 * settings, not properties of an aquarium - and because by this point the user has seen their first
 * live readings, which is what makes "°C or °F" a concrete question rather than an abstract one.
 */
public class SetupCompleteFragment extends Fragment {

    @Nullable
    private SharedPreferenceHelper prefs;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setup_complete, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.prefs = SharedPreferenceHelper.getInstance(requireContext());

        this.showSummary(view);
        this.setUpDisplayPreferences(view);

        view.findViewById(R.id.btnGoToDashboard)
                .setOnClickListener(v -> AuthNavigator.goToDashboard(requireActivity()));

        // Setup is finished and the steps behind this page have been popped, so back goes where
        // the button goes rather than out of the app.
        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        AuthNavigator.goToDashboard(requireActivity());
                    }
                });

        if (savedInstanceState == null) {
            RevealSequence.play(
                    view.findViewById(R.id.completeBadge),
                    view.findViewById(R.id.completeTitle),
                    view.findViewById(R.id.tvCompleteBody),
                    view.findViewById(R.id.completeSummary),
                    view.findViewById(R.id.completeDisplayPrefs),
                    view.findViewById(R.id.completeActions));
        }
    }

    private void showSummary(@NonNull View view) {
        Bundle args = getArguments();
        NewAquariumConfig config =
                args == null ? null : args.getParcelable(SetupArgs.AQUARIUM_CONFIG);

        String name = config == null ? "" : config.getName();
        ((TextView) view.findViewById(R.id.tvCompleteBody))
                .setText(getString(R.string.setup_complete_body, name));
        ((TextView) view.findViewById(R.id.tvCompleteName)).setText(name);

        WaterType waterType = config == null
                ? BuiltInTemplates.getDefault().getWaterType()
                : config.getWaterType();
        ((TextView) view.findViewById(R.id.tvCompleteWaterType)).setText(waterType.getLabelResId());

        // Every aquarium is created from a template, so the row only goes if this page is somehow
        // reached outside the wizard and there is no argument naming one.
        AquariumTemplate template =
                BuiltInTemplates.fromId(args == null ? null : args.getString(SetupArgs.TEMPLATE_ID));
        if (template == null) {
            view.findViewById(R.id.completeTemplateDivider).setVisibility(View.GONE);
            view.findViewById(R.id.completeTemplateRow).setVisibility(View.GONE);
            return;
        }
        ((TextView) view.findViewById(R.id.tvCompleteTemplate)).setText(template.getNameResId());
    }

    /**
     * The two app-wide display choices. Each writes through the moment it is picked, matching how
     * they behave on the Display &amp; Units screen: there is no save on this page, and the button
     * below it leaves for the dashboard.
     */
    private void setUpDisplayPreferences(@NonNull View view) {
        RadioGroup tempUnit = view.findViewById(R.id.rgSetupTempUnit);
        RadioGroup precision = view.findViewById(R.id.rgSetupPrecision);
        if (this.prefs == null) {
            // No preference store means nothing to read or write, so the card would only offer
            // choices that do not stick.
            view.findViewById(R.id.completeDisplayPrefs).setVisibility(View.GONE);
            return;
        }

        // Checked before the listeners are attached, so restoring the current values does not
        // count as the user picking them.
        tempUnit.check(isCelsius() ? R.id.rbSetupCelsius : R.id.rbSetupFahrenheit);
        precision.check(isPrecise()
                ? R.id.rbSetupPrecisionPrecise
                : R.id.rbSetupPrecisionStandard);

        tempUnit.setOnCheckedChangeListener((group, checkedId) -> prefs.setString(
                SettingsRepository.KEY_TEMP_UNIT,
                checkedId == R.id.rbSetupFahrenheit ? TEMP_UNIT_FAHRENHEIT : TEMP_UNIT_CELSIUS));

        precision.setOnCheckedChangeListener((group, checkedId) -> prefs.setString(
                SettingsRepository.KEY_READING_PRECISION,
                checkedId == R.id.rbSetupPrecisionPrecise
                        ? SettingsRepository.PRECISION_PRECISE
                        : SettingsRepository.PRECISION_STANDARD));
    }

    // The two values KEY_TEMP_UNIT holds, which the rest of the app reads through ReadingFormatter.
    private static final String TEMP_UNIT_CELSIUS = "C";
    private static final String TEMP_UNIT_FAHRENHEIT = "F";

    private boolean isCelsius() {
        return this.prefs == null || TEMP_UNIT_CELSIUS.equals(
                this.prefs.getString(SettingsRepository.KEY_TEMP_UNIT, TEMP_UNIT_CELSIUS));
    }

    private boolean isPrecise() {
        return this.prefs != null && SettingsRepository.PRECISION_PRECISE.equals(
                this.prefs.getString(SettingsRepository.KEY_READING_PRECISION,
                        SettingsRepository.PRECISION_STANDARD));
    }
}
