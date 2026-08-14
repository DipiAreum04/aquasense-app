package ca.team6.aquasense.startup;

import ca.team6.aquasense.R;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import ca.team6.aquasense.auth.AuthNavigator;
import ca.team6.aquasense.auth.AuthRepository;
import ca.team6.aquasense.settings.SharedPreferenceHelper;

public class SplashActivity extends AppCompatActivity {

    private static final long MIN_SPLASH_DURATION_MS = 800L;
    private static final long NAVIGATION_OVERLAP_MS = 250L;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private long splashStartTimeMs;
    private boolean navigationStarted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
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

        if (authRepository.isLoggedIn()) {
            AuthNavigator.continueAfterAuth(this, authRepository, false);
        } else {
            AuthNavigator.goToLogin(this, false);
        }

        AuthNavigator.applyNoAnimation(this);

        handler.postDelayed(this::finish, NAVIGATION_OVERLAP_MS);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
