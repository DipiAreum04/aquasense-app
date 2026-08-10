package ca.team6.aquasense.analytics;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import ca.team6.aquasense.R;

/**
 * Asks before clearing one window of readings, and names all three of the things that identify it:
 * the aquarium, the sensor and the period.
 *
 * <p>All three, because none of them is implied by the others. The page has an aquarium selected, a
 * sensor tab open and a period button raised, and a dialog that said only "delete these readings"
 * would be asking about whichever of those the user last touched rather than about the node. What
 * is being deleted is the intersection, so the dialog quotes the intersection back.
 *
 * <p>Nothing happens on the way out. Cancelling, tapping outside and the back button all just
 * dismiss, and only the confirm button reports anything to the caller - so the safe answer is every
 * answer except the one deliberate press.
 */
public final class AnalyticsClearDialog {

    /** Called once, on the main thread, if and only if the user presses the confirm button. */
    public interface ConfirmListener {
        void onConfirmed();
    }

    private AnalyticsClearDialog() {}

    /**
     * @param aquariumName  as the page names it above the tabs, not the ID underneath.
     * @param sensorNameResId the sensor's own name, so the dialog and the tab cannot disagree.
     */
    public static void show(@NonNull Activity activity,
                            @NonNull String aquariumName,
                            @StringRes int sensorNameResId,
                            @NonNull AnalyticsPeriod period,
                            @NonNull ConfirmListener listener) {
        View content = activity.getLayoutInflater().inflate(R.layout.dialog_analytics_clear, null);

        ((TextView) content.findViewById(R.id.analyticsClearAquarium)).setText(aquariumName);
        ((TextView) content.findViewById(R.id.analyticsClearSensor)).setText(sensorNameResId);
        ((TextView) content.findViewById(R.id.analyticsClearPeriod))
                .setText(period.getLongLabelResId());

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(content)
                .create();

        Window window = dialog.getWindow();
        if (window != null) {
            // The layout draws its own rounded card, so the window behind it has to stop drawing
            // one: AlertDialog's background is an opaque square-cornered surface, and left in
            // place it shows at all four corners of the card as a grey right angle.
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        content.findViewById(R.id.analyticsClearCancel).setOnClickListener(v -> dialog.dismiss());
        content.findViewById(R.id.analyticsClearConfirm).setOnClickListener(v -> {
            dialog.dismiss();
            listener.onConfirmed();
        });

        dialog.show();
    }
}
