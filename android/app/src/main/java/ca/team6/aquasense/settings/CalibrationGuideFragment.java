package ca.team6.aquasense.settings;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import android.content.Context;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.activity.OnBackPressedCallback;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.Aquarium;
import ca.team6.aquasense.model.AquariumRepository;
import ca.team6.aquasense.model.CalibrationMath;
import ca.team6.aquasense.model.DatabaseSchema;
import ca.team6.aquasense.model.ReadingFormatter;
import ca.team6.aquasense.model.SensorReading;
import ca.team6.aquasense.model.TelemetryRepository;
import ca.team6.aquasense.model.aquarium_sensors.AquariumSensor;

public class CalibrationGuideFragment extends Fragment {

    public static final String RESULT_KEY = "calibration_guide_result";
    public static final String RESULT_SENSOR_ID = "sensorId";
    public static final String RESULT_STEPS = "steps";

    interface LiveReadingSource {
        double rawValue(@NonNull String sensorId);

        @Nullable
        String displayText(@NonNull String sensorId);
    }

    private static final String ARG_TITLE = "title";
    private static final String ARG_NOTE = "note";
    private static final String ARG_STEPS = "steps";
    private static final String ARG_SENSOR_ID = "sensorId";

    private static final long STALENESS_CHECK_INTERVAL_MS = 5_000;

    public static final long SAMPLE_DURATION_MS = 60_000L;

    private static final long SAMPLE_TICK_MS = 1_000L;

    private ArrayList<StepData> steps;
    private String sensorId;
    private Button btnDone;
    private Button btnBack;
    private int maxReachedPosition = 0;

    private TelemetryRepository telemetryRepository;
    private ViewPager2 vpGuide;

    private final List<Double> stepSamples = new ArrayList<>();
    private final Set<Long> sampledTimestamps = new HashSet<>();

    private boolean sampling;
    private long samplingEndsAt;

    private final Handler stalenessHandler = new Handler(Looper.getMainLooper());
    private final Runnable stalenessTick = new Runnable() {
        @Override
        public void run() {
            showLiveReadings();
            stalenessHandler.postDelayed(this, STALENESS_CHECK_INTERVAL_MS);
        }
    };

    private final Handler samplingHandler = new Handler(Looper.getMainLooper());
    private final Runnable samplingTick = new Runnable() {
        @Override
        public void run() {
            collectSample();
            if (SystemClock.elapsedRealtime() >= samplingEndsAt) {
                finishSampling();
                return;
            }
            showLiveReadings();
            samplingHandler.postDelayed(this, SAMPLE_TICK_MS);
        }
    };

    private final TelemetryRepository.TelemetryObserver telemetryObserver =
            readings -> showLiveReadings();

    private final LiveReadingSource liveReadingSource = new LiveReadingSource() {
        @Override
        public double rawValue(@NonNull String sensorId) {
            return currentRawValue(sensorId);
        }

        @Override
        @Nullable
        public String displayText(@NonNull String sensorId) {
            return currentDisplayText(sensorId);
        }
    };

    public static Bundle argsFor(String title, String note, ArrayList<StepData> steps,
                                 String sensorId) {
        Bundle args = new Bundle();
        args.putString(ARG_TITLE, title);
        args.putString(ARG_NOTE, note);
        args.putParcelableArrayList(ARG_STEPS, steps);
        args.putString(ARG_SENSOR_ID, sensorId);
        return args;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_calibration_guide, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = requireArguments();
        String title = args.getString(ARG_TITLE);
        String note = args.getString(ARG_NOTE);
        steps = args.getParcelableArrayList(ARG_STEPS);
        sensorId = args.getString(ARG_SENSOR_ID);
        if (steps == null) {
            steps = new ArrayList<>();
        }

        TextView tvTitle = view.findViewById(R.id.tvGuideTitle);
        TextView tvNote = view.findViewById(R.id.tvGuideNote);
        vpGuide = view.findViewById(R.id.vpGuide);
        LinearLayout layoutDots = view.findViewById(R.id.layoutDots);
        btnDone = view.findViewById(R.id.btnDone);
        btnBack = view.findViewById(R.id.btnBack);

        tvTitle.setText(title);
        if (note != null && !note.isEmpty()) {
            tvNote.setText(note);
            tvNote.setVisibility(View.VISIBLE);
        }

        telemetryRepository = TelemetryRepository.getInstance();
        Aquarium active = AquariumRepository.getInstance(requireContext()).getActiveAquarium();
        if (active != null) {
            telemetryRepository.watchAquarium(active.getId());
        }
        GuideAdapter adapter = new GuideAdapter(steps, this::validateCurrentPage, liveReadingSource);
        vpGuide.setAdapter(adapter);
        vpGuide.setUserInputEnabled(true);
        setupDots(layoutDots, steps.size());

        telemetryRepository.addObserver(telemetryObserver);
        stalenessHandler.post(stalenessTick);

        vpGuide.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                if (position > maxReachedPosition) {
                    vpGuide.setCurrentItem(maxReachedPosition, false);
                    return;
                }

                resetSamples();
                updateDots(layoutDots, position);
                validateCurrentPage(vpGuide);

                if (btnBack != null) {
                    btnBack.setVisibility(position > 0 ? View.VISIBLE : View.INVISIBLE);
                }

                updatePrimaryButtonText(position);
            }
        });

        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                int current = vpGuide.getCurrentItem();
                if (current > 0) {
                    vpGuide.setCurrentItem(current - 1);
                }
            });
        }

        btnDone.setOnClickListener(v -> {
            if (sampling) {
                cancelSampling();
                return;
            }
            int current = vpGuide.getCurrentItem();
            if (steps.get(current).isCalibrationPoint()) {
                startSampling();
                return;
            }
            advanceFrom(current);
        });

        updatePrimaryButtonText(0);

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(),
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        confirmExit();
                    }
                });
    }

    private void confirmExit() {
        new MaterialAlertDialogBuilder(requireContext(),
                R.style.ThemeOverlay_AquaSense_MaterialAlertDialog_Danger)
                .setTitle(R.string.calib_exit_title)
                .setMessage(R.string.calib_exit_message)
                .setPositiveButton(R.string.calib_exit_confirm, (dialog, which) -> leave())
                .setNegativeButton(R.string.calib_exit_cancel, null)
                .show();
    }

    private void leave() {
        cancelSampling();
        NavHostFragment.findNavController(this).popBackStack();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stalenessHandler.removeCallbacks(stalenessTick);
        samplingHandler.removeCallbacks(samplingTick);
        sampling = false;
        if (telemetryRepository != null) {
            telemetryRepository.removeObserver(telemetryObserver);
        }
        vpGuide = null;
    }

    private void updatePrimaryButtonText(int position) {
        if (steps == null || position < 0 || position >= steps.size()) {
            return;
        }
        if (steps.get(position).isCalibrationPoint()) {
            btnDone.setText(R.string.calibration_start);
        } else if (position == steps.size() - 1) {
            btnDone.setText(R.string.action_done);
        } else {
            btnDone.setText(R.string.action_next);
        }
    }

    private void advanceFrom(int current) {
        if (current < steps.size() - 1) {
            maxReachedPosition = Math.max(maxReachedPosition, current + 1);
            vpGuide.setCurrentItem(current + 1);
            return;
        }
        Bundle result = new Bundle();
        result.putString(RESULT_SENSOR_ID, sensorId);
        result.putParcelableArrayList(RESULT_STEPS, steps);
        getParentFragmentManager().setFragmentResult(RESULT_KEY, result);
        NavHostFragment.findNavController(this).popBackStack();
    }

    private void startSampling() {
        resetSamples();
        sampling = true;
        samplingEndsAt = SystemClock.elapsedRealtime() + SAMPLE_DURATION_MS;

        btnDone.setText(R.string.action_cancel);
        btnDone.setEnabled(true);
        btnDone.setAlpha(1f);
        tintPrimaryButton(R.color.danger);
        vpGuide.setUserInputEnabled(false);
        if (btnBack != null) {
            btnBack.setEnabled(false);
        }

        collectSample();
        showLiveReadings();
        samplingHandler.postDelayed(samplingTick, SAMPLE_TICK_MS);
    }

    private void cancelSampling() {
        if (!sampling) {
            return;
        }
        closeSamplingWindow();
        resetSamples();
        showLiveReadings();
    }

    private void closeSamplingWindow() {
        sampling = false;
        samplingHandler.removeCallbacks(samplingTick);
        samplingEndsAt = 0L;

        if (vpGuide != null) {
            vpGuide.setUserInputEnabled(true);
            updatePrimaryButtonText(vpGuide.getCurrentItem());
        }
        if (btnBack != null) {
            btnBack.setEnabled(true);
        }
        btnDone.setEnabled(true);
        btnDone.setAlpha(1f);
        tintPrimaryButton(R.color.accent);
    }

    private void tintPrimaryButton(@ColorRes int colorResId) {
        btnDone.setBackgroundTintList(ColorStateList.valueOf(
                ContextCompat.getColor(btnDone.getContext(), colorResId)));
    }

    private void finishSampling() {
        closeSamplingWindow();
        if (vpGuide == null) {
            return;
        }

        int current = vpGuide.getCurrentItem();
        StepData step = steps.get(current);
        if (stepSamples.isEmpty()) {
            showLiveReadings();
            if (getContext() != null) {
                Toast.makeText(requireContext(), R.string.calibration_no_samples,
                        Toast.LENGTH_LONG).show();
            }
            return;
        }

        step.capturedValue = CalibrationMath.average(stepSamples);
        advanceFrom(current);
    }

    private void collectSample() {
        if (!sampling || vpGuide == null || steps == null) {
            return;
        }
        int current = vpGuide.getCurrentItem();
        if (current < 0 || current >= steps.size()) {
            return;
        }
        StepData step = steps.get(current);
        if (!step.isCalibrationPoint()) {
            return;
        }
        SensorReading reading = freshRawReading(step.sensorId);
        if (reading != null && sampledTimestamps.add(reading.getTimestampSeconds())) {
            stepSamples.add(reading.getValue());
        }
    }

    private void resetSamples() {
        stepSamples.clear();
        sampledTimestamps.clear();
    }

    @Nullable
    private SensorReading freshRawReading(@NonNull String sensorId) {
        if (telemetryRepository == null) {
            return null;
        }
        SensorReading reading = telemetryRepository.getRawReading(sensorId);
        if (reading == null || reading.isOffline()) {
            return null;
        }
        long ageSeconds = reading.ageSeconds(telemetryRepository.nowMillis() / 1000L);
        if (Math.abs(ageSeconds) > AquariumSensor.STALE_THRESHOLD_SECONDS) {
            return null;
        }
        return reading;
    }

    private double currentRawValue(@NonNull String sensorId) {
        SensorReading reading = freshRawReading(sensorId);
        return reading == null ? Double.NaN : reading.getValue();
    }

    @Nullable
    private String currentDisplayText(@NonNull String sensorId) {
        if (telemetryRepository == null || getContext() == null) {
            return null;
        }
        if (freshRawReading(sensorId) == null) {
            return null;
        }
        Map<String, SensorReading> corrected = telemetryRepository.getReadings();
        SensorReading reading = corrected.get(sensorId);
        if (reading == null) {
            return null;
        }
        return formatReading(requireContext(), sensorId, reading.getValue());
    }

    static String formatReading(@NonNull Context context, @NonNull String sensorId,
                                double databaseValue) {
        String unit = unitFor(context, sensorId);
        String number = ReadingFormatter.format(context, sensorId, databaseValue);
        return unit.isEmpty() ? number : number + " " + unit;
    }

    static String unitFor(@NonNull Context context, @NonNull String sensorId) {
        return context.getString(ReadingFormatter.unitResIdFor(
                sensorId, ReadingFormatter.isFahrenheit(context))).trim();
    }

    static String safeRangeText(@NonNull Context context, @NonNull StepData step) {
        String unit = unitFor(context, step.sensorId);
        String range = ReadingFormatter.format(context, step.sensorId, step.safeLow)
                + " – " + ReadingFormatter.format(context, step.sensorId, step.safeHigh);
        return unit.isEmpty() ? range : range + " " + unit;
    }

    static double operatingPointOf(@NonNull Context context, @NonNull StepData step) {
        double typed = CalibrationMath.parseOperatingPoint(step.typedOperatingPoint());
        if (Double.isNaN(typed)) {
            return Double.NaN;
        }
        double database = DatabaseSchema.TEMPERATURE_KEY.equals(step.sensorId)
                ? ReadingFormatter.fromDisplayTemperature(typed,
                        ReadingFormatter.isFahrenheit(context))
                : typed;
        return CalibrationMath.isWithinSafeRange(database, step.safeLow, step.safeHigh)
                ? database
                : Double.NaN;
    }

    @Nullable
    static String operatingPointError(@NonNull Context context, @NonNull StepData step) {
        String raw = step.typedOperatingPoint();
        if (raw.isEmpty()) {
            return null;
        }
        if (Double.isNaN(CalibrationMath.parseOperatingPoint(raw))) {
            return context.getString(R.string.calib_operating_point_precision);
        }
        return Double.isNaN(operatingPointOf(context, step))
                ? context.getString(R.string.calib_operating_point_out_of_range,
                        safeRangeText(context, step))
                : null;
    }

    private void showLiveReadings() {
        collectSample();
        if (vpGuide == null || !(vpGuide.getChildAt(0) instanceof RecyclerView)) {
            return;
        }

        long remainingMs = Math.max(0L, samplingEndsAt - SystemClock.elapsedRealtime());
        int secondsLeft = (int) Math.ceil(remainingMs / 1000d);
        int percent = (int) (100L * (SAMPLE_DURATION_MS - remainingMs) / SAMPLE_DURATION_MS);

        RecyclerView rv = (RecyclerView) vpGuide.getChildAt(0);
        for (int i = 0; i < rv.getChildCount(); i++) {
            RecyclerView.ViewHolder holder = rv.getChildViewHolder(rv.getChildAt(i));
            if (holder instanceof GuideAdapter.ViewHolder) {
                GuideAdapter.ViewHolder guideHolder = (GuideAdapter.ViewHolder) holder;
                guideHolder.showLiveReading();
                guideHolder.showSampling(sampling, stepSamples.size(), secondsLeft, percent);
            }
        }
        validateCurrentPage(vpGuide);
    }

    private void validateCurrentPage(ViewPager2 vp) {
        if (sampling) {
            return;
        }
        int current = vp.getCurrentItem();

        RecyclerView rv = (RecyclerView) vp.getChildAt(0);
        RecyclerView.ViewHolder holder = rv.findViewHolderForAdapterPosition(current);
        if (holder instanceof GuideAdapter.ViewHolder) {
            boolean isValid = ((GuideAdapter.ViewHolder) holder).isPageValid();
            btnDone.setEnabled(isValid);
            btnDone.setAlpha(isValid ? 1.0f : 0.5f);

            if (isValid) {
                maxReachedPosition = Math.max(maxReachedPosition, current + 1);
            } else {
                maxReachedPosition = current;
            }
        }
    }

    private void setupDots(LinearLayout layout, int count) {
        layout.removeAllViews();
        for (int i = 0; i < count; i++) {
            ImageView dot = new ImageView(getContext());
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMargins(8, 0, 8, 0);
            dot.setLayoutParams(params);
            dot.setImageResource(R.drawable.ic_step_dot);
            dot.setSelected(i == 0);
            layout.addView(dot);
        }
    }

    private void updateDots(LinearLayout layout, int position) {
        for (int i = 0; i < layout.getChildCount(); i++) {
            layout.getChildAt(i).setSelected(i == position);
        }
    }

    private static class GuideAdapter extends RecyclerView.Adapter<GuideAdapter.ViewHolder> {
        private final List<StepData> steps;
        private final ValidationCallback callback;
        private final LiveReadingSource liveReadingSource;

        interface ValidationCallback {
            void onValidationChanged(ViewPager2 vp);
        }

        GuideAdapter(List<StepData> steps, ValidationCallback callback,
                     LiveReadingSource liveReadingSource) {
            this.steps = steps;
            this.callback = callback;
            this.liveReadingSource = liveReadingSource;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_calibration_step, parent, false),
                    callback, liveReadingSource);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(steps.get(position));
        }

        @Override
        public int getItemCount() {
            return steps.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            final TextView tvStepText;
            final ImageView ivIllustration;
            final LinearLayout layoutChecklist;
            final LinearLayout layoutInputs;
            final LinearLayout layoutChoice;
            final LinearLayout layoutLiveReading;
            final LinearLayout layoutOperatingPoint;
            final LinearLayout layoutSampling;
            final ProgressBar calibrationProgress;
            final TextView tvCalibrationProgress;
            final TextView tvSafeRange;
            final TextView tvChoiceQuestion;
            final TextView tvLiveReadingValue;
            final RadioGroup rgChoice;
            final ValidationCallback callback;
            final LiveReadingSource liveReadingSource;
            final List<CheckBox> checkBoxes = new ArrayList<>();
            final List<EditText> editTexts = new ArrayList<>();
            @Nullable
            TextInputLayout operatingPointInput;
            StepData boundData;

            ViewHolder(View view, ValidationCallback callback, LiveReadingSource liveReadingSource) {
                super(view);
                this.callback = callback;
                this.liveReadingSource = liveReadingSource;
                tvStepText = view.findViewById(R.id.tvStepText);
                ivIllustration = view.findViewById(R.id.ivStepIllustration);
                layoutChecklist = view.findViewById(R.id.layoutChecklist);
                layoutInputs = view.findViewById(R.id.layoutInputs);
                layoutChoice = view.findViewById(R.id.layoutChoice);
                layoutLiveReading = view.findViewById(R.id.layoutLiveReading);
                layoutOperatingPoint = view.findViewById(R.id.layoutOperatingPoint);
                layoutSampling = view.findViewById(R.id.layoutSampling);
                calibrationProgress = view.findViewById(R.id.calibrationProgress);
                tvCalibrationProgress = view.findViewById(R.id.tvCalibrationProgress);
                tvSafeRange = view.findViewById(R.id.tvSafeRange);
                tvChoiceQuestion = view.findViewById(R.id.tvChoiceQuestion);
                tvLiveReadingValue = view.findViewById(R.id.tvLiveReadingValue);
                rgChoice = view.findViewById(R.id.rgChoice);
            }

            void bind(StepData data) {
                this.boundData = data;
                this.operatingPointInput = null;
                tvStepText.setText(data.instruction);
                if (data.imageResId != 0) {
                    ivIllustration.setImageResource(data.imageResId);
                    ivIllustration.setVisibility(View.VISIBLE);
                } else {
                    ivIllustration.setVisibility(View.GONE);
                }

                layoutChecklist.removeAllViews();
                checkBoxes.clear();
                for (int i = 0; i < data.checklist.size(); i++) {
                    final int index = i;
                    View row = LayoutInflater.from(itemView.getContext())
                            .inflate(R.layout.item_calibration_checklist_row, layoutChecklist, false);
                    CheckBox cb = row.findViewById(R.id.checkBox);
                    TextView tv = row.findViewById(R.id.tvCheckLabel);
                    tv.setText(data.checklist.get(i));
                    cb.setChecked(data.checklistStates[i]);
                    cb.setOnCheckedChangeListener((b, isChecked) -> {
                        data.checklistStates[index] = isChecked;
                        triggerValidation();
                    });
                    checkBoxes.add(cb);
                    layoutChecklist.addView(row);
                }

                if (data.choiceQuestion != null) {
                    layoutChoice.setVisibility(View.VISIBLE);
                    tvChoiceQuestion.setText(data.choiceQuestion);
                    rgChoice.setOnCheckedChangeListener(null);
                    if (data.selectedChoice == 0) rgChoice.check(R.id.rbYes);
                    else if (data.selectedChoice == 1) rgChoice.check(R.id.rbNo);
                    else rgChoice.clearCheck();

                    rgChoice.setOnCheckedChangeListener((group, checkedId) -> {
                        if (checkedId == R.id.rbYes) data.selectedChoice = 0;
                        else if (checkedId == R.id.rbNo) data.selectedChoice = 1;
                        toggleInputVisibility(data);
                        triggerValidation();
                    });
                } else {
                    layoutChoice.setVisibility(View.GONE);
                }

                layoutInputs.removeAllViews();
                editTexts.clear();
                for (int i = 0; i < data.inputHints.size(); i++) {
                    final int index = i;
                    TextInputLayout til = (TextInputLayout) LayoutInflater.from(itemView.getContext())
                            .inflate(R.layout.item_calibration_input, layoutInputs, false);
                    til.setHint(data.inputHints.get(i));
                    EditText et = til.findViewById(R.id.etCalibrationInput);
                    et.setText(data.inputValues[i]);
                    et.addTextChangedListener(new TextWatcher() {
                        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                        @Override public void afterTextChanged(Editable s) {
                            data.inputValues[index] = s.toString();
                            showOperatingPointError();
                            triggerValidation();
                        }
                    });
                    editTexts.add(et);
                    layoutInputs.addView(til);
                    if (index == 0 && data.isCalibrationPoint()) {
                        operatingPointInput = til;
                    }
                }

                toggleInputVisibility(data);
                showSafeRange();
                showOperatingPointError();
                showLiveReading();
                showSampling(false, 0, 0, 0);
            }

            void showSafeRange() {
                if (boundData == null || !boundData.isCalibrationPoint()
                        || Double.isNaN(boundData.safeLow) || Double.isNaN(boundData.safeHigh)) {
                    layoutOperatingPoint.setVisibility(View.GONE);
                    return;
                }
                layoutOperatingPoint.setVisibility(View.VISIBLE);
                tvSafeRange.setText(safeRangeText(itemView.getContext(), boundData));
            }

            void showOperatingPointError() {
                if (operatingPointInput == null || boundData == null) {
                    return;
                }
                operatingPointInput.setError(
                        operatingPointError(itemView.getContext(), boundData));
            }

            void showSampling(boolean sampling, int samples, int secondsLeft, int percent) {
                if (!sampling || boundData == null || !boundData.isCalibrationPoint()) {
                    layoutSampling.setVisibility(View.GONE);
                    calibrationProgress.setProgress(0);
                    if (boundData != null) {
                        toggleInputVisibility(boundData);
                    }
                    return;
                }
                layoutSampling.setVisibility(View.VISIBLE);
                layoutInputs.setVisibility(View.GONE);
                calibrationProgress.setProgress(percent);
                tvCalibrationProgress.setText(itemView.getContext().getString(
                        R.string.calibration_collecting, samples, secondsLeft));
            }

            void showLiveReading() {
                if (boundData == null || !boundData.isCalibrationPoint()) {
                    layoutLiveReading.setVisibility(View.GONE);
                    return;
                }
                layoutLiveReading.setVisibility(View.VISIBLE);

                String text = liveReadingSource.displayText(boundData.sensorId);
                tvLiveReadingValue.setText(text != null
                        ? text
                        : itemView.getContext().getString(R.string.calib_live_reading_none));
            }

            private void toggleInputVisibility(StepData data) {
                if (data.choiceQuestion != null) {
                    layoutInputs.setVisibility(data.selectedChoice == 0 ? View.VISIBLE : View.GONE);
                } else {
                    layoutInputs.setVisibility(data.inputHints.isEmpty() ? View.GONE : View.VISIBLE);
                }
            }

            private void triggerValidation() {
                if (itemView.getParent() instanceof RecyclerView) {
                    RecyclerView rv = (RecyclerView) itemView.getParent();
                    if (rv.getParent() instanceof ViewPager2) {
                        callback.onValidationChanged((ViewPager2) rv.getParent());
                    }
                }
            }

            boolean isPageValid() {
                for (CheckBox cb : checkBoxes) {
                    if (!cb.isChecked()) return false;
                }

                if (boundData.isCalibrationPoint()) {
                    return !Double.isNaN(operatingPointOf(itemView.getContext(), boundData))
                            && hasReadingToCompareAgainst();
                }

                if (boundData.choiceQuestion != null) {
                    if (boundData.selectedChoice == -1) return false;
                    if (boundData.selectedChoice == 0) {
                        return inputsFilled();
                    }
                    return true;
                }

                return inputsFilled();
            }

            private boolean hasReadingToCompareAgainst() {
                return !Double.isNaN(liveReadingSource.rawValue(boundData.sensorId));
            }

            private boolean inputsFilled() {
                for (EditText et : editTexts) {
                    if (et.getText().toString().trim().isEmpty()) return false;
                }
                return true;
            }
        }
    }
}
