package ca.team6.aquasense.dashboard;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import java.util.ArrayList;
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
import ca.team6.aquasense.model.NewAquariumConfig;
import ca.team6.aquasense.model.ProfileInputValidator;
import ca.team6.aquasense.model.WaterType;
import ca.team6.aquasense.model.aquarium_templates.AquariumTemplate;
import ca.team6.aquasense.model.aquarium_templates.BuiltInTemplates;
import ca.team6.aquasense.pairing.PairingEntryMode;
import ca.team6.aquasense.settings.AquariumTemplateCardBinder;
import ca.team6.aquasense.settings.ThresholdEditor;
import ca.team6.aquasense.setup.SetupArgs;
import ca.team6.aquasense.ui.FragmentToolbar;
import ca.team6.aquasense.ui.WizardProgress;

public class AddAquariumFragment extends Fragment {

    public static final String ARG_FIRST_RUN = "firstRun";

    private static final int EXPANDER_INDENT_DP = 54;

    private static final float CHEVRON_OPEN_ROTATION = 90f;

    private static final String STATE_SELECTED_CHOICE = "selectedChoiceId";
    private static final String STATE_EXPANDED_CHOICES = "expandedChoiceIds";
    private static final String STATE_ADVANCED_EXPANDED = "advancedExpanded";
    private static final String STATE_ADVANCED_EDITOR = "advancedEditor";

    private static final float DISABLED_BUTTON_ALPHA = 0.5f;

    private EditText etAquariumName;
    private TextView tvAquariumNameError;
    private ScrollView scrollView;
    private View stepTemplateHeader;
    private Button btnCreate;
    private boolean scrolledToTemplates;
    private boolean firstRun;

    private ThresholdEditor advancedEditor;
    private View advancedEditorView;
    private View advancedChevron;
    private TextView tvAdvancedHint;
    private boolean advancedExpanded;
    @Nullable
    private Bundle pendingEditorState;

    private final ActivityResultLauncher<Intent> pairingLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && isAdded()) {
                            Navigation.findNavController(requireView()).popBackStack();
                        }
                    });

    private final Map<String, View> templateChoices = new LinkedHashMap<>();
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
        restoreSelection(savedInstanceState);

        FragmentToolbar.setup(this, view, R.id.toolbar_add_aquarium);
        if (firstRun) {
            WizardProgress.show(view, 1);
        }

        btnCreate = view.findViewById(R.id.btnCreateAquarium);
        btnCreate.setText(firstRun
                ? R.string.add_aquarium_first_run_next
                : R.string.add_aquarium_create_and_pair);
        btnCreate.setOnClickListener(this::createAquarium);

        View btnSkip = view.findViewById(R.id.btnSkipPairing);
        btnSkip.setVisibility(firstRun ? View.VISIBLE : View.GONE);
        btnSkip.setOnClickListener(v -> skipPairing());

        setupNameField(view);
        setupAdvanced(view);
        setupTemplateChoices(view);
        syncAdvanced(null);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_SELECTED_CHOICE, selectedChoiceId);
        outState.putStringArrayList(STATE_EXPANDED_CHOICES, new ArrayList<>(expandedChoiceIds));
        outState.putBoolean(STATE_ADVANCED_EXPANDED, advancedExpanded);

        Bundle editorState = new Bundle();
        advancedEditor.saveState(editorState);
        outState.putBundle(STATE_ADVANCED_EDITOR, editorState);
    }

    private void restoreSelection(@Nullable Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }
        String restored = savedInstanceState.getString(STATE_SELECTED_CHOICE);
        selectedChoiceId = BuiltInTemplates.fromId(restored) == null ? null : restored;

        advancedExpanded = savedInstanceState.getBoolean(STATE_ADVANCED_EXPANDED, false);
        pendingEditorState = savedInstanceState.getBundle(STATE_ADVANCED_EDITOR);

        List<String> expanded = savedInstanceState.getStringArrayList(STATE_EXPANDED_CHOICES);
        if (expanded != null) {
            expandedChoiceIds.clear();
            expandedChoiceIds.addAll(expanded);
        }
    }

    private void goToMountHub(@NonNull NewAquariumConfig config) {
        Bundle args = new Bundle();
        args.putParcelable(SetupArgs.AQUARIUM_CONFIG, config);
        args.putString(SetupArgs.TEMPLATE_ID, selectedChoiceId);

        Navigation.findNavController(requireView())
                .navigate(R.id.action_addAquariumFragment_to_mountHubFragment, args);
    }

    private void skipPairing() {
        AuthRepository authRepository = AuthRepository.getInstance(requireContext());
        authRepository.setPairingComplete(true);
        AuthNavigator.goToDashboard(requireActivity());
    }

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

    private boolean isNameUsable() {
        String name = typedName();
        return !name.isEmpty()
                && !ProfileInputValidator.isInvalidAquariumName(name)
                && !isDuplicateName(name);
    }

    private boolean isDuplicateName(@NonNull String name) {
        for (Aquarium existing : AquariumRepository.getInstance(requireContext()).getAquariums()) {
            if (existing.getName().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

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

    private void updateCreateEnabled() {
        boolean ready = selectedChoiceId != null && isNameUsable() && advancedEditor.isValid();
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
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.dialog_limit_reached_title)
                    .setMessage(getString(
                            R.string.dialog_limit_reached_message, AquariumRepository.MAX_AQUARIUMS_LIMIT))
                    .setPositiveButton(R.string.action_ok, null)
                    .show();
            return;
        }

        if (!advancedEditor.isValid()) {
            if (!advancedExpanded) {
                showAdvanced(true);
            }
            Toast.makeText(getContext(), R.string.toast_fix_advanced, Toast.LENGTH_SHORT).show();
            return;
        }

        NewAquariumConfig config = buildConfig(name);
        if (firstRun) {
            goToMountHub(config);
            return;
        }
        pairingLauncher.launch(PairingActivity.intent(
                requireContext(), config, PairingEntryMode.ADD_AQUARIUM));
    }

    @NonNull
    private NewAquariumConfig buildConfig(@NonNull String name) {
        ThresholdEditor.Snapshot snapshot = advancedEditor.snapshotAll();
        return new NewAquariumConfig(
                name, selectedWaterType(), snapshot.getThresholds(), snapshot.getSpikeDeltas());
    }

    @Nullable
    private AquariumTemplate selectedTemplate() {
        return BuiltInTemplates.fromId(selectedChoiceId);
    }

    @NonNull
    private WaterType selectedWaterType() {
        AquariumTemplate template = selectedTemplate();
        return template != null
                ? template.getWaterType()
                : BuiltInTemplates.getDefault().getWaterType();
    }

    private void setupTemplateChoices(View view) {
        LinearLayout container = view.findViewById(R.id.templateChoiceContainer);
        LayoutInflater inflater = LayoutInflater.from(requireContext());

        templateChoices.clear();
        container.removeAllViews();
        for (AquariumTemplate template : BuiltInTemplates.all()) {
            container.addView(buildTemplateChoice(inflater, container, template));
        }

        showSelectedChoice();
        for (String choiceId : templateChoices.keySet()) {
            showDetails(choiceId);
        }
    }

    private void setupAdvanced(View view) {
        advancedEditorView = view.findViewById(R.id.advancedEditor);
        advancedChevron = view.findViewById(R.id.advancedChevron);
        tvAdvancedHint = view.findViewById(R.id.tvAdvancedHint);

        advancedEditor = new ThresholdEditor(advancedEditorView, this::onAdvancedChanged);

        view.findViewById(R.id.advancedToggle).setOnClickListener(v -> {
            if (selectedChoiceId != null) {
                showAdvanced(!advancedExpanded);
            }
        });
    }

    private void syncAdvanced(@Nullable String replacedTemplateId) {
        AquariumTemplate template = selectedTemplate();
        if (template == null) {
            advancedEditor.clear();
            showAdvanced(false);
            return;
        }

        boolean hadEdits = advancedEditor.hasUnsavedEdits();
        advancedEditor.showTemplate(template);
        advancedEditor.restoreState(pendingEditorState);
        pendingEditorState = null;

        if (hadEdits && replacedTemplateId != null && !replacedTemplateId.equals(selectedChoiceId)) {
            Toast.makeText(getContext(),
                    getString(R.string.advanced_reset_to_template, getString(template.getNameResId())),
                    Toast.LENGTH_SHORT).show();
        }

        showAdvanced(advancedExpanded);
    }

    private void showAdvanced(boolean expanded) {
        advancedExpanded = expanded;
        advancedEditorView.setVisibility(expanded ? View.VISIBLE : View.GONE);
        advancedChevron.setRotation(expanded ? CHEVRON_OPEN_ROTATION : 0f);
        onAdvancedChanged();
    }

    private void onAdvancedChanged() {
        showAdvancedHint();
        updateCreateEnabled();
    }

    private void showAdvancedHint() {
        AquariumTemplate template = selectedTemplate();
        boolean problem = !advancedEditor.isValid();

        tvAdvancedHint.setTextColor(ContextCompat.getColor(
                requireContext(), problem ? R.color.danger : R.color.text_secondary));

        if (problem) {
            tvAdvancedHint.setText(R.string.advanced_needs_attention);
        } else if (template == null) {
            tvAdvancedHint.setText(R.string.advanced_pick_template_first);
        } else if (advancedExpanded) {
            tvAdvancedHint.setText(
                    getString(R.string.advanced_expanded_hint, getString(template.getNameResId())));
        } else {
            tvAdvancedHint.setText(R.string.advanced_collapsed_hint);
        }
    }

    @Override
    public void onDestroyView() {
        pendingEditorState = new Bundle();
        advancedEditor.saveState(pendingEditorState);

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

    private void selectChoice(@NonNull String choiceId) {
        String replaced = selectedChoiceId;
        selectedChoiceId = choiceId.equals(selectedChoiceId) ? null : choiceId;
        showSelectedChoice();
        syncAdvanced(replaced);
    }

    private void toggleDetails(@NonNull String choiceId) {
        if (!expandedChoiceIds.remove(choiceId)) {
            expandedChoiceIds.add(choiceId);
        }
        showDetails(choiceId);
    }

    private void showSelectedChoice() {
        for (Map.Entry<String, View> choice : templateChoices.entrySet()) {
            ((RadioButton) choice.getValue().findViewById(R.id.rbTemplateChoice))
                    .setChecked(choice.getKey().equals(selectedChoiceId));
        }
        updateCreateEnabled();
    }

    private void showDetails(@NonNull String choiceId) {
        View row = templateChoices.get(choiceId);
        if (row == null) {
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
        params.setMarginStart(expanded ? 0 : Math.round(
                EXPANDER_INDENT_DP * getResources().getDisplayMetrics().density));
        expander.setLayoutParams(params);
    }
}
