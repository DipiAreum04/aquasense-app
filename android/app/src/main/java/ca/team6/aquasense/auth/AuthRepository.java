package ca.team6.aquasense.auth;

import android.app.Activity;
import android.content.Context;
import android.os.CancellationSignal;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.google.android.gms.tasks.Task;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Locale;
import java.util.concurrent.Executors;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.AppSettings;
import ca.team6.aquasense.model.SettingsRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;

/**
 * Wrapper functions for Firebase Auth and the user profile node in Realtime Database.
 */
public class AuthRepository {

    public interface AuthCallback {
        void onSuccess(@NonNull FirebaseUser user);

        void onError(@StringRes int messageResId);
    }

    private final Context appContext;
    private final FirebaseAuth firebaseAuth;
    private final SharedPreferenceHelper prefs;
    private final CredentialManager credentialManager;

    public AuthRepository(@NonNull Context context) {
        this.appContext = context.getApplicationContext();
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.prefs = SharedPreferenceHelper.getInstance(appContext);
        this.credentialManager = CredentialManager.create(appContext);
    }

    @Nullable
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public boolean isLoggedIn() {
        return getCurrentUser() != null;
    }

    public boolean needsPairing() {
        return prefs == null || !prefs.getBoolean(SettingsRepository.KEY_PAIRING_COMPLETE, false);
    }

    public void setPairingComplete(boolean complete) {
        if (prefs != null) {
            prefs.setBoolean(SettingsRepository.KEY_PAIRING_COMPLETE, complete);
        }
    }

    public void signIn(@NonNull String email,
                       @NonNull String password,
                       @NonNull AuthCallback callback) {
        firebaseAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> handleAuthResult(task, email, null, false, callback));
    }

    public void register(@NonNull String firstName,
                         @NonNull String lastName,
                         @NonNull String email,
                         @NonNull String password,
                         @NonNull AuthCallback callback) {
        firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        callback.onError(mapError(task.getException()));
                        return;
                    }

                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user == null) {
                        callback.onError(R.string.auth_error_generic);
                        return;
                    }

                    String displayName = (firstName + " " + lastName).trim();
                    UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                            .setDisplayName(displayName)
                            .build();

                    user.updateProfile(profileUpdate)
                            .addOnCompleteListener(profileTask ->
                                    handleAuthResult(task, email, displayName, true, callback));
                });
    }


    // Google Sign-In via Credential Manager to get Firebase Auth credential.
    // Uses the Web client ID from google-services.json ({@code R.string.default_web_client_id}).
    public void signInWithGoogle(@NonNull Activity activity, @NonNull AuthCallback callback) {
        String serverClientId = appContext.getString(R.string.default_web_client_id);
        GetSignInWithGoogleOption googleOption = new GetSignInWithGoogleOption.Builder(serverClientId).build();

        GetCredentialRequest request = new GetCredentialRequest.Builder().addCredentialOption(googleOption).build();

        credentialManager.getCredentialAsync(
                activity,
                request,
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        // Credential Manager may call back off the main thread.
                        activity.runOnUiThread(() ->
                                handleGoogleCredential(result.getCredential(), callback));
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        activity.runOnUiThread(() -> {
                            if (e instanceof GetCredentialCancellationException) {
                                callback.onError(R.string.auth_error_google_cancelled);
                            } else if (e instanceof NoCredentialException) {
                                // The device has no Google account
                                callback.onError(R.string.auth_error_google_no_account);
                            } else {
                                callback.onError(R.string.auth_error_google);
                            }
                        });
                    }
                }
        );
    }


    // Fetch display name from Firebase when available, otherwise fetch from locally cached profile name
    @NonNull
    public String getProfileDisplayName() {
        FirebaseUser user = getCurrentUser();
        if (user != null && !TextUtils.isEmpty(user.getDisplayName())) {
            return user.getDisplayName();
        }
        if (prefs != null) {
            String cached = prefs.getString(SettingsRepository.KEY_PROFILE_NAME, new AppSettings().profileName);
            if (!TextUtils.isEmpty(cached)) {
                return cached;
            }
        }
        return "";
    }

    
    // Fetch email from Firebase when available, otherwise fetch from locally cached profile email
    @NonNull
    public String getProfileEmail() {
        FirebaseUser user = getCurrentUser();
        if (user != null && !TextUtils.isEmpty(user.getEmail())) {
            return user.getEmail();
        }
        if (prefs != null) {
            String cached = prefs.getString(SettingsRepository.KEY_PROFILE_EMAIL, new AppSettings().profileEmail);
            if (!TextUtils.isEmpty(cached)) {
                return cached;
            }
        }
        return "";
    }

    
    // Write Firebase Auth profile fields into local prefs when present so settings
    // screens stay in sync after login / register.
    public void syncProfileCacheFromFirebase() {
        FirebaseUser user = getCurrentUser();
        if (user == null || prefs == null) {
            return;
        }
        if (!TextUtils.isEmpty(user.getEmail())) {
            prefs.updateField(SettingsRepository.KEY_PROFILE_EMAIL, user.getEmail());
        }
        if (!TextUtils.isEmpty(user.getDisplayName())) {
            prefs.updateField(SettingsRepository.KEY_PROFILE_NAME, user.getDisplayName());
        }
    }

    public void signOut() {
        firebaseAuth.signOut();
        if (prefs != null) {
            // Keep KEY_HAS_AUTHENTICATED so the next launch opens login instead of register
            prefs.updateField(SettingsRepository.KEY_PROFILE_NAME, "");
            prefs.updateField(SettingsRepository.KEY_PROFILE_EMAIL, "");
        }
    }

    private void handleGoogleCredential(@NonNull Credential credential,
                                        @NonNull AuthCallback callback) {
        if (!(credential instanceof CustomCredential)) {
            callback.onError(R.string.auth_error_google);
            return;
        }
        CustomCredential customCredential = (CustomCredential) credential;
        if (!GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                .equals(customCredential.getType())) {
            callback.onError(R.string.auth_error_google);
            return;
        }

        GoogleIdTokenCredential googleIdTokenCredential =
                GoogleIdTokenCredential.createFrom(customCredential.getData());
        AuthCredential firebaseCredential =
                GoogleAuthProvider.getCredential(googleIdTokenCredential.getIdToken(), null);

        firebaseAuth.signInWithCredential(firebaseCredential)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        callback.onError(mapError(task.getException()));
                        return;
                    }

                    FirebaseUser user = firebaseAuth.getCurrentUser();
                    if (user == null) {
                        callback.onError(R.string.auth_error_generic);
                        return;
                    }

                    boolean isNewUser = task.getResult() != null
                            && task.getResult().getAdditionalUserInfo() != null
                            && task.getResult().getAdditionalUserInfo().isNewUser();

                    String email = user.getEmail() != null ? user.getEmail() : "";
                    String displayName = user.getDisplayName();
                    handleAuthResult(task, email, displayName, isNewUser, callback);
                });
    }

    private void handleAuthResult(@NonNull Task<AuthResult> task,
                                  @NonNull String email,
                                  @Nullable String displayName,
                                  boolean isNewAccount,
                                  @NonNull AuthCallback callback) {
        if (!task.isSuccessful()) {
            callback.onError(mapError(task.getException()));
            return;
        }

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) {
            callback.onError(R.string.auth_error_generic);
            return;
        }

        String resolvedEmail = !TextUtils.isEmpty(email)
                ? email
                : (user.getEmail() != null ? user.getEmail() : "");
        String resolvedName = !TextUtils.isEmpty(displayName)
                ? displayName
                : user.getDisplayName();

        if (prefs != null) {
            if (!TextUtils.isEmpty(resolvedEmail)) {
                prefs.updateField(SettingsRepository.KEY_PROFILE_EMAIL, resolvedEmail);
            }
            if (!TextUtils.isEmpty(resolvedName)) {
                prefs.updateField(SettingsRepository.KEY_PROFILE_NAME, resolvedName);
            }
            // After any successful authentication, future logged-out launches start at login.
            prefs.setBoolean(SettingsRepository.KEY_HAS_AUTHENTICATED, true);
            if (isNewAccount) {
                // New accounts always go through the SETTINGS-03 pairing wizard.
                prefs.setBoolean(SettingsRepository.KEY_PAIRING_COMPLETE, false);
            }
        }

        // /{uid} requires email (UID is the root key) per database schema
        if (TextUtils.isEmpty(resolvedEmail)) {
            callback.onError(R.string.auth_error_generic);
            return;
        }
        writeUserProfile(user.getUid(), resolvedEmail, isNewAccount, user, callback);
    }


    // Writes /{uid}/email to the realtime database per database schema
    // On write failure for a brand-new account, signs out so the user is not left half-registered.
    // Returning logins also write emails to the db so a prior failed write can be fixed.
    private void writeUserProfile(@NonNull String uid,
                                  @NonNull String email,
                                  boolean isNewAccount,
                                  @NonNull FirebaseUser user,
                                  @NonNull AuthCallback callback) {
        FirebaseDatabase.getInstance()
                .getReference()
                .child(uid)
                .child("email")
                .setValue(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(user);
                        return;
                    }
                    if (isNewAccount) {
                        // Auth user may already exist, so clear the local session to allow retry.
                        signOut();
                        callback.onError(R.string.auth_error_profile_save);
                        return;
                    }
                    // TODO: FIX THIS LATER:Schema write is retried on the next auth success if it fails.
                    callback.onSuccess(user);
                });
    }

    @StringRes
    private static int mapError(@Nullable Exception exception) {
        if (exception instanceof FirebaseAuthException) {
            String code = ((FirebaseAuthException) exception).getErrorCode();
            switch (code) {
                case "ERROR_INVALID_EMAIL":
                    return R.string.email_invalid;
                case "ERROR_WRONG_PASSWORD":
                    return R.string.auth_error_wrong_password;
                case "ERROR_USER_NOT_FOUND":
                    return R.string.auth_error_user_not_found;
                case "ERROR_EMAIL_ALREADY_IN_USE":
                    return R.string.auth_error_email_in_use;
                // Google sign-in with an email already registered via email/password.
                case "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL":
                    return R.string.auth_error_account_exists_with_password;
                case "ERROR_WEAK_PASSWORD":
                    return R.string.auth_error_weak_password;
                case "ERROR_INVALID_CREDENTIAL":
                case "ERROR_INVALID_LOGIN_CREDENTIALS":
                    return R.string.auth_error_invalid_credentials;
                case "ERROR_NETWORK_REQUEST_FAILED":
                    return R.string.auth_error_network;
                default:
                    return R.string.auth_error_generic;
            }
        }
        if (exception != null && exception.getMessage() != null
            && exception.getMessage().toLowerCase(Locale.ROOT).contains("network")) { // Check if the error message contains "network" to map to the correct error message
            return R.string.auth_error_network;
        }
        return R.string.auth_error_generic;
    }
}
