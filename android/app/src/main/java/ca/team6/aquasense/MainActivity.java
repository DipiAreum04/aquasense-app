package ca.team6.aquasense;

import android.os.Bundle;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;

import ca.team6.aquasense.model.ScopedLogger;
import ca.team6.aquasense.model.SharedPreferenceHelper;

public class MainActivity extends ComponentActivity {
    SharedPreferenceHelper sharedPreferenceHelper;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        sharedPreferenceHelper = SharedPreferenceHelper.getInstance(this);

        ScopedLogger.info("test");
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