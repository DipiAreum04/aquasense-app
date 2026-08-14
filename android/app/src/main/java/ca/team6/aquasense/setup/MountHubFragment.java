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
import ca.team6.aquasense.model.NewAquariumConfig;
import ca.team6.aquasense.model.ScopedLogger;
import ca.team6.aquasense.pairing.PairingEntryMode;
import ca.team6.aquasense.ui.FragmentToolbar;
import ca.team6.aquasense.ui.WizardProgress;

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

    private void goToPairing() {
        NewAquariumConfig config =
                this.requireArgs().getParcelable(SetupArgs.AQUARIUM_CONFIG);
        if (config == null) {
            ScopedLogger.error("Reached the mount-hub step with no aquarium configuration.");
            return;
        }

        this.pairingLauncher.launch(
                PairingActivity.intent(requireContext(), config, PairingEntryMode.FIRST_RUN));
    }

    private void goToSetupSummary() {
        Navigation.findNavController(requireView()).navigate(
                R.id.action_mountHubFragment_to_setupCompleteFragment, new Bundle(this.requireArgs()));
    }

    private void skipSetup() {
        AuthRepository.getInstance(requireContext()).setPairingComplete(true);
        AuthNavigator.goToDashboard(requireActivity());
    }

    @NonNull
    private Bundle requireArgs() {
        Bundle args = getArguments();
        return args == null ? new Bundle() : args;
    }
}
