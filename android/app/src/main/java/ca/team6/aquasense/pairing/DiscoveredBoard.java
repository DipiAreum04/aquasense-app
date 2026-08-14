package ca.team6.aquasense.pairing;

import android.bluetooth.BluetoothDevice;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

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

    @NonNull
    public BluetoothDevice getDevice() {
        return this.device;
    }

    @NonNull
    public String getAddress() {
        return this.address;
    }

    @Nullable
    public String getName() {
        return this.name;
    }

    public int getRssi() {
        return this.rssi;
    }

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
