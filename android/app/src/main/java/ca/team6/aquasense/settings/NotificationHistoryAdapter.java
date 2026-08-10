package ca.team6.aquasense.settings;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ca.team6.aquasense.R;
import ca.team6.aquasense.model.Aquarium;
import ca.team6.aquasense.model.NotificationLogEntry;
import ca.team6.aquasense.model.NotificationTrigger;
import ca.team6.aquasense.model.SensorType;
import ca.team6.aquasense.model.aquarium_sensors.SensorStatus;

public class NotificationHistoryAdapter extends RecyclerView.Adapter<NotificationHistoryViewHolder> {
    private final List<NotificationLogEntry> entries;
    private final List<Aquarium> aquariums;
    private final SimpleDateFormat dateFormat =
            new SimpleDateFormat("MMM d, yyyy h:mm a", Locale.getDefault());

    public NotificationHistoryAdapter(List<NotificationLogEntry> entries, List<Aquarium> aquariums) {
        this.entries = entries;
        this.aquariums = aquariums;
    }

    @NonNull
    @Override
    public NotificationHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.notification_history_item, parent, false);
        return new NotificationHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationHistoryViewHolder holder, int position) {
        NotificationLogEntry entry = entries.get(position);
        Context context = holder.itemView.getContext();

        holder.ivSeverity.setImageResource(severityIconResId(entry.severity));
        holder.ivSensorIcon.setImageResource(sensorIconResId(entry.sensorType));

        String sensorName = context.getString(sensorNameResId(entry.sensorType));
        String trigger = context.getString(triggerResId(entry.trigger));
        holder.tvSensorAndTrigger.setText(context.getString(
                R.string.notification_history_subtitle, sensorName, trigger));

        String aquariumName = aquariumNameFor(entry.aquariumId);
        String timestamp = dateFormat.format(new Date(entry.timestamp));
        holder.tvAquariumAndTime.setText(context.getString(
                R.string.notification_history_subtitle, aquariumName, timestamp));
    }

    @Override
    public int getItemCount() {
        return entries.size();
    }

    public NotificationLogEntry getEntry(int position) {
        return entries.get(position);
    }

    private String aquariumNameFor(String aquariumId) {
        for (Aquarium aquarium : aquariums) {
            if (aquarium.getId().equals(aquariumId)) {
                return aquarium.getName();
            }
        }
        return aquariumId;
    }

    private static int severityIconResId(SensorStatus severity) {
        switch (severity) {
            case CRITICAL:
                return R.drawable.red_circle_24;
            case WARNING:
                return R.drawable.orange_circle_24;
            case NORMAL:
                return R.drawable.green_circle_24;
            case DISCONNECTED:
            default:
                return R.drawable.gray_circle_24;
        }
    }

    private static int sensorIconResId(SensorType sensorType) {
        switch (sensorType) {
            case TEMPERATURE:
                return R.drawable.thermometer_24px;
            case DISSOLVED_SOLIDS:
                return R.drawable.total_dissolved_solids_24px;
            case PH_LEVEL:
                return R.drawable.water_ph_24px;
            case HUB:
                return R.drawable.wifi_24px;
            case WATER_LEVEL:
            default:
                return R.drawable.water_24px;
        }
    }

    private static int sensorNameResId(SensorType sensorType) {
        switch (sensorType) {
            case TEMPERATURE:
                return R.string.temperature;
            case DISSOLVED_SOLIDS:
                return R.string.dissolved_solids;
            case PH_LEVEL:
                return R.string.ph_level;
            case HUB:
                return R.string.notification_history_hub;
            case WATER_LEVEL:
            default:
                return R.string.water_level;
        }
    }

    private static int triggerResId(NotificationTrigger trigger) {
        switch (trigger) {
            case OFFLINE:
                return R.string.trigger_offline;
            case TIME_TRIGGERED:
                return R.string.trigger_time_triggered;
            case THRESHOLD:
            default:
                return R.string.trigger_threshold;
        }
    }
}
