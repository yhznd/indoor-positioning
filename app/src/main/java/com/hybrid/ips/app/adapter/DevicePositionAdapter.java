package com.hybrid.ips.app.adapter;


import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.icu.text.SimpleDateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.hybrid.ips.app.Device;
import com.hybrid.ips.app.filters.KalmanFilter;
import com.hybrid.ips.app.R;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.UUID;

import io.realm.Realm;
import io.realm.exceptions.RealmPrimaryKeyConstraintException;


public class DevicePositionAdapter extends RecyclerView.Adapter<DevicePositionAdapter.ViewHolder> {
    private ArrayList<BluetoothDevice> deviceList;
    private static final String TAG = "BLE_Connection";
    private HashMap<BluetoothDevice, Double> hashRssiMap;
    private HashMap<BluetoothDevice, Double> hashTxPowerMap;
    private HashMap<String, KalmanFilter> mKalmanFilters;
    private DecimalFormat df2 = new DecimalFormat("#.####");
    private Realm realm;
    private Context context;
    private static final double KALMAN_R = 0.125d;
    private static final double KALMAN_Q = 0.5d;


    public DevicePositionAdapter(Context context)
    {
        this.context=context;
        deviceList = new ArrayList<BluetoothDevice>();
        hashRssiMap = new HashMap<BluetoothDevice, Double>();
        hashTxPowerMap = new HashMap<BluetoothDevice, Double>();
        mKalmanFilters=new HashMap<String,KalmanFilter>();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.device_list, parent, false);
        return new ViewHolder(view);
    }

    public void addDevice(BluetoothDevice device)
    {
        if (!deviceList.contains(device) && device.getName()!=null && device.getName().trim().equals("iTAG"))
        {
            deviceList.add(device);


        }
    }

    public void addRssi(BluetoothDevice device, double rssi)
    {
        double smoothedRssi;
        if (deviceList.contains(device))
        {

            if (mKalmanFilters.keySet().contains(device.getAddress()))
            {
                KalmanFilter mKalman = mKalmanFilters.get(device.getAddress());

                // This will give you a smoothed RSSI value because 'x == lastRssi'
                smoothedRssi = mKalman.applyFilter(rssi);
                Log.i(TAG, "Old Rssi: " + rssi + "Smoothed RSSI: " + smoothedRssi);
                // Do what you want with this rssi
            }
            else
            {
                KalmanFilter mKalman = new KalmanFilter(KALMAN_R, KALMAN_Q);
                smoothedRssi = mKalman.applyFilter(rssi);
                mKalmanFilters.put(device.getAddress(), mKalman);
                Log.i(TAG, "Old Rssi: " + rssi + "Smoothed RSSI: " + smoothedRssi);
            }


            hashRssiMap.put(device, smoothedRssi);
        }
    }

    public void addTxPower(BluetoothDevice device, double txPower)
    {
        if (deviceList.contains(device)) {
            hashTxPowerMap.put(device, txPower);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position)
    {
        final BluetoothDevice device = deviceList.get(position);
        holder.deviceMac.setText(context.getString(R.string.ble_device_name,device.getAddress()));
        holder.deviceRssi.setText(context.getString(R.string.ble_rssi,hashRssiMap.get(device)));
        holder.deviceCoordinates.setText(context.getString(R.string.ble_coordinates,
                                                            determineX(device.getAddress()),
                                                            determineY(device.getAddress())));
        holder.deviceDistance.setText(context.getString(R.string.ble_distance,df2.format(calculateDistance(hashRssiMap.get(device), hashTxPowerMap.get(device)))));

        holder.saveButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {

                SharedPreferences pref = context.getSharedPreferences("KEY", 0);
                final String fId = pref.getString("UUID", "not defined"); // getting UUID
                String currentDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
                String macAddress = device.getAddress();
                double distance = calculateDistance(hashRssiMap.get(device), hashTxPowerMap.get(device));
                double x = determineX(device.getAddress());
                double y = determineY(device.getAddress());
                double rssi = hashRssiMap.get(device);

                final Device deviceBLE = new Device(fId, macAddress, distance, x, y, rssi, currentDate);
                realm = Realm.getDefaultInstance();
                realm.executeTransaction(new Realm.Transaction()
                {
                    @Override
                    public void execute(Realm bgRealm) {
                        try {
                            Device device1 = bgRealm.createObject(Device.class, UUID.randomUUID().toString());
                            device1.setUUID(fId);
                            device1.setMacAddres(deviceBLE.getMacAddres());
                            device1.setDistance(deviceBLE.getDistance());
                            device1.setX(deviceBLE.getX());
                            device1.setY(deviceBLE.getY());
                            device1.setRssi(deviceBLE.getRssi());
                            device1.setCreatedAt(deviceBLE.getCreatedAt());
                            //Toast.makeText(context, device.getAddress() + " saved!", Toast.LENGTH_SHORT).show();
                        } catch (RealmPrimaryKeyConstraintException ex) {
                            Toast.makeText(context, "BLE information alreayd exists for this ID!", Toast.LENGTH_SHORT).show();

                        }

                    }
                });

                removeAt(position);
            }
         });

    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView deviceMac;
        TextView deviceRssi;
        TextView deviceDistance;
        TextView deviceCoordinates;
        Button saveButton;
        public ViewHolder(@NonNull View view) {
            super(view);
            deviceRssi = view.findViewById(R.id.device_rssi);
            deviceMac = view.findViewById(R.id.device_name);
            deviceDistance = view.findViewById(R.id.device_distance);
            deviceCoordinates=view.findViewById(R.id.device_coordinates);
            saveButton=view.findViewById(R.id.saveLocation);
        }
    }


    public double calculateDistance(double rssi,double txPower)
    {
        return Math.pow(10d, ( txPower - rssi) / (10 * 2));
    }
    
    public double determineX(String deviceAddress)
    {
        double x = 0.0;
        if(deviceAddress.trim().equals("FF:FF:49:A2:8D:81")) //iBeacon1-origin
            x=2.05;
        else if(deviceAddress.trim().equals("FF:FF:AA:00:4A:C8")) //iBeacon2
            x=2.05;
        else if(deviceAddress.trim().equals("FF:FF:3B:55:93:00")) //iBeacon3--origin
            x=0.0;
        else if(deviceAddress.trim().equals("FF:FF:25:1C:CD:80")) //iBeacon4
            x=0.0;
        else if(deviceAddress.trim().equals("FF:FF:9C:08:A1:80")) //iBeacon5
            x=4.10;
        else if(deviceAddress.trim().equals("FF:FF:AA:00:4A:AE")) //iBeacon6
            x=4.10;

        return  x;
    }

    public double determineY(String deviceAddress)
    {
        double y = 0.0;
        if(deviceAddress.trim().equals("FF:FF:49:A2:8D:81")) //iBeacon1
            y=0.0;
        else if(deviceAddress.trim().equals("FF:FF:AA:00:4A:C8")) //iBeacon2
            y=2.8;
        else if(deviceAddress.trim().equals("FF:FF:3B:55:93:00")) //iBeacon3--origin
            y=0.0;
        else if(deviceAddress.trim().equals("FF:FF:25:1C:CD:80")) //iBeacon4
            y=2.8;
        else if(deviceAddress.trim().equals("FF:FF:9C:08:A1:80")) //iBeacon5
            y=0.0;
        else if(deviceAddress.trim().equals("FF:FF:AA:00:4A:AE")) //iBeacon6
            y=2.8;

        return  y;
    }

    public void removeAt(int position)
    {
        deviceList.remove(position);
        notifyItemRemoved(position);
        notifyItemRangeChanged(position, deviceList.size());
    }
}