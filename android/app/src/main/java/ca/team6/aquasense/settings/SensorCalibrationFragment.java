package ca.team6.aquasense.settings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ca.team6.aquasense.R;
import ca.team6.aquasense.settings.AppSettings;
import ca.team6.aquasense.aquarium.Aquarium;
import ca.team6.aquasense.aquarium.AquariumRepository;
import ca.team6.aquasense.settings.CalibrationMath;
import ca.team6.aquasense.settings.CalibrationOffsetStore;
import ca.team6.aquasense.firebase.DatabaseSchema;
import ca.team6.aquasense.settings.SettingsRepository;
import ca.team6.aquasense.settings.SharedPreferenceHelper;
import ca.team6.aquasense.aquarium.TelemetryRepository;
import ca.team6.aquasense.aquarium.ThresholdBand;

public class SensorCalibrationFragment extends Fragment {

    private SettingsRepository repo;
    private SharedPreferenceHelper prefs;
    private AppSettings currentSettings;

    private AquariumRepository aquariumRepository;
    private CalibrationOffsetStore offsets;
    @Nullable
    private Aquarium activeAquarium;
    @Nullable
    private View root;

    private final AquariumRepository.AquariumsObserver aquariumsObserver = ignored -> {
        activeAquarium = aquariumRepository.getActiveAquarium();
        if (root != null) {
            refreshStatus(root);
        }
    };

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
        root = view;
        repo = new SettingsRepository(requireContext());
        prefs = SharedPreferenceHelper.getInstance(requireContext());
        offsets = new CalibrationOffsetStore(requireContext());
        aquariumRepository = AquariumRepository.getInstance(requireContext());
        activeAquarium = aquariumRepository.getActiveAquarium();
        aquariumRepository.addObserver(aquariumsObserver);

        repo.loadSettings(s -> currentSettings = s);
        refreshStatus(view);

        getParentFragmentManager().setFragmentResultListener(
                CalibrationGuideFragment.RESULT_KEY, getViewLifecycleOwner(),
                (requestKey, result) -> {
                    String sensorId = result.getString(CalibrationGuideFragment.RESULT_SENSOR_ID);
                    ArrayList<StepData> completed =
                            result.getParcelableArrayList(CalibrationGuideFragment.RESULT_STEPS);
                    if (sensorId != null && completed != null) {
                        onCalibrated(calibrationKeyFor(sensorId), sensorId, completed);
                    }
                });

        view.findViewById(R.id.rowGuideTemp)
                .setOnClickListener(v -> startGuide(v, DatabaseSchema.TEMPERATURE_KEY));
        view.findViewById(R.id.rowGuideTds)
                .setOnClickListener(v -> startGuide(v, DatabaseSchema.DISSOLVED_SOLIDS_KEY));
        view.findViewById(R.id.rowGuidePh)
                .setOnClickListener(v -> startGuide(v, DatabaseSchema.PH_LEVEL_KEY));
    }

    @Override
    public void onDestroyView() {
        aquariumRepository.removeObserver(aquariumsObserver);
        root = null;
        super.onDestroyView();
    }

    private void startGuide(@NonNull View anchor, @NonNull String sensorId) {
        Aquarium aquarium = activeAquarium;
        if (aquarium == null) {
            toast(R.string.calibration_no_aquarium);
            return;
        }
        if (!aquarium.isSensorApplicable(sensorId)) {
            toast(R.string.calibration_sensor_unavailable);
            return;
        }
        ThresholdBand band = aquarium.effectiveThresholdFor(sensorId);
        if (band == null) {
            toast(R.string.calibration_thresholds_required);
            return;
        }

        int windowSeconds = (int) (CalibrationGuideFragment.SAMPLE_DURATION_MS / 1000L);
        String unit = CalibrationGuideFragment.unitFor(requireContext(), sensorId);
        String hint = unit.isEmpty()
                ? getString(R.string.calib_operating_point_hint_plain)
                : getString(R.string.calib_operating_point_hint, unit);

        ArrayList<StepData> steps = new ArrayList<>();
        steps.add(new StepData(
                getString(R.string.calibration_prepare_environment),
                R.drawable.ic_calib_water,
                Collections.singletonList(getString(R.string.calibration_environment_ready))));
        steps.add(new StepData(
                getString(R.string.calibration_place_sensor),
                placeImageFor(sensorId),
                Arrays.asList(getString(R.string.calibration_sensor_submerged),
                        getString(R.string.calibration_sensor_connected))));
        steps.add(StepData.calibrationPoint(
                getString(R.string.calibration_ready_to_sample, windowSeconds),
                R.drawable.ic_calib_gear,
                Collections.singletonList(getString(R.string.calibration_keep_still)),
                hint,
                sensorId,
                band.getSafeLow(),
                band.getSafeHigh()));

        TelemetryRepository.getInstance().watchAquarium(aquarium.getId());

        Navigation.findNavController(anchor).navigate(
                R.id.action_sensorCalibration_to_calibrationGuide,
                CalibrationGuideFragment.argsFor(
                        getString(titleFor(sensorId)),
                        getString(R.string.calibration_reference_note),
                        steps,
                        sensorId));
    }

    private void onCalibrated(String key, String sensorId, List<StepData> steps) {
        long now = System.currentTimeMillis();
        prefs.updateField(key, now);
        if (currentSettings != null) {
            if (key.equals(SettingsRepository.KEY_CALIB_TEMP)) currentSettings.lastCalibratedTemp = now;
            if (key.equals(SettingsRepository.KEY_CALIB_TDS)) currentSettings.lastCalibratedTds = now;
            if (key.equals(SettingsRepository.KEY_CALIB_PH)) currentSettings.lastCalibratedPh = now;
        }

        saveOffset(sensorId, steps);
        if (root != null) {
            refreshStatus(root);
        }
    }

    private void saveOffset(String sensorId, List<StepData> steps) {
        Aquarium aquarium = activeAquarium;
        if (aquarium == null || !CalibrationOffsetStore.isCalibratable(sensorId)) {
            return;
        }

        for (StepData step : steps) {
            if (!step.isCalibrationPoint() || !sensorId.equals(step.sensorId)) {
                continue;
            }
            double operatingPoint =
                    CalibrationGuideFragment.operatingPointOf(requireContext(), step);
            if (Double.isNaN(step.capturedValue) || Double.isNaN(operatingPoint)) {
                continue;
            }

            offsets.set(aquarium.getId(), sensorId,
                    CalibrationMath.offset(operatingPoint, step.capturedValue));

            TelemetryRepository.getInstance().refreshCalibration();

            String offset = offsets.describe(aquarium.getId(), sensorId);
            Toast.makeText(requireContext(),
                    offset == null
                            ? getString(R.string.calib_offset_cleared)
                            : getString(R.string.calib_offset_saved, offset),
                    Toast.LENGTH_LONG).show();
            return;
        }
    }

    private void refreshStatus(View root) {
        updateProbeRow(root, R.id.tvTempDays, R.id.tvTempWeather,
                DatabaseSchema.TEMPERATURE_KEY);
        updateProbeRow(root, R.id.tvTdsDays, R.id.tvTdsWeather,
                DatabaseSchema.DISSOLVED_SOLIDS_KEY);
        updateProbeRow(root, R.id.tvPhDays, R.id.tvPhWeather, DatabaseSchema.PH_LEVEL_KEY);
    }

    private void updateProbeRow(View root, int lastCalibratedId, int weatherId,
                                String sensorId) {
        TextView tvLastCalibrated = root.findViewById(lastCalibratedId);
        TextView tvWeather = root.findViewById(weatherId);

        Aquarium aquarium = activeAquarium;
        long lastMs = aquarium == null ? 0L : offsets.calibratedAt(aquarium.getId(), sensorId);

        if (lastMs == 0L) {
            tvLastCalibrated.setText(getString(R.string.calib_last_calibrated,
                    getString(R.string.calib_value_none)));
            tvWeather.setText(R.string.calib_weather_moon);
            tvWeather.setContentDescription(
                    getString(R.string.calib_weather_never_accessibility));
            return;
        }

        tvLastCalibrated.setText(getString(R.string.calib_last_calibrated, formatTimestamp(lastMs)));

        ThresholdBand band = aquarium.effectiveThresholdFor(sensorId);
        double percent = band == null ? Double.NaN : CalibrationMath.percentError(
                offsets.get(aquarium.getId(), sensorId),
                band.getSafeLow(), band.getSafeHigh());
        if (Double.isNaN(percent)) {
            tvWeather.setText(R.string.calib_weather_moon);
            tvWeather.setContentDescription(
                    getString(R.string.calib_weather_value_unavailable_accessibility));
            return;
        }

        tvWeather.setText(weatherIconFor(percent));
        tvWeather.setContentDescription(getString(
                R.string.calib_weather_value_accessibility,
                String.format(Locale.getDefault(), "%.1f", percent),
                getString(weatherMeaningFor(percent))));
    }

    @StringRes
    private static int weatherIconFor(double percent) {
        if (percent <= 5d) return R.string.calib_weather_sunny;
        if (percent <= 10d) return R.string.calib_weather_partly_cloudy;
        if (percent <= 25d) return R.string.calib_weather_cloudy;
        if (percent <= 50d) return R.string.calib_weather_rainy;
        return R.string.calib_weather_stormy;
    }

    @StringRes
    private static int weatherMeaningFor(double percent) {
        if (percent <= 5d) return R.string.calib_weather_meaning_very_small;
        if (percent <= 10d) return R.string.calib_weather_meaning_small;
        if (percent <= 25d) return R.string.calib_weather_meaning_moderate;
        if (percent <= 50d) return R.string.calib_weather_meaning_large;
        return R.string.calib_weather_meaning_very_large;
    }

    private static String formatTimestamp(long epochMillis) {
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT,
                Locale.getDefault()).format(new Date(epochMillis));
    }

    private void toast(@StringRes int messageResId) {
        Toast.makeText(requireContext(), messageResId, Toast.LENGTH_SHORT).show();
    }

    @DrawableRes
    private static int placeImageFor(String sensorId) {
        switch (sensorId) {
            case DatabaseSchema.TEMPERATURE_KEY:
                return R.drawable.tempsincup;
            case DatabaseSchema.PH_LEVEL_KEY:
                return R.drawable.ph6;
            default:
                return R.drawable.ic_calib_water;
        }
    }

    @StringRes
    private static int titleFor(String sensorId) {
        switch (sensorId) {
            case DatabaseSchema.TEMPERATURE_KEY:
                return R.string.sensor_info_temperature_title;
            case DatabaseSchema.PH_LEVEL_KEY:
                return R.string.sensor_info_ph_level_title;
            default:
                return R.string.sensor_info_dissolved_solids_title;
        }
    }

    private static String calibrationKeyFor(String sensorId) {
        switch (sensorId) {
            case DatabaseSchema.TEMPERATURE_KEY:
                return SettingsRepository.KEY_CALIB_TEMP;
            case DatabaseSchema.PH_LEVEL_KEY:
                return SettingsRepository.KEY_CALIB_PH;
            default:
                return SettingsRepository.KEY_CALIB_TDS;
        }
    }
}
