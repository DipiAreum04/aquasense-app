package ca.team6.aquasense.settings;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import ca.team6.aquasense.R;
import ca.team6.aquasense.auth.AuthNavigator;
import ca.team6.aquasense.auth.AuthRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;

/**
 * Entry point for SETTINGS-03 (Hardware Setup & Pairing Wizard).
 * Shown after first login until pairing is implemented.
 * TODO: TEMPORARY; FULL GUIDED FLOW WILL REPLACE THIS PLACEHOLDER
 */
public class PairingWizardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferenceHelper prefs = SharedPreferenceHelper.getInstance(this);
        if (prefs != null) {
            prefs.applySavedThemeMode();
        }

        AuthRepository authRepository = new AuthRepository(this);
        if (!authRepository.isLoggedIn()) {
            AuthNavigator.goToLogin(this);
            return;
        }

        setContentView(R.layout.activity_pairing_wizard);

        Toolbar toolbar = findViewById(R.id.toolbar_pairing);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.pairing_wizard_title);
        }

        TextView heading = findViewById(R.id.tvPairingHeading);
        TextView body = findViewById(R.id.tvPairingBody);
        Button continueButton = findViewById(R.id.btnPairingContinue);

        heading.setText(R.string.pairing_wizard_heading);
        body.setText(R.string.pairing_wizard_body);

        continueButton.setOnClickListener(v -> {
            authRepository.setPairingComplete(true);
            AuthNavigator.goToDashboard(this);
        });
    }
}
