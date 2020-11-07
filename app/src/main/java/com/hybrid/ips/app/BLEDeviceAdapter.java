package com.hybrid.ips.app;


import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.icu.text.SimpleDateFormat;
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
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;


public class BLEDeviceAdapter extends RecyclerView.Adapter<BLEDeviceAdapter.ViewHolder> {
    private ArrayList<BluetoothDevice> deviceList;
    private static final String TAG = "BLE_Connection";
    private HashMap<BluetoothDevice, Double> hashRssiMap;
    private HashMap<BluetoothDevice, Double> hashTxPowerMap;
    private HashMap<String, KalmanFilter> mKalmanFilters;
    private DecimalFormat df2 = new DecimalFormat("#.###");
    private FirebaseDatabase database;
    private DatabaseReference myRef;
    private Context context;
    private static final double KALMAN_R = 0.125d;
    private static final double KALMAN_Q = 0.5d;

    public BLEDeviceAdapter(Context context,String scenario)
    {
        this.context=context;
        deviceList = new ArrayList<BluetoothDevice>();
        hashRssiMap = new HashMap<BluetoothDevice, Double>();
        hashTxPowerMap = new HashMap<BluetoothDevice, Double>();
        database=FirebaseDatabase.getInstance();
        mKalmanFilters=new HashMap<String,KalmanFilter>();
        myRef=database.getReference("online_phase/devices/ibeacons/"+scenario);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.online_list_devices, parent, false);
        return new ViewHolder(view);
    }

    public void addDevice(BluetoothDevice device)
    {
        if (!deviceList.contains(device) && device.getName()!=null && device.getName().trim().equals("iTAG"))
        {
            deviceList.add(device);
            myRef.child(device.getAddress()).child(getCurrentDate()).setValue(device.getName());

        }
    }

    public void addRssi(BluetoothDevice device, Integer rssi)
    {
        double smoothedRssi=0;
        if (deviceList.contains(device))
        {

            if (mKalmanFilters.keySet().contains(device.getAddress())) {
                KalmanFilter mKalman = mKalmanFilters.get(device.getAddress());

                // This will give you a smoothed RSSI value because 'x == lastRssi'
                smoothedRssi = mKalman.applyFilter(rssi);

            } else {
                KalmanFilter mKalman = new KalmanFilter(KALMAN_R, KALMAN_Q);
                smoothedRssi = mKalman.applyFilter(rssi);
                mKalmanFilters.put(device.getAddress(), mKalman);
            }

            hashRssiMap.put(device, smoothedRssi);
            myRef.child(device.getAddress()).child(getCurrentDate()).child("rssi").setValue(smoothedRssi);
        }

    }

    public void addTxPower(BluetoothDevice device, Double txPower) {
        if (deviceList.contains(device)) {
            hashTxPowerMap.put(device, txPower);
            myRef.child(device.getAddress()).child(getCurrentDate()).child("tx_power").setValue(txPower);

        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position)
    {
        final BluetoothDevice device = deviceList.get(position);

        holder.deviceName.setText(context.getString(R.string.ble_device_name,device.getName()));
        holder.deviceRssi.setText(context.getString(R.string.ble_rssi,hashRssiMap.get(device)));
        /*holder.deviceDistance.setText(context.getString(R.string.ble_distance,df2.format(calculateDistance(
                hashRssiMap.get(device),
                hashTxPowerMap.get(device)))));
        myRef.child(device.getAddress()).child(getCurrentDate()).child("distance").setValue(calculateDistance(hashRssiMap.get(device),
                hashTxPowerMap.get(device)));*/

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
            //deviceDistance = view.findViewById(R.id.device_distance);
        }
    }


   /* public double calculateDistance(double rssi,double txPower)
    {
        if ( rssi == 0)
        {
            return -1.0; // if we cannot determine distance, return -1.
        }

        double ratio = rssi * 1.0 / txPower;

        if (ratio < 1.0)
        {
            return Math.pow(ratio, 10);
        }
        else
        {
            return (0.89976) * Math.pow(ratio, 7.7095) + 0.111;

        }
    }*/

    private String getCurrentDate()
    {
        return new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
    }
}