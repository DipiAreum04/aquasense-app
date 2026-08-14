package ca.team6.aquasense;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import ca.team6.aquasense.auth.AuthNavigator;
import ca.team6.aquasense.auth.AuthRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;

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

        if (!AuthRepository.getInstance(this).isLoggedIn()) {
            AuthNavigator.goToLogin(this);
            return;
        }

        setContentView(R.layout.activity_setup_wizard);
    }
}
