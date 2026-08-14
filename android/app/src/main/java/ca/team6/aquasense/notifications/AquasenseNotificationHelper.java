package ca.team6.aquasense.notifications;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import ca.team6.aquasense.dashboard.MainActivity;
import ca.team6.aquasense.R;
import ca.team6.aquasense.notifications.NotificationLogMapper;
import ca.team6.aquasense.notifications.NotificationLogRepository;
import ca.team6.aquasense.logging.ScopedLogger;
import ca.team6.aquasense.aquarium.sensors.SensorStatus;

public final class AquasenseNotificationHelper {

    public static final String CHANNEL_ID = "aquasense_threshold_alerts";
    public static final String MONITORING_CHANNEL_ID = "aquasense_background_monitoring";

    public static final String EXTRA_AQUARIUM_ID = "ca.team6.aquasense.extra.AQUARIUM_ID";

    private static final int MONITORING_REQUEST_CODE = 1;

    private AquasenseNotificationHelper() {}

    public static void ensureChannel(@NonNull Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.notification_channel_threshold_name),
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription(context.getString(R.string.notification_channel_threshold_desc));
        manager.createNotificationChannel(channel);
    }

    public static void ensureMonitoringChannel(@NonNull Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager == null) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                MONITORING_CHANNEL_ID,
                context.getString(R.string.notification_channel_monitoring_name),
                NotificationManager.IMPORTANCE_LOW
        );
        channel.setDescription(context.getString(R.string.notification_channel_monitoring_desc));
        manager.createNotificationChannel(channel);
    }

    @NonNull
    public static Notification buildMonitoringNotification(@NonNull Context context) {
        ensureMonitoringChannel(context);
        return new NotificationCompat.Builder(context, MONITORING_CHANNEL_ID)
                .setSmallIcon(R.drawable.notifications_24px)
                .setContentTitle(context.getString(R.string.notification_monitoring_title))
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setContentIntent(openAppIntent(context, null, MONITORING_REQUEST_CODE))
                .setOngoing(true)
                .build();
    }

    public static void showThresholdAlert(
            @NonNull Context context,
            @NonNull ThresholdViolation violation,
            @NonNull String aquariumName
    ) {
        ensureChannel(context);

        ThresholdAlertFormatter formatter = new ThresholdAlertFormatter(context);
        String title = formatter.title(violation, aquariumName);
        String message = formatter.message(violation);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.notifications_24px)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(violation.isCritical()
                        ? NotificationCompat.PRIORITY_HIGH
                        : NotificationCompat.PRIORITY_DEFAULT)
                .setColor(ContextCompat.getColor(context, accentColorResIdFor(violation)))
                .setContentIntent(openAppIntent(
                        context, violation.aquariumId, violation.notificationId()))
                .setAutoCancel(true);

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ScopedLogger.warn("Threshold alert dropped: POST_NOTIFICATIONS not granted (" + title + ")");
            return;
        }
        NotificationManagerCompat.from(context).notify(violation.notificationId(), builder.build());
        new NotificationLogRepository(context).addEntry(NotificationLogMapper.fromViolation(violation));
    }

    @ColorRes
    private static int accentColorResIdFor(@NonNull ThresholdViolation violation) {
        switch (violation.kind) {
            case SENSOR_OFFLINE:
            case HUB_DISCONNECTED:
                return SensorStatus.DISCONNECTED.colorResourceId;
            default:
                return violation.severity.colorResourceId;
        }
    }

    private static PendingIntent openAppIntent(
            @NonNull Context context,
            @Nullable String aquariumId,
            int requestCode
    ) {
        Intent intent = new Intent(context, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_CLEAR_TOP
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        if (aquariumId != null && !aquariumId.isEmpty()) {
            intent.putExtra(EXTRA_AQUARIUM_ID, aquariumId);
        }
        return PendingIntent.getActivity(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
}
