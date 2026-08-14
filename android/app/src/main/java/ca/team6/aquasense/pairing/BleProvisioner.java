package ca.team6.aquasense.pairing;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.BluetoothStatusCodes;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import ca.team6.aquasense.model.ScopedLogger;

@SuppressLint("MissingPermission")
public final class BleProvisioner implements Provisioner {

    private interface GattOperation {
        boolean dispatch();
    }

    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ArrayDeque<GattOperation> pending = new ArrayDeque<>();
    private final Set<String> boardsSeen = new HashSet<>();

    @Nullable
    private Provisioner.Listener listener;
    @Nullable
    private BluetoothGatt gatt;
    @Nullable
    private BluetoothLeScanner scanner;

    private boolean scanning;
    private boolean operationInFlight;

    private boolean finished = true;

    private boolean commitWritten;

    private String ssid = "";
    private String password = "";
    private String ownerUid = "";

    private final Runnable scanTimeout = this::finishScan;
    private final Runnable gattTimeout = () -> {
        ScopedLogger.error("Provisioning exchange timed out before the board acknowledged.");
        fail(PairingFailure.TRANSFER_FAILED);
    };

    public BleProvisioner(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public void setListener(@Nullable Provisioner.Listener listener) {
        this.listener = listener;
    }


    @Override
    public void startScan() {
        this.stopScan();
        PairingFailure blocked = this.checkRadio();
        if (blocked != null) {
            this.report(blocked);
            return;
        }

        BluetoothAdapter adapter = BlePermissions.getAdapter(this.context);
        this.scanner = adapter == null ? null : adapter.getBluetoothLeScanner();
        if (this.scanner == null) {
            this.report(PairingFailure.BLUETOOTH_OFF);
            return;
        }

        this.boardsSeen.clear();
        this.scanning = true;

        List<ScanFilter> filters = Collections.singletonList(
                new ScanFilter.Builder()
                        .setServiceUuid(new ParcelUuid(PairingContract.SERVICE_UUID))
                        .build());

        ScanSettings settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();

        this.scanner.startScan(filters, settings, this.scanCallback);
        this.main.postDelayed(this.scanTimeout, PairingContract.SCAN_TIMEOUT_MS);
    }

    @Override
    public void stopScan() {
        this.main.removeCallbacks(this.scanTimeout);
        if (!this.scanning) {
            return;
        }
        this.scanning = false;
        if (this.scanner != null) {
            this.scanner.stopScan(this.scanCallback);
        }
    }

    private void finishScan() {
        int count = this.boardsSeen.size();
        this.stopScan();
        Listener current = this.listener;
        if (current != null) {
            current.onScanFinished(count);
        }
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            if (device == null) {
                return;
            }
            ScanRecord record = result.getScanRecord();
            DiscoveredBoard board = new DiscoveredBoard(
                    device, record == null ? null : record.getDeviceName(), result.getRssi());

            main.post(() -> {
                if (!scanning) {
                    return;
                }
                boardsSeen.add(board.getAddress());
                Listener current = listener;
                if (current != null) {
                    current.onBoardFound(board);
                }
            });
        }

        @Override
        public void onScanFailed(int errorCode) {
            ScopedLogger.error("BLE scan failed with error code " + errorCode);
            main.post(() -> {
                stopScan();
                report(PairingFailure.NO_BOARD_FOUND);
            });
        }
    };


    @Override
    public void provision(@NonNull DiscoveredBoard board,
                          @NonNull String ssid,
                          @NonNull String password,
                          @NonNull String ownerUid) {
        this.stopScan();
        this.teardownGatt();

        PairingFailure blocked = this.checkRadio();
        if (blocked != null) {
            this.report(blocked);
            return;
        }

        BluetoothDevice device = board.getDevice();

        this.ssid = ssid;
        this.password = password;
        this.ownerUid = ownerUid;
        this.finished = false;
        this.commitWritten = false;

        this.gatt = device.connectGatt(
                this.context, false, this.gattCallback, BluetoothDevice.TRANSPORT_LE);

        if (this.gatt == null) {
            this.fail(PairingFailure.CONNECTION_LOST);
            return;
        }
        this.main.postDelayed(this.gattTimeout, PairingContract.GATT_TIMEOUT_MS);
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt g, int status, int newState) {
            main.post(() -> {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    if (status != BluetoothGatt.GATT_SUCCESS || gatt == null) {
                        fail(PairingFailure.CONNECTION_LOST);
                        return;
                    }
                    if (!gatt.discoverServices()) {
                        fail(PairingFailure.CONNECTION_LOST);
                    }
                    return;
                }
                if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    onLinkDropped();
                }
            });
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt g, int status) {
            main.post(() -> {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    fail(PairingFailure.CONNECTION_LOST);
                    return;
                }
                if (!hasProvisioningService()) {
                    ScopedLogger.error("Board is missing the provisioning service or its "
                            + "characteristics; firmware and PairingContract are out of step.");
                    fail(PairingFailure.INCOMPATIBLE_BOARD);
                    return;
                }
                queueExchange();
            });
        }

        @Override
        public void onMtuChanged(BluetoothGatt g, int mtu, int status) {
            main.post(() -> {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    ScopedLogger.error("MTU request refused; continuing at the default size.");
                }
                completeOperation(true);
            });
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt g, BluetoothGattDescriptor descriptor, int status) {
            main.post(() -> completeOperation(status == BluetoothGatt.GATT_SUCCESS));
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt g,
                                         BluetoothGattCharacteristic characteristic,
                                         @NonNull byte[] value,
                                         int status) {
            String deviceUid = new String(value, StandardCharsets.UTF_8).trim();
            main.post(() -> {
                if (status != BluetoothGatt.GATT_SUCCESS || deviceUid.isEmpty()) {
                    ScopedLogger.error("Could not read the board's UID (status " + status + ").");
                    completeOperation(false);
                    return;
                }
                Listener current = listener;
                if (current != null) {
                    current.onBoardIdentified(deviceUid);
                }
                completeOperation(true);
            });
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt g,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
            boolean wasCommit = PairingContract.COMMIT_UUID.equals(characteristic.getUuid());
            main.post(() -> {
                if (status == BluetoothGatt.GATT_SUCCESS && wasCommit) {
                    commitWritten = true;
                }
                completeOperation(status == BluetoothGatt.GATT_SUCCESS);
            });
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt g,
                                            BluetoothGattCharacteristic characteristic,
                                            @NonNull byte[] value) {
            String reported = new String(value, StandardCharsets.UTF_8).trim();
            main.post(() -> {
                if (PairingContract.STATUS_RECEIVED.equals(reported)) {
                    succeed();
                }
            });
        }
    };

    private void queueExchange() {
        this.enqueue(this::requestMtu);
        this.enqueue(this::subscribeToStatus);
        this.enqueue(this::readDeviceUid);
        this.enqueue(() -> this.writeText(PairingContract.SSID_UUID, this.ssid));
        this.enqueue(() -> this.writeText(PairingContract.PASSWORD_UUID, this.password));
        this.enqueue(() -> this.writeText(PairingContract.OWNER_UID_UUID, this.ownerUid));
        this.enqueue(() -> this.writeText(PairingContract.COMMIT_UUID, "1"));
    }

    private void enqueue(@NonNull GattOperation operation) {
        this.pending.add(operation);
        this.drain();
    }

    private void drain() {
        if (this.finished || this.operationInFlight || this.pending.isEmpty()) {
            return;
        }
        GattOperation operation = this.pending.poll();
        this.operationInFlight = true;
        if (operation == null || !operation.dispatch()) {
            this.operationInFlight = false;
            ScopedLogger.error("The Bluetooth stack refused a provisioning operation.");
            this.fail(PairingFailure.TRANSFER_FAILED);
        }
    }

    private void completeOperation(boolean succeeded) {
        this.operationInFlight = false;
        if (!succeeded) {
            this.fail(PairingFailure.TRANSFER_FAILED);
            return;
        }
        this.drain();
    }

    private boolean requestMtu() {
        return this.gatt != null && this.gatt.requestMtu(PairingContract.MTU_REQUEST);
    }

    private boolean subscribeToStatus() {
        BluetoothGattCharacteristic status = this.characteristic(PairingContract.STATUS_UUID);
        if (this.gatt == null || status == null) {
            return false;
        }
        if (!this.gatt.setCharacteristicNotification(status, true)) {
            return false;
        }
        BluetoothGattDescriptor cccd = status.getDescriptor(PairingContract.CCCD_UUID);
        if (cccd == null) {
            return false;
        }
        return this.gatt.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                == BluetoothStatusCodes.SUCCESS;
    }

    private boolean readDeviceUid() {
        BluetoothGattCharacteristic deviceUid =
                this.characteristic(PairingContract.DEVICE_UID_UUID);
        return this.gatt != null && deviceUid != null && this.gatt.readCharacteristic(deviceUid);
    }

    private boolean writeText(@NonNull UUID uuid, @NonNull String value) {
        BluetoothGattCharacteristic characteristic = this.characteristic(uuid);
        if (this.gatt == null || characteristic == null) {
            return false;
        }
        return this.gatt.writeCharacteristic(
                characteristic,
                value.getBytes(StandardCharsets.UTF_8),
                BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT) == BluetoothStatusCodes.SUCCESS;
    }

    @Nullable
    private BluetoothGattCharacteristic characteristic(@NonNull UUID uuid) {
        if (this.gatt == null) {
            return null;
        }
        BluetoothGattService service = this.gatt.getService(PairingContract.SERVICE_UUID);
        return service == null ? null : service.getCharacteristic(uuid);
    }

    private boolean hasProvisioningService() {
        UUID[] required = {
                PairingContract.DEVICE_UID_UUID,
                PairingContract.SSID_UUID,
                PairingContract.PASSWORD_UUID,
                PairingContract.OWNER_UID_UUID,
                PairingContract.COMMIT_UUID,
                PairingContract.STATUS_UUID,
        };
        for (UUID uuid : required) {
            if (this.characteristic(uuid) == null) {
                return false;
            }
        }
        return true;
    }

    private void onLinkDropped() {
        if (this.finished) {
            return;
        }
        if (this.commitWritten) {
            this.succeed();
            return;
        }
        this.fail(PairingFailure.CONNECTION_LOST);
    }

    private void succeed() {
        if (this.finished) {
            return;
        }
        this.finished = true;
        this.main.removeCallbacks(this.gattTimeout);
        Listener current = this.listener;
        this.teardownGatt();
        if (current != null) {
            current.onCredentialsStored();
        }
    }

    private void fail(@NonNull PairingFailure failure) {
        if (this.finished) {
            return;
        }
        this.finished = true;
        this.main.removeCallbacks(this.gattTimeout);
        Listener current = this.listener;
        this.teardownGatt();
        if (current != null) {
            current.onFailed(failure);
        }
    }

    private void report(@NonNull PairingFailure failure) {
        Listener current = this.listener;
        if (current != null) {
            current.onFailed(failure);
        }
    }

    @Nullable
    private PairingFailure checkRadio() {
        if (!BlePermissions.isSupported(this.context)) {
            return PairingFailure.UNSUPPORTED;
        }
        if (!BlePermissions.isGranted(this.context)) {
            return PairingFailure.PERMISSION_DENIED;
        }
        if (!BlePermissions.isEnabled(this.context)) {
            return PairingFailure.BLUETOOTH_OFF;
        }
        return null;
    }

    private void teardownGatt() {
        this.pending.clear();
        this.operationInFlight = false;
        if (this.gatt != null) {
            this.gatt.disconnect();
            this.gatt.close();
            this.gatt = null;
        }
    }

    @Override
    public void close() {
        this.stopScan();
        this.main.removeCallbacks(this.gattTimeout);
        this.finished = true;
        this.teardownGatt();
        this.listener = null;
    }
}
