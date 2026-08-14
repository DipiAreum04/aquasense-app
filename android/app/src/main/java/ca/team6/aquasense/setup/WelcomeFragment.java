package ca.team6.aquasense.setup;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import ca.team6.aquasense.R;
import ca.team6.aquasense.auth.AuthNavigator;
import ca.team6.aquasense.auth.AuthRepository;
import ca.team6.aquasense.ui.RevealSequence;

public class WelcomeFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setup_welcome, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.btnGetStarted).setOnClickListener(v ->
                Navigation.findNavController(v)
                        .navigate(R.id.action_welcomeFragment_to_addAquariumFragment));
        view.findViewById(R.id.btnSkipSetup).setOnClickListener(v -> this.skipSetup());

        if (savedInstanceState == null) {
            RevealSequence.play(
                    view.findViewById(R.id.welcomeLogo),
                    view.findViewById(R.id.welcomeBrand),
                    view.findViewById(R.id.welcomeTitle),
                    view.findViewById(R.id.welcomeBody),
                    view.findViewById(R.id.welcomeIllustration),
                    view.findViewById(R.id.welcomeStepTank),
                    view.findViewById(R.id.welcomeStepMount),
                    view.findViewById(R.id.welcomeStepHub),
                    view.findViewById(R.id.welcomeActions));
        }
    }

    private void skipSetup() {
        AuthRepository.getInstance(requireContext()).setPairingComplete(true);
        AuthNavigator.goToDashboard(requireActivity());
    }
}
