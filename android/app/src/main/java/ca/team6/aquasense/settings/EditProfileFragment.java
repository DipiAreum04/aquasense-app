package ca.team6.aquasense.settings;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import ca.team6.aquasense.R;
import ca.team6.aquasense.auth.AuthRepository;
import ca.team6.aquasense.model.ProfileInputValidator;

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
        TextView tvEmail = view.findViewById(R.id.tvEditProfileEmail);
        Button btnSave = view.findViewById(R.id.btnSaveProfile);

        AuthRepository authRepository = new AuthRepository(requireContext());
        authRepository.syncProfileCacheFromFirebase();

        String name = authRepository.getProfileDisplayName();
        String email = authRepository.getProfileEmail();
        if (!TextUtils.isEmpty(name)) {
            etName.setText(name);
        }
        if (!TextUtils.isEmpty(email)) {
            tvEmail.setText(email);
        }

        btnSave.setOnClickListener(v -> {
            String editedName = etName.getText() != null
                    ? etName.getText().toString().trim()
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
            // If the name is unchanged, do nothing
            if (editedName.equals(name)) {
                Navigation.findNavController(v).navigateUp();
                return;
            }

            setSaving(btnSave, true);
            authRepository.updateProfileName(editedName, new AuthRepository.ActionCallback() {
                @Override
                public void onSuccess() {
                    if (!isAdded()) {
                        return;
                    }
                    Toast.makeText(requireContext(),
                            R.string.edit_profile_saved,
                            Toast.LENGTH_SHORT).show();
                    Navigation.findNavController(requireView()).navigateUp();
                }

                @Override
                public void onError(int messageResId) {
                    if (!isAdded()) {
                        return;
                    }
                    setSaving(btnSave, false);
                    Toast.makeText(requireContext(), messageResId, Toast.LENGTH_LONG).show();
                }
            });
        });
    }

    // Firebase Auth and the database node are two sequential writes, so block a second tap
    // from starting a competing rename while the first is still in flight.
    private void setSaving(@NonNull Button btnSave, boolean saving) {
        btnSave.setEnabled(!saving);
        btnSave.setText(saving ? R.string.edit_profile_saving : R.string.edit_profile_save);
    }
}
