package ca.team6.aquasense.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ca.team6.aquasense.R;


public final class InputFieldError {

    public enum State {
        NEUTRAL,
        ERROR,
        VALID,
    }

    public interface Check {
        boolean isValid();
    }

    private InputFieldError() {}

    public static void set(@NonNull View box, @NonNull State state) {
        set(box, state, null);
    }

    public static void set(@NonNull View box, @NonNull State state, @Nullable View tick) {
        if (tick != null) {
            tick.setVisibility(state == State.VALID ? View.VISIBLE : View.GONE);
        }
        if (stateOf(box) == state) {
            return;
        }

        int left = box.getPaddingLeft();
        int top = box.getPaddingTop();
        int right = box.getPaddingRight();
        int bottom = box.getPaddingBottom();

        box.setBackgroundResource(backgroundFor(state));

        box.setPadding(left, top, right, bottom);
        box.setTag(R.id.tag_input_error, state);
    }

    private static int backgroundFor(@NonNull State state) {
        switch (state) {
            case ERROR:
                return R.drawable.bg_auth_input_error;
            case VALID:
                return R.drawable.bg_auth_input_valid;
            default:
                return R.drawable.bg_auth_input;
        }
    }

    @NonNull
    public static State stateOf(@NonNull View box) {
        Object tag = box.getTag(R.id.tag_input_error);
        return tag instanceof State ? (State) tag : State.NEUTRAL;
    }

    public static void track(@NonNull EditText field,
                             @NonNull View box,
                             @NonNull Check check) {
        track(field, box, check, State.NEUTRAL, null);
    }

    public static void track(@NonNull EditText field,
                             @NonNull View box,
                             @NonNull Check check,
                             @Nullable View tick) {
        track(field, box, check, State.VALID, tick);
    }

    public static void watch(@NonNull EditText field,
                             @NonNull View box,
                             @NonNull Check check,
                             @Nullable View tick) {
        watch(field, box, check, tick == null ? State.NEUTRAL : State.VALID, tick);
    }

    private static void track(@NonNull EditText field,
                              @NonNull View box,
                              @NonNull Check check,
                              @NonNull State passState,
                              @Nullable View tick) {
        watch(field, box, check, passState, tick);

        field.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                return;
            }
            if (field.getText() == null || field.getText().toString().trim().isEmpty()) {
                set(box, State.NEUTRAL, tick);
                return;
            }
            set(box, check.isValid() ? passState : State.ERROR, tick);
        });
    }

    private static void watch(@NonNull EditText field,
                              @NonNull View box,
                              @NonNull Check check,
                              @NonNull State passState,
                              @Nullable View tick) {
        field.addTextChangedListener(afterEdit(() -> {
            boolean valid = check.isValid();
            if (stateOf(box) == State.ERROR && valid) {
                set(box, passState, tick);
            } else if (stateOf(box) == State.VALID && !valid) {
                set(box, State.NEUTRAL, tick);
            } else if (stateOf(box) == State.NEUTRAL && valid && passState == State.VALID) {
                set(box, State.VALID, tick);
            }
        }));
    }

    private static TextWatcher afterEdit(@NonNull Runnable action) {
        return new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                action.run();
            }
        };
    }

    public static final class Rejection {

        private final View firstBox;
        private final View secondBox;
        private boolean showing;

        private Rejection(@NonNull EditText firstField, @NonNull View firstBox,
                          @NonNull EditText secondField, @NonNull View secondBox) {
            this.firstBox = firstBox;
            this.secondBox = secondBox;
            firstField.addTextChangedListener(afterEdit(this::clearIfShowing));
            secondField.addTextChangedListener(afterEdit(this::clearIfShowing));
        }

        public void show() {
            showing = true;
            set(firstBox, State.ERROR);
            set(secondBox, State.ERROR);
        }

        public void reset() {
            showing = false;
        }

        private void clearIfShowing() {
            if (!showing) {
                return;
            }
            showing = false;
            set(firstBox, State.NEUTRAL);
            set(secondBox, State.NEUTRAL);
        }
    }

    @NonNull
    public static Rejection rejectionFor(@NonNull EditText firstField, @NonNull View firstBox,
                                         @NonNull EditText secondField, @NonNull View secondBox) {
        return new Rejection(firstField, firstBox, secondField, secondBox);
    }
}
