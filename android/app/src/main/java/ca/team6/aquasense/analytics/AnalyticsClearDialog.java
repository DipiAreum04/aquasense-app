package ca.team6.aquasense.analytics;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

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

public final class AnalyticsClearDialog {

    public interface ConfirmListener {
        void onConfirmed();
    }

    private AnalyticsClearDialog() {}

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

        AlertDialog dialog = new MaterialAlertDialogBuilder(activity)
                .setView(content)
                .create();

        Window window = dialog.getWindow();
        if (window != null) {
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
