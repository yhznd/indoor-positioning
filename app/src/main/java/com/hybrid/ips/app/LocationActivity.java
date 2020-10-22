package com.hybrid.ips.app;


import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class LocationActivity extends AppCompatActivity
{
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_BLUETOOTH = 1;
    private static final String TAG = "BLE_Connection";
    private FloatingActionButton scanButton;
    private BluetoothLeScanner bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
    private boolean mScanning;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 10000;
    private LocationAdapter locationAdapter;
    private RecyclerView recyclerView;
    private ConstraintLayout constraintLayout;
    private FirebaseDatabase database;
    private DatabaseReference myRef;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.location_activity);
        constraintLayout = (ConstraintLayout) findViewById(R.id.mainLocationLayout);
        database=FirebaseDatabase.getInstance();
        myRef=database.getReference("offline_phase/devices/wifi");
        mHandler = new Handler();
        scanButton = findViewById(R.id.scanLocationButton);

        recyclerView = (RecyclerView) findViewById(R.id.recycleViewLocation);
        locationAdapter = new LocationAdapter(getApplicationContext());
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(locationAdapter);

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();


        scanButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, R.string.search_devices, Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
                if(isWifiConnected())
                    getWifiInfos();
                scanLeDevice();

            }
        });

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
        }

        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, PERMISSION_REQUEST_BLUETOOTH);
        }
        if (!isLocationPermissionGranted())
        {
            askLocationPermission();
        }

        if (isWifiConnected())
        {
            getWifiInfos();
        }



    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this
                    );
                    builder.setTitle("Çevrimdışı");
                    builder.setMessage("Bu uygulama için konum servislerine erişim izni sağlanmadı.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }

                    });
                    builder.show();
                }
                return;
            }
        }
    }


    private void askLocationPermission() {
        // Android M Location Permission check
        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
        {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Konum servislerinize erişim");
            builder.setMessage("iBeacon cihazlarının keşfedilebilmesi için konum servislerine erişime izin verin. ");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                public void onDismiss(DialogInterface dialog) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, PERMISSION_REQUEST_COARSE_LOCATION);

                }
            });
            builder.show();
        }

    }


    private boolean isLocationPermissionGranted() {
        return this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void getWifiInfos()
    {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo();
        int numberOfLevels = 5;
        double distance = WifiManager.calculateSignalLevel(info.getRssi(), numberOfLevels);
        myRef.child(info.getSSID());
        myRef.child(info.getSSID()).child("rssi").setValue(info.getRssi());
        myRef.child(info.getSSID()).child("distance").setValue(distance);
        Snackbar.make(constraintLayout, "SSID : " + info.getSSID() + ", RSSI: " + info.getRssi()+", Distance: "+distance+" m", Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();
    }

    private boolean isWifiConnected()
    {
        ConnectivityManager cm = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
        return (cm != null) && (cm.getActiveNetworkInfo() != null) && (cm.getActiveNetworkInfo().getType() == 1);
    }


    private void scanLeDevice() {
        if (!mScanning) {
            // Stops scanning after a pre-defined scan period
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    bluetoothLeScanner.stopScan(leScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            bluetoothLeScanner.startScan(leScanCallback);
        } else
        {
            mScanning = false;
            bluetoothLeScanner.stopScan(leScanCallback);
        }
    }
    // Device scan callback.
    private ScanCallback leScanCallback =
            new ScanCallback() {
                @Override
                public void onScanResult(int callbackType, ScanResult result) 
                {
                    super.onScanResult(callbackType, result);
                    locationAdapter.addDevice(result.getDevice());
                    locationAdapter.addRssi(result.getDevice(),result.getRssi());
                    locationAdapter.addTxPower(result.getDevice(), -69.0);
                    locationAdapter.notifyDataSetChanged();

                }
            };
}
