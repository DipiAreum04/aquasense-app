package ca.team6.aquasense.auth;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseUser;

import ca.team6.aquasense.R;
import ca.team6.aquasense.auth.ProfileInputValidator;
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

        authRepository = AuthRepository.getInstance(requireContext());
        etEmail = view.findViewById(R.id.etLoginEmail);
        etPassword = view.findViewById(R.id.etLoginPassword);
        boxEmail = view.findViewById(R.id.boxLoginEmail);
        boxPassword = view.findViewById(R.id.boxLoginPassword);
        btnLogin = view.findViewById(R.id.btnLogin);
        btnGoogle = view.findViewById(R.id.btnGoogleLogin);
        btnTogglePassword = view.findViewById(R.id.btnToggleLoginPassword);
        TextView linkRegister = view.findViewById(R.id.tvGoToRegister);
        TextView linkForgotPassword = view.findViewById(R.id.tvForgotPassword);

        InputFieldError.track(etEmail, boxEmail,
                () -> emailErrorFor(textOf(etEmail)) == 0);
        InputFieldError.track(etPassword, boxPassword,
                () -> passwordErrorFor(rawTextOf(etPassword)) == 0);
        credentialRejection =
                InputFieldError.rejectionFor(etEmail, boxEmail, etPassword, boxPassword);

        btnLogin.setOnClickListener(v -> attemptLogin());
        btnGoogle.setOnClickListener(v -> attemptGoogleSignIn());
        btnTogglePassword.setOnClickListener(v -> togglePasswordVisibility());
        linkForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
        linkRegister.setOnClickListener(v ->
                NavHostFragment.findNavController(this).navigate(R.id.action_login_to_register));
    }

    private void attemptLogin() {
        if (submitting) {
            return;
        }

        String email = textOf(etEmail);
        String password = rawTextOf(etPassword);

        credentialRejection.reset();

        int emailError = emailErrorFor(email);
        int passwordError = passwordErrorFor(password);

        InputFieldError.set(boxEmail, stateFor(emailError));
        InputFieldError.set(boxPassword, stateFor(passwordError));

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

    private void showForgotPasswordDialog() {
        View content = getLayoutInflater().inflate(R.layout.dialog_forgot_password, null);
        EditText etResetEmail = content.findViewById(R.id.etForgotPasswordEmail);
        etResetEmail.setText(textOf(etEmail));
        etResetEmail.setSelection(etResetEmail.getText().length());

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.auth_forgot_password_title)
                .setView(content)
                .setNegativeButton(R.string.delete_profile_cancel, null)
                .setPositiveButton(R.string.auth_forgot_password_send, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String email = textOf(etResetEmail);
                    if (TextUtils.isEmpty(email)) {
                        etResetEmail.setError(getString(R.string.email_required));
                        return;
                    }
                    if (ProfileInputValidator.isInvalidEmail(email)) {
                        etResetEmail.setError(getString(R.string.email_invalid));
                        return;
                    }
                    setResetDialogBusy(dialog, true);
                    authRepository.sendPasswordReset(email, new AuthRepository.ActionCallback() {
                        @Override
                        public void onSuccess() {
                            if (!isAdded()) {
                                return;
                            }
                            dialog.dismiss();
                            showResetSentDialog(email);
                        }

                        @Override
                        public void onError(int messageResId) {
                            if (!isAdded()) {
                                return;
                            }
                            setResetDialogBusy(dialog, false);
                            toast(messageResId);
                        }
                    });
                }));
        dialog.show();
    }

    private void showResetSentDialog(@NonNull String email) {
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.auth_forgot_password_sent_title)
                .setMessage(getString(R.string.auth_forgot_password_sent, email))
                .setPositiveButton(R.string.got_it, null)
                .show();
    }

    private void setResetDialogBusy(@NonNull AlertDialog dialog, boolean busy) {
        dialog.setCancelable(!busy);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(!busy);
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setText(busy
                ? R.string.auth_forgot_password_sending
                : R.string.auth_forgot_password_send);
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(!busy);
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

    private static boolean isCredentialError(int messageResId) {
        return messageResId == R.string.auth_error_invalid_credentials
                || messageResId == R.string.auth_error_wrong_password
                || messageResId == R.string.auth_error_user_not_found;
    }

    private static InputFieldError.State stateFor(int errorResId) {
        return errorResId == 0 ? InputFieldError.State.NEUTRAL : InputFieldError.State.ERROR;
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
