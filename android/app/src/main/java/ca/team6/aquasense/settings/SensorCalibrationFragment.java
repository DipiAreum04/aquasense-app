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
import ca.team6.aquasense.model.AppSettings;
import ca.team6.aquasense.model.Aquarium;
import ca.team6.aquasense.model.AquariumRepository;
import ca.team6.aquasense.model.CalibrationMath;
import ca.team6.aquasense.model.CalibrationOffsetStore;
import ca.team6.aquasense.model.DatabaseSchema;
import ca.team6.aquasense.model.SettingsRepository;
import ca.team6.aquasense.model.SharedPreferenceHelper;
import ca.team6.aquasense.model.TelemetryRepository;
import ca.team6.aquasense.model.ThresholdBand;

/**
 * Settings &rarr; Sensor Calibration: one guide per correctable sensor, each of which has the user
 * name the value they have brought the water to, measures the sensor there, and stores the
 * difference.
 *
 * <p>Each row reports the correction in force as a percentage of the sensor's safe band, which is
 * what makes it comparable between sensors: the same absolute offset is noise on a dissolved-solids
 * band hundreds wide and enormous on a pH one.
 */
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

    // An offset belongs to one tank's probe, so the page has to know which tank it is looking at -
    // and the list arrives from Firebase, so it may well not be known yet when the view is created.
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

        // The guide is a page of its own now, so its result comes back through the fragment manager
        // rather than a listener held across a dialog.
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

        // Water level has no row here to wire up: it is a detector rather than a scale, so there
        // is nothing to compare against a reference and nothing to correct. Its row says so.
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

    /**
     * Prepare, place, measure. The first two steps are instructions the user ticks off; the third is
     * the calibration point, where they name the value they brought the water to and the guide
     * takes over and reads the sensor itself.
     */
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
        // Without a band there is nothing to check the typed operating point against, and no way to
        // say how big the resulting correction is. That is a range to go and set, not a guess.
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

        // Opening Settings cold leaves nothing subscribed, and the guide reads live samples off
        // this subscription. The dashboard holds it for the whole session, so arriving from there
        // costs nothing.
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
        // Still recorded: these are the account-wide "last calibrated" fields Settings backs up.
        // What this screen shows now comes from the per-aquarium store, which is written by
        // saveOffset below alongside the offset it belongs to.
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

    /**
     * Stores the correction the guide measured: the operating point the user held the probe at,
     * minus the mean of what the board reported while it was there.
     *
     * <p>The typed value is converted out of the display unit; the samples are raw by construction.
     * A window that caught no samples, or a value that did not survive validation, leaves the stored
     * offset alone rather than overwriting a good correction with one derived from nothing.
     */
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

            // The samples already in hand were published under the old offset, so they are
            // re-corrected rather than left to be overwritten by whatever the board sends next.
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
        updateProbeRow(root, R.id.tvTempDays, R.id.tvTempError, DatabaseSchema.TEMPERATURE_KEY);
        updateProbeRow(root, R.id.tvTdsDays, R.id.tvTdsError, DatabaseSchema.DISSOLVED_SOLIDS_KEY);
        updateProbeRow(root, R.id.tvPhDays, R.id.tvPhError, DatabaseSchema.PH_LEVEL_KEY);
    }

    /**
     * When this aquarium's probe was last calibrated, and how large the correction that left on it
     * is against the sensor's safe band. Both read from the offset store and this aquarium's
     * thresholds, so the row describes one tank's hardware throughout.
     *
     * <p>The subtitle is the timestamp alone. The correction itself is a raw quantity in the
     * sensor's own unit, which says nothing on its own about whether the probe is in good shape -
     * the percentage on the right is the reading of it that means the same thing for every sensor.
     */
    private void updateProbeRow(View root, int lastCalibratedId, int errorId, String sensorId) {
        TextView tvLastCalibrated = root.findViewById(lastCalibratedId);
        TextView tvError = root.findViewById(errorId);

        Aquarium aquarium = activeAquarium;
        long lastMs = aquarium == null ? 0L : offsets.calibratedAt(aquarium.getId(), sensorId);

        if (lastMs == 0L) {
            tvLastCalibrated.setText(getString(R.string.calib_last_calibrated,
                    getString(R.string.calib_value_none)));
            tvError.setText(R.string.calib_value_none);
            return;
        }

        tvLastCalibrated.setText(getString(R.string.calib_last_calibrated, formatTimestamp(lastMs)));

        ThresholdBand band = aquarium.effectiveThresholdFor(sensorId);
        double percent = band == null ? Double.NaN : CalibrationMath.percentError(
                offsets.get(aquarium.getId(), sensorId),
                band.getSafeLow(), band.getSafeHigh());
        if (Double.isNaN(percent)) {
            // No band, or one with no width: the sensor has been calibrated, but there is nothing
            // to measure the correction against, so the column says so rather than guessing.
            tvError.setText(R.string.calib_value_none);
            return;
        }
        tvError.setText(getString(R.string.calib_percent_error,
                String.format(Locale.getDefault(), "%.1f", percent)));
    }

    /** The moment a calibration was run, in the phone's own date and time format. */
    private static String formatTimestamp(long epochMillis) {
        return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT,
                Locale.getDefault()).format(new Date(epochMillis));
    }

    private void toast(@StringRes int messageResId) {
        Toast.makeText(requireContext(), messageResId, Toast.LENGTH_SHORT).show();
    }

    /** Shown while the probe goes in, so the picture matches the thing being handled. */
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
