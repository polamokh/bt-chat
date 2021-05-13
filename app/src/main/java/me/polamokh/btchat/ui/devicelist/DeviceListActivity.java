package me.polamokh.btchat.ui.devicelist;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import me.polamokh.btchat.R;
import me.polamokh.btchat.databinding.ActivityDeviceListBinding;
import me.polamokh.btchat.ui.OnItemClickListener;

public class DeviceListActivity extends AppCompatActivity
        implements OnItemClickListener<BluetoothDevice> {

    private static final String TAG = "DeviceListActivity";

    public static final String EXTRA_DEVICE_ADDRESS = "device_address";

    private static final int RC_LOCATION_PERMISSION = 102;

    private BluetoothAdapter bluetoothAdapter;

    private DeviceListAdapter deviceListAdapter;

    private List<BluetoothDevice> bluetoothDevices;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ActivityDeviceListBinding binding =
                DataBindingUtil.setContentView(this, R.layout.activity_device_list);

        setResult(Activity.RESULT_CANCELED);

        if (ContextCompat.checkSelfPermission(getBaseContext(), Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION
            }, RC_LOCATION_PERMISSION);
        }

        registerReceiver(receiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        bluetoothDevices = new ArrayList<>();

        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice pairedDevice : pairedDevices) {
                Log.i(TAG, "onCreate: " + pairedDevice.getName() + "\n" + pairedDevice.getAddress());
                bluetoothDevices.add(pairedDevice);
            }
        }

        startDiscovery();

        deviceListAdapter = new DeviceListAdapter(this);
        binding.devicesList.setAdapter(deviceListAdapter);
        deviceListAdapter.submitList(bluetoothDevices);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (bluetoothAdapter != null)
            bluetoothAdapter.cancelDiscovery();

        unregisterReceiver(receiver);
    }

    @Override
    public void clickListener(BluetoothDevice item) {
        bluetoothAdapter.cancelDiscovery();

        Intent intent = new Intent();
        intent.putExtra(EXTRA_DEVICE_ADDRESS, item.getAddress());

        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    private void startDiscovery() {
        if (bluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }

        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        bluetoothAdapter.startDiscovery();
    }

    private final BroadcastReceiver receiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i(TAG, "onReceive: " + device.getName() + '\n' + device.getAddress());
                if (device != null && device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    bluetoothDevices.add(device);
                    deviceListAdapter.notifyItemChanged(bluetoothDevices.size() - 1);
                }
            }
        }
    };
}