package ca.team6.aquasense;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import ca.team6.aquasense.model.SharedPreferenceHelper;

public class MainActivity extends AppCompatActivity {
    SharedPreferenceHelper prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        prefs = SharedPreferenceHelper.getInstance(this);

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