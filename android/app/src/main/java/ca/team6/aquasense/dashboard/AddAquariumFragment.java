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

/**
 * Dedicated fragment screen for configuring new aquarium profiles with template
 * selection.
 */
public class AddAquariumFragment extends Fragment {

    /** Set when the form is the first thing shown after signing up, which adds the skip option. */
    public static final String ARG_FIRST_RUN = "firstRun";

    private static final int EXPANDER_INDENT_DP = 54;

    /** Quarter turn on the Advanced panel's chevron, which points right while it is shut. */
    private static final float CHEVRON_OPEN_ROTATION = 90f;

    private static final String STATE_SELECTED_CHOICE = "selectedChoiceId";
    private static final String STATE_EXPANDED_CHOICES = "expandedChoiceIds";
    private static final String STATE_ADVANCED_EXPANDED = "advancedExpanded";
    private static final String STATE_ADVANCED_EDITOR = "advancedEditor";

    // Dimming the disabled Create button while the form is incomplete
    private static final float DISABLED_BUTTON_ALPHA = 0.5f;

    private EditText etAquariumName;
    private TextView tvAquariumNameError;
    private ScrollView scrollView;
    private View stepTemplateHeader;
    private Button btnCreate;
    private boolean scrolledToTemplates;
    private boolean firstRun;

    // Step 3. Seeded from whichever template is selected, whether or not the panel is ever opened:
    // what it holds is what gets written either way.
    private ThresholdEditor advancedEditor;
    private View advancedEditorView;
    private View advancedChevron;
    private TextView tvAdvancedHint;
    private boolean advancedExpanded;
    /**
     * Held from a rotation until the editor has been seeded with the same set of sensors, which
     * cannot happen until the template rows are rebuilt and the selection restored.
     */
    @Nullable
    private Bundle pendingEditorState;

    // Only the route in from the aquarium list pairs from this screen. The wizard has a
    // mount-the-hardware step in between, so there it is MountHubFragment that launches pairing.
    private final ActivityResultLauncher<Intent> pairingLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && isAdded()) {
                            Navigation.findNavController(requireView()).popBackStack();
                        }
                    });

    // Template ID to its row, in the order the rows are shown. Rebuilt with the view, so it is
    // cleared in onDestroyView rather than holding a dead hierarchy.
    private final Map<String, View> templateChoices = new LinkedHashMap<>();
    // Held as an ID rather than an AquariumTemplate so the selection survives the view being
    // recreated on rotation, where only what went into the bundle comes back.
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
        // Read before the rows are built, since it is what they are drawn from. Restores the form
        // after a rotation, and after backing up to it from the wizard's next step.
        restoreSelection(savedInstanceState);

        FragmentToolbar.setup(this, view, R.id.toolbar_add_aquarium);
        // On a first installation this is step 1 of the setup wizard, which is the only route that
        // has a step count to report. The strip stays hidden when the screen is opened from the
        // aquarium list.
        if (firstRun) {
            WizardProgress.show(view, 1);
        }

        btnCreate = view.findViewById(R.id.btnCreateAquarium);
        // In the wizard this only carries the form on to the mounting instructions; pairing, and
        // with it the aquarium's creation, is two steps away.
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
        // Seeds the panel from whatever selection was restored, and puts back anything typed into
        // it before the rotation. Runs after the rows exist, since it is driven by the selection.
        syncAdvanced(null);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // The typed name is restored by the EditText itself; which template is picked, which rows
        // are opened, and everything in the Advanced panel are only held here.
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
        // Dropped if it names a template that no longer exists, which leaves the form asking for a
        // choice rather than claiming one the user cannot see.
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

    /**
     * Carries the form on to the wizard's mounting instructions, which pairs from there. Nothing
     * is saved yet: the aquarium is keyed by the hub's own UID, so it cannot be written until a
     * hub reports one.
     */
    private void goToMountHub(@NonNull NewAquariumConfig config) {
        Bundle args = new Bundle();
        args.putParcelable(SetupArgs.AQUARIUM_CONFIG, config);
        // Carried beside the configuration rather than inside it: the summary page names the
        // template the user picked, but the template itself is never stored on the aquarium.
        args.putString(SetupArgs.TEMPLATE_ID, selectedChoiceId);

        Navigation.findNavController(requireView())
                .navigate(R.id.action_addAquariumFragment_to_mountHubFragment, args);
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
     * Enables the Create button once the user has entered a valid name, selected a template, and
     * left the Advanced panel in a state that can actually be written. The panel puts up its own
     * note naming the tab that is holding it up, so the button is never inexplicably dead.
     */
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
            new AlertDialog.Builder(requireContext())
                    .setTitle(R.string.dialog_limit_reached_title)
                    .setMessage(getString(
                            R.string.dialog_limit_reached_message, AquariumRepository.MAX_AQUARIUMS_LIMIT))
                    .setPositiveButton(R.string.action_ok, null)
                    .show();
            return;
        }

        // Every threshold on screen is written with the aquarium, so a panel that does not hold
        // together blocks the form even while it is collapsed and its errors are out of sight.
        if (!advancedEditor.isValid()) {
            if (!advancedExpanded) {
                showAdvanced(true);
            }
            Toast.makeText(getContext(), R.string.toast_fix_advanced, Toast.LENGTH_SHORT).show();
            return;
        }

        // The aquarium is written by PairingRepository once the board reports the UID it will be
        // keyed by, so this screen hands the details over rather than saving them itself.
        // TODO: Persist the tank volume and notes, which have nowhere in the schema to go yet.
        NewAquariumConfig config = buildConfig(name);
        if (firstRun) {
            // The wizard fits the hardware to the aquarium before pairing it, so the details go
            // to that step and it starts the pairing flow when the user is ready.
            goToMountHub(config);
            return;
        }
        pairingLauncher.launch(PairingActivity.intent(
                requireContext(), config, PairingEntryMode.ADD_AQUARIUM));
    }

    /**
     * Everything the form collected, ready to be written once there is a board UID to key it by.
     *
     * <p>The thresholds come from the Advanced panel whether or not the user ever opened it: it is
     * seeded from the selected template the moment that template is picked, so a form left alone
     * produces exactly the template's own numbers.
     */
    @NonNull
    private NewAquariumConfig buildConfig(@NonNull String name) {
        ThresholdEditor.Snapshot snapshot = advancedEditor.snapshotAll();
        return new NewAquariumConfig(
                name, selectedWaterType(), snapshot.getThresholds(), snapshot.getSpikeDeltas());
    }

    /** The template the user picked, or null while they have picked none. */
    @Nullable
    private AquariumTemplate selectedTemplate() {
        return BuiltInTemplates.fromId(selectedChoiceId);
    }

    /**
     * The water type to store, taken from the selected template.
     *
     * <p>Every template declares one and the form cannot be submitted without a template, so the
     * fallback here is unreachable rather than a default anyone relies on.
     */
    @NonNull
    private WaterType selectedWaterType() {
        AquariumTemplate template = selectedTemplate();
        return template != null
                ? template.getWaterType()
                : BuiltInTemplates.getDefault().getWaterType();
    }

    /**
     * Builds one row per built-in template. Driven by {@link BuiltInTemplates#all()} so a template
     * added there shows up here with no edit to this screen, and so the names, descriptions and
     * water types always match the thresholds the dashboard enforces.
     *
     * <p>There is no Custom row. Between them the templates cover both water types, so Custom's
     * only unique offer was a starting point with nothing behind it - and Advanced Settings edits
     * every one of these numbers anyway, from a baseline the user can actually reason about.
     */
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

    /**
     * Step 3. The panel is built and wired once; what it holds is replaced whenever the template
     * above it changes.
     */
    private void setupAdvanced(View view) {
        advancedEditorView = view.findViewById(R.id.advancedEditor);
        advancedChevron = view.findViewById(R.id.advancedChevron);
        tvAdvancedHint = view.findViewById(R.id.tvAdvancedHint);

        advancedEditor = new ThresholdEditor(advancedEditorView, this::onAdvancedChanged);

        view.findViewById(R.id.advancedToggle).setOnClickListener(v -> {
            // Nothing to show until a template has supplied the numbers to start from.
            if (selectedChoiceId != null) {
                showAdvanced(!advancedExpanded);
            }
        });
    }

    /**
     * Refills the panel from the selected template.
     *
     * <p>Whatever was typed against the previous template goes with it. Thresholds only mean
     * something next to the water they describe, so carrying coldwater numbers over onto a
     * saltwater tank would leave the form quietly claiming a template it no longer matches.
     *
     * @param replacedTemplateId the template being moved away from, or null when the panel is
     *     being filled in for the first time and there is nothing to have lost.
     */
    private void syncAdvanced(@Nullable String replacedTemplateId) {
        AquariumTemplate template = selectedTemplate();
        if (template == null) {
            // Deselecting takes the panel's contents with it, so there is nothing left to be
            // invalid and nothing for the note to warn about.
            advancedEditor.clear();
            showAdvanced(false);
            return;
        }

        boolean hadEdits = advancedEditor.hasUnsavedEdits();
        advancedEditor.showTemplate(template);
        advancedEditor.restoreState(pendingEditorState);
        pendingEditorState = null;

        // Said out loud only when there was something to lose: the reset is invisible otherwise,
        // and announcing it for a panel the user never touched would be noise.
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

    /**
     * The line under the panel's own header, which is the only thing it says while it is shut.
     *
     * <p>A broken threshold is called out here as well as inside, because the panel can be
     * collapsed over one: the errors then go off screen and the disabled Create button would be
     * the only sign left that anything is wrong.
     */
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
        // What was typed into the Advanced panel lives in the editor, which goes with the view.
        // Stepping forward in the wizard destroys the view but keeps this fragment on the back
        // stack, and returns with no saved-state bundle at all - so it is held here rather than
        // left to onSaveInstanceState, which that route never calls.
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

    private void selectChoice(@NonNull String choiceId) {
        String replaced = selectedChoiceId;
        selectedChoiceId = choiceId.equals(selectedChoiceId) ? null : choiceId;
        showSelectedChoice();
        syncAdvanced(replaced);
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
        // Matches the icon tile's width plus its gap, the indent the layout starts this row at.
        params.setMarginStart(expanded ? 0 : Math.round(
                EXPANDER_INDENT_DP * getResources().getDisplayMetrics().density));
        expander.setLayoutParams(params);
    }
}
