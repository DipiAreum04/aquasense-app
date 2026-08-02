package ca.team6.aquasense.settings;

import android.app.TimePickerDialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import java.util.Locale;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.SettingsRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;

public class NotificationsFragment extends Fragment {

    // TODO: TEMPORARY; SHOULD BE ADDRESSED BY END OF SPRINT 2
    @SuppressWarnings("FieldCanBeLocal")
    private SettingsRepository repo;
    private SharedPreferenceHelper prefs;

    // Quiet-hours time row references (shown/hidden with toggle)
    private View dividerQuietStart, dividerQuietEnd;
    private View rowQuietStart, rowQuietEnd;
    private TextView tvQuietStart, tvQuietEnd;

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
            switchSensor.setChecked(s.notifySensorOffline);
            switchHubOffline.setChecked(s.notifyHubDisconnected);
            tvQuietStart.setText(s.quietHoursStart);
            tvQuietEnd.setText(s.quietHoursEnd);
            setQuietRowsVisible(s.quietHours);
        });

        // TODO: Wire push notifications (FCM or local notifications).
        switchPush.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_PUSH_NOTIF, checked);
            SharedPreferenceHelper.showComingSoon(requireContext());
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
        // TODO: Enforce critical-only filtering when sending alerts.
        switchCritical.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_CRITICAL_ONLY, checked);
            SharedPreferenceHelper.showComingSoon(requireContext());
        });
        // TODO: Mute non-critical alerts during quiet hours; critical alerts always deliver.
        switchQuiet.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_QUIET_HOURS, checked);
            setQuietRowsVisible(checked);
            SharedPreferenceHelper.showComingSoon(requireContext());
        });
        // TODO: Suppress all notifications while Maintenance Mode is on.
        switchMaintenance.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_FEEDING_SILENCE, checked);
            SharedPreferenceHelper.showComingSoon(requireContext());
        });
        // TODO: Notify when a water parameter is out of range.
        switchParam.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_NOTIFY_PARAM, checked);
            SharedPreferenceHelper.showComingSoon(requireContext());
        });
        // TODO: ARMAAN: Wire this to the notification logic.
        
        // Detect abnormal value jumps that stay inside the safe range.
        // Runs alongside the threshold check, not instead of it.
        switchJumps.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_NOTIFY_ABNORMAL_JUMPS, checked);
            SharedPreferenceHelper.showComingSoon(requireContext());
        });

        // Before sending a notification, check the flag for the sensor it concerns
        // If false, drop the alert regardless of the notification-type toggles. 
        // Critical alerts are the one exception and must still deliver.
        switchSensorTemp.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_SENSOR_ALERTS_TEMP, checked);
            SharedPreferenceHelper.showComingSoon(requireContext());
        });
        switchSensorLevel.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_SENSOR_ALERTS_LEVEL, checked);
            SharedPreferenceHelper.showComingSoon(requireContext());
        });
        switchSensorTds.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_SENSOR_ALERTS_TDS, checked);
            SharedPreferenceHelper.showComingSoon(requireContext());
        });
        switchSensorPh.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_SENSOR_ALERTS_PH, checked);
            SharedPreferenceHelper.showComingSoon(requireContext());
        });

        // TODO: Notify when a sensor goes offline.
        switchSensor.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_NOTIFY_SENSOR, checked);
            SharedPreferenceHelper.showComingSoon(requireContext());
        });
        // TODO: Notify when the hub stops reporting.
        switchHubOffline.setOnCheckedChangeListener((b, checked) -> {
            prefs.updateField(SettingsRepository.KEY_NOTIFY_HUB_DISCONNECTED, checked);
            SharedPreferenceHelper.showComingSoon(requireContext());
        });

        rowQuietStart.setOnClickListener(v2 ->
                showTimePicker(tvQuietStart.getText().toString(),
                        SettingsRepository.KEY_QUIET_START, tvQuietStart));
        rowQuietEnd.setOnClickListener(v2 ->
                showTimePicker(tvQuietEnd.getText().toString(),
                        SettingsRepository.KEY_QUIET_END, tvQuietEnd));
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
