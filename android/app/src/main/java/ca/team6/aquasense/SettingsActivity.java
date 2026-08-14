package ca.team6.aquasense;

import android.os.Bundle;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import ca.team6.aquasense.auth.AuthNavigator;
import ca.team6.aquasense.auth.AuthRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;

public class SettingsActivity extends AppCompatActivity {

    public static final String EXTRA_START_DESTINATION = "start_destination";

    private NavController navController;
    private AppBarConfiguration appBarConfiguration;
    private int shortcutDestinationId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferenceHelper prefs = SharedPreferenceHelper.getInstance(this);
        if (prefs != null) {
            prefs.applySavedThemeMode();
        }

        AuthRepository authRepository = AuthRepository.getInstance(this);
        if (!authRepository.isLoggedIn()) {
            AuthNavigator.goToLogin(this);
            return;
        }

        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar_settings);
        setSupportActionBar(toolbar);

        NavHostFragment navHost = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_settings);
        if (navHost != null) {
            navController = navHost.getNavController();

            appBarConfiguration = new AppBarConfiguration.Builder()
                    .setFallbackOnNavigateUpListener(() -> {
                        finish();
                        return true;
                    })
                    .build();
            NavigationUI.setupActionBarWithNavController(
                    this, navController, appBarConfiguration);

            shortcutDestinationId = getIntent().getIntExtra(EXTRA_START_DESTINATION, 0);
            if (shortcutDestinationId != 0) {
                navController.navigate(shortcutDestinationId);
            }

            getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
                @Override
                public void handleOnBackPressed() {
                    if (isAtShortcutDestination()) {
                        finish();
                    } else if (!navController.popBackStack()) {
                        finish();
                    }
                }
            });
        }
    }

    private boolean isAtShortcutDestination() {
        NavDestination current = navController.getCurrentDestination();
        return shortcutDestinationId != 0 && current != null && current.getId() == shortcutDestinationId;
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (navController != null && isAtShortcutDestination()) {
            finish();
            return true;
        }
        if (navController != null && appBarConfiguration != null) {
            return NavigationUI.navigateUp(navController, appBarConfiguration)
                    || super.onSupportNavigateUp();
        }
        return super.onSupportNavigateUp();
    }
}
