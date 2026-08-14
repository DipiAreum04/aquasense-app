package ca.team6.aquasense.notifications;

import android.app.Notification;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ServiceCompat;

import ca.team6.aquasense.logging.ScopedLogger;

public final class ThresholdMonitorService extends Service {

    private static final int FOREGROUND_NOTIFICATION_ID = 9001;

    @Nullable
    private FirebaseThresholdMonitor monitor;

    public static void start(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        try {
            appContext.startForegroundService(
                    new Intent(appContext, ThresholdMonitorService.class));
        } catch (RuntimeException e) {
            ScopedLogger.error("Could not start threshold monitor: " + e.getMessage());
        }
    }

    public static void stop(@NonNull Context context) {
        Context appContext = context.getApplicationContext();
        appContext.stopService(new Intent(appContext, ThresholdMonitorService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AquasenseNotificationHelper.ensureMonitoringChannel(this);
        monitor = new FirebaseThresholdMonitor(this);
    }

    @Override
    public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        Notification notification = AquasenseNotificationHelper.buildMonitoringNotification(this);
        ServiceCompat.startForeground(
                this,
                FOREGROUND_NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);

        if (!MonitoringController.shouldMonitor(this)) {
            stopMonitoring();
            return START_NOT_STICKY;
        }

        if (monitor != null) {
            monitor.start();
        }
        return START_STICKY;
    }

    @Override
    public void onTimeout(int startId, int fgsType) {
        ScopedLogger.warn("Threshold monitor timed out (type " + fgsType + "); stopping.");
        stopMonitoring();
    }

    @Override
    public void onTimeout(int startId) {
        ScopedLogger.warn("Threshold monitor timed out; stopping.");
        stopMonitoring();
    }

    private void stopMonitoring() {
        if (monitor != null) {
            monitor.stop();
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE);
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (monitor != null) {
            monitor.stop();
            monitor = null;
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
