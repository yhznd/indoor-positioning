package com.hybrid.ips.app;


import android.app.AlertDialog;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.DialogInterface;
import android.icu.text.SimpleDateFormat;
import android.icu.util.Calendar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import java.util.concurrent.TimeUnit;


public class LocationAdapter extends RecyclerView.Adapter<LocationAdapter.ViewHolder> {
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
    private String choosenScenario;

    public LocationAdapter(Context context,String scenario)
    {
        this.context=context;
        deviceList = new ArrayList<BluetoothDevice>();
        hashRssiMap = new HashMap<BluetoothDevice, Double>();
        hashTxPowerMap = new HashMap<BluetoothDevice, Double>();
        database=FirebaseDatabase.getInstance();
        mKalmanFilters=new HashMap<String,KalmanFilter>();
        myRef=database.getReference("offline_phase/devices/ibeacons/"+scenario);
        choosenScenario=scenario;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.location_list, parent, false);
        return new ViewHolder(view);
    }

    public void addDevice(BluetoothDevice device)
    {
        if (!deviceList.contains(device) && device.getName()!=null && device.getName().trim().equals("iTAG"))
        {
            deviceList.add(device);


        }
    }

    public void addRssi(BluetoothDevice device, Integer rssi)
    {
        double smoothedRssi;
        if (deviceList.contains(device))
        {

            if (mKalmanFilters.keySet().contains(device.getAddress())) {
                KalmanFilter mKalman = mKalmanFilters.get(device.getAddress());

                // This will give you a smoothed RSSI value because 'x == lastRssi'
                smoothedRssi = mKalman.applyFilter(rssi);

                // Do what you want with this rssi
            } else {
                KalmanFilter mKalman = new KalmanFilter(KALMAN_R, KALMAN_Q);
                smoothedRssi = mKalman.applyFilter(rssi);
                mKalmanFilters.put(device.getAddress(), mKalman);
            }

            Log.i(TAG, "Old Rssi: " + rssi + "Smooth RSSI: " + smoothedRssi);
            hashRssiMap.put(device, smoothedRssi);
        }

    }

    public void addTxPower(BluetoothDevice device, Double txPower) {
        if (deviceList.contains(device)) {
            hashTxPowerMap.put(device, txPower);


        }
    }

    @Override
    public void onBindViewHolder(@NonNull final ViewHolder holder, int position)
    {
        final BluetoothDevice device = deviceList.get(position);
        final String[] locatedArea = new String[1];

        holder.deviceName.setText(context.getString(R.string.ble_device_name,device.getName()));
        holder.deviceRssi.setText(context.getString(R.string.ble_rssi,hashRssiMap.get(device)));
        holder.deviceDistance.setText(context.getString(R.string.ble_distance,df2.format(calculateDistance(
                hashRssiMap.get(device),
                hashTxPowerMap.get(device)))));
        holder.saveButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                final String[] areas;
                if(choosenScenario.equals("scenario1") || choosenScenario.equals("scenario4"))
                    areas = context.getResources().getStringArray(R.array.areas6);
                else
                    areas = context.getResources().getStringArray(R.array.areas4);
             int checkedItem=0; //A1;
             AlertDialog.Builder builder = new AlertDialog.Builder(view.getRootView().getContext());
             builder.setTitle(context.getResources().getString(R.string.whichPosition));
             builder.setSingleChoiceItems(areas, checkedItem, new DialogInterface.OnClickListener()
             {
                 @Override
                 public void onClick(DialogInterface dialogInterface, int which)
                 {
                     switch (which)
                     {
                         case 0: // A1
                             locatedArea[0] = areas[0];
                             break;
                         case 1: // A2
                             locatedArea[0] = areas[1];
                             break;
                         case 2: // A3
                             locatedArea[0] = areas[2];
                             break;
                         case 3: // A4
                             locatedArea[0] = areas[3];
                             break;
                         case 4: // A5
                             locatedArea[0] = areas[4];
                             break;
                         case 5: // A6
                             locatedArea[0] = areas[5];
                             break;
                     }
                 }
             });
             builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener()
             {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        String currentDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());

                        myRef.child(device.getAddress()).child(currentDate).setValue(device.getName());
                        myRef.child(device.getAddress()).child(currentDate).child("area").setValue(locatedArea[0]);
                        myRef.child(device.getAddress()).child(currentDate).child("rssi").setValue(hashRssiMap.get(device));
                        myRef.child(device.getAddress()).child(currentDate).child("tx_power").setValue(hashTxPowerMap.get(device));
                        myRef.child(device.getAddress()).child(currentDate).child("distance").setValue(calculateDistance(hashRssiMap.get(device),
                                hashTxPowerMap.get(device)));
                        dialogInterface.dismiss();
                    }
             });
             builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
             {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i)
                    {
                        dialogInterface.dismiss();
                    }
             });
             AlertDialog dialog = builder.create();
             dialog.show();
            }
        });


    }

    @Override
    public int getItemCount() {
        return deviceList.size();
    }


    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView deviceName;
        TextView deviceRssi;
        TextView deviceDistance;
        Button saveButton;
        public ViewHolder(@NonNull View view) {
            super(view);
            deviceRssi = view.findViewById(R.id.device_rssi);
            deviceName = view.findViewById(R.id.device_name);
            deviceDistance = view.findViewById(R.id.device_distance);
            saveButton=view.findViewById(R.id.saveLocation);
        }
    }


    public double calculateDistance(double rssi,double txPower)
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
    }
}