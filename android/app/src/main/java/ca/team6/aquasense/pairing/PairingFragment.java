package ca.team6.aquasense.pairing;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ca.team6.aquasense.PairingActivity;
import ca.team6.aquasense.R;
import ca.team6.aquasense.model.Aquarium;
import ca.team6.aquasense.model.AquariumRepository;
import ca.team6.aquasense.model.NewAquariumConfig;
import ca.team6.aquasense.model.ScopedLogger;
import ca.team6.aquasense.ui.BleStatusOrb;
import ca.team6.aquasense.ui.InputFieldError;

public class PairingFragment extends Fragment {

    public static final String ARG_AQUARIUM_CONFIG = "aquariumConfig";
    public static final String ARG_ENTRY_MODE = "entryMode";

    private static final long REVEAL_DURATION_MS = 220L;

    private static final String STATE_SELECTED_ADDRESS = "selectedAddress";

    private PairingRepository pairingRepository;
    private DiscoveredBoardAdapter boardAdapter;

    private ViewGroup root;
    private View heroSection;
    private View devicesSection;
    private View wifiSection;
    private View noticeCard;

    private BleStatusOrb orb;
    private TextView heroTitle;
    private TextView heroBody;
    private Button heroAction;

    private EditText ssidInput;
    private EditText passwordInput;
    private View ssidBox;
    private View passwordBox;
    private TextView ssidError;
    private TextView passwordError;
    private ImageButton passwordToggle;
    private Button wifiSubmit;

    private ProgressBar noticeProgress;
    private ImageView noticeIcon;
    private TextView noticeTitle;
    private TextView noticeBody;
    private Button noticeRetry;

    @Nullable
    private NewAquariumConfig aquariumConfig;
    private PairingEntryMode entryMode = PairingEntryMode.ADD_AQUARIUM;

    @Nullable
    private String selectedAddress;

    private boolean passwordVisible;

    private List<DiscoveredBoard> boards = new ArrayList<>();

    @Nullable
    private PairingState renderedState;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    granted -> {
                        if (BlePermissions.isGranted(requireContext()) && currentBlocker() == null) {
                            pairingRepository.startScan();
                        }
                        render();
                    });

    private final ActivityResultLauncher<Intent> enableBluetoothLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (BlePermissions.isEnabled(requireContext()) && currentBlocker() == null) {
                            pairingRepository.startScan();
                        }
                        render();
                    });

    private final BroadcastReceiver bluetoothStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (getView() == null) {
                return;
            }
            int newState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            if (newState != BluetoothAdapter.STATE_ON && newState != BluetoothAdapter.STATE_OFF) {
                return;
            }
            if (newState == BluetoothAdapter.STATE_OFF
                    && isPreProvisioning(pairingRepository.getState())) {
                pairingRepository.reset();
            }
            render();
        }
    };

    private final PairingRepository.PairingObserver observer =
            new PairingRepository.PairingObserver() {
                @Override
                public void onPairingStateChanged(@NonNull PairingState state) {
                    render();
                }

                @Override
                public void onBoardsChanged(@NonNull List<DiscoveredBoard> discovered) {
                    boards = discovered;
                    render();
                }
            };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_pairing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        this.readArguments();
        this.bindViews(view);

        if (savedInstanceState != null) {
            this.selectedAddress = savedInstanceState.getString(STATE_SELECTED_ADDRESS);
        }

        this.pairingRepository = PairingRepository.getInstance(requireContext());

        if (savedInstanceState == null && isFinished(this.pairingRepository.getState())) {
            this.pairingRepository.reset();
        }

        this.boardAdapter = new DiscoveredBoardAdapter(this::selectBoard);
        RecyclerView boardList = view.findViewById(R.id.rvBoards);
        boardList.setLayoutManager(new LinearLayoutManager(requireContext()));
        boardList.setAdapter(this.boardAdapter);

        this.passwordToggle.setOnClickListener(v -> this.togglePasswordVisibility());
        this.wifiSubmit.setOnClickListener(v -> this.submitCredentials());
        this.noticeRetry.setOnClickListener(v -> this.retry());

        InputFieldError.track(this.ssidInput, this.ssidBox,
                () -> !this.ssidInput.getText().toString().trim().isEmpty());
        InputFieldError.track(this.passwordInput, this.passwordBox,
                () -> !this.passwordInput.getText().toString().isEmpty());

        this.ssidInput.addTextChangedListener(afterTextChanged(() -> {
            if (!this.ssidInput.getText().toString().trim().isEmpty()) {
                this.ssidError.setVisibility(View.GONE);
            }
        }));
        this.passwordInput.addTextChangedListener(afterTextChanged(() -> {
            if (!this.passwordInput.getText().toString().isEmpty()) {
                this.passwordError.setVisibility(View.GONE);
            }
        }));

        this.pairingRepository.addObserver(this.observer);
    }

    @Override
    public void onStart() {
        super.onStart();
        ContextCompat.registerReceiver(requireContext(), this.bluetoothStateReceiver,
                new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED),
                ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onResume() {
        super.onResume();
        this.render();
    }

    @Override
    public void onStop() {
        super.onStop();
        this.pairingRepository.stopScan();
        requireContext().unregisterReceiver(this.bluetoothStateReceiver);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(STATE_SELECTED_ADDRESS, this.selectedAddress);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.pairingRepository.removeObserver(this.observer);
    }

    private void readArguments() {
        Bundle args = getArguments();
        if (args == null) {
            return;
        }
        this.aquariumConfig = args.getParcelable(ARG_AQUARIUM_CONFIG);
        this.entryMode = PairingEntryMode.fromName(args.getString(ARG_ENTRY_MODE));
    }

    private void bindViews(@NonNull View view) {
        this.root = view.findViewById(R.id.pairingRoot);
        this.heroSection = view.findViewById(R.id.heroSection);
        this.devicesSection = view.findViewById(R.id.devicesSection);
        this.wifiSection = view.findViewById(R.id.wifiSection);
        this.noticeCard = view.findViewById(R.id.noticeCard);

        this.orb = view.findViewById(R.id.pairingOrb);
        this.heroTitle = view.findViewById(R.id.tvHeroTitle);
        this.heroBody = view.findViewById(R.id.tvHeroBody);
        this.heroAction = view.findViewById(R.id.btnHeroAction);

        this.ssidInput = view.findViewById(R.id.etWifiSsid);
        this.passwordInput = view.findViewById(R.id.etWifiPassword);
        this.ssidBox = view.findViewById(R.id.wifiSsidBox);
        this.passwordBox = view.findViewById(R.id.wifiPasswordBox);
        this.ssidError = view.findViewById(R.id.tvWifiSsidError);
        this.passwordError = view.findViewById(R.id.tvWifiPasswordError);
        this.passwordToggle = view.findViewById(R.id.btnToggleWifiPassword);
        this.wifiSubmit = view.findViewById(R.id.btnWifiSubmit);

        this.noticeProgress = view.findViewById(R.id.noticeProgress);
        this.noticeIcon = view.findViewById(R.id.ivNoticeIcon);
        this.noticeTitle = view.findViewById(R.id.tvNoticeTitle);
        this.noticeBody = view.findViewById(R.id.tvNoticeBody);
        this.noticeRetry = view.findViewById(R.id.btnNoticeRetry);
    }


    private void render() {
        if (getView() == null) {
            return;
        }

        PairingState state = this.pairingRepository.getState();
        if (state != this.renderedState) {
            TransitionManager.beginDelayedTransition(this.root,
                    new AutoTransition().setDuration(REVEAL_DURATION_MS));
            this.renderedState = state;
        }

        PairingFailure failure = this.pairingRepository.getFailure();
        this.renderHero(state, failure);
        this.renderDevices(state);
        this.renderWifi(state);
        this.renderNotice(state, failure);
    }

    private void renderHero(@NonNull PairingState state, @Nullable PairingFailure failure) {
        PairingFailure blocker = this.currentBlocker();

        if (blocker != null && isPreProvisioning(state)) {
            this.showHero(BleStatusOrb.State.BLOCKED, R.string.pairing_blocked_title,
                    blocker.getMessageResId());
            this.showBlockerAction(blocker);
            return;
        }

        switch (state) {
            case IDLE:
                this.showHero(BleStatusOrb.State.IDLE, R.string.pairing_hero_idle_title,
                        R.string.pairing_hero_idle_body);
                this.showHeroAction(R.string.pairing_hero_start, v -> this.startScan());
                break;

            case SCANNING:
                this.showHero(BleStatusOrb.State.SCANNING, R.string.pairing_hero_scanning_title,
                        R.string.pairing_hero_scanning_body);
                this.heroAction.setVisibility(View.GONE);
                break;

            case SCAN_COMPLETE:
                if (this.boards.isEmpty()) {
                    this.showHero(BleStatusOrb.State.BLOCKED, R.string.pairing_hero_empty_title,
                            R.string.pairing_hero_empty_body);
                    this.showHeroAction(R.string.pairing_hero_rescan, v -> this.startScan());
                } else {
                    this.showHero(BleStatusOrb.State.IDLE, R.string.pairing_hero_found_title,
                            R.string.pairing_hero_found_body);
                    this.heroAction.setVisibility(View.GONE);
                }
                break;

            case FAILED:
                if (failure == PairingFailure.NO_BOARD_FOUND) {
                    this.showHero(BleStatusOrb.State.BLOCKED, R.string.pairing_hero_empty_title,
                            R.string.pairing_hero_empty_body);
                    this.showHeroAction(R.string.pairing_hero_rescan, v -> this.retry());
                } else {
                    this.heroSection.setVisibility(View.GONE);
                }
                break;

            case SUCCESS:
                this.renderSuccessHero();
                break;

            default:
                this.heroSection.setVisibility(View.GONE);
                break;
        }
    }

    private void renderSuccessHero() {
        this.heroSection.setVisibility(View.VISIBLE);
        this.orb.setState(BleStatusOrb.State.SUCCESS);
        this.heroTitle.setText(R.string.pairing_success_hero_title);
        this.heroBody.setText(getString(R.string.pairing_success_hero_body, this.pairedAquariumName()));
        this.heroBody.setVisibility(View.VISIBLE);

        this.heroAction.setBackgroundResource(R.drawable.bg_auth_primary_button);
        this.heroAction.setTextColor(ContextCompat.getColor(requireContext(), R.color.white));
        this.showHeroAction(this.entryMode == PairingEntryMode.FIRST_RUN
                        ? R.string.pairing_action_continue
                        : R.string.pairing_action_dashboard,
                v -> this.leavePairing());
    }

    private void showHero(@NonNull BleStatusOrb.State orbState, int titleResId, int bodyResId) {
        this.heroSection.setVisibility(View.VISIBLE);
        this.orb.setState(orbState);
        this.heroTitle.setText(titleResId);
        this.heroBody.setText(bodyResId);
        this.heroBody.setVisibility(View.VISIBLE);

        this.heroAction.setBackgroundResource(R.drawable.bg_pairing_secondary);
        this.heroAction.setTextColor(ContextCompat.getColor(requireContext(), R.color.accent));
    }

    private void showHeroAction(int labelResId, @NonNull View.OnClickListener onClick) {
        this.heroAction.setText(labelResId);
        this.heroAction.setOnClickListener(onClick);
        this.heroAction.setVisibility(View.VISIBLE);
    }


    private void showBlockerAction(@NonNull PairingFailure blocker) {
        switch (blocker) {
            case BLUETOOTH_OFF:
                this.showHeroAction(R.string.pairing_bluetooth_turn_on,
                        v -> this.enableBluetoothLauncher.launch(BlePermissions.enableBluetoothIntent()));
                break;

            case PERMISSION_DENIED:
                if (BlePermissions.isPermanentlyDenied(requireActivity())) {
                    this.showHeroAction(R.string.pairing_permission_settings,
                            v -> startActivity(BlePermissions.appSettingsIntent(requireContext())));
                } else {
                    this.heroBody.setText(R.string.pairing_permission_rationale);
                    this.showHeroAction(R.string.pairing_grant_permission,
                            v -> this.permissionLauncher.launch(BlePermissions.REQUIRED));
                }
                break;

            default:
                this.heroAction.setVisibility(View.GONE);
                break;
        }
    }

    private void renderDevices(@NonNull PairingState state) {
        boolean listed = !this.boards.isEmpty()
                && state != PairingState.IDLE
                && state != PairingState.SUCCESS
                && !(state == PairingState.FAILED && this.selectedAddress == null);
        this.devicesSection.setVisibility(listed ? View.VISIBLE : View.GONE);

        this.boardAdapter.submitList(this.displayedBoards());
        this.boardAdapter.setSelected(this.selectedAddress, rowStateFor(state));
    }

    @NonNull
    private List<DiscoveredBoard> displayedBoards() {
        if (this.selectedAddress == null) {
            return this.boards;
        }
        for (DiscoveredBoard board : this.boards) {
            if (this.selectedAddress.equals(board.getAddress())) {
                return Collections.singletonList(board);
            }
        }
        return this.boards;
    }

    @NonNull
    private DiscoveredBoardAdapter.RowState rowStateFor(@NonNull PairingState state) {
        if (this.selectedAddress == null) {
            return DiscoveredBoardAdapter.RowState.IDLE;
        }
        switch (state) {
            case PROVISIONING:
            case CLAIMING:
            case AWAITING_BOARD:
                return DiscoveredBoardAdapter.RowState.BUSY;
            case SUCCESS:
                return DiscoveredBoardAdapter.RowState.DONE;
            default:
                return DiscoveredBoardAdapter.RowState.SELECTED;
        }
    }

    private void renderWifi(@NonNull PairingState state) {
        boolean editable = this.selectedAddress != null
                && (state == PairingState.SCANNING || state == PairingState.SCAN_COMPLETE);
        this.wifiSection.setVisibility(editable ? View.VISIBLE : View.GONE);
    }

    private void renderNotice(@NonNull PairingState state, @Nullable PairingFailure failure) {
        switch (state) {
            case PROVISIONING:
                this.showProgressNotice(R.string.pairing_progress_provisioning, 0);
                break;

            case CLAIMING:
                this.showProgressNotice(R.string.pairing_progress_claiming, 0);
                break;

            case AWAITING_BOARD:
                this.showProgressNotice(R.string.pairing_progress_awaiting,
                        R.string.pairing_progress_awaiting_hint);
                break;

            case FAILED:
                if (failure == null || failure == PairingFailure.NO_BOARD_FOUND
                        || this.currentBlocker() != null) {
                    this.noticeCard.setVisibility(View.GONE);
                    return;
                }
                this.showTerminalNotice(R.drawable.bg_pairing_notice_error,
                        R.drawable.info_24px, R.color.pairing_error_stroke,
                        R.color.pairing_error_text,
                        getString(R.string.pairing_hero_failed_title),
                        getString(failure.getMessageResId()),
                        failure.isRetryable());
                break;

            default:
                this.noticeCard.setVisibility(View.GONE);
                break;
        }
    }

    private void showProgressNotice(int titleResId, int bodyResId) {
        this.noticeCard.setVisibility(View.VISIBLE);
        this.noticeCard.setBackgroundResource(R.drawable.bg_pairing_notice);
        this.noticeProgress.setVisibility(View.VISIBLE);
        this.noticeIcon.setVisibility(View.GONE);
        this.noticeRetry.setVisibility(View.GONE);

        this.noticeTitle.setText(titleResId);
        this.noticeTitle.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.text_primary));

        if (bodyResId == 0) {
            this.noticeBody.setVisibility(View.GONE);
        } else {
            this.noticeBody.setText(bodyResId);
            this.noticeBody.setVisibility(View.VISIBLE);
        }
    }

    private void showTerminalNotice(int backgroundResId,
                                    int iconResId,
                                    int iconTintResId,
                                    int titleTintResId,
                                    @NonNull String title,
                                    @NonNull String body,
                                    boolean retryable) {
        this.noticeCard.setVisibility(View.VISIBLE);
        this.noticeCard.setBackgroundResource(backgroundResId);
        this.noticeProgress.setVisibility(View.GONE);

        this.noticeIcon.setVisibility(View.VISIBLE);
        this.noticeIcon.setImageResource(iconResId);
        this.noticeIcon.setImageTintList(
                ContextCompat.getColorStateList(requireContext(), iconTintResId));

        this.noticeTitle.setText(title);
        this.noticeTitle.setTextColor(ContextCompat.getColor(requireContext(), titleTintResId));
        this.noticeBody.setText(body);
        this.noticeBody.setVisibility(View.VISIBLE);
        this.noticeRetry.setVisibility(retryable ? View.VISIBLE : View.GONE);
    }


    private void startScan() {
        if (!BlePermissions.isGranted(requireContext())) {
            this.permissionLauncher.launch(BlePermissions.REQUIRED);
            return;
        }
        if (this.currentBlocker() != null) {
            this.render();
            return;
        }
        this.selectedAddress = null;
        this.pairingRepository.startScan();
    }

    private void selectBoard(@NonNull DiscoveredBoard board) {
        this.selectedAddress = board.getAddress();
        this.pairingRepository.stopScan();
        this.render();
        this.revealWifiForm();
    }

    private void revealWifiForm() {
        View view = getView();
        if (view == null) {
            return;
        }
        this.wifiSection.post(() -> {
            View scroll = view.findViewById(R.id.pairingScroll);
            if (scroll != null) {
                scroll.scrollTo(0, this.wifiSection.getTop());
            }
            this.ssidInput.requestFocus();
        });
    }

    private void submitCredentials() {

        String ssid = this.ssidInput.getText().toString().trim();
        String password = this.passwordInput.getText().toString();

        boolean ssidEmpty = ssid.isEmpty();
        boolean passwordEmpty = password.isEmpty();
        InputFieldError.set(this.ssidBox,
                ssidEmpty ? InputFieldError.State.ERROR : InputFieldError.State.NEUTRAL);
        InputFieldError.set(this.passwordBox,
                passwordEmpty ? InputFieldError.State.ERROR : InputFieldError.State.NEUTRAL);
        this.ssidError.setVisibility(ssidEmpty ? View.VISIBLE : View.GONE);
        this.passwordError.setVisibility(passwordEmpty ? View.VISIBLE : View.GONE);
        if (ssidEmpty || passwordEmpty) {
            return;
        }

        DiscoveredBoard board = this.pairingRepository.getBoard(this.selectedAddress);
        if (board == null) {
            this.selectedAddress = null;
            this.ssidInput.setError(getString(R.string.pairing_wifi_error_board));
            this.render();
            return;
        }
        if (this.aquariumConfig == null) {
            ScopedLogger.error("Reached the pairing screen with no aquarium configuration.");
            return;
        }

        this.hideKeyboard();
        this.pairingRepository.pair(board, ssid, password, this.aquariumConfig);
    }

    private void retry() {
        this.pairingRepository.reset();
        this.selectedAddress = null;
        this.passwordInput.setText("");
        this.startScan();
    }

    private static TextWatcher afterTextChanged(@NonNull Runnable action) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                action.run();
            }
        };
    }

    private void togglePasswordVisibility() {
        this.passwordVisible = !this.passwordVisible;

        int selection = this.passwordInput.getSelectionStart();
        this.passwordInput.setInputType(InputType.TYPE_CLASS_TEXT | (this.passwordVisible
                ? InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                : InputType.TYPE_TEXT_VARIATION_PASSWORD));
        this.passwordInput.setSelection(selection);

        this.passwordToggle.setImageResource(this.passwordVisible
                ? R.drawable.visibility_off_24px
                : R.drawable.visibility_24px);
    }

    private void leavePairing() {
        this.pairingRepository.removeObserver(this.observer);
        this.pairingRepository.reset();

        if (getActivity() instanceof PairingActivity) {
            ((PairingActivity) getActivity()).finishPairing(this.entryMode);
        }
    }


    @Nullable
    private PairingFailure currentBlocker() {
        if (!BlePermissions.isSupported(requireContext())) {
            return PairingFailure.UNSUPPORTED;
        }
        if (!BlePermissions.isGranted(requireContext())) {
            return PairingFailure.PERMISSION_DENIED;
        }
        if (!BlePermissions.isEnabled(requireContext())) {
            return PairingFailure.BLUETOOTH_OFF;
        }
        return null;
    }

    private static boolean isPreProvisioning(@NonNull PairingState state) {
        return state == PairingState.IDLE
                || state == PairingState.SCANNING
                || state == PairingState.SCAN_COMPLETE
                || state == PairingState.FAILED;
    }

    private static boolean isFinished(@NonNull PairingState state) {
        return state == PairingState.SUCCESS || state == PairingState.FAILED;
    }

    @NonNull
    private String pairedAquariumName() {
        Aquarium paired = AquariumRepository.getInstance(requireContext())
                .getAquarium(this.pairingRepository.getDeviceUid());
        if (paired != null) {
            return paired.getName();
        }
        return this.aquariumConfig == null ? "" : this.aquariumConfig.getName();
    }

    private void hideKeyboard() {
        InputMethodManager manager =
                ContextCompat.getSystemService(requireContext(), InputMethodManager.class);
        if (manager != null) {
            manager.hideSoftInputFromWindow(this.passwordInput.getWindowToken(), 0);
        }
    }
}
