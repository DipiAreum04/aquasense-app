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

import ca.team6.aquasense.MainActivity;
import ca.team6.aquasense.R;
import ca.team6.aquasense.model.NotificationLogMapper;
import ca.team6.aquasense.model.NotificationLogRepository;
import ca.team6.aquasense.model.ScopedLogger;
import ca.team6.aquasense.model.aquarium_sensors.SensorStatus;

public final class AquasenseNotificationHelper {

    public static final String CHANNEL_ID = "aquasense_threshold_alerts";
    public static final String MONITORING_CHANNEL_ID = "aquasense_background_monitoring";

    /** The aquarium a tapped alert is about, so the dashboard opens on that tank. */
    public static final String EXTRA_AQUARIUM_ID = "ca.team6.aquasense.extra.AQUARIUM_ID";

    // The monitoring notice is not about one aquarium, so it only needs a request code of its own.
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
                // Low importance keeps the persistent monitoring notice silent and collapsed.
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
                // A spike is advisory, a band breach needs attention now.
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

    /**
     * The colour the shade tints an alert's icon and app name with (ST-7.3), taken from the same
     * {@link SensorStatus} the dashboard colours the sensor's card with. A notification and the
     * card it sends you to therefore agree on sight, before either has been read.
     *
     * <p>A violation that is not about a reading has no severity to show. Both of those - a sensor
     * that stopped reporting and a hub that went quiet - are the disconnected grey the dashboard
     * greys the same card to, rather than a red that would claim the water is in trouble when what
     * is actually in trouble is the hardware.
     */
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

    /**
     * Tapping an AquaSense notification opens the dashboard, on {@code aquariumId} when one is
     * given.
     *
     * <p>{@code requestCode} must be unique per notification. Two PendingIntents are the same when
     * their request codes match and their intents are {@link Intent#filterEquals} — which ignores
     * extras — so a shared request code would let {@link PendingIntent#FLAG_UPDATE_CURRENT} rewrite
     * the target aquarium of every alert already sitting in the shade.
     */
    private static PendingIntent openAppIntent(
            @NonNull Context context,
            @Nullable String aquariumId,
            int requestCode
    ) {
        Intent intent = new Intent(context, MainActivity.class)
                // SINGLE_TOP hands the intent to a running MainActivity's onNewIntent instead of
                // tearing the dashboard down and rebuilding it.
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
