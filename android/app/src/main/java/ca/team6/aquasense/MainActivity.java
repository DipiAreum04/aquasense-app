package ca.team6.aquasense;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.activity.ComponentActivity;

import ca.team6.aquasense.model.ScopedLogger;
import ca.team6.aquasense.model.SharedPreferenceHelper;

public class MainActivity extends ComponentActivity {
    SharedPreferenceHelper sharedPreferenceHelper;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        setContentView(R.layout.activity_main);
        sharedPreferenceHelper = SharedPreferenceHelper.getInstance(this);

        ScopedLogger.info("test");

        Button btnOpenSettings = findViewById(R.id.btnOpenSettings);
        btnOpenSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class))
        );
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