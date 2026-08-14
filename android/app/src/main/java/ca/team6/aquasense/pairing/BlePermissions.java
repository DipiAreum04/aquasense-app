package ca.team6.aquasense.pairing;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public final class BlePermissions {

    private BlePermissions() {}

    public static final String[] REQUIRED = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
    };

    public static boolean isSupported(@NonNull Context context) {
        return context.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    public static boolean isEnabled(@NonNull Context context) {
        BluetoothAdapter adapter = getAdapter(context);
        return adapter != null && adapter.isEnabled();
    }

    @Nullable
    public static BluetoothAdapter getAdapter(@NonNull Context context) {
        BluetoothManager manager = ContextCompat.getSystemService(context, BluetoothManager.class);
        return manager == null ? null : manager.getAdapter();
    }

    public static boolean isGranted(@NonNull Context context) {
        for (String permission : REQUIRED) {
            if (ContextCompat.checkSelfPermission(context, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public static boolean isPermanentlyDenied(@NonNull Activity activity) {
        for (String permission : REQUIRED) {
            boolean denied = ContextCompat.checkSelfPermission(activity, permission)
                    != PackageManager.PERMISSION_GRANTED;
            if (denied && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    public static Intent appSettingsIntent(@NonNull Context context) {
        return new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.getPackageName(), null));
    }

    @NonNull
    public static Intent enableBluetoothIntent() {
        return new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    }
}
