package ca.team6.aquasense;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.util.List;

import ca.team6.aquasense.auth.AuthNavigator;
import ca.team6.aquasense.auth.AuthRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;
import ca.team6.aquasense.model.WaterType;
import ca.team6.aquasense.pairing.DiscoveredBoard;
import ca.team6.aquasense.pairing.PairingEntryMode;
import ca.team6.aquasense.pairing.PairingFragment;
import ca.team6.aquasense.pairing.PairingRepository;
import ca.team6.aquasense.pairing.PairingState;

/**
 * This activity hosts the app to microcontroller pairing flow (BLE-02).
 */
public class PairingActivity extends AppCompatActivity {

    private static final String EXTRA_AQUARIUM_NAME = "extra_aquarium_name";
    private static final String EXTRA_WATER_TYPE = "extra_water_type";
    private static final String EXTRA_TEMPLATE_ID = "extra_template_id";
    private static final String EXTRA_ENTRY_MODE = "extra_entry_mode";

    private static final String TAG_PAIRING_FRAGMENT = "pairing";

    @NonNull
    public static Intent intent(@NonNull Context context,
                                @NonNull String aquariumName,
                                @NonNull WaterType waterType,
                                @Nullable String templateId,
                                @NonNull PairingEntryMode entryMode) {
        return new Intent(context, PairingActivity.class)
                .putExtra(EXTRA_AQUARIUM_NAME, aquariumName)
                .putExtra(EXTRA_WATER_TYPE, waterType.getKey())
                .putExtra(EXTRA_TEMPLATE_ID, templateId)
                .putExtra(EXTRA_ENTRY_MODE, entryMode.name());
    }

    private PairingEntryMode entryMode = PairingEntryMode.ADD_AQUARIUM;

    // Only showed on a first installation, where the Skip action exists.
    // Null in every other case, so the observer is never attached.
    private PairingRepository pairingRepository;

    private final PairingRepository.PairingObserver pairingObserver =
            new PairingRepository.PairingObserver() {
                @Override
                public void onPairingStateChanged(@NonNull PairingState state) {
                    // Re-evaluates whether Skip should still be displayed; it is withdrawn on SUCCESS.
                    invalidateOptionsMenu();
                }

                @Override
                public void onBoardsChanged(@NonNull List<DiscoveredBoard> discovered) {
                }
            };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferenceHelper prefs = SharedPreferenceHelper.getInstance(this);
        if (prefs != null) {
            prefs.applySavedThemeMode();
        }

        // Pairing requires a logged in user, otherwise it cannot write the aquarium to the database.
        if (!AuthRepository.getInstance(this).isLoggedIn()) {
            AuthNavigator.goToLogin(this);
            return;
        }

        this.entryMode = PairingEntryMode.fromName(getIntent().getStringExtra(EXTRA_ENTRY_MODE));

        if (this.entryMode == PairingEntryMode.FIRST_RUN) {
            this.pairingRepository = PairingRepository.getInstance(this);
            this.pairingRepository.addObserver(this.pairingObserver);
        }

        setContentView(R.layout.activity_pairing);

        Toolbar toolbar = findViewById(R.id.toolbar_pairing_flow);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.pairing_flow_title);
            getSupportActionBar().setDisplayHomeAsUpEnabled(this.entryMode.canNavigateUp());
        }

        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.pairingContainer, this.buildPairingFragment(), TAG_PAIRING_FRAGMENT)
                    .commit();
        }
    }

    @NonNull
    private PairingFragment buildPairingFragment() {
        Intent intent = getIntent();
        String name = intent.getStringExtra(EXTRA_AQUARIUM_NAME);

        Bundle args = new Bundle();
        args.putString(PairingFragment.ARG_AQUARIUM_NAME, name == null ? "" : name);
        args.putString(PairingFragment.ARG_WATER_TYPE, intent.getStringExtra(EXTRA_WATER_TYPE));
        args.putString(PairingFragment.ARG_TEMPLATE_ID, intent.getStringExtra(EXTRA_TEMPLATE_ID));
        args.putString(PairingFragment.ARG_ENTRY_MODE, this.entryMode.name());

        PairingFragment fragment = new PairingFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_pairing, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(@NonNull Menu menu) {
        MenuItem skip = menu.findItem(R.id.action_skip_pairing);
        if (skip != null) {
            // Displayed on a first installation right up until the hub is online, at which point the flow's
            // own "Go to dashboard" takes over and skipping no longer means anything.
            boolean paired = this.pairingRepository != null
                    && this.pairingRepository.getState() == PairingState.SUCCESS;
            skip.setVisible(this.entryMode == PairingEntryMode.FIRST_RUN && !paired);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_skip_pairing) {
            this.skipPairing();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.pairingRepository != null) {
            this.pairingRepository.removeObserver(this.pairingObserver);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    private void skipPairing() {
        AuthRepository.getInstance(this).setPairingComplete(true);
        AuthNavigator.goToDashboard(this);
    }

    public void finishPairing(@NonNull PairingEntryMode mode) {
        AuthRepository.getInstance(this).setPairingComplete(true);
        if (mode == PairingEntryMode.FIRST_RUN) {
            AuthNavigator.goToDashboard(this);
            return;
        }
        setResult(RESULT_OK);
        finish();
    }
}
