package ca.team6.aquasense.settings;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.ProfileInputValidator;
import ca.team6.aquasense.model.SettingsRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;

public class EditProfileFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit_profile, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        EditText etName = view.findViewById(R.id.etEditProfileName);
        EditText etEmail = view.findViewById(R.id.etEditProfileEmail);
        Button btnSave = view.findViewById(R.id.btnSaveProfile);

        SharedPreferenceHelper prefs = SharedPreferenceHelper.getInstance(requireContext());
        SettingsRepository repo = new SettingsRepository(requireContext());
        repo.loadSettings(settings -> {
            if (!TextUtils.isEmpty(settings.profileName)) {
                etName.setText(settings.profileName);
            }
            if (!TextUtils.isEmpty(settings.profileEmail)) {
                etEmail.setText(settings.profileEmail);
            }
        });

        btnSave.setOnClickListener(v -> {
            String name = etName.getText() != null
                    ? etName.getText().toString().trim()
                    : "";
            String email = etEmail.getText() != null
                    ? etEmail.getText().toString().trim()
                    : "";

            if (TextUtils.isEmpty(name)) {
                Toast.makeText(requireContext(),
                        R.string.edit_profile_name_required,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (ProfileInputValidator.isInvalidName(name)) {
                Toast.makeText(requireContext(),
                        R.string.edit_profile_name_invalid,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(requireContext(),
                        R.string.edit_profile_email_required,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (ProfileInputValidator.isInvalidEmail(email)) {
                Toast.makeText(requireContext(),
                        R.string.edit_profile_email_invalid,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // TODO: After Firebase is set up, sync profile to Firebase Auth / Firestore.
            // Implement Registration and Login functionality first.
            prefs.updateField(SettingsRepository.KEY_PROFILE_NAME, name);
            prefs.updateField(SettingsRepository.KEY_PROFILE_EMAIL, email);

            Toast.makeText(requireContext(),
                    R.string.edit_profile_saved,
                    Toast.LENGTH_SHORT).show();
            Navigation.findNavController(v).navigateUp();
        });
    }
}
