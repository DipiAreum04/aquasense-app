package ca.team6.aquasense;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import ca.team6.aquasense.model.SharedPreferenceHelper;

public class MainActivity extends AppCompatActivity {
    SharedPreferenceHelper prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = SharedPreferenceHelper.getInstance(this);
        if (prefs != null) {
            prefs.applySavedDarkMode();
        }
        setContentView(R.layout.activity_main);

        getSupportFragmentManager().findFragmentById(R.id.nav_host_dashboard);
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }
}
