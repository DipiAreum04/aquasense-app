package ca.team6.aquasense.auth;

import android.os.Bundle;
import android.text.InputType;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;

import ca.team6.aquasense.R;
import ca.team6.aquasense.auth.ProfileInputValidator;
import ca.team6.aquasense.ui.InputFieldError;

public class RegisterFragment extends Fragment {

    private AuthRepository authRepository;
    private EditText etFullName;
    private EditText etEmail;
    private EditText etPassword;
    private EditText etConfirmPassword;
    private View boxFullName;
    private View boxEmail;
    private View boxPassword;
    private View boxConfirmPassword;
    private View icPasswordValid;
    private View icConfirmPasswordValid;
    private MaterialButton btnRegister;
    private MaterialButton btnGoogle;
    private ImageButton btnTogglePassword;
    private ImageButton btnToggleConfirmPassword;
    private boolean submitting;
    private boolean passwordVisible;
    private boolean confirmPasswordVisible;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_register, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authRepository = AuthRepository.getInstance(requireContext());
        etFullName = view.findViewById(R.id.etRegisterFullName);
        etEmail = view.findViewById(R.id.etRegisterEmail);
        etPassword = view.findViewById(R.id.etRegisterPassword);
        etConfirmPassword = view.findViewById(R.id.etRegisterConfirmPassword);
        boxFullName = view.findViewById(R.id.boxRegisterFullName);
        boxEmail = view.findViewById(R.id.boxRegisterEmail);
        boxPassword = view.findViewById(R.id.boxRegisterPassword);
        boxConfirmPassword = view.findViewById(R.id.boxRegisterConfirmPassword);
        icPasswordValid = view.findViewById(R.id.icRegisterPasswordValid);
        icConfirmPasswordValid = view.findViewById(R.id.icRegisterConfirmPasswordValid);
        btnRegister = view.findViewById(R.id.btnRegister);
        btnGoogle = view.findViewById(R.id.btnGoogleRegister);
        btnTogglePassword = view.findViewById(R.id.btnToggleRegisterPassword);
        btnToggleConfirmPassword = view.findViewById(R.id.btnToggleRegisterConfirmPassword);
        TextView linkLogin = view.findViewById(R.id.tvGoToLogin);

        InputFieldError.track(etFullName, boxFullName,
                () -> nameErrorFor(textOf(etFullName), R.string.auth_full_name_required) == 0);
        InputFieldError.track(etEmail, boxEmail,
                () -> emailErrorFor(textOf(etEmail)) == 0);

        InputFieldError.track(etPassword, boxPassword,
                () -> ProfileInputValidator.getPasswordErrorResId(rawTextOf(etPassword)) == 0,
                icPasswordValid);
        InputFieldError.track(etConfirmPassword, boxConfirmPassword,
                () -> confirmPasswordErrorFor(rawTextOf(etPassword), rawTextOf(etConfirmPassword)) == 0,
                icConfirmPasswordValid);
        InputFieldError.watch(etPassword, boxConfirmPassword,
                () -> confirmPasswordErrorFor(rawTextOf(etPassword), rawTextOf(etConfirmPassword)) == 0,
                icConfirmPasswordValid);

        btnRegister.setOnClickListener(v -> attemptRegister());
        btnGoogle.setOnClickListener(v -> attemptGoogleSignIn());
        btnTogglePassword.setOnClickListener(v ->
                passwordVisible = togglePasswordField(etPassword, btnTogglePassword, passwordVisible));
        btnToggleConfirmPassword.setOnClickListener(v ->
                confirmPasswordVisible = togglePasswordField(
                        etConfirmPassword, btnToggleConfirmPassword, confirmPasswordVisible));
        linkLogin.setOnClickListener(v -> goToLogin());
    }


    private void goToLogin() {
        NavController navController = NavHostFragment.findNavController(this);
        if (!navController.popBackStack(R.id.loginFragment, false)) {
            navController.navigate(R.id.loginFragment);
        }
    }

    private void attemptRegister() {
        if (submitting) {
            return;
        }

        String fullName = textOf(etFullName);
        String email = textOf(etEmail);
        String password = rawTextOf(etPassword);
        String confirmPassword = rawTextOf(etConfirmPassword);

        int fullNameError = nameErrorFor(fullName, R.string.auth_full_name_required);
        int emailError = emailErrorFor(email);
        int passwordError = ProfileInputValidator.getPasswordErrorResId(password);
        int confirmPasswordError = confirmPasswordErrorFor(password, confirmPassword);

        InputFieldError.set(boxFullName, stateFor(fullNameError));
        InputFieldError.set(boxEmail, stateFor(emailError));
        InputFieldError.set(boxPassword, passStateFor(passwordError), icPasswordValid);
        InputFieldError.set(boxConfirmPassword, passStateFor(confirmPasswordError),
                icConfirmPasswordValid);

        int firstError = firstNonZero(fullNameError, emailError,
                passwordError, confirmPasswordError);
        if (firstError != 0) {
            toast(firstError);
            return;
        }

        setSubmitting(true);
        authRepository.register(fullName, email, password,
                new AuthRepository.AuthCallback() {
                    @Override
                    public void onSuccess(@NonNull FirebaseUser user) {
                        if (!isAdded()) {
                            return;
                        }
                        setSubmitting(false);
                        AuthNavigator.continueAfterAuth(requireActivity(), authRepository);
                    }

                    @Override
                    public void onError(int messageResId) {
                        if (!isAdded()) {
                            return;
                        }
                        setSubmitting(false);
                        toast(messageResId);
                    }
                });
    }

    private void attemptGoogleSignIn() {
        if (submitting) {
            return;
        }
        setSubmitting(true);
        authRepository.signInWithGoogle(requireActivity(), new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(@NonNull FirebaseUser user) {
                if (!isAdded()) {
                    return;
                }
                setSubmitting(false);
                AuthNavigator.continueAfterAuth(requireActivity(), authRepository);
            }

            @Override
            public void onError(int messageResId) {
                if (!isAdded()) {
                    return;
                }
                setSubmitting(false);
                toast(messageResId);
            }
        });
    }

    private boolean togglePasswordField(@NonNull EditText editText,
                                        @NonNull ImageButton toggleButton,
                                        boolean currentlyVisible) {
        boolean visible = !currentlyVisible;
        int selection = editText.getSelectionEnd();
        if (visible) {
            editText.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            toggleButton.setImageResource(R.drawable.visibility_off_24px);
            toggleButton.setContentDescription(getString(R.string.auth_hide_password));
        } else {
            editText.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            toggleButton.setImageResource(R.drawable.visibility_24px);
            toggleButton.setContentDescription(getString(R.string.auth_show_password));
        }
        editText.setSelection(Math.max(selection, 0));
        return visible;
    }

    private static InputFieldError.State stateFor(int errorResId) {
        return errorResId == 0 ? InputFieldError.State.NEUTRAL : InputFieldError.State.ERROR;
    }

    private static InputFieldError.State passStateFor(int errorResId) {
        return errorResId == 0 ? InputFieldError.State.VALID : InputFieldError.State.ERROR;
    }

    @StringRes
    private static int nameErrorFor(String name, @StringRes int requiredMessage) {
        if (TextUtils.isEmpty(name)) {
            return requiredMessage;
        }
        if (ProfileInputValidator.isInvalidName(name)) {
            return R.string.edit_profile_name_invalid;
        }
        return 0;
    }

    @StringRes
    private static int emailErrorFor(String email) {
        if (TextUtils.isEmpty(email)) {
            return R.string.email_required;
        }
        if (ProfileInputValidator.isInvalidEmail(email)) {
            return R.string.email_invalid;
        }
        return 0;
    }

    @StringRes
    private static int confirmPasswordErrorFor(String password, String confirmPassword) {
        if (TextUtils.isEmpty(confirmPassword)) {
            return R.string.auth_confirm_password_required;
        }
        if (ProfileInputValidator.passwordsDoNotMatch(password, confirmPassword)) {
            return R.string.auth_passwords_do_not_match;
        }
        return 0;
    }

    @StringRes
    private static int firstNonZero(int... errors) {
        for (int error : errors) {
            if (error != 0) {
                return error;
            }
        }
        return 0;
    }

    private void setSubmitting(boolean value) {
        submitting = value;
        btnRegister.setEnabled(!value);
        btnGoogle.setEnabled(!value);
    }

    private static String textOf(@Nullable EditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString().trim();
    }

    private static String rawTextOf(@Nullable EditText editText) {
        if (editText == null || editText.getText() == null) {
            return "";
        }
        return editText.getText().toString();
    }

    private void toast(int messageResId) {
        Toast.makeText(requireContext(), messageResId, Toast.LENGTH_SHORT).show();
    }
}
