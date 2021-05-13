package me.polamokh.btchat.ui;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import me.polamokh.btchat.R;
import me.polamokh.btchat.databinding.ActivityMainBinding;
import me.polamokh.btchat.ui.devicelist.DeviceListActivity;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private static final int RC_ENABLE_BT = 100;
    private static final int RC_CONNECT_DEVICE = 101;

    private ActivityMainBinding binding;

    private BluetoothAdapter bluetoothAdapter;

    private BluetoothChatService chatService = null;
    private StringBuffer outStringBuffer;
    private String connectedDeviceName = null;

    private StringBuilder chat;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG)
                    .show();
            finish();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (!bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, RC_ENABLE_BT);
        } else if (chatService == null)
            setupChat();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (chatService != null)
            if (chatService.getState() == BluetoothChatService.STATE_NONE)
                chatService.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (chatService != null) {
            chatService.stop();
        }
    }

    private void setupChat() {
        binding.sendButton.setOnClickListener(v -> {
            String text = binding.messageText.getEditText().getText().toString();
            sendMessage(text);
        });

        chatService = new BluetoothChatService(handler);

        outStringBuffer = new StringBuffer();

        chat = new StringBuilder();
    }

    private void sendMessage(String message) {
        if (chatService.getState() != BluetoothChatService.STATE_CONNECTED)
            return;

        if (message.length() > 0) {
            byte[] send = message.getBytes();
            chatService.write(send);

            outStringBuffer.setLength(0);
            binding.messageText.getEditText().setText(outStringBuffer);
        }
    }

    private void setStatus(int resId) {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar == null)
            return;
        actionBar.setSubtitle(resId);
    }

    private void setStatus(CharSequence subTitle) {
        final ActionBar actionBar = getSupportActionBar();
        if (actionBar == null)
            return;
        actionBar.setSubtitle(subTitle);
    }

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_CONNECTED:
                            setStatus("Connected to: " + connectedDeviceName);
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            setStatus("Connecting...");
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                        case BluetoothChatService.STATE_NONE:
                            setStatus("Not connected");
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuffer = (byte[]) msg.obj;
                    chat.append("\nme: ").append(new String(writeBuffer));
                    binding.chatText.setText(chat.toString());
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuffer = (byte[]) msg.obj;
                    chat.append('\n').append(connectedDeviceName).append(": ")
                            .append(new String(readBuffer, 0, msg.arg1));
                    binding.chatText.setText(chat.toString());
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    connectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                    Toast.makeText(MainActivity.this, "Connected to "
                            + connectedDeviceName, Toast.LENGTH_SHORT)
                            .show();
                    break;
                case Constants.MESSAGE_TOAST:
                    Toast.makeText(MainActivity.this, msg.getData().getString(Constants.TOAST),
                            Toast.LENGTH_SHORT).show();
                    break;

            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_conntect) {
            startActivityForResult(new Intent(this, DeviceListActivity.class),
                    RC_CONNECT_DEVICE);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case RC_ENABLE_BT:
                if (resultCode == RESULT_OK)
                    setupChat();
                else
                    finish();
                break;
            case RC_CONNECT_DEVICE:
                if (resultCode == RESULT_OK) {
                    String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
                    chatService.connect(device);
                }
                break;
        }
    }
}