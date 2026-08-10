package ca.team6.aquasense.settings;

import android.Manifest;
import android.app.TimePickerDialog;
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

    // TODO: TEMPORARY; SHOULD BE ADDRESSED BY END OF SPRINT 2
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

    // Quiet-hours time row references (shown/hidden with toggle)
    private View dividerQuietStart, dividerQuietEnd;
    private View rowQuietStart, rowQuietEnd;
    private View dividerNotificationPermission, rowNotificationPermission;
    private TextView tvQuietStart, tvQuietEnd;

    // Maintenance snooze duration row (shown while maintenance mode is active)
    private View dividerMaintenanceDuration, rowMaintenanceDuration;
    private TextView tvMaintenanceRemaining;

    private final ActivityResultLauncher<String> requestNotificationPermission =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                // MainActivity only asks once per install, so a grant from here is what arms the
                // monitor for users who reached this row after refusing on first launch.
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
        SwitchCompat switchEmail     = view.findViewById(R.id.switchEmailAlerts);
        SwitchCompat switchSms       = view.findViewById(R.id.switchSmsAlerts);
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

        // Bind before listeners so the initial values do not toast.
        repo.loadSettings(s -> {
            switchPush.setChecked(s.pushNotifications);
            switchEmail.setChecked(s.emailAlerts);
            switchSms.setChecked(s.smsAlerts);
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
            // Starts or stops the background monitor immediately, so the persistent "Monitoring
            // aquarium" notice disappears the moment alerts are switched off.
            MonitoringController.sync(requireContext());
        });
        // TODO: Wire email alerts (SMTP / SendGrid / backend API; Firebase not required).
        switchEmail.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_EMAIL_ALERTS, checked);
            SharedPreferenceHelper.showComingSoon(requireContext());
        });
        // TODO: Wire SMS alerts (e.g. Twilio or similar).
        switchSms.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_SMS_ALERTS, checked);
            SharedPreferenceHelper.showComingSoon(requireContext());
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
        // NF-1.1: handled by FirebaseThresholdMonitor when push + param alerts are enabled.
        switchParam.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_NOTIFY_PARAM, checked);
            MonitoringController.sync(requireContext());
        });
        // NF-1.3: abnormal value jump alerts within the safe range.
        switchJumps.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_NOTIFY_ABNORMAL_JUMPS, checked);
            MonitoringController.sync(requireContext());
        });

        // NF-1.3: per-sensor toggles. Before sending a notification, check the flag for the sensor
        // it concerns. If false, drop the alert regardless of the notification-type toggles.
        // Critical alerts are the one exception and must still deliver.
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

        // NF-1.2: handled by FirebaseThresholdMonitor when push + sensor offline alerts are enabled.
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
        // The dashboard switches the active aquarium through local preferences, which the profile
        // listener never sees. Without re-reading it here the toggle keeps describing whichever
        // aquarium was selected when this screen was last opened, and switching it off would
        // release maintenance mode on that one instead of the aquarium now on screen.
        syncActiveAquariumFromPrefs();
        refreshMaintenanceModeToggle();
        // The row tracks system state that the user can change while this screen is backgrounded.
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

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(content)
                .create();

        Window window = dialog.getWindow();
        if (window != null) {
            // The layout draws its own rounded card, so the window behind it has to stop drawing
            // one: AlertDialog's background is an opaque square-cornered surface, and left in
            // place it shows at all four corners of the card as a grey right angle.
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        // Paired with DURATION_OPTIONS by position, as the labels array was.
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

        // Only a duration commits. Back and a tap outside both land here and put the switch back
        // the way it was, so the picker cannot leave maintenance mode half on.
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

    /**
     * Sends the user wherever the permission can still be granted. The system sheet handles the
     * first refusal, but the platform stops showing it after the second and answers silently, so
     * from that point the app's notification settings are the only way through.
     */
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
        new TimePickerDialog(requireContext(), (tp, h, m) -> {
            String newTime = String.format(Locale.getDefault(), "%02d:%02d", h, m);
            displayView.setText(newTime);
            prefs.updateField(key, newTime);
        }, hour, minute, true).show();
    }
}
