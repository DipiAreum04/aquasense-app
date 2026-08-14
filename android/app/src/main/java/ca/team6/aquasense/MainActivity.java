package ca.team6.aquasense;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import ca.team6.aquasense.auth.AuthNavigator;
import ca.team6.aquasense.auth.AuthRepository;
import ca.team6.aquasense.model.AquariumRepository;
import ca.team6.aquasense.model.SettingsRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;
import ca.team6.aquasense.notifications.AquasenseNotificationHelper;
import ca.team6.aquasense.notifications.MonitoringController;

public class MainActivity extends AppCompatActivity {
    SharedPreferenceHelper prefs;

    private boolean permissionRequestInFlight;

    private final ActivityResultLauncher<String> requestNotificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                permissionRequestInFlight = false;
                if (prefs != null) {
                    prefs.setBooleanSync(SettingsRepository.KEY_NOTIF_PERMISSION_ASKED, true);
                }
                MonitoringController.sync(this);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = SharedPreferenceHelper.getInstance(this);
        if (prefs != null) {
            prefs.applySavedThemeMode();
        }

        AuthRepository authRepository = AuthRepository.getInstance(this);
        if (!authRepository.isLoggedIn()) {
            AuthNavigator.goToLogin(this);
            return;
        }

        AquariumRepository.getInstance(this);
        AquasenseNotificationHelper.ensureChannel(this);
        applyNotificationAquarium(getIntent());
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null && authRepository.needsPairing()) {
            startFirstRunSetup();
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        applyNotificationAquarium(intent);
    }

    private void applyNotificationAquarium(@Nullable Intent intent) {
        if (intent == null) {
            return;
        }
        String aquariumId =
                intent.getStringExtra(AquasenseNotificationHelper.EXTRA_AQUARIUM_ID);
        if (aquariumId == null || aquariumId.isEmpty()) {
            return;
        }
        intent.removeExtra(AquasenseNotificationHelper.EXTRA_AQUARIUM_ID);
        AquariumRepository.getInstance(this).setActiveAquariumId(aquariumId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reconcileMonitoringAndPermissions();
    }

    private void startFirstRunSetup() {
        startActivity(SetupWizardActivity.intent(this));
        AuthNavigator.applyNoAnimation(this);
        finish();
    }

    private boolean hasNotificationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void reconcileMonitoringAndPermissions() {
        if (isFinishing() || permissionRequestInFlight) {
            return;
        }
        if (hasNotificationPermission()) {
            MonitoringController.sync(this);
            return;
        }
        if (prefs == null
                || prefs.getBoolean(SettingsRepository.KEY_NOTIF_PERMISSION_ASKED, false)) {
            MonitoringController.sync(this);
            return;
        }
        permissionRequestInFlight = true;
        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
    }
}
