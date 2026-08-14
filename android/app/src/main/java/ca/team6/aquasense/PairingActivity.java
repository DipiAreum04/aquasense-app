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
import ca.team6.aquasense.model.NewAquariumConfig;
import ca.team6.aquasense.model.SharedPreferenceHelper;
import ca.team6.aquasense.pairing.DiscoveredBoard;
import ca.team6.aquasense.pairing.PairingEntryMode;
import ca.team6.aquasense.pairing.PairingFragment;
import ca.team6.aquasense.pairing.PairingRepository;
import ca.team6.aquasense.pairing.PairingState;
import ca.team6.aquasense.ui.WizardProgress;

public class PairingActivity extends AppCompatActivity {

    private static final String EXTRA_AQUARIUM_CONFIG = "extra_aquarium_config";
    private static final String EXTRA_ENTRY_MODE = "extra_entry_mode";

    private static final String TAG_PAIRING_FRAGMENT = "pairing";

    @NonNull
    public static Intent intent(@NonNull Context context,
                                @NonNull NewAquariumConfig config,
                                @NonNull PairingEntryMode entryMode) {
        return new Intent(context, PairingActivity.class)
                .putExtra(EXTRA_AQUARIUM_CONFIG, config)
                .putExtra(EXTRA_ENTRY_MODE, entryMode.name());
    }

    private PairingEntryMode entryMode = PairingEntryMode.ADD_AQUARIUM;

    private PairingRepository pairingRepository;

    private final PairingRepository.PairingObserver pairingObserver =
            new PairingRepository.PairingObserver() {
                @Override
                public void onPairingStateChanged(@NonNull PairingState state) {
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

        if (this.entryMode == PairingEntryMode.FIRST_RUN) {
            WizardProgress.show(this, WizardProgress.TOTAL_STEPS);
        }

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
        Bundle args = new Bundle();
        args.putParcelable(PairingFragment.ARG_AQUARIUM_CONFIG,
                getIntent().getParcelableExtra(EXTRA_AQUARIUM_CONFIG));
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
        setResult(RESULT_OK);
        finish();
    }
}
