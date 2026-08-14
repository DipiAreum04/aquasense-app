package ca.team6.aquasense.settings;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
import androidx.fragment.app.Fragment;

import ca.team6.aquasense.R;
import ca.team6.aquasense.analytics.AquariumDropdown;
import ca.team6.aquasense.aquarium.Aquarium;
import ca.team6.aquasense.aquarium.AquariumRepository;
import ca.team6.aquasense.aquarium.WaterType;

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

    private void setUpAquariumSelector(View root) {
        View card = root.findViewById(R.id.waterParametersAquariumSelector);
        AquariumDropdown dropdown = new AquariumDropdown(
                card, root.findViewById(R.id.waterParametersAquariumChevron), this::selectAquarium);

        card.setOnClickListener(v -> dropdown.show(
                this.aquariums.getAquariums(), this.aquariums.getActiveAquariumId()));
    }

    private void selectAquarium(@NonNull Aquarium picked) {
        if (this.aquarium != null && picked.getId().equals(this.aquarium.getId())) {
            return;
        }
        if (this.editor.hasUnsavedEdits()) {
            new MaterialAlertDialogBuilder(requireContext(),
                    R.style.ThemeOverlay_AquaSense_MaterialAlertDialog_Danger)
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
        this.pendingEditorState = null;
        this.showAquarium(picked);
    }

    private void bindFirstAquarium() {
        if (this.aquarium != null || this.tvAquariumName == null) {
            return;
        }

        Aquarium active = this.aquariums.getActiveAquarium();
        if (active == null) {
            this.showNoAquarium(this.aquariums.isLoaded());
            return;
        }

        this.detachObserver();
        this.showAquarium(active);
    }

    private void showAquarium(@NonNull Aquarium active) {
        this.aquarium = active;
        this.showAquariumCard(active);
        this.editor.showAquarium(active);

        if (!this.editor.hasSensors()) {
            this.showNoAquarium(true);
            return;
        }

        this.editor.restoreState(this.pendingEditorState);
        this.pendingEditorState = null;

        this.tvEmpty.setVisibility(View.GONE);
        this.form.setVisibility(View.VISIBLE);

        int disabledNoteResId = active.getSensorDisabledNoteResId();
        boolean explainMissingTab = disabledNoteResId != 0
                && this.editor.getSensorIds().size() < ThresholdForm.CONFIGURABLE_SENSOR_IDS.size();
        if (explainMissingTab) {
            this.tvTdsNote.setText(disabledNoteResId);
        }
        this.cardTdsNote.setVisibility(explainMissingTab ? View.VISIBLE : View.GONE);

        this.updateSaveState();
    }

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
        this.cardTdsNote.setVisibility(View.GONE);
    }

    private void updateSaveState() {
        boolean enabled = this.editor.isDirty() && this.editor.isValid() && !this.saving;
        this.btnSave.setEnabled(enabled);
        this.btnSave.setAlpha(enabled ? 1f : DISABLED_BUTTON_ALPHA);
    }

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
