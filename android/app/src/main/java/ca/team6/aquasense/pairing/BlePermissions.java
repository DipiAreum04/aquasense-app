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

/**
 * The runtime checks that must pass before {@link BleProvisioner} can scan or connect.
 *
 * <p>Three separate things can block pairing:
 *
 * <ol>
 *   <li>the device has no Bluetooth LE radio at all (the user cannot do anything);
 *   <li>Bluetooth is switched off (the user can turn it on);
 *   <li>the permissions are not granted (the user can grant them).
 * </ol>
 */
public final class BlePermissions {

    private BlePermissions() {}

    /** Requested together: scanning finds the device and connecting talks to it */
    public static final String[] REQUIRED = {
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
    };

    /** False when the hardware has no BLE radio, which means the device is incompatible or buggy */
    public static boolean isSupported(@NonNull Context context) {
        return context.getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE);
    }

    /** True when Bluetooth is switched on */
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

    /** True when the user has denied a permission permanently */
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

    /** Opens this app's system settings page, the only route back from a permanent denial. */
    @NonNull
    public static Intent appSettingsIntent(@NonNull Context context) {
        return new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", context.getPackageName(), null));
    }

    /** Asks the mobile system to turn Bluetooth on */
    @NonNull
    public static Intent enableBluetoothIntent() {
        return new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    }
}
