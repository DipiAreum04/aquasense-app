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

    public final boolean[] checklistStates;
    public final String[] inputValues;

    public final String choiceQuestion;
    public int selectedChoice = -1;

    public final String sensorId;

    public final double safeLow;
    public final double safeHigh;

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

    public boolean isCalibrationPoint() {
        return this.sensorId != null;
    }

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
