package ca.team6.aquasense.settings;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import android.Manifest;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.Aquarium;
import ca.team6.aquasense.model.DatabaseSchema;
import ca.team6.aquasense.model.FirebaseDatabaseHelper;
import ca.team6.aquasense.model.SettingsRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;
import ca.team6.aquasense.notifications.BackgroundMonitoringPrompt;
import ca.team6.aquasense.notifications.MaintenanceModeStore;
import ca.team6.aquasense.notifications.MonitoringController;

public class NotificationsFragment extends Fragment {

    @SuppressWarnings("FieldCanBeLocal")
    private SettingsRepository repo;
    private SharedPreferenceHelper prefs;
    private MaintenanceModeStore maintenanceModeStore;

    @Nullable
    private String activeAquariumId;
    @Nullable
    private DatabaseReference userProfileRef;
    @Nullable
    private ValueEventListener userProfileListener;
    private SwitchCompat switchMaintenance;
    private boolean suppressMaintenanceToggleCallback;

    private View dividerQuietStart, dividerQuietEnd;
    private View rowQuietStart, rowQuietEnd;
    private View dividerNotificationPermission, rowNotificationPermission;
    private TextView tvQuietStart, tvQuietEnd;

    private View dividerMaintenanceDuration, rowMaintenanceDuration;
    private TextView tvMaintenanceRemaining;

    private final ActivityResultLauncher<String> requestNotificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                prefs.setBooleanSync(SettingsRepository.KEY_NOTIF_PERMISSION_ASKED, true);
                MonitoringController.sync(requireContext());
                updateNotificationPermissionRowVisibility();
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_notifications, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = new SettingsRepository(requireContext());
        prefs = SharedPreferenceHelper.getInstance(requireContext());
        maintenanceModeStore = new MaintenanceModeStore(requireContext());

        SwitchCompat switchPush      = view.findViewById(R.id.switchPushNotifications);
        SwitchCompat switchCritical  = view.findViewById(R.id.switchCriticalOnly);
        SwitchCompat switchQuiet     = view.findViewById(R.id.switchQuietHours);
        switchMaintenance = view.findViewById(R.id.switchFeedingSilence);
        SwitchCompat switchParam     = view.findViewById(R.id.switchNotifyParam);
        SwitchCompat switchJumps     = view.findViewById(R.id.switchNotifyAbnormalJumps);
        SwitchCompat switchSensor    = view.findViewById(R.id.switchNotifySensor);
        SwitchCompat switchHubOffline = view.findViewById(R.id.switchNotifyHubDisconnected);

        dividerQuietStart = view.findViewById(R.id.dividerQuietStart);
        dividerQuietEnd   = view.findViewById(R.id.dividerQuietEnd);
        rowQuietStart     = view.findViewById(R.id.rowQuietStart);
        rowQuietEnd       = view.findViewById(R.id.rowQuietEnd);
        dividerNotificationPermission = view.findViewById(R.id.dividerNotificationPermission);
        rowNotificationPermission = view.findViewById(R.id.rowNotificationPermission);
        tvQuietStart      = view.findViewById(R.id.tvQuietStart);
        tvQuietEnd        = view.findViewById(R.id.tvQuietEnd);

        dividerMaintenanceDuration = view.findViewById(R.id.dividerMaintenanceDuration);
        rowMaintenanceDuration     = view.findViewById(R.id.rowMaintenanceDuration);
        tvMaintenanceRemaining     = view.findViewById(R.id.tvMaintenanceRemaining);

        SwitchCompat switchSensorTemp  = view.findViewById(R.id.switchSensorAlertsTemperature);
        SwitchCompat switchSensorLevel = view.findViewById(R.id.switchSensorAlertsWaterLevel);
        SwitchCompat switchSensorTds   = view.findViewById(R.id.switchSensorAlertsDissolvedSolids);
        SwitchCompat switchSensorPh    = view.findViewById(R.id.switchSensorAlertsPhLevel);

        repo.loadSettings(s -> {
            switchPush.setChecked(s.pushNotifications);
            switchCritical.setChecked(s.criticalAlertsOnly);
            switchQuiet.setChecked(s.quietHours);
            switchParam.setChecked(s.notifyParamOutOfRange);
            switchJumps.setChecked(s.notifyAbnormalJumps);
            switchSensorTemp.setChecked(s.sensorAlertsTemperature);
            switchSensorLevel.setChecked(s.sensorAlertsWaterLevel);
            switchSensorTds.setChecked(s.sensorAlertsDissolvedSolids);
            switchSensorPh.setChecked(s.sensorAlertsPhLevel);
            switchSensor.setChecked(s.notifySensorDisconnected);
            switchHubOffline.setChecked(s.notifyHubDisconnected);
            tvQuietStart.setText(s.quietHoursStart);
            tvQuietEnd.setText(s.quietHoursEnd);
            setQuietRowsVisible(s.quietHours);
        });

        switchPush.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_PUSH_NOTIF, checked);
            MonitoringController.sync(requireContext());
        });
        switchCritical.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_CRITICAL_ONLY, checked);
        });
        switchQuiet.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_QUIET_HOURS, checked);
            setQuietRowsVisible(checked);
        });
        switchMaintenance.setOnCheckedChangeListener((b, checked) -> {
            if (suppressMaintenanceToggleCallback || activeAquariumId == null) {
                return;
            }
            if (checked) {
                showMaintenanceDurationPicker(activeAquariumId);
                return;
            }
            maintenanceModeStore.setActive(activeAquariumId, false);
            setMaintenanceDurationRowVisible(false);
            MonitoringController.sync(requireContext());
        });
        switchParam.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_NOTIFY_PARAM, checked);
            MonitoringController.sync(requireContext());
        });
        switchJumps.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_NOTIFY_ABNORMAL_JUMPS, checked);
            MonitoringController.sync(requireContext());
        });

        switchSensorTemp.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_SENSOR_ALERTS_TEMP, checked);
        });
        switchSensorLevel.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_SENSOR_ALERTS_LEVEL, checked);
        });
        switchSensorTds.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_SENSOR_ALERTS_TDS, checked);
        });
        switchSensorPh.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_SENSOR_ALERTS_PH, checked);
        });

        switchSensor.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_NOTIFY_SENSOR, checked);
            MonitoringController.sync(requireContext());
        });
        switchHubOffline.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_NOTIFY_HUB_DISCONNECTED, checked);
            MonitoringController.sync(requireContext());
        });

        rowQuietStart.setOnClickListener(v2 ->
                showTimePicker(tvQuietStart.getText().toString(),
                        SettingsRepository.KEY_QUIET_START, tvQuietStart));
        rowQuietEnd.setOnClickListener(v2 ->
                showTimePicker(tvQuietEnd.getText().toString(),
                        SettingsRepository.KEY_QUIET_END, tvQuietEnd));

        rowMaintenanceDuration.setOnClickListener(v -> {
            if (activeAquariumId != null) {
                showMaintenanceDurationPicker(activeAquariumId);
            }
        });

        attachUserProfileListener();

        rowNotificationPermission.setOnClickListener(v -> requestNotificationAccess());
        updateNotificationPermissionRowVisibility();
    }

    @Override
    public void onResume() {
        super.onResume();
        syncActiveAquariumFromPrefs();
        refreshMaintenanceModeToggle();
        updateNotificationPermissionRowVisibility();
    }

    private void syncActiveAquariumFromPrefs() {
        if (prefs == null) {
            return;
        }
        String savedId = prefs.getString(SettingsRepository.KEY_ACTIVE_AQUARIUM, null);
        if (savedId != null && !savedId.isEmpty()) {
            activeAquariumId = savedId;
        }
    }

    @Override
    public void onDestroyView() {
        detachUserProfileListener();
        super.onDestroyView();
    }

    private void attachUserProfileListener() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            applyActiveAquarium(null);
            return;
        }

        detachUserProfileListener();
        userProfileRef = FirebaseDatabase.getInstance()
                .getReference()
                .child(user.getUid());
        userProfileListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                applyActiveAquarium(resolveActiveAquariumId(
                        FirebaseDatabaseHelper.parseAquariums(
                                snapshot.child(DatabaseSchema.AQUARIUMS_KEY))));
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                applyActiveAquarium(null);
            }
        };
        userProfileRef.addValueEventListener(userProfileListener);
    }

    private void detachUserProfileListener() {
        if (userProfileRef != null && userProfileListener != null) {
            userProfileRef.removeEventListener(userProfileListener);
        }
        userProfileRef = null;
        userProfileListener = null;
    }

    @Nullable
    private String resolveActiveAquariumId(@NonNull List<Aquarium> aquariums) {
        if (aquariums.isEmpty() || prefs == null) {
            return null;
        }

        Set<String> aquariumIds = new HashSet<>();
        for (Aquarium aquarium : aquariums) {
            aquariumIds.add(aquarium.getId());
        }

        String savedId = prefs.getString(SettingsRepository.KEY_ACTIVE_AQUARIUM, null);
        if (savedId != null && aquariumIds.contains(savedId)) {
            return savedId;
        }

        String firstId = aquariums.get(0).getId();
        prefs.setString(SettingsRepository.KEY_ACTIVE_AQUARIUM, firstId);
        return firstId;
    }

    private void applyActiveAquarium(@Nullable String aquariumId) {
        activeAquariumId = aquariumId;
        refreshMaintenanceModeToggle();
    }

    private void refreshMaintenanceModeToggle() {
        if (switchMaintenance == null) {
            return;
        }

        boolean hasAquarium = activeAquariumId != null;
        switchMaintenance.setEnabled(hasAquarium);

        suppressMaintenanceToggleCallback = true;
        boolean active = hasAquarium && maintenanceModeStore.isActive(activeAquariumId);
        switchMaintenance.setChecked(active);
        suppressMaintenanceToggleCallback = false;

        setMaintenanceDurationRowVisible(active);
        updateMaintenanceRemainingText();
    }

    private void showMaintenanceDurationPicker(@Nullable String aquariumId) {
        if (aquariumId == null) {
            refreshMaintenanceModeToggle();
            return;
        }

        View content = getLayoutInflater().inflate(R.layout.dialog_maintenance_duration, null);

        AlertDialog dialog = new MaterialAlertDialogBuilder(requireContext())
                .setView(content)
                .create();

        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        int[] optionIds = {
                R.id.maintenanceDuration15m,
                R.id.maintenanceDuration30m,
                R.id.maintenanceDuration1h,
                R.id.maintenanceDuration2h,
        };
        for (int i = 0; i < optionIds.length; i++) {
            long durationMs = MaintenanceModeStore.DURATION_OPTIONS[i];
            content.findViewById(optionIds[i]).setOnClickListener(v -> {
                dialog.dismiss();
                maintenanceModeStore.activateForDuration(aquariumId, durationMs);
                MonitoringController.sync(requireContext());
                setMaintenanceDurationRowVisible(true);
                updateMaintenanceRemainingText();
            });
        }

        dialog.setOnCancelListener(d -> refreshMaintenanceModeToggle());
        dialog.show();
    }

    private void setMaintenanceDurationRowVisible(boolean visible) {
        if (dividerMaintenanceDuration == null || rowMaintenanceDuration == null) {
            return;
        }
        int visibility = visible ? View.VISIBLE : View.GONE;
        dividerMaintenanceDuration.setVisibility(visibility);
        rowMaintenanceDuration.setVisibility(visibility);
    }

    private void updateMaintenanceRemainingText() {
        if (tvMaintenanceRemaining == null || activeAquariumId == null) {
            return;
        }

        if (!maintenanceModeStore.isActive(activeAquariumId)) {
            return;
        }

        long remainingMs = maintenanceModeStore.getRemainingMs(activeAquariumId);
        if (remainingMs < 60_000L) {
            tvMaintenanceRemaining.setText(R.string.maintenance_remaining_less_than_minute);
            return;
        }
        tvMaintenanceRemaining.setText(getString(
                R.string.maintenance_remaining,
                formatRemainingDuration(remainingMs)));
    }

    @NonNull
    private String formatRemainingDuration(long remainingMs) {
        long totalMinutes = (remainingMs + 59_999L) / 60_000L;
        long hours = totalMinutes / 60L;
        long minutes = totalMinutes % 60L;
        if (hours == 0L) {
            return totalMinutes + " min";
        }
        if (minutes == 0L) {
            return hours == 1L ? "1 hr" : hours + " hr";
        }
        return hours + " hr " + minutes + " min";
    }

    private void requestNotificationAccess() {
        boolean askedBefore =
                prefs.getBoolean(SettingsRepository.KEY_NOTIF_PERMISSION_ASKED, false);
        if (!askedBefore
                || shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            requestNotificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
            return;
        }
        BackgroundMonitoringPrompt.openAppNotificationSettings(requireActivity());
    }

    private void updateNotificationPermissionRowVisibility() {
        if (rowNotificationPermission == null || dividerNotificationPermission == null) {
            return;
        }
        int visibility =
                BackgroundMonitoringPrompt.isNotificationPermissionMissing(requireContext())
                        ? View.VISIBLE
                        : View.GONE;
        rowNotificationPermission.setVisibility(visibility);
        dividerNotificationPermission.setVisibility(visibility);
    }

    private void setQuietRowsVisible(boolean visible) {
        int vis = visible ? View.VISIBLE : View.GONE;
        dividerQuietStart.setVisibility(vis);
        rowQuietStart.setVisibility(vis);
        dividerQuietEnd.setVisibility(vis);
        rowQuietEnd.setVisibility(vis);
    }

    private void showTimePicker(String currentTime, String key, TextView displayView) {
        String[] parts = currentTime.split(":");
        int hour   = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTimeFormat(TimeFormat.CLOCK_24H)
                .setHour(hour)
                .setMinute(minute)
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            String newTime = String.format(
                    Locale.getDefault(), "%02d:%02d", picker.getHour(), picker.getMinute());
            displayView.setText(newTime);
            prefs.updateField(key, newTime);
        });
        picker.show(getParentFragmentManager(), "quiet-hours-time-picker");
    }
}
