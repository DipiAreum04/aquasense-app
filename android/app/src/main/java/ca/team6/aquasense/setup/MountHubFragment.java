package ca.team6.aquasense.setup;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import ca.team6.aquasense.PairingActivity;
import ca.team6.aquasense.R;
import ca.team6.aquasense.auth.AuthNavigator;
import ca.team6.aquasense.auth.AuthRepository;
import ca.team6.aquasense.model.WaterType;
import ca.team6.aquasense.model.aquarium_templates.BuiltInTemplates;
import ca.team6.aquasense.pairing.PairingEntryMode;
import ca.team6.aquasense.ui.FragmentToolbar;
import ca.team6.aquasense.ui.WizardProgress;

/**
 * Step 2 of the first-installation wizard: fitting the hub and its four sensors to the aquarium.
 *
 * <p>Reading only. It sits between the form and pairing because that is the order the work
 * happens in: pairing ends by waiting for the board's first real reading, so a hub that is still
 * in its box, or whose probes are dry, fails the last stage of step 3 rather than the first.
 *
 * <p>It also carries the aquarium details across, and launches pairing itself, because pairing is
 * a separate activity: whoever starts it is the one that hears it finish, and the summary page
 * comes after.
 */
public class MountHubFragment extends Fragment {

    private final ActivityResultLauncher<Intent> pairingLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == Activity.RESULT_OK && isAdded()) {
                            this.goToSetupSummary();
                        }
                    });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setup_mount_hub, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        FragmentToolbar.setup(this, view, R.id.toolbar_mount_hub);
        WizardProgress.show(view, 2);

        view.findViewById(R.id.btnMountHubNext).setOnClickListener(v -> this.goToPairing());
        view.findViewById(R.id.btnSkipSetup).setOnClickListener(v -> this.skipSetup());
    }

    /**
     * Hands the details collected on step 1 to the pairing flow, which is what writes the
     * aquarium once the board reports the UID it will be keyed by.
     */
    private void goToPairing() {
        Bundle args = this.requireArgs();
        WaterType waterType = WaterType.fromKey(args.getString(SetupArgs.WATER_TYPE));

        this.pairingLauncher.launch(PairingActivity.intent(
                requireContext(),
                args.getString(SetupArgs.AQUARIUM_NAME, ""),
                // The form always sends one; a null here would mean the argument was lost in
                // transit, and marine thresholds on a freshwater tank are the safer way to be
                // wrong than the reverse.
                waterType == null ? BuiltInTemplates.getDefault().getWaterType() : waterType,
                args.getString(SetupArgs.TEMPLATE_ID),
                PairingEntryMode.FIRST_RUN));
    }

    /** Closes the wizard on its summary page, carrying the same details one destination further. */
    private void goToSetupSummary() {
        Navigation.findNavController(requireView()).navigate(
                R.id.action_mountHubFragment_to_setupCompleteFragment, new Bundle(this.requireArgs()));
    }

    /**
     * Leaves setup with nothing created. The dashboard has an empty state for an account with no
     * aquarium, and pairing can be started again from there.
     */
    private void skipSetup() {
        AuthRepository.getInstance(requireContext()).setPairingComplete(true);
        AuthNavigator.goToDashboard(requireActivity());
    }

    /**
     * The destination's arguments, which the graph gives defaults for, so this is only empty if
     * the fragment is ever shown outside the wizard.
     */
    @NonNull
    private Bundle requireArgs() {
        Bundle args = getArguments();
        return args == null ? new Bundle() : args;
    }
}
