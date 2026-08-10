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

    // Stops a second resume from stacking another sheet before the first result lands.
    private boolean permissionRequestInFlight;

    private final ActivityResultLauncher<String> requestNotificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                permissionRequestInFlight = false;
                // The one ask per install is spent here, once the system has actually answered,
                // rather than before the launch. Marking it up front burned the ask on any launch
                // the platform declined to show, leaving no way back to the sheet.
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

        // Subscribes to the signed-in user's aquariums. Done before the dashboard is inflated so
        // its first snapshot is already in flight by the time the header asks for it.
        AquariumRepository.getInstance(this);
        AquasenseNotificationHelper.ensureChannel(this);
        // Applied before the dashboard is inflated so its first draw is already on the
        // alerting aquarium, instead of switching under the user a moment later.
        applyNotificationAquarium(getIntent());
        setContentView(R.layout.activity_main);

        // If the app is being launched for the first time, start the first-installation setup flow.
        if (savedInstanceState == null && authRepository.needsPairing()) {
            startFirstRunSetup();
        }
    }

    @Override
    protected void onNewIntent(@NonNull Intent intent) {
        super.onNewIntent(intent);
        // Notification intents are SINGLE_TOP, so a tap that finds this activity already
        // running arrives here instead of going through onCreate.
        setIntent(intent);
        applyNotificationAquarium(intent);
    }

    /**
     * Points the dashboard at the aquarium a tapped notification is about. The extra is
     * consumed, because onCreate replays the same intent on every configuration change,
     * which would otherwise re-pin that aquarium over a selection made in the meantime.
     */
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
        // An aquarium deleted since the alert was posted leaves getActiveAquarium() to fall
        // back to the first one, which is what the selector screen does too.
        AquariumRepository.getInstance(this).setActiveAquariumId(aquariumId);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Settings may have changed on the Notifications screen, and a low-memory kill may have
        // taken the service down, so the running state is reconciled on every resume.
        reconcileMonitoringAndPermissions();
    }

    /**
     * First-installation setup entry point
     */
    private void startFirstRunSetup() {
        // TODO: Add a custom landing page when user skips pairing on first installation instead of empty dashboard.
    }

    private boolean hasNotificationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void reconcileMonitoringAndPermissions() {
        // onCreate finishes this activity when nobody is signed in, and the sheet would be torn
        // down with it, spending the ask on a dialog the user never got to see.
        if (isFinishing() || permissionRequestInFlight) {
            return;
        }
        if (hasNotificationPermission()) {
            MonitoringController.sync(this);
            return;
        }
        // Asked once per install. Re-launching on every resume put the system dialog in front of
        // users who had already declined, which the platform silently stops honouring anyway.
        // Settings > Notifications carries the row that reopens it when the ask was missed.
        if (prefs == null
                || prefs.getBoolean(SettingsRepository.KEY_NOTIF_PERMISSION_ASKED, false)) {
            MonitoringController.sync(this);
            return;
        }
        // Hold the foreground monitor until the one system permission sheet is answered so OEM
        // builds do not stack that sheet with a service-start prompt on first launch.
        permissionRequestInFlight = true;
        requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
    }
}
