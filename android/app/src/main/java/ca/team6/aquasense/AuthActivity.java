package ca.team6.aquasense;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.fragment.NavHostFragment;

import ca.team6.aquasense.auth.AuthNavigator;
import ca.team6.aquasense.auth.AuthRepository;
import ca.team6.aquasense.model.SettingsRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;

/**
 * Implements the login / registration activity page.
 * Redirects to registration page until the user has successfully authenticated at least once;
 * later logged-out launches open login.
 */
public class AuthActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferenceHelper prefs = SharedPreferenceHelper.getInstance(this);
        if (prefs != null) {
            prefs.applySavedThemeMode();
        }

        AuthRepository authRepository = AuthRepository.getInstance(this);
        if (authRepository.isLoggedIn()) {
            AuthNavigator.continueAfterAuth(this, authRepository);
            return;
        }

        setContentView(R.layout.activity_auth);

        // Only choose the start screen on the first creation
        if (savedInstanceState == null) {
            setupAuthStartDestination(prefs);
        }
    }

    private void setupAuthStartDestination(SharedPreferenceHelper prefs) {
        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_auth);
        if (navHost == null) {
            return;
        }

        boolean hasAuthenticated = prefs != null
                && prefs.getBoolean(SettingsRepository.KEY_HAS_AUTHENTICATED, false);
        int startDestination = hasAuthenticated
                ? R.id.loginFragment
                : R.id.registerFragment;

        NavController navController = navHost.getNavController();
        // XML already attached nav_auth (start = login). Re-apply with the correct start
        // destination before the first frame for first-time vs returning users.
        if (navController.getGraph().getStartDestinationId() != startDestination) {
            NavGraph graph = navController.getNavInflater().inflate(R.navigation.nav_auth);
            graph.setStartDestination(startDestination);
            navController.setGraph(graph);
        }
    }
}
