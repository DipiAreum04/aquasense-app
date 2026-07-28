package ca.team6.aquasense.ui;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import ca.team6.aquasense.R;

/**
 * Outlines the container of an input field to reflect whether its value passes validation.
 * 
 * A field is never marked invalid while the user is still typing into it.
 * Once they leave the field or press submit, the invalid fields will be outlined in red.
 * But a field that is already red-outlined will clear the moment their input passes validation.
 * 
 * For password confirmation fields, a green outline is shown while the user is typing to indicate
 * that the password matches or password rules are being met.
 */

public final class InputFieldError {

    public enum State {
        NEUTRAL, // No outline: untouched, or empty and not yet submitted
        ERROR, // Red outline: failed validation on submit or clicking away from the field
        VALID, // Green outline: passed validation while typing
    }

    // Returns true when the field's current text meets the validation rules
    public interface Check {
        boolean isValid();
    }

    private InputFieldError() {}

    // Applies an outline to an input container based on the state
    public static void set(@NonNull View box, @NonNull State state) {
        set(box, state, null);
    }

    // As above, and shows the tick only while the field is VALID (password confirmation fields only)
    public static void set(@NonNull View box, @NonNull State state, @Nullable View tick) {
        if (tick != null) {
            tick.setVisibility(state == State.VALID ? View.VISIBLE : View.GONE);
        }
        if (stateOf(box) == state) {
            return;
        }

        // setBackgroundResource overwrites the view's padding with the drawable's, so the
        // padding set in the layout has to be captured and restored around the swap.
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

    /**
     * Wires up live feedback for a field: validates when the user clicks away from the field, 
     * and clears a red outline as soon as the value becomes valid while typing.
     */
    public static void track(@NonNull EditText field,
                             @NonNull View box,
                             @NonNull Check check) {
        track(field, box, check, State.NEUTRAL, null);
    }

    // Marks a passing field green and reveals the tick beside it (password confirmation fields only)
    public static void track(@NonNull EditText field,
                             @NonNull View box,
                             @NonNull Check check,
                             @Nullable View tick) {
        track(field, box, check, State.VALID, tick);
    }

    /**
     * Watches the field for changes in its value and updates the outline based on the validation rules.
     */
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
            // Leaving a field the user never filled in is not an error yet
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

    /**
     * A pair of fields the server rejected together, such as a login email and password.
     *
     * <p>Both are outlined without indicating which was wrong since distinguishing "no such
     * account" from "wrong password" would let someone determine which email addresses are
     * registered by watching which box lights up. The outlines clear on the first edit to either field.
     */
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

        // Outlines both fields after the server refuses the combination.
        public void show() {
            showing = true;
            set(firstBox, State.ERROR);
            set(secondBox, State.ERROR);
        }

        // Resets the rejection so at the next submit, the outlines are determined by the new validation
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
