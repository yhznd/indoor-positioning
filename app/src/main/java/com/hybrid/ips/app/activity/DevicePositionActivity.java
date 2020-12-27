package com.hybrid.ips.app.activity;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.icu.text.SimpleDateFormat;
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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.hybrid.ips.app.Device;
import com.hybrid.ips.app.R;
import com.hybrid.ips.app.adapter.DevicePositionAdapter;
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver;
import com.lemmingapex.trilateration.TrilaterationFunction;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

import io.realm.Realm;

public class DevicePositionActivity extends AppCompatActivity
{
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_BLUETOOTH = 1;
    private static final String TAG = "BLE_Connection";
    private FloatingActionButton scanBLEButton, scanWifiButton, saveLocation;
    private BluetoothLeScanner bluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
    private boolean mScanning;
    private Handler mHandler;
    private static final long SCAN_PERIOD = 3000; //3 second
    private DecimalFormat df2 = new DecimalFormat("#.###");
    private DevicePositionAdapter devicePositionAdapter;
    private RecyclerView recyclerView;
    private ConstraintLayout constraintLayout;
    private String firebaseId;
    private ArrayList<Double> x_coordinateList=new ArrayList<>();
    private ArrayList<Double> y_coordinateList=new ArrayList<>();
    private ArrayList<Double> distanceList=new ArrayList<>();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_position);
        constraintLayout = findViewById(R.id.mainLocationLayout);
        mHandler = new Handler();
        scanBLEButton = findViewById(R.id.scanBLEButton);
        scanWifiButton = findViewById(R.id.scanWifiButton);
        saveLocation= findViewById(R.id.saveLocation);
        recyclerView = findViewById(R.id.recycleViewLocation);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();

        setAll();

        scanBLEButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, R.string.search_devices, Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
                scanLeDevice();


            }
        });

        scanWifiButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, R.string.search_devices, Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
                if(isWifiConnected())
                    getWifiInfo();



            }
        });

        saveLocation.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                if (x_coordinateList.size()!=0 && y_coordinateList.size()!=0 && distanceList.size()!=0)
                    determineLocation();
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


    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode)
        {
            case PERMISSION_REQUEST_COARSE_LOCATION:
                {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED)
                {
                    Log.d(TAG, "Location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this
                    );
                    builder.setTitle(this.getResources().getString(R.string.offline));
                    builder.setMessage(this.getResources().getString(R.string.notPermitted));
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
            builder.setTitle(this.getResources().getString(R.string.accessLocation));
            builder.setMessage(this.getResources().getString(R.string.accessBLE));
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

    @SuppressLint("HardwareIds")
    private void getWifiInfo()
    {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo();
        int numberOfLevels = 5;
        final double distance = WifiManager.calculateSignalLevel(info.getRssi(), numberOfLevels);
        final String currentDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());

        final Device device=new Device(firebaseId,info.getMacAddress(),distance,4.55,0.0,(double)info.getRssi(),currentDate);

        Snackbar.make(constraintLayout, "SSID : " + info.getSSID() +
                        ", RSSI: " + info.getRssi()+
                        ", Distance: "+distance+" m",
                Snackbar.LENGTH_LONG)
                .setAction("Action", null).show();

        Realm realm = Realm.getDefaultInstance();
        realm.executeTransactionAsync(new Realm.Transaction()
        {
            @Override
            public void execute(Realm bgRealm) {
                Device device1 = bgRealm.createObject(Device.class, UUID.randomUUID().toString());
                device1.setMacAddres(device.getMacAddres());
                device1.setDistance(distance);
                device1.setX(4.55);
                device1.setY(0.0);
                device1.setRssi(device.getRssi());
                device1.setCreated(currentDate);
            }
        }, new Realm.Transaction.OnSuccess()
        {
            @Override
            public void onSuccess() {
                // Transaction was a success.
                Toast.makeText(DevicePositionActivity.this,"Wifi information Saved!",Toast.LENGTH_LONG).show();
            }
        }, new Realm.Transaction.OnError() {
            @Override
            public void onError(Throwable error) {
                // Transaction failed and was automatically canceled.
                System.out.print(error.getMessage());
            }
        });

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
                    devicePositionAdapter.addDevice(result.getDevice());
                    devicePositionAdapter.addRssi(result.getDevice(),result.getRssi());
                    devicePositionAdapter.addTxPower(result.getDevice(),-69.0);
                    devicePositionAdapter.notifyDataSetChanged();

                }
            };

    private void setAll()
    {
        createNewUUID();
        devicePositionAdapter = new DevicePositionAdapter(getApplicationContext());
        recyclerView.setAdapter(devicePositionAdapter);

    }

    public void determineLocation()
    {
        createNewUUID();
        Log.d("Alo size", String.valueOf(x_coordinateList.size()));
        Log.d("Alo size", String.valueOf(y_coordinateList.size()));

        double[] x_positions = new double[x_coordinateList.size()];
        double[] y_positions = new double[y_coordinateList.size()];
        double[][] positions = new double[x_coordinateList.size()][y_coordinateList.size()];
        double[] distances = new double[distanceList.size()];

        for(int i=0;i<x_positions.length;i++) //X
        {
            x_positions[i]= x_coordinateList.get(i);
            Log.d("Alo", String.valueOf(x_positions[i]));

        }

        for(int i=0;i<y_positions.length;i++)  //Y
        {
            y_positions[i]= y_coordinateList.get(i);
            Log.d("Alo", String.valueOf(y_positions[i]));
        }
        for (int i = 0; i < positions.length; i++)
        {
            positions[i][0] = x_positions[i];
            positions[0][i] = y_positions[i];
        }

        for(int i=0;i<distances.length;i++) //Distance
        {
            distances[i]= distanceList.get(i);
        }


        try
        {
            NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
            LeastSquaresOptimizer.Optimum optimum = solver.solve();
            double[] centroid = optimum.getPoint().toArray();
            Toast.makeText(this, "X:"+df2.format(centroid[0])+" Y:"+df2.format(centroid[1]),Toast.LENGTH_LONG).show();
            x_coordinateList.clear();
            y_coordinateList.clear();
            distanceList.clear();
        }
        catch (IllegalArgumentException ex)
        {
            ex.printStackTrace();
            Toast.makeText(this,ex.getMessage(),Toast.LENGTH_LONG).show();

        }


    }


    private void createNewUUID()
    {
        firebaseId = UUID.randomUUID().toString();
        SharedPreferences pref = getSharedPreferences("KEY", 0); // 0 - for private mode
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("UUID", firebaseId); // Storing string
        editor.apply();
        Toast.makeText(this, "New ID created", Toast.LENGTH_SHORT).show();
    }

    public void assignVariables(Device device)
    {
        if (device!=null)
        {
            x_coordinateList.add(device.getX());
            y_coordinateList.add(device.getY());
            distanceList.add(device.getDistance());
        }

    }


}
