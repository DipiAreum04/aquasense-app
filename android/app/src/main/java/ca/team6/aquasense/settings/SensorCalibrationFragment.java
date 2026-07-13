package ca.team6.aquasense.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.AppSettings;
import ca.team6.aquasense.model.SettingsRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;

public class SensorCalibrationFragment extends Fragment {

    private static final long MS_PER_DAY = 1000L * 60 * 60 * 24;

    private SettingsRepository repo;
    private SharedPreferenceHelper prefs;
    private AppSettings currentSettings;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_sensor_calibration, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        repo = new SettingsRepository(requireContext());
        prefs = SharedPreferenceHelper.getInstance(requireContext());

        repo.loadSettings(s -> {
            currentSettings = s;
            refreshStatus(view, s);
        });

        view.findViewById(R.id.rowGuidePh).setOnClickListener(v ->
                showGuide(R.string.guide_title_ph, R.string.guide_ph));
        view.findViewById(R.id.rowGuideTemp).setOnClickListener(v ->
                showGuide(R.string.guide_title_temp, R.string.guide_temp));
        view.findViewById(R.id.rowGuideSalinity).setOnClickListener(v ->
                showGuide(R.string.guide_title_salinity, R.string.guide_salinity));
        view.findViewById(R.id.rowGuideAmmonia).setOnClickListener(v ->
                showGuide(R.string.guide_title_ammonia, R.string.guide_ammonia));
        view.findViewById(R.id.rowGuideDo2).setOnClickListener(v ->
                showGuide(R.string.guide_title_do2, R.string.guide_do2));

        view.findViewById(R.id.btnCalibratePh).setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            prefs.updateField(SettingsRepository.KEY_CALIB_PH, now);
            currentSettings.lastCalibratedPh = now;
            refreshStatus(view, currentSettings);
        });
        view.findViewById(R.id.btnCalibrateTemp).setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            prefs.updateField(SettingsRepository.KEY_CALIB_TEMP, now);
            currentSettings.lastCalibratedTemp = now;
            refreshStatus(view, currentSettings);
        });
        view.findViewById(R.id.btnCalibrateSalinity).setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            prefs.updateField(SettingsRepository.KEY_CALIB_SALINITY, now);
            currentSettings.lastCalibratedSalinity = now;
            refreshStatus(view, currentSettings);
        });
        view.findViewById(R.id.btnCalibrateAmmonia).setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            prefs.updateField(SettingsRepository.KEY_CALIB_AMMONIA, now);
            currentSettings.lastCalibratedAmmonia = now;
            refreshStatus(view, currentSettings);
        });
        view.findViewById(R.id.btnCalibrateDo2).setOnClickListener(v -> {
            long now = System.currentTimeMillis();
            prefs.updateField(SettingsRepository.KEY_CALIB_DO2, now);
            currentSettings.lastCalibratedDissolvedO2 = now;
            refreshStatus(view, currentSettings);
        });
    }

    private void refreshStatus(View root, AppSettings s) {
        updateProbeRow(root,
                R.id.tvPhStatus, R.id.tvPhDays, s.lastCalibratedPh);
        updateProbeRow(root,
                R.id.tvTempStatus, R.id.tvTempDays, s.lastCalibratedTemp);
        updateProbeRow(root,
                R.id.tvSalinityStatus, R.id.tvSalinityDays, s.lastCalibratedSalinity);
        updateProbeRow(root,
                R.id.tvAmmoniaStatus, R.id.tvAmmoniaDays, s.lastCalibratedAmmonia);
        updateProbeRow(root,
                R.id.tvDo2Status, R.id.tvDo2Days, s.lastCalibratedDissolvedO2);
    }

    private void updateProbeRow(View root, int statusId, int daysId, long lastMs) {
        TextView tvStatus = root.findViewById(statusId);
        TextView tvDays = root.findViewById(daysId);

        if (lastMs == 0L) {
            tvStatus.setText(R.string.never);
            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_secondary));
            tvDays.setText(R.string.never_calibrated);
            return;
        }

        long days = (System.currentTimeMillis() - lastMs) / MS_PER_DAY;
        if (days == 0) {
            tvDays.setText(R.string.calib_today);
        } else if (days == 1) {
            tvDays.setText(getString(R.string.calib_day_ago, days));
        } else {
            tvDays.setText(getString(R.string.calib_days_ago, days));
        }

        if (days < 30) {
            tvStatus.setText(R.string.calib_status_good);
            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.plan_badge_text));
        } else if (days < 90) {
            tvStatus.setText(R.string.calib_status_overdue);
            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.warning));
        } else {
            tvStatus.setText(R.string.calib_status_replace);
            tvStatus.setTextColor(ContextCompat.getColor(requireContext(), R.color.danger));
        }
    }

    private void showGuide(int titleRes, int guideRes) {
        new AlertDialog.Builder(requireContext())
                .setTitle(titleRes)
                .setMessage(guideRes)
                .setPositiveButton(R.string.got_it, null)
                .show();
    }
}
