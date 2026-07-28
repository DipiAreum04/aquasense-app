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
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.ProfileInputValidator;
import ca.team6.aquasense.ui.InputFieldError;

public class LoginFragment extends Fragment {

    private AuthRepository authRepository;
    private EditText etEmail;
    private EditText etPassword;
    private View boxEmail;
    private View boxPassword;
    private InputFieldError.Rejection credentialRejection;
    private MaterialButton btnLogin;
    private MaterialButton btnGoogle;
    private ImageButton btnTogglePassword;
    private boolean submitting;
    private boolean passwordVisible;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        authRepository = new AuthRepository(requireContext());
        etEmail = view.findViewById(R.id.etLoginEmail);
        etPassword = view.findViewById(R.id.etLoginPassword);
        boxEmail = view.findViewById(R.id.boxLoginEmail);
        boxPassword = view.findViewById(R.id.boxLoginPassword);
        btnLogin = view.findViewById(R.id.btnLogin);
        btnGoogle = view.findViewById(R.id.btnGoogleLogin);
        btnTogglePassword = view.findViewById(R.id.btnToggleLoginPassword);
        TextView linkRegister = view.findViewById(R.id.tvGoToRegister);

        InputFieldError.track(etEmail, boxEmail,
                () -> emailErrorFor(textOf(etEmail)) == 0);
        InputFieldError.track(etPassword, boxPassword,
                () -> passwordErrorFor(rawTextOf(etPassword)) == 0);
        credentialRejection =
                InputFieldError.rejectionFor(etEmail, boxEmail, etPassword, boxPassword);

        btnLogin.setOnClickListener(v -> attemptLogin());
        btnGoogle.setOnClickListener(v -> attemptGoogleSignIn());
        btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
        linkRegister.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_login_to_register)); // TODO: check if this is correct
    }

    private void attemptLogin() {
        if (submitting) {
            return;
        }

        String email = textOf(etEmail);
        String password = rawTextOf(etPassword);

        // A new attempt supersedes whatever the last one was rejected for
        credentialRejection.reset();

        // Re-check every field on each submit so outlines reflect the current state
        int emailError = emailErrorFor(email);
        int passwordError = passwordErrorFor(password);

        InputFieldError.set(boxEmail, stateFor(emailError));
        InputFieldError.set(boxPassword, stateFor(passwordError));

        // Every failing field is outlined, but only the first message is toasted.
        if (emailError != 0) {
            toast(emailError);
            return;
        }
        if (passwordError != 0) {
            toast(passwordError);
            return;
        }

        setSubmitting(true);
        authRepository.signIn(email, password, new AuthRepository.AuthCallback() {
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
                if (isCredentialError(messageResId)) {
                    credentialRejection.show();
                }
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

    private void togglePasswordVisibility() {
        passwordVisible = !passwordVisible;
        int selection = etPassword.getSelectionEnd();
        if (passwordVisible) {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.visibility_off_24px);
            btnTogglePassword.setContentDescription(getString(R.string.auth_hide_password));
        } else {
            etPassword.setInputType(InputType.TYPE_CLASS_TEXT
                    | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            btnTogglePassword.setImageResource(R.drawable.visibility_24px);
            btnTogglePassword.setContentDescription(getString(R.string.auth_show_password));
        }
        etPassword.setSelection(Math.max(selection, 0));
    }

    /**
     * True for errors that mean "this email and password combination is invalid".
     * Network and generic failures are excluded on purpose.
     */
    private static boolean isCredentialError(int messageResId) {
        return messageResId == R.string.auth_error_invalid_credentials
                || messageResId == R.string.auth_error_wrong_password
                || messageResId == R.string.auth_error_user_not_found;
    }

    // Login fields stay unoutlined when they pass
    private static InputFieldError.State stateFor(int errorResId) {
        return errorResId == 0 ? InputFieldError.State.NEUTRAL : InputFieldError.State.ERROR;
    }

    // Returns the message for the first failed rule, or 0 when the value is acceptable.
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

    // Login only checks that a password was entered.
    @StringRes
    private static int passwordErrorFor(String password) {
        if (TextUtils.isEmpty(password)) {
            return R.string.auth_password_required;
        }
        return 0;
    }

    private void setSubmitting(boolean value) {
        submitting = value;
        btnLogin.setEnabled(!value);
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
