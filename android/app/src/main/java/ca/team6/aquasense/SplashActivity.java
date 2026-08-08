package ca.team6.aquasense;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import ca.team6.aquasense.auth.AuthNavigator;
import ca.team6.aquasense.auth.AuthRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;

/**
 * Splash Screen Activity.
 * Transitions from the system splash into the branded activity loading screen.
 * Implements layered timing to prevent white flash gaps during navigation.
 */
public class SplashActivity extends AppCompatActivity {

    private static final long MIN_SPLASH_DURATION_MS = 800L;
    private static final long NAVIGATION_OVERLAP_MS = 250L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private long splashStartTimeMs;
    private boolean navigationStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Transition from system splash to activity immediately.
        SplashScreen.installSplashScreen(this);
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        SharedPreferenceHelper prefs = SharedPreferenceHelper.getInstance(this);
        if (prefs != null) {
            prefs.applySavedThemeMode();
        }

        splashStartTimeMs = System.currentTimeMillis();
        scheduleNavigation();
    }

    private void scheduleNavigation() {
        long elapsed = System.currentTimeMillis() - splashStartTimeMs;
        long remaining = Math.max(0, MIN_SPLASH_DURATION_MS - elapsed);
        handler.postDelayed(this::navigateNext, remaining);
    }

    private void navigateNext() {
        if (navigationStarted || isFinishing()) {
            return;
        }
        navigationStarted = true;

        AuthRepository authRepository = AuthRepository.getInstance(this);
        
        // Start the next activity WITHOUT finishing this one immediately.
        // This keeps the blue splash background visible behind the next screen while it loads.
        if (authRepository.isLoggedIn()) {
            AuthNavigator.continueAfterAuth(this, authRepository, false);
        } else {
            AuthNavigator.goToLogin(this, false);
        }
        
        // Ensure the jump is instant (no slide/fade)
        AuthNavigator.applyNoAnimation(this);

        // Wait for the next screen to warm up, then close the splash.
        handler.postDelayed(this::finish, NAVIGATION_OVERLAP_MS);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
