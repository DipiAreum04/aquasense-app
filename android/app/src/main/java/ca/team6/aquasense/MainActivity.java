package ca.team6.aquasense;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import ca.team6.aquasense.auth.AuthNavigator;
import ca.team6.aquasense.auth.AuthRepository;
import ca.team6.aquasense.model.AquariumRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;

public class MainActivity extends AppCompatActivity {
    SharedPreferenceHelper prefs;

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

        setContentView(R.layout.activity_main);

        // If the app is being launched for the first time, start the first-installation setup flow. 
        if (savedInstanceState == null && authRepository.needsPairing()) {
            startFirstRunSetup();
        }
    }

    /**
     * First-installation setup entry point
     */
    private void startFirstRunSetup() {
        // TODO: Add a custom landing page when user skips pairing on first installation instead of empty dashboard.
    }
}
