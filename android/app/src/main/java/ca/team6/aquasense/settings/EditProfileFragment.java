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
import ca.team6.aquasense.auth.AuthRepository;
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
        AuthRepository authRepository = new AuthRepository(requireContext());
        authRepository.syncProfileCacheFromFirebase();

        String name = authRepository.getProfileDisplayName();
        String email = authRepository.getProfileEmail();
        if (!TextUtils.isEmpty(name)) {
            etName.setText(name);
        }
        if (!TextUtils.isEmpty(email)) {
            etEmail.setText(email);
        }

        btnSave.setOnClickListener(v -> {
            String editedName = etName.getText() != null
                    ? etName.getText().toString().trim()
                    : "";
            String editedEmail = etEmail.getText() != null
                    ? etEmail.getText().toString().trim()
                    : "";

            if (TextUtils.isEmpty(editedName)) {
                Toast.makeText(requireContext(),
                        R.string.edit_profile_name_required,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (ProfileInputValidator.isInvalidName(editedName)) {
                Toast.makeText(requireContext(),
                        R.string.edit_profile_name_invalid,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(editedEmail)) {
                Toast.makeText(requireContext(),
                        R.string.email_required,
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (ProfileInputValidator.isInvalidEmail(editedEmail)) {
                Toast.makeText(requireContext(),
                        R.string.email_invalid,
                        Toast.LENGTH_SHORT).show();
                return;
            }

            // TODO: After Firebase is set up, sync profile to Firebase Auth / Firestore.
            // Implement Registration and Login functionality first.
            prefs.updateField(SettingsRepository.KEY_PROFILE_NAME, editedName);
            prefs.updateField(SettingsRepository.KEY_PROFILE_EMAIL, editedEmail);

            Toast.makeText(requireContext(),
                    R.string.edit_profile_saved,
                    Toast.LENGTH_SHORT).show();
            Navigation.findNavController(v).navigateUp();
        });
    }
}
