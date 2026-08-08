package ca.team6.aquasense.pairing;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

/**
 * This class represents a discovered board shown in the board picker list during the pairing process.
 */
public final class DiscoveredBoard {

    @NonNull
    private final BluetoothDevice device;
    private final String address;
    @Nullable
    private final String name;
    private final int rssi;

    public DiscoveredBoard(@NonNull BluetoothDevice device, @Nullable String name, int rssi) {
        this.device = device;
        this.address = device.getAddress();
        this.name = name;
        this.rssi = rssi;
    }

    /** The device to open a GATT link against. */
    @NonNull
    public BluetoothDevice getDevice() {
        return this.device;
    }

    /** MAC address or device address of the board */
    @NonNull
    public String getAddress() {
        return this.address;
    }

    /** Advertised local name of the board */
    @Nullable
    public String getName() {
        return this.name;
    }

    /** Signal strength in dBm. Less negative is closer. */
    public int getRssi() {
        return this.rssi;
    }

    /**
     * Rough distance banding for the picker, 0 (far) to 3 (very close).
     *
     * <p>RSSI is noisy and reflects orientation as much as distance, so this is only ever used to
     * order and illustrate the list, never to pick a board automatically.
     */
    public int getSignalBars() {
        if (this.rssi >= -60) {
            return 3;
        }
        if (this.rssi >= -75) {
            return 2;
        }
        if (this.rssi >= -90) {
            return 1;
        }
        return 0;
    }

    /**
     * Identity is the address alone: a scan reports the same board repeatedly with a different
     * RSSI each time, and those must collapse to one row instead of stacking up.
     */
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof DiscoveredBoard)) {
            return false;
        }
        return this.address.equals(((DiscoveredBoard) other).address);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(this.address);
    }
}
