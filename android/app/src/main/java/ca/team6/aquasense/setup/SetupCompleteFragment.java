package ca.team6.aquasense.setup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import ca.team6.aquasense.R;
import ca.team6.aquasense.auth.AuthNavigator;
import ca.team6.aquasense.model.WaterType;
import ca.team6.aquasense.model.aquarium_templates.AquariumTemplate;
import ca.team6.aquasense.model.aquarium_templates.BuiltInTemplates;
import ca.team6.aquasense.ui.RevealSequence;

/**
 * Closing page of the first-installation wizard (SETTINGS-03): confirms what was set up and hands
 * the user to the dashboard.
 *
 * <p>Only reached once the hub is paired, which is also the point at which the aquarium was
 * written to the database. Everything shown here is echoed back from the wizard's own arguments
 * rather than read from the database, so the page does not sit blank waiting for a snapshot.
 */
public class SetupCompleteFragment extends Fragment {

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

        this.showSummary(view);

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
                    view.findViewById(R.id.completeActions));
        }
    }

    private void showSummary(@NonNull View view) {
        Bundle args = getArguments();
        String name = args == null ? "" : args.getString(SetupArgs.AQUARIUM_NAME, "");

        ((TextView) view.findViewById(R.id.tvCompleteBody))
                .setText(getString(R.string.setup_complete_body, name));
        ((TextView) view.findViewById(R.id.tvCompleteName)).setText(name);

        WaterType waterType =
                WaterType.fromKey(args == null ? null : args.getString(SetupArgs.WATER_TYPE));
        ((TextView) view.findViewById(R.id.tvCompleteWaterType)).setText(
                (waterType == null ? BuiltInTemplates.getDefault().getWaterType() : waterType)
                        .getLabelResId());

        // Custom aquariums are not backed by a template, so the row goes rather than showing blank.
        AquariumTemplate template =
                BuiltInTemplates.fromId(args == null ? null : args.getString(SetupArgs.TEMPLATE_ID));
        if (template == null) {
            view.findViewById(R.id.completeTemplateDivider).setVisibility(View.GONE);
            view.findViewById(R.id.completeTemplateRow).setVisibility(View.GONE);
            return;
        }
        ((TextView) view.findViewById(R.id.tvCompleteTemplate)).setText(template.getNameResId());
    }
}
