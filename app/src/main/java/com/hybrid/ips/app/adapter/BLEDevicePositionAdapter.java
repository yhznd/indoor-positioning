package com.hybrid.ips.app.adapter;


import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.SharedPreferences;
import android.icu.text.SimpleDateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import com.hybrid.ips.app.util.Device;
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


public class BLEDevicePositionAdapter extends RecyclerView.Adapter<BLEDevicePositionAdapter.ViewHolder> {
    private ArrayList<BluetoothDevice> deviceList;
    private static final String TAG = "BLE_Connection";
    private HashMap<BluetoothDevice, Double> hashRssiMap;
    private HashMap<BluetoothDevice, Double> hashTxPowerMap;
    private HashMap<BluetoothDevice, Integer> hashBatteryLevel;
    private HashMap<String, KalmanFilter> mKalmanFilters;
    private DecimalFormat df2 = new DecimalFormat("#.####");
    private Realm realm;
    private Context context;
    private static final double KALMAN_R = 0.125d;
    private static final double KALMAN_Q = 0.5d;


    public BLEDevicePositionAdapter(Context context)
    {
        this.context=context;
        deviceList = new ArrayList<BluetoothDevice>();
        hashRssiMap = new HashMap<BluetoothDevice, Double>();
        hashTxPowerMap = new HashMap<BluetoothDevice, Double>();
        hashBatteryLevel = new HashMap<BluetoothDevice, Integer>();
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
        if (!deviceList.contains(device) && device.getName()!=null && device.getName().trim().equals("POI")
         || !deviceList.contains(device) && device.getName()!=null && device.getName().trim().equals("iTAG"))
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

            if (smoothedRssi>=-80.0)
                hashRssiMap.put(device, smoothedRssi);
            else
            {
                removeAt(deviceList.indexOf(device));
            }
        }
    }

    public void addTxPower(BluetoothDevice device, double txPower)
    {
        if (deviceList.contains(device)) {
            hashTxPowerMap.put(device, txPower);
        }
    }

    public void addBatteryLevel(BluetoothDevice device, Integer batteryLevel)
    {
        if (deviceList.contains(device))
        {
            hashBatteryLevel.put(device, batteryLevel);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, final int position)
    {
        final BluetoothDevice device = deviceList.get(position);
        holder.deviceMac.setText(context.getString(R.string.ble_device_name,device.getAddress()));
        holder.deviceRssi.setText(context.getString(R.string.ble_rssi,hashRssiMap.get(device)));
        holder.deviceBattery.setText("%"+context.getString(R.string.ble_battery,hashBatteryLevel.get(device)));
        holder.deviceCoordinates.setText(context.getString(R.string.ble_coordinates,
                                                            determineX(device.getAddress()),
                                                            determineY(device.getAddress())));
        holder.deviceDistance.setText(context.getString(R.string.ble_distance,df2.format(calculateDistance(hashRssiMap.get(device), hashTxPowerMap.get(device)))));

        holder.cardView.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {

                SharedPreferences pref = context.getSharedPreferences("KEY", 0);
                final String fId = pref.getString("UUID", "not defined"); // getting UUID
                String currentDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
                String macAddress = device.getAddress();
                double distance = calculateDistance(hashRssiMap.get(device), hashTxPowerMap.get(device));
                int battery=hashBatteryLevel.get(device);
                double x = determineX(device.getAddress());
                double y = determineY(device.getAddress());
                double rssi = hashRssiMap.get(device);


                final Device deviceBLE = new Device(fId, macAddress, distance, x, y, rssi, battery,currentDate);
                realm = Realm.getDefaultInstance();
                realm.executeTransaction(new Realm.Transaction()
                {
                    @Override
                    public void execute(Realm bgRealm) {
                        try {
                            Device device1 = bgRealm.createObject(Device.class, UUID.randomUUID().toString());
                            device1.setMeasureId(fId);
                            device1.setMacAddres(deviceBLE.getMacAddres());
                            device1.setDistance(deviceBLE.getDistance());
                            device1.setX(deviceBLE.getX());
                            device1.setY(deviceBLE.getY());
                            device1.setRssi(deviceBLE.getRssi());
                            device1.setBatteryLevel(deviceBLE.getBatteryLevel());
                            device1.setCreatedAt(deviceBLE.getCreatedAt());
                            //Toast.makeText(context, device.getAddress() + " saved!", Toast.LENGTH_SHORT).show();
                        } catch (RealmPrimaryKeyConstraintException ex) {
                            Toast.makeText(context, "BLE information already exists for this ID!", Toast.LENGTH_SHORT).show();

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
        TextView deviceBattery;
        Button saveButton;
        CardView cardView;
        public ViewHolder(@NonNull View view)
        {
            super(view);
            deviceRssi = view.findViewById(R.id.device_rssi);
            deviceMac = view.findViewById(R.id.device_name);
            deviceDistance = view.findViewById(R.id.device_distance);
            deviceCoordinates=view.findViewById(R.id.device_coordinates);
            deviceBattery = view.findViewById(R.id.deviceBattery);
            saveButton=view.findViewById(R.id.saveLocation);
            cardView=view.findViewById(R.id.cardView);
        }
    }


    protected static double calculateDistance(double rssi, double txPower)
    {
        return Math.pow(10d, ( txPower - rssi) / (10 * 2));
    }

    public double determineX(String deviceAddress)
    {
        double x = 0.0;
        if(deviceAddress.trim().contains("ED:7B:9D")) //iBeacon1-origin
            x=0.0;
        else if(deviceAddress.trim().contains("FF:FF:25")) //iBeacon2
            x=2.4;
        else if(deviceAddress.trim().contains("FF:A2:5C")) //iBeacon3
            x=2.5;
        else if(deviceAddress.trim().contains("FF:0F:D9")) //iBeacon4
            x=2.5;
        else if(deviceAddress.trim().contains("E1:F4:AD")) //iBeacon5
            x=6.6;
        else if(deviceAddress.trim().contains("D3:3F:DD")) //iBeacon6
            x=6.6;

        return  x;
    }

    public double determineY(String deviceAddress)
    {
        double y = 0.0;
        if(deviceAddress.trim().contains("ED:7B:9D")) //iBeacon1-origin
            y=0.0;
        else if(deviceAddress.trim().contains("FF:FF:25")) //iBeacon2
            y=1.1;
        else if(deviceAddress.trim().contains("FF:A2:5C")) //iBeacon3
            y=0.0;
        else if(deviceAddress.trim().contains("FF:0F:D9")) //iBeacon4
            y=2.8;
        else if(deviceAddress.trim().contains("E1:F4:AD")) //iBeacon5
            y=0.0;
        else if(deviceAddress.trim().contains("D3:3F:DD")) //iBeacon6
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