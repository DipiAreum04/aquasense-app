package ca.team6.aquasense.ui;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.ImageViewCompat;

import java.util.ArrayList;
import java.util.List;

import ca.team6.aquasense.R;

/**
 * The Bluetooth status dynamic icon at the top of the pairing screen: a tinted icon, a soft halo and three
 * rings that sweep outwards while the radio is busy.
 */
public class BleStatusOrb extends FrameLayout {

    public enum State {

        IDLE(R.drawable.bluetooth_24px, R.drawable.bg_pairing_orb, R.color.pairing_orb_icon, true, false),

        SCANNING(R.drawable.bluetooth_searching_24px, R.drawable.bg_pairing_orb, R.color.pairing_orb_icon, true, true),

        // A GATT link is open and the credentials are going across
        CONNECTING(R.drawable.bluetooth_connected_24px, R.drawable.bg_pairing_orb, R.color.pairing_orb_icon, true, true),

        // The board has disconnected its Bluetooth to connect to Wi-Fi, so the icon switches to Wi-Fi
        AWAITING(R.drawable.wifi_24px, R.drawable.bg_pairing_orb, R.color.pairing_orb_icon, true, true),

        // The board published telemetry, so the pairing is successful.
        SUCCESS(R.drawable.wifi_24px, R.drawable.bg_pairing_orb_success, R.color.pairing_success_stroke, false, false),

        BLOCKED(R.drawable.bluetooth_disabled_24px, R.drawable.bg_pairing_orb_error, R.color.pairing_error_stroke, false, false);

        @DrawableRes
        private final int iconResId;
        @DrawableRes
        private final int discResId;
        private final int tintResId;
        private final boolean glowing;
        private final boolean sweeping;

        State(@DrawableRes int iconResId,
              @DrawableRes int discResId,
              int tintResId,
              boolean glowing,
              boolean sweeping) {
            this.iconResId = iconResId;
            this.discResId = discResId;
            this.tintResId = tintResId;
            this.glowing = glowing;
            this.sweeping = sweeping;
        }
    }

    // One full sweep duration, from the icon circle's edge out to the faded rim.
    private static final long SWEEP_DURATION_MS = 1_800L;

    // Three rings evenly spaced across one sweep, so a new one leaves as the last fades.
    private static final long SWEEP_STAGGER_MS = SWEEP_DURATION_MS / 3;

    // How far a ring travels
    private static final float SWEEP_MAX_SCALE = 1.75f;

    private final List<View> rings = new ArrayList<>(3);
    private final List<ValueAnimator> sweeps = new ArrayList<>(3);

    private View glow;
    private View disc;
    private ImageView icon;

    private State state = State.IDLE;

    public BleStatusOrb(@NonNull Context context) {
        this(context, null);
    }

    public BleStatusOrb(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public BleStatusOrb(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        LayoutInflater.from(context).inflate(R.layout.view_ble_status_orb, this, true);

        this.glow = findViewById(R.id.orbGlow);
        this.disc = findViewById(R.id.orbDisc);
        this.icon = findViewById(R.id.orbIcon);

        this.rings.add(findViewById(R.id.orbRing1));
        this.rings.add(findViewById(R.id.orbRing2));
        this.rings.add(findViewById(R.id.orbRing3));

        this.render();
    }

    @NonNull
    public State getState() {
        return this.state;
    }

    /** Repeated calls with the same state are ignored, so a re-render cannot restart the sweep. */
    public void setState(@NonNull State next) {
        if (this.state == next) {
            return;
        }
        this.state = next;
        this.render();
    }

    private void render() {
        this.disc.setBackgroundResource(this.state.discResId);
        this.icon.setImageResource(this.state.iconResId);
        ImageViewCompat.setImageTintList(this.icon,
                ContextCompat.getColorStateList(getContext(), this.state.tintResId));
        this.glow.setVisibility(this.state.glowing ? View.VISIBLE : View.INVISIBLE);

        if (this.state.sweeping) {
            this.startSweeping();
        } else {
            this.stopSweeping();
        }
    }

    private void startSweeping() {
        if (!this.sweeps.isEmpty()) {
            return;
        }

        for (int index = 0; index < this.rings.size(); index++) {
            View ring = this.rings.get(index);
            ring.setVisibility(View.VISIBLE);

            ValueAnimator sweep = ValueAnimator.ofFloat(0f, 1f);
            sweep.setDuration(SWEEP_DURATION_MS);
            sweep.setStartDelay(index * SWEEP_STAGGER_MS);
            sweep.setRepeatCount(ValueAnimator.INFINITE);
            sweep.setInterpolator(new LinearInterpolator());
            sweep.addUpdateListener(animation -> {
                float progress = (float) animation.getAnimatedValue();
                float scale = 1f + (SWEEP_MAX_SCALE - 1f) * progress;
                ring.setScaleX(scale);
                ring.setScaleY(scale);
                ring.setAlpha(1f - progress);
            });
            sweep.start();

            this.sweeps.add(sweep);
        }
    }

    private void stopSweeping() {
        for (ValueAnimator sweep : this.sweeps) {
            sweep.cancel();
        }
        this.sweeps.clear();

        for (View ring : this.rings) {
            ring.setVisibility(View.INVISIBLE);
            ring.setScaleX(1f);
            ring.setScaleY(1f);
            ring.setAlpha(1f);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        this.stopSweeping();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (this.state.sweeping) {
            this.startSweeping();
        }
    }
}
