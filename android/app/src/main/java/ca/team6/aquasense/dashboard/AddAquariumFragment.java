package ca.team6.aquasense.dashboard;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.team6.aquasense.PairingActivity;
import ca.team6.aquasense.R;
import ca.team6.aquasense.auth.AuthNavigator;
import ca.team6.aquasense.auth.AuthRepository;
import ca.team6.aquasense.model.AquariumRepository;
import ca.team6.aquasense.model.Aquarium;
import ca.team6.aquasense.model.ProfileInputValidator;
import ca.team6.aquasense.model.WaterType;
import ca.team6.aquasense.model.aquarium_templates.AquariumTemplate;
import ca.team6.aquasense.model.aquarium_templates.BuiltInTemplates;
import ca.team6.aquasense.pairing.PairingEntryMode;
import ca.team6.aquasense.settings.AquariumTemplateCardBinder;
import ca.team6.aquasense.ui.FragmentToolbar;

/**
 * Dedicated fragment screen for configuring new aquarium profiles with template
 * selection.
 */
public class AddAquariumFragment extends Fragment {

    /** Set when the form is the first thing shown after signing up, which adds the skip option. */
    public static final String ARG_FIRST_RUN = "firstRun";

    // Identifies the Custom row in templateChoices. Cannot collide with a template ID, since
    // BuiltInTemplates keys those on the water chemistry family.
    private static final String CUSTOM_CHOICE_ID = "custom";

    private static final int EXPANDER_INDENT_DP = 54;

    // Dimming the disabled Create button while the form is incomplete
    private static final float DISABLED_BUTTON_ALPHA = 0.5f;

    private EditText etAquariumName;
    private TextView tvAquariumNameError;
    private ScrollView scrollView;
    private View stepTemplateHeader;
    private Button btnCreate;
    private boolean scrolledToTemplates;
    private boolean firstRun;

    private final ActivityResultLauncher<Intent> pairingLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && isAdded()) {
                            Navigation.findNavController(requireView()).popBackStack();
                        }
                    });

    // Template ID (or CUSTOM_CHOICE_ID) to its row, in the order the rows are shown. Rebuilt with
    // the view, so it is cleared in onDestroyView rather than holding a dead hierarchy.
    private final Map<String, View> templateChoices = new LinkedHashMap<>();
    // Held as an ID rather than an AquariumTemplate so that Custom, which has no template, is a
    // value like any other and the selection survives the view being recreated on rotation.
    @Nullable
    private String selectedChoiceId;
    private final Set<String> expandedChoiceIds = new HashSet<>();

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

        firstRun = getArguments() != null && getArguments().getBoolean(ARG_FIRST_RUN, false);

        // First run is reached straight after sign-up with nothing to navigate back to, so the
        // toolbar's up arrow is only wired up when the screen is opened from elsewhere.
        if (!firstRun) {
            FragmentToolbar.setup(this, view, R.id.toolbar_add_aquarium);
        }

        btnCreate = view.findViewById(R.id.btnCreateAquarium);
        btnCreate.setText(R.string.add_aquarium_create_and_pair);
        btnCreate.setOnClickListener(this::createAquarium);

        View btnSkip = view.findViewById(R.id.btnSkipPairing);
        btnSkip.setVisibility(firstRun ? View.VISIBLE : View.GONE);
        btnSkip.setOnClickListener(v -> skipPairing());

        setupNameField(view);
        setupTemplateChoices(view);
    }

    /**
     * Leaves setup without creating anything.
     */
    private void skipPairing() {
        AuthRepository authRepository = AuthRepository.getInstance(requireContext());
        authRepository.setPairingComplete(true);
        AuthNavigator.goToDashboard(requireActivity());
    }

    /**
     * Step 1. Validates the name as it is typed rather than at submit time
     */
    private void setupNameField(View view) {
        etAquariumName = view.findViewById(R.id.etAquariumName);
        tvAquariumNameError = view.findViewById(R.id.tvAquariumNameError);
        scrollView = view.findViewById(R.id.addAquariumScroll);
        stepTemplateHeader = view.findViewById(R.id.stepTemplateHeader);

        etAquariumName.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                showNameProblem();
                updateCreateEnabled();
            }
        });

        etAquariumName.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                scrollToTemplates();
            }
            return false;
        });
        etAquariumName.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                scrollToTemplates();
            }
        });
    }

    @NonNull
    private String typedName() {
        return etAquariumName.getText().toString().trim();
    }

    /** Whether the typed name is valid and not already in use */
    private boolean isNameUsable() {
        String name = typedName();
        return !name.isEmpty()
                && !ProfileInputValidator.isInvalidAquariumName(name)
                && !isDuplicateName(name);
    }

    /**
     * Whether an aquarium by this name already exists under the current user.
     * Reads the live list the repository keeps in sync with the database.
     */
    private boolean isDuplicateName(@NonNull String name) {
        for (Aquarium existing : AquariumRepository.getInstance(requireContext()).getAquariums()) {
            if (existing.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    /** Writes the reason the typed name cannot be used under the field. */
    private void showNameProblem() {
        String name = typedName();

        if (name.isEmpty()) {
            tvAquariumNameError.setVisibility(View.GONE);
            return;
        }
        if (ProfileInputValidator.isInvalidAquariumName(name)) {
            tvAquariumNameError.setText(R.string.toast_invalid_aquarium_name);
            tvAquariumNameError.setVisibility(View.VISIBLE);
            return;
        }
        if (isDuplicateName(name)) {
            tvAquariumNameError.setText(getString(R.string.toast_duplicate_aquarium_name, name));
            tvAquariumNameError.setVisibility(View.VISIBLE);
            return;
        }
        tvAquariumNameError.setVisibility(View.GONE);
    }

    /**
     * Enables the Create button only once the user has entered a valid name and selected a template.
     */
    private void updateCreateEnabled() {
        boolean ready = selectedChoiceId != null && isNameUsable();
        btnCreate.setEnabled(ready);
        btnCreate.setAlpha(ready ? 1f : DISABLED_BUTTON_ALPHA);
    }

    private void scrollToTemplates() {
        if (scrolledToTemplates || selectedChoiceId != null || !isNameUsable()) {
            return;
        }
        scrolledToTemplates = true;

        ScrollView scroll = scrollView;
        View header = stepTemplateHeader;
        scroll.post(() -> scroll.smoothScrollTo(0, header.getTop()));
    }

    private void createAquarium(@NonNull View clicked) {
        String name = typedName();
        if (name.isEmpty()) {
            Toast.makeText(getContext(), R.string.toast_enter_aquarium_name, Toast.LENGTH_SHORT).show();
            return;
        }
        if (ProfileInputValidator.isInvalidAquariumName(name)) {
            Toast.makeText(getContext(), R.string.toast_invalid_aquarium_name, Toast.LENGTH_SHORT).show();
            return;
        }
        if (selectedChoiceId == null) {
            Toast.makeText(getContext(), R.string.toast_choose_template, Toast.LENGTH_SHORT).show();
            return;
        }
        if (isDuplicateName(name)) {
            showNameProblem();
            Toast.makeText(getContext(),
                    getString(R.string.toast_duplicate_aquarium_name, name),
                    Toast.LENGTH_SHORT).show();
            return;
        }

        AquariumRepository repository = AquariumRepository.getInstance(requireContext());
        List<Aquarium> existingAquariums = repository.getAquariums();

        if (existingAquariums.size() >= AquariumRepository.MAX_AQUARIUMS_LIMIT) {
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.dialog_limit_reached_title)
                    .setMessage(getString(
                            R.string.dialog_limit_reached_message, AquariumRepository.MAX_AQUARIUMS_LIMIT))
                    .setPositiveButton(R.string.action_ok, null)
                    .show();
            return;
        }

        // The aquarium is written by PairingRepository once the board reports the UID it will be
        // keyed by, so this screen hands the details over rather than saving them itself.
        // TODO: Persist the tank volume and notes, which have nowhere in the schema to go yet.
        pairingLauncher.launch(PairingActivity.intent(requireContext(), name, selectedWaterType(),
                selectedChoiceId, firstRun ? PairingEntryMode.FIRST_RUN : PairingEntryMode.ADD_AQUARIUM));
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

        showSelectedChoice();
        for (String choiceId : templateChoices.keySet()) {
            showDetails(choiceId);
        }
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

        AquariumTemplateCardBinder.bindIconTile(
                row.findViewById(R.id.ivTemplateChoiceIcon), template);
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

        ViewGroup details = row.findViewById(R.id.templateChoiceDetails);
        details.addView(AquariumTemplateCardBinder.createDetails(inflater, details, template));

        templateChoices.put(template.getId(), row);
        row.setOnClickListener(v -> selectChoice(template.getId()));
        row.findViewById(R.id.templateChoiceExpander)
                .setOnClickListener(v -> toggleDetails(template.getId()));
        return row;
    }

    private View buildCustomChoice(LayoutInflater inflater, ViewGroup container) {
        View row = inflater.inflate(R.layout.item_template_choice, container, false);

        AquariumTemplateCardBinder.bindIconTile(row.findViewById(R.id.ivTemplateChoiceIcon),
                R.drawable.template_custom_24, R.color.accent);
        ImageViewCompat.setImageTintList(
                row.findViewById(R.id.ivTemplateChoiceIcon),
                ContextCompat.getColorStateList(requireContext(), R.color.accent));

        ((TextView) row.findViewById(R.id.tvTemplateChoiceName)).setText(R.string.template_custom_title);
        ((TextView) row.findViewById(R.id.tvTemplateChoiceDescription))
                .setText(R.string.template_custom_desc);
        row.findViewById(R.id.templateChoiceMeta).setVisibility(View.GONE);
        row.findViewById(R.id.templateChoiceExpander).setVisibility(View.GONE);

        templateChoices.put(CUSTOM_CHOICE_ID, row);
        row.setOnClickListener(v -> selectChoice(CUSTOM_CHOICE_ID));
        return row;
    }


    private void selectChoice(@NonNull String choiceId) {
        selectedChoiceId = choiceId.equals(selectedChoiceId) ? null : choiceId;
        showSelectedChoice();
    }

    /**
     * Opens or closes one row's species and threshold detail.
     */
    private void toggleDetails(@NonNull String choiceId) {
        if (!expandedChoiceIds.remove(choiceId)) {
            expandedChoiceIds.add(choiceId);
        }
        showDetails(choiceId);
    }

    /** Leaves at most one row checked, the one named by {@link #selectedChoiceId}. */
    private void showSelectedChoice() {
        for (Map.Entry<String, View> choice : templateChoices.entrySet()) {
            ((RadioButton) choice.getValue().findViewById(R.id.rbTemplateChoice))
                    .setChecked(choice.getKey().equals(selectedChoiceId));
        }
        updateCreateEnabled();
    }

    private void showDetails(@NonNull String choiceId) {
        View row = templateChoices.get(choiceId);
        if (row == null || BuiltInTemplates.fromId(choiceId) == null) {
            return;
        }

        boolean expanded = expandedChoiceIds.contains(choiceId);

        row.findViewById(R.id.templateChoiceDetails)
                .setVisibility(expanded ? View.VISIBLE : View.GONE);
        ((TextView) row.findViewById(R.id.tvTemplateChoiceHint)).setText(expanded
                ? R.string.template_choice_hide_details
                : R.string.template_choice_show_details);
        showExpanderAlignment(row, expanded);
    }

    private void showExpanderAlignment(@NonNull View row, boolean expanded) {
        LinearLayout expander = row.findViewById(R.id.templateChoiceExpander);
        expander.setGravity(expanded ? Gravity.CENTER : Gravity.START | Gravity.CENTER_VERTICAL);

        ViewGroup.MarginLayoutParams params =
                (ViewGroup.MarginLayoutParams) expander.getLayoutParams();
        // Matches the icon tile's width plus its gap, the indent the layout starts this row at.
        params.setMarginStart(expanded ? 0 : Math.round(
                EXPANDER_INDENT_DP * getResources().getDisplayMetrics().density));
        expander.setLayoutParams(params);
    }
}
