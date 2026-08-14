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
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.AppSettings;
import ca.team6.aquasense.model.FirebaseDatabaseHelper;
import ca.team6.aquasense.model.SettingsRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;
import ca.team6.aquasense.notifications.ThresholdMonitorService;

public class AuthRepository {

    public interface AuthCallback {
        void onSuccess(@NonNull FirebaseUser user);

        void onError(@StringRes int messageResId);
    }

    public interface ActionCallback {
        void onSuccess();

        void onError(@StringRes int messageResId);
    }

    private interface GoogleCredentialCallback {
        void onCredential(@NonNull AuthCredential credential);

        void onError(@StringRes int messageResId);
    }

    private static volatile AuthRepository instance;

    private final Context appContext;
    private final FirebaseAuth firebaseAuth;
    private final SharedPreferenceHelper prefs;
    private final CredentialManager credentialManager;
    private final FirebaseDatabaseHelper database;
    private final ExecutorService credentialExecutor;

    private AuthRepository(@NonNull Context appContext) {
        this.appContext = appContext;
        this.firebaseAuth = FirebaseAuth.getInstance();
        this.prefs = SharedPreferenceHelper.getInstance(appContext);
        this.credentialManager = CredentialManager.create(appContext);
        this.database = FirebaseDatabaseHelper.getInstance();
        this.credentialExecutor = Executors.newSingleThreadExecutor();
    }

    public static AuthRepository getInstance(@NonNull Context context) {
        if (instance == null) {
            synchronized (AuthRepository.class) {
                if (instance == null) {
                    instance = new AuthRepository(context.getApplicationContext());
                }
            }
        }
        return instance;
    }

    @Nullable
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    public boolean isLoggedIn() {
        return getCurrentUser() != null;
    }

    @Nullable
    public String getUid() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
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

    public void register(@NonNull String fullName,
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

                    String displayName = fullName.trim();
                    UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                            .setDisplayName(displayName)
                            .build();

                    user.updateProfile(profileUpdate)
                            .addOnCompleteListener(profileTask ->
                                    handleAuthResult(task, email, displayName, true, callback));
                });
    }


    public void signInWithGoogle(@NonNull Activity activity, @NonNull AuthCallback callback) {
        requestGoogleCredential(activity, new GoogleCredentialCallback() {
            @Override
            public void onCredential(@NonNull AuthCredential credential) {
                signInWithGoogleCredential(credential, callback);
            }

            @Override
            public void onError(@StringRes int messageResId) {
                callback.onError(messageResId);
            }
        });
    }

    private void requestGoogleCredential(@NonNull Activity activity,
                                         @NonNull GoogleCredentialCallback callback) {
        String serverClientId = appContext.getString(R.string.default_web_client_id);
        GetSignInWithGoogleOption googleOption = new GetSignInWithGoogleOption.Builder(serverClientId).build();

        GetCredentialRequest request = new GetCredentialRequest.Builder().addCredentialOption(googleOption).build();

        credentialManager.getCredentialAsync(
                activity,
                request,
                new CancellationSignal(),
                credentialExecutor,
                new CredentialManagerCallback<>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        activity.runOnUiThread(() -> {
                            AuthCredential credential = toFirebaseCredential(result.getCredential());
                            if (credential == null) {
                                callback.onError(R.string.auth_error_google);
                                return;
                            }
                            callback.onCredential(credential);
                        });
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        activity.runOnUiThread(() -> {
                            if (e instanceof GetCredentialCancellationException) {
                                callback.onError(R.string.auth_error_google_cancelled);
                            } else if (e instanceof NoCredentialException) {
                                callback.onError(R.string.auth_error_google_no_account);
                            } else {
                                callback.onError(R.string.auth_error_google);
                            }
                        });
                    }
                }
        );
    }

    @Nullable
    private static AuthCredential toFirebaseCredential(@NonNull Credential credential) {
        if (!(credential instanceof CustomCredential)) {
            return null;
        }
        CustomCredential customCredential = (CustomCredential) credential;
        if (!GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                .equals(customCredential.getType())) {
            return null;
        }
        GoogleIdTokenCredential googleIdTokenCredential =
                GoogleIdTokenCredential.createFrom(customCredential.getData());
        return GoogleAuthProvider.getCredential(googleIdTokenCredential.getIdToken(), null);
    }


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

    public void sendPasswordReset(@NonNull String email,
                                  @NonNull ActionCallback callback) {
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() || isUserNotFound(task.getException())) {
                        callback.onSuccess();
                        return;
                    }
                    callback.onError(mapError(task.getException()));
                });
    }

    private static boolean isUserNotFound(@Nullable Exception exception) {
        return exception instanceof FirebaseAuthException
                && "ERROR_USER_NOT_FOUND".equals(((FirebaseAuthException) exception).getErrorCode());
    }

    public void updateProfileName(@NonNull String name,
                                  @NonNull ActionCallback callback) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            callback.onError(R.string.auth_error_generic);
            return;
        }

        UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                .setDisplayName(name)
                .build();

        user.updateProfile(profileUpdate).addOnCompleteListener(profileTask -> {
            if (!profileTask.isSuccessful()) {
                callback.onError(mapError(profileTask.getException()));
                return;
            }
            database.updateAccountName(user.getUid(), name, new FirebaseDatabaseHelper.DbCallback() {
                @Override
                public void onSuccess() {
                    cacheProfileName(name);
                    callback.onSuccess();
                }

                @Override
                public void onError(@Nullable Exception exception) {
                    cacheProfileName(name);
                    callback.onError(R.string.edit_profile_save_failed);
                }
            });
        });
    }

    private void cacheProfileName(@NonNull String name) {
        if (prefs != null) {
            prefs.updateField(SettingsRepository.KEY_PROFILE_NAME, name);
        }
    }

    public void signOut() {
        ThresholdMonitorService.stop(appContext);
        firebaseAuth.signOut();
        if (prefs != null) {
            prefs.updateField(SettingsRepository.KEY_PROFILE_NAME, "");
            prefs.updateField(SettingsRepository.KEY_PROFILE_EMAIL, "");
        }
    }

    public boolean hasPasswordProvider() {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            return false;
        }
        for (UserInfo info : user.getProviderData()) {
            if (EmailAuthProvider.PROVIDER_ID.equals(info.getProviderId())) {
                return true;
            }
        }
        return false;
    }

    public void deleteAccountWithPassword(@NonNull String password,
                                          @NonNull ActionCallback callback) {
        FirebaseUser user = getCurrentUser();
        if (user == null || TextUtils.isEmpty(user.getEmail())) {
            callback.onError(R.string.auth_error_generic);
            return;
        }
        AuthCredential credential =
                EmailAuthProvider.getCredential(user.getEmail(), password);
        reauthenticateAndDelete(user, credential, callback);
    }

    public void deleteAccountWithGoogle(@NonNull Activity activity,
                                        @NonNull ActionCallback callback) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            callback.onError(R.string.auth_error_generic);
            return;
        }
        requestGoogleCredential(activity, new GoogleCredentialCallback() {
            @Override
            public void onCredential(@NonNull AuthCredential credential) {
                reauthenticateAndDelete(user, credential, callback);
            }

            @Override
            public void onError(@StringRes int messageResId) {
                callback.onError(messageResId);
            }
        });
    }

    private void reauthenticateAndDelete(@NonNull FirebaseUser user,
                                         @NonNull AuthCredential credential,
                                         @NonNull ActionCallback callback) {
        user.reauthenticate(credential)
                .addOnCompleteListener(task -> {
                    if (!task.isSuccessful()) {
                        callback.onError(mapError(task.getException()));
                        return;
                    }
                    deleteProfileNodeThenUser(user, callback);
                });
    }

    private void deleteProfileNodeThenUser(@NonNull FirebaseUser user,
                                           @NonNull ActionCallback callback) {
        database.deleteUserNode(user.getUid(), new FirebaseDatabaseHelper.DbCallback() {
            @Override
            public void onSuccess() {
                user.delete().addOnCompleteListener(deleteTask -> {
                    if (!deleteTask.isSuccessful()) {
                        callback.onError(mapError(deleteTask.getException()));
                        return;
                    }
                    clearSessionAfterDelete();
                    callback.onSuccess();
                });
            }

            @Override
            public void onError(@Nullable Exception exception) {
                callback.onError(R.string.delete_profile_error_data);
            }
        });
    }

    private void clearSessionAfterDelete() {
        firebaseAuth.signOut();
        if (prefs != null) {
            prefs.updateField(SettingsRepository.KEY_PROFILE_NAME, "");
            prefs.updateField(SettingsRepository.KEY_PROFILE_EMAIL, "");
            prefs.setBoolean(SettingsRepository.KEY_HAS_AUTHENTICATED, false);
            prefs.setBoolean(SettingsRepository.KEY_PAIRING_COMPLETE, false);
        }
    }

    private void signInWithGoogleCredential(@NonNull AuthCredential firebaseCredential,
                                            @NonNull AuthCallback callback) {
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
            prefs.setBoolean(SettingsRepository.KEY_HAS_AUTHENTICATED, true);
            if (isNewAccount) {
                prefs.setBoolean(SettingsRepository.KEY_PAIRING_COMPLETE, false);
            }
        }

        if (TextUtils.isEmpty(resolvedEmail)) {
            callback.onError(R.string.auth_error_generic);
            return;
        }
        writeUserProfile(user.getUid(), resolvedEmail, resolvedName, isNewAccount, user, callback);
    }


    private void writeUserProfile(@NonNull String uid,
                                  @NonNull String email,
                                  @Nullable String name,
                                  boolean isNewAccount,
                                  @NonNull FirebaseUser user,
                                  @NonNull AuthCallback callback) {
        database.writeAccount(uid, name != null ? name : "", email,
                new FirebaseDatabaseHelper.DbCallback() {
                    @Override
                    public void onSuccess() {
                        callback.onSuccess(user);
                    }

                    @Override
                    public void onError(@Nullable Exception exception) {
                        if (isNewAccount) {
                            signOut();
                            callback.onError(R.string.auth_error_profile_save);
                            return;
                        }
                        callback.onSuccess(user);
                    }
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
                case "ERROR_ACCOUNT_EXISTS_WITH_DIFFERENT_CREDENTIAL":
                    return R.string.auth_error_account_exists_with_password;
                case "ERROR_WEAK_PASSWORD":
                    return R.string.auth_error_weak_password;
                case "ERROR_INVALID_CREDENTIAL":
                case "ERROR_INVALID_LOGIN_CREDENTIALS":
                    return R.string.auth_error_invalid_credentials;
                case "ERROR_NETWORK_REQUEST_FAILED":
                    return R.string.auth_error_network;
                case "ERROR_REQUIRES_RECENT_LOGIN":
                    return R.string.delete_profile_error_recent_login;
                default:
                    return R.string.auth_error_generic;
            }
        }
        if (exception != null && exception.getMessage() != null
            && exception.getMessage().toLowerCase(Locale.ROOT).contains("network")) {
            return R.string.auth_error_network;
        }
        return R.string.auth_error_generic;
    }
}
