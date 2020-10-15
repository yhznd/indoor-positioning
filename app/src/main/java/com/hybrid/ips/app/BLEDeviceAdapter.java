package com.hybrid.ips.app;


import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;


public class BLEDeviceAdapter extends RecyclerView.Adapter<BLEDeviceAdapter.ViewHolder> {
    private ArrayList<BluetoothDevice> deviceList;
    private static final String TAG = "BLE_Connection";
    private HashMap<BluetoothDevice, Integer> hashRssiMap;
    private HashMap<BluetoothDevice, Integer> hashTxPowerMap;
    private DecimalFormat df2 = new DecimalFormat("#.###");
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private Context context;

    BLEDeviceAdapter(Context context)
    {
        this.context=context;
        deviceList = new ArrayList<BluetoothDevice>();
        hashRssiMap = new HashMap<BluetoothDevice, Integer>();
        hashTxPowerMap = new HashMap<BluetoothDevice, Integer>();
        database=FirebaseDatabase.getInstance();
        myRef=database.getReference("devices/iBeacons");
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_devices, parent, false);
        return new ViewHolder(view);
    }

    public void addDevice(BluetoothDevice device)
    {
        if (!deviceList.contains(device) && device.getName()!=null && device.getName().trim().equals("iTAG"))
        {
            deviceList.add(device);
            myRef.child(device.getAddress()).setValue(device.getName());

        }
    }

    public void addRssi(BluetoothDevice device, Integer rssi)
    {
        if (deviceList.contains(device))
        {
            hashRssiMap.put(device, rssi);
            myRef.child(device.getAddress()).child("rssi").setValue(rssi);

        }
    }

    public void addTxPower(BluetoothDevice device, Integer txPower) {
        if (deviceList.contains(device)) {
            hashTxPowerMap.put(device, txPower);
            myRef.child(device.getAddress()).child("tx_power").setValue(txPower);

        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        final BluetoothDevice device = deviceList.get(position);

        holder.deviceName.setText(context.getString(R.string.ble_device_name,device.getName()));
        holder.deviceRssi.setText(context.getString(R.string.ble_rssi,hashRssiMap.get(device)));
        holder.deviceDistance.setText(context.getString(R.string.ble_distance,df2.format(calculateDistance(
                String.valueOf(hashRssiMap.get(device)),
                String.valueOf(hashTxPowerMap.get(device))))));

        myRef.child(device.getAddress()).child("distance").setValue(calculateDistance(String.valueOf(hashRssiMap.get(device)),
                String.valueOf(hashTxPowerMap.get(device))));

    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName;
        TextView deviceRssi;
        TextView deviceDistance;

        public ViewHolder(@NonNull View view) {
            super(view);
            deviceRssi = view.findViewById(R.id.device_rssi);
            deviceName = view.findViewById(R.id.device_name);
            deviceDistance = view.findViewById(R.id.device_distance);
        }
    }


    public double calculateDistance(String rssi,String txPower)
    {
        if (rssi.equalsIgnoreCase("0") )
        {
            return -1.0; // if we cannot determine distance, return -1.
        }

        double ratio = Double.parseDouble(rssi) * 1.0 / Double.parseDouble(txPower);

        if (ratio < 1.0)
        {
            return Math.pow(ratio, 10);
        }
        else
        {
            double accuracy = (0.89976) * Math.pow(ratio, 7.7095) + 0.111;;
            return accuracy;
        }
    }
}