package ca.team6.aquasense.settings;

import android.Manifest;
import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import java.util.Locale;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.SettingsRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;
import ca.team6.aquasense.notifications.BackgroundMonitoringPrompt;
import ca.team6.aquasense.notifications.MonitoringController;

public class NotificationsFragment extends Fragment {

    // TODO: TEMPORARY; SHOULD BE ADDRESSED BY END OF SPRINT 2
    @SuppressWarnings("FieldCanBeLocal")
    private SettingsRepository repo;
    private SharedPreferenceHelper prefs;

    // Quiet-hours time row references (shown/hidden with toggle)
    private View dividerQuietStart, dividerQuietEnd;
    private View rowQuietStart, rowQuietEnd;
    private View dividerNotificationPermission, rowNotificationPermission;
    private TextView tvQuietStart, tvQuietEnd;

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

        SwitchCompat switchPush      = view.findViewById(R.id.switchPushNotifications);
        SwitchCompat switchEmail     = view.findViewById(R.id.switchEmailAlerts);
        SwitchCompat switchSms       = view.findViewById(R.id.switchSmsAlerts);
        SwitchCompat switchCritical  = view.findViewById(R.id.switchCriticalOnly);
        SwitchCompat switchQuiet     = view.findViewById(R.id.switchQuietHours);
        SwitchCompat switchMaintenance = view.findViewById(R.id.switchFeedingSilence);
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
            switchMaintenance.setChecked(s.feedingModeSilence);
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
            prefs.updateField(SettingsRepository.KEY_FEEDING_SILENCE, checked);
        });
        // NF-1.1: handled by FirebaseThresholdMonitor when push + param alerts are enabled.
        switchParam.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_NOTIFY_PARAM, checked);
            MonitoringController.sync(requireContext());
        });
        switchJumps.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_NOTIFY_ABNORMAL_JUMPS, checked);
            MonitoringController.sync(requireContext());
        });

        // Before sending a notification, check the flag for the sensor it concerns.
        // If false, drop the alert regardless of the notification-type toggles.
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

        rowNotificationPermission.setOnClickListener(v -> requestNotificationAccess());
        updateNotificationPermissionRowVisibility();
    }

    @Override
    public void onResume() {
        super.onResume();
        // The row tracks system state that the user can change while this screen is backgrounded.
        updateNotificationPermissionRowVisibility();
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
