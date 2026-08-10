package ca.team6.aquasense;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import ca.team6.aquasense.auth.AuthNavigator;
import ca.team6.aquasense.auth.AuthRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;

/**
 * Hosts the first-installation setup wizard (SETTINGS-03): a welcome page, the two setup steps and
 * a summary.
 *
 * <p>Both steps are screens that already exist for the dashboard's own "Add New Aquarium" route,
 * so the wizard is the order they are shown in rather than a second copy of either. Step 1, the
 * add-aquarium form, is a destination in {@code nav_setup}; step 2 is PairingActivity, which that
 * form launches itself and which reports back to it.
 *
 * <p>Every page offers a way out, and leaving marks pairing complete so the wizard does not
 * reappear on the next launch: the dashboard has an empty state for an account with no hub.
 */
public class SetupWizardActivity extends AppCompatActivity {

    @NonNull
    public static Intent intent(@NonNull Context context) {
        return new Intent(context, SetupWizardActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferenceHelper prefs = SharedPreferenceHelper.getInstance(this);
        if (prefs != null) {
            prefs.applySavedThemeMode();
        }

        // The wizard creates an aquarium under the signed-in user, so it cannot run signed out.
        if (!AuthRepository.getInstance(this).isLoggedIn()) {
            AuthNavigator.goToLogin(this);
            return;
        }

        setContentView(R.layout.activity_setup_wizard);
    }
}
