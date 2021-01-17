package com.hybrid.ips.app.activity;


import android.Manifest;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
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
import com.hybrid.ips.app.R;
import com.hybrid.ips.app.adapter.RealDevicePositionAdapter;
import com.hybrid.ips.app.util.Device;
import com.hybrid.ips.app.util.Location;
import com.hybrid.ips.app.util.RealDevice;
import com.lemmingapex.trilateration.NonLinearLeastSquaresSolver;
import com.lemmingapex.trilateration.TrilaterationFunction;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LevenbergMarquardtOptimizer;
import java.text.DecimalFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import io.realm.Realm;
import io.realm.RealmResults;
import io.realm.Sort;
import io.realm.exceptions.RealmPrimaryKeyConstraintException;

public class RealDevicePositionActivity extends AppCompatActivity
{
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;
    private static final int PERMISSION_REQUEST_BLUETOOTH = 1;
    private static final String TAG = "Device_Connection";
    private final static UUID BATTERY_UUID = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb");
    private final static UUID BATTERY_LEVEL= UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb");
    private FloatingActionButton scanBLEButton, scanWifiButton, saveLocation;
    private BluetoothAdapter mBluetoothAdapter;
    private DecimalFormat df = new DecimalFormat("#.###");
    private RealDevicePositionAdapter realDevicePositionAdapter;
    private RecyclerView recyclerView;
    private ConstraintLayout constraintLayout;
    private String fId;
    private Realm realm;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        realm = Realm.getDefaultInstance();
        setContentView(R.layout.activity_real_device_position);
        constraintLayout = findViewById(R.id.mainLocationLayout);
        scanBLEButton = findViewById(R.id.scanBLEButton);
        scanWifiButton = findViewById(R.id.scanWifiButton);
        saveLocation= findViewById(R.id.saveLocation);
        recyclerView = findViewById(R.id.recycleViewRLocation);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        setAll();

        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())
        {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, PERMISSION_REQUEST_BLUETOOTH);
        }
        else
        {
            mBluetoothAdapter.startDiscovery();
            IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
            registerReceiver(mReceiver, filter);
        }

        scanBLEButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, R.string.search_devices, Snackbar.LENGTH_SHORT)
                        .setAction("Action", null).show();
                if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled())
                {
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, PERMISSION_REQUEST_BLUETOOTH);
                }
                else
                {

                    scanDevice();
                }



            }
        });

        scanWifiButton.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                if(isWifiConnected())
                    getWifiInfo();



            }
        });

        saveLocation.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                determineLocation();
            }
        });

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
        {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
        }

        if (!isLocationPermissionGranted())
        {
            askLocationPermission();
        }


    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode)
        {
            case PERMISSION_REQUEST_BLUETOOTH:
                Log.d(TAG, "Result");
                scanDevice();
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


    private void getWifiInfo()
    {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        WifiInfo info = wifiManager.getConnectionInfo();
        final double distance=calculateDistance(info.getRssi(),info.getFrequency());
        final String currentDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());

        final RealDevice device=new RealDevice(fId,info.getMacAddress(),distance,4.55,0.0,(double)info.getRssi(),100,currentDate);
        realm.executeTransaction(new Realm.Transaction() {
            @Override
            public void execute(Realm bgRealm)
            {
                try
                {
                    RealDevice device1 = bgRealm.createObject(RealDevice.class, UUID.randomUUID().toString());
                    device1.setMeasureId(fId);
                    device1.setMacAddres(device.getMacAddres());
                    device1.setDistance(distance);
                    device1.setX(4.55);
                    device1.setY(0.0);
                    device1.setRssi(device.getRssi());
                    device1.setBatteryLevel(100);
                    device1.setCreatedAt(currentDate);
                    Toast.makeText(RealDevicePositionActivity.this,"Wifi information Saved!",Toast.LENGTH_SHORT).show();
                }
                catch (RealmPrimaryKeyConstraintException ex)
                {
                    Toast.makeText(RealDevicePositionActivity.this,"Wifi information alreayd exists for this ID!",Toast.LENGTH_SHORT).show();

                }


            }
        });


    }

    private boolean isWifiConnected()
    {
        ConnectivityManager cm = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);
        return (cm != null) && (cm.getActiveNetworkInfo() != null) && (cm.getActiveNetworkInfo().getType() == 1);
    }



    private void scanDevice()
    {
        mBluetoothAdapter.startDiscovery();
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(mReceiver, filter);
    }
    // Device scan callback.

    private final BroadcastReceiver mReceiver = new BroadcastReceiver()
    {
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                realDevicePositionAdapter.addDevice(device);
                realDevicePositionAdapter.addBatteryLevel(device,99);
                realDevicePositionAdapter.addRssi(device,intent.getShortExtra(BluetoothDevice.EXTRA_RSSI,Short.MIN_VALUE));
                realDevicePositionAdapter.notifyDataSetChanged();

            }
        }
    };

    private void setAll()
    {
        createNewMeasureID();
        realDevicePositionAdapter = new RealDevicePositionAdapter(getApplicationContext());
        recyclerView.setAdapter(realDevicePositionAdapter);

    }

    public void determineLocation()
    {

        RealDevice lastDevice = realm.where(RealDevice.class).sort("createdAt", Sort.DESCENDING).findFirst();
        String lastUUID=lastDevice.getMeasureId();

        RealmResults<Device> result = realm.where(Device.class).equalTo("measureId",lastUUID).findAll();

        final String currentDate = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        int size = result.size();
        double[][] positions = new double[size][2];
        double[] distances = new double[size];

        for (int i = 0; i < size; i++)
        {
            positions[i][0] = result.get(i).getX();
            positions[i][1] = result.get(i).getY();
        }

        for (int i = 0; i < size; i++)
        {
            distances[i] = result.get(i).getDistance();
        }
        try
        {
            NonLinearLeastSquaresSolver solver = new NonLinearLeastSquaresSolver(new TrilaterationFunction(positions, distances), new LevenbergMarquardtOptimizer());
            LeastSquaresOptimizer.Optimum optimum = solver.solve();
            final double[] centroid = optimum.getPoint().toArray();
            final Location calculatedLocation = new Location(UUID.randomUUID().toString(), fId, centroid[0], centroid[1], currentDate);
            realm.executeTransaction(new Realm.Transaction()
            {
                @Override
                public void execute(Realm bgRealm)
                {
                    try
                    {
                        Location location = bgRealm.createObject(Location.class, UUID.randomUUID().toString());
                        location.setLocationX(calculatedLocation.getLocationX());
                        location.setLocationY(calculatedLocation.getLocationY());
                        location.setMeasureId(fId);
                        location.setCreatedAt(currentDate);
                        Toast.makeText(RealDevicePositionActivity.this, "X:" + df.format(centroid[0]) + " Y:" + df.format(centroid[1]), Toast.LENGTH_LONG).show();
                    }
                    catch (RealmPrimaryKeyConstraintException ex)
                    {
                        Toast.makeText(RealDevicePositionActivity.this, ex.getMessage(), Toast.LENGTH_SHORT).show();

                    }


                }

            });
        } catch (IllegalArgumentException ex) {
            ex.printStackTrace();
            Toast.makeText(this, ex.getMessage(), Toast.LENGTH_LONG).show();

        }
        createNewMeasureID();
    }

    private void createNewMeasureID()
    {
        fId = UUID.randomUUID().toString();
        SharedPreferences pref = getSharedPreferences("KEY", 0); // 0 - for private mode
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("measureId", fId); // Storing string
        editor.apply();
        Toast.makeText(this, "New ID created", Toast.LENGTH_SHORT).show();
    }


    public double calculateDistance(double signalLevelInDb, double freqInMHz) {
        double exp = (27.55 - (20 * Math.log10(freqInMHz)) + Math.abs(signalLevelInDb)) / 20.0;
        return Math.pow(10.0, exp);

        //Resource: https://en.wikipedia.org/wiki/Free-space_path_loss#Free-space_path_loss_in_decibels
    }

    @Override
    protected void onDestroy()
    {
        unregisterReceiver(mReceiver);
        super.onDestroy();
        realm.close();
    }
}
