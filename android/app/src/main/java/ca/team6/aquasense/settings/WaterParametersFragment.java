package ca.team6.aquasense.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import ca.team6.aquasense.R;
import ca.team6.aquasense.analytics.AquariumDropdown;
import ca.team6.aquasense.model.Aquarium;
import ca.team6.aquasense.model.AquariumRepository;
import ca.team6.aquasense.model.WaterType;

/**
 * SETTINGS-07 - lets the user set the thresholds their aquarium is judged against.
 *
 * <p>The numbers edited here are the same ones the dashboard colours its cards by, the analytics
 * page counts in-range readings against and the background monitor raises alerts from, so nothing
 * is stored locally: a save writes straight to {@code /{uid}/aquariums/{id}} and the live
 * subscription carries it back to every one of those screens.
 *
 * <p>The form itself is {@link ThresholdEditor}, shared with the add-aquarium screen's Advanced
 * Settings panel. What this screen adds around it is an aquarium to point it at and a save: one
 * press writes every sensor that changed, so a screen where two tabs were edited cannot half-save.
 *
 * <p>Switching <em>aquarium</em> rebuilds the form from the database, and anything unsaved is
 * offered up for discarding rather than carried across to a tank it was not typed for.
 */
public class WaterParametersFragment extends Fragment {

    private static final float DISABLED_BUTTON_ALPHA = 0.5f;

    private static final String STATE_EDITOR = "waterParametersEditor";

    private AquariumRepository aquariums;
    @Nullable
    private AquariumRepository.AquariumsObserver observer;
    @Nullable
    private Aquarium aquarium;
    private ThresholdEditor editor;

    private ImageView ivAquariumIcon;
    private TextView tvAquariumName;
    private TextView tvEmpty;
    private View form;
    private View cardTdsNote;
    private TextView tvTdsNote;
    private View btnSave;

    private boolean saving;

    /**
     * Held until the aquarium it was taken for is on screen. The first snapshot arrives
     * asynchronously, so a rotation can rebuild this screen before there is a form to restore onto.
     */
    @Nullable
    private Bundle pendingEditorState;

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

        this.ivAquariumIcon = view.findViewById(R.id.waterParametersAquariumIcon);
        this.tvAquariumName = view.findViewById(R.id.waterParametersAquariumName);
        this.tvEmpty = view.findViewById(R.id.tvWaterParametersEmpty);
        this.form = view.findViewById(R.id.containerWaterParametersForm);
        this.cardTdsNote = view.findViewById(R.id.cardWaterParametersTdsNote);
        this.tvTdsNote = view.findViewById(R.id.tvWaterParametersTdsNote);
        this.btnSave = view.findViewById(R.id.btnSaveThresholds);

        this.editor = new ThresholdEditor(
                view.findViewById(R.id.waterParametersEditor), this::updateSaveState);

        if (savedInstanceState != null) {
            this.pendingEditorState = savedInstanceState.getBundle(STATE_EDITOR);
        }

        this.setUpAquariumSelector(view);
        this.btnSave.setOnClickListener(v -> this.save());

        // Fires immediately with whatever the cache holds, and again when the first snapshot lands
        // for a user who opened settings straight after signing in.
        this.observer = list -> this.bindFirstAquarium();
        this.aquariums.addObserver(this.observer);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        if (this.editor == null) {
            return;
        }
        Bundle editorState = new Bundle();
        this.editor.saveState(editorState);
        outState.putBundle(STATE_EDITOR, editorState);
    }

    @Override
    public void onDestroyView() {
        this.detachObserver();
        super.onDestroyView();
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
        if (this.editor.hasUnsavedEdits()) {
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
        // Values restored from a rotation belong to whichever aquarium was on screen then, so a
        // deliberate switch drops them rather than pouring them into a different tank's form.
        this.pendingEditorState = null;
        this.showAquarium(picked);
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
        this.aquarium = active;
        this.showAquariumCard(active);
        this.editor.showAquarium(active);

        if (!this.editor.hasSensors()) {
            this.showNoAquarium(true);
            return;
        }

        // Applied after the editor is seeded, since it only restores onto the same set of tabs.
        this.editor.restoreState(this.pendingEditorState);
        this.pendingEditorState = null;

        this.tvEmpty.setVisibility(View.GONE);
        this.form.setVisibility(View.VISIBLE);

        // Only the saltwater template carries one, for the TDS sensor it cannot read.
        int disabledNoteResId = active.getSensorDisabledNoteResId();
        boolean explainMissingTab = disabledNoteResId != 0
                && this.editor.getSensorIds().size() < ThresholdForm.CONFIGURABLE_SENSOR_IDS.size();
        if (explainMissingTab) {
            this.tvTdsNote.setText(disabledNoteResId);
        }
        this.cardTdsNote.setVisibility(explainMissingTab ? View.VISIBLE : View.GONE);

        this.updateSaveState();
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
     * Enables the save only when there is something to save and every sensor's draft holds
     * together, including the ones on tabs that are not on screen. The editor puts up its own note
     * naming the tab that is holding it up, so the button is never inexplicably dead.
     */
    private void updateSaveState() {
        boolean enabled = this.editor.isDirty() && this.editor.isValid() && !this.saving;
        this.btnSave.setEnabled(enabled);
        this.btnSave.setAlpha(enabled ? 1f : DISABLED_BUTTON_ALPHA);
    }

    /**
     * Writes every sensor whose draft differs from the database, in one update, so a screen where
     * two tabs were edited cannot half-save.
     */
    private void save() {
        if (this.aquarium == null || this.saving) {
            return;
        }

        ThresholdEditor.Snapshot snapshot = this.editor.snapshotDirty();
        if (snapshot.isEmpty()) {
            return;
        }

        this.saving = true;
        this.updateSaveState();

        this.aquariums.saveSensorThresholds(this.aquarium, snapshot.getThresholds(),
                snapshot.getSpikeDeltas(), new AquariumRepository.WriteCallback() {
                    @Override
                    public void onSuccess() {
                        if (getView() == null) {
                            return;
                        }
                        snapshot.markSaved();
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
}
