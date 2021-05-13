package me.polamokh.btchat.ui.devicelist;

import android.bluetooth.BluetoothDevice;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import me.polamokh.btchat.databinding.ItemDeviceBinding;
import me.polamokh.btchat.ui.OnItemClickListener;

public class DeviceListAdapter extends ListAdapter<BluetoothDevice, DeviceListAdapter.DeviceListViewHolder> {

    private static final DiffUtil.ItemCallback<BluetoothDevice> diffCallback =
            new DiffUtil.ItemCallback<BluetoothDevice>() {
                @Override
                public boolean areItemsTheSame(@NonNull BluetoothDevice oldItem,
                                               @NonNull BluetoothDevice newItem) {
                    return oldItem.getAddress().equals(newItem.getAddress());
                }

                @Override
                public boolean areContentsTheSame(@NonNull BluetoothDevice oldItem,
                                                  @NonNull BluetoothDevice newItem) {
                    return oldItem.getName().equals(newItem.getName());
                }
            };

    private static OnItemClickListener<BluetoothDevice> listener;

    protected DeviceListAdapter(OnItemClickListener<BluetoothDevice> clickListener) {
        super(diffCallback);
        DeviceListAdapter.listener = clickListener;
    }

    @NonNull
    @Override
    public DeviceListViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return DeviceListViewHolder.from(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull DeviceListViewHolder holder, int position) {
        BluetoothDevice item = getItem(position);
        holder.bind(item);
    }

    public static class DeviceListViewHolder extends RecyclerView.ViewHolder {

        private final ItemDeviceBinding binding;

        public DeviceListViewHolder(ItemDeviceBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public static DeviceListViewHolder from(ViewGroup parent) {
            ItemDeviceBinding binding = ItemDeviceBinding.inflate(
                    LayoutInflater.from(parent.getContext()),
                    parent, false);
            return new DeviceListViewHolder(binding);
        }

        public void bind(BluetoothDevice item) {
            binding.deviceName.setText(item.getName());
            binding.deviceAddress.setText(item.getAddress());

            binding.getRoot().setOnClickListener(v -> {
                DeviceListAdapter.listener.clickListener(item);
            });

            binding.executePendingBindings();
        }
    }
}
