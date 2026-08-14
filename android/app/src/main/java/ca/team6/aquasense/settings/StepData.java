package ca.team6.aquasense.settings;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.annotation.DrawableRes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class StepData implements Parcelable {
    public final String instruction;
    @DrawableRes public final int imageResId;
    public final ArrayList<String> checklist;
    public final ArrayList<String> inputHints;

    // State preservation
    public final boolean[] checklistStates;
    public final String[] inputValues;

    // Branching logic
    public final String choiceQuestion;
    public int selectedChoice = -1; // -1 = none, 0 = Yes, 1 = No

    /**
     * The sensor this step measures, which makes it a calibration point: the user names a value they
     * have put the probe in, the guide samples the sensor while it is there, and the mean is left in
     * {@link #capturedValue}. Null on every step that is only instructions.
     */
    public final String sensorId;

    /**
     * The sensor's safe band for this aquarium, in the unit the database stores.
     *
     * <p>Carried on the step because it is what the operating point the user types is checked
     * against: a correction is exact where it was measured and drifts either side of it, so one
     * taken outside the range the tank is meant to hold is accurate in a place it will never sit.
     * {@link Double#NaN} on steps that are not calibration points.
     */
    public final double safeLow;
    public final double safeHigh;

    /**
     * The mean of what the board reported for {@link #sensorId} across the sampling window, raw and
     * in the unit the database stores. {@link Double#NaN} until the window has run, and also when
     * the sensor never reported during it - there is nothing to measure an offset from in that case.
     */
    public double capturedValue = Double.NaN;

    public StepData(String instruction, @DrawableRes int imageResId, List<String> checklist) {
        this(instruction, imageResId, checklist, new ArrayList<>(), null, null,
                Double.NaN, Double.NaN);
    }

    public StepData(String instruction, @DrawableRes int imageResId, List<String> checklist, List<String> inputHints) {
        this(instruction, imageResId, checklist, inputHints, null, null, Double.NaN, Double.NaN);
    }

    public StepData(String instruction, @DrawableRes int imageResId, List<String> checklist, List<String> inputHints, String choiceQuestion) {
        this(instruction, imageResId, checklist, inputHints, choiceQuestion, null,
                Double.NaN, Double.NaN);
    }

    /**
     * The calibration point of a guide: the user types the value they have brought the water to,
     * which has to land inside {@code [safeLow, safeHigh]}, and the guide samples the sensor there.
     */
    public static StepData calibrationPoint(String instruction, @DrawableRes int imageResId,
                                            List<String> checklist, String inputHint,
                                            String sensorId, double safeLow, double safeHigh) {
        return new StepData(instruction, imageResId, checklist,
                Collections.singletonList(inputHint), null, sensorId, safeLow, safeHigh);
    }

    public StepData(String instruction, @DrawableRes int imageResId, List<String> checklist,
                    List<String> inputHints, String choiceQuestion, String sensorId,
                    double safeLow, double safeHigh) {
        this.instruction = instruction;
        this.imageResId = imageResId;
        this.checklist = new ArrayList<>(checklist);
        this.inputHints = new ArrayList<>(inputHints);
        this.choiceQuestion = choiceQuestion;
        this.sensorId = sensorId;
        this.safeLow = safeLow;
        this.safeHigh = safeHigh;

        this.checklistStates = new boolean[this.checklist.size()];
        this.inputValues = new String[this.inputHints.size()];
        for (int i = 0; i < this.inputValues.length; i++) {
            this.inputValues[i] = "";
        }
    }

    /** Whether this step is one the calibration reads an offset out of. */
    public boolean isCalibrationPoint() {
        return this.sensorId != null;
    }

    /**
     * What the user typed as the operating point, trimmed, or an empty string when they have not
     * typed anything. Unparsed - what counts as a usable value depends on the display unit, which
     * is the guide's business rather than this holder's.
     */
    public String typedOperatingPoint() {
        return this.inputValues.length == 0 || this.inputValues[0] == null
                ? ""
                : this.inputValues[0].trim();
    }

    protected StepData(Parcel in) {
        instruction = in.readString();
        imageResId = in.readInt();
        checklist = in.createStringArrayList();
        inputHints = in.createStringArrayList();
        checklistStates = in.createBooleanArray();
        inputValues = in.createStringArray();
        choiceQuestion = in.readString();
        selectedChoice = in.readInt();
        sensorId = in.readString();
        safeLow = in.readDouble();
        safeHigh = in.readDouble();
        capturedValue = in.readDouble();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(instruction);
        dest.writeInt(imageResId);
        dest.writeStringList(checklist);
        dest.writeStringList(inputHints);
        dest.writeBooleanArray(checklistStates);
        dest.writeStringArray(inputValues);
        dest.writeString(choiceQuestion);
        dest.writeInt(selectedChoice);
        dest.writeString(sensorId);
        dest.writeDouble(safeLow);
        dest.writeDouble(safeHigh);
        dest.writeDouble(capturedValue);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<StepData> CREATOR = new Creator<StepData>() {
        @Override
        public StepData createFromParcel(Parcel in) {
            return new StepData(in);
        }

        @Override
        public StepData[] newArray(int size) {
            return new StepData[size];
        }
    };
}
