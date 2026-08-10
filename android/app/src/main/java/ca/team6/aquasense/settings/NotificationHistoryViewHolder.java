package ca.team6.aquasense.settings;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import ca.team6.aquasense.R;

public class NotificationHistoryViewHolder extends RecyclerView.ViewHolder {
    ImageView ivSeverity;
    ImageView ivSensorIcon;
    TextView tvSensorAndTrigger;
    TextView tvAquariumAndTime;

    public NotificationHistoryViewHolder(@NonNull View view) {
        super(view);

        this.ivSeverity = view.findViewById(R.id.ivSeverity);
        this.ivSensorIcon = view.findViewById(R.id.ivSensorIcon);
        this.tvSensorAndTrigger = view.findViewById(R.id.tvSensorAndTrigger);
        this.tvAquariumAndTime = view.findViewById(R.id.tvAquariumAndTime);
    }
}
