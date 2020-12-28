package com.hybrid.ips.app.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.hybrid.ips.app.R;

import io.realm.Realm;
import io.realm.RealmConfiguration;

public class MainActivity extends AppCompatActivity
{

    private FloatingActionButton fbDevicePosition;
    private FloatingActionButton fbSeeLocation;
    private ConstraintLayout constraintLayout;
    private Realm realm;
    private RealmConfiguration realmConfiguration;
    private ImageView view;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        constraintLayout = (ConstraintLayout) findViewById(R.id.mainConstraint);
        fbDevicePosition=findViewById(R.id.devicePositionButton);
        fbSeeLocation=findViewById(R.id.seeLocationButton);
        view=findViewById(R.id.bleIcon);
        Realm.init(this);
        realmConfiguration = new RealmConfiguration.Builder().allowWritesOnUiThread(true).build();
        realm = Realm.getInstance(realmConfiguration);


        fbSeeLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                //Intent onlineActivity=new Intent(MainActivity.this,BeaconListActivity.class);
                //startActivity(onlineActivity);
            }
        });

        fbDevicePosition.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent devicePositionActivity=new Intent(MainActivity.this, DevicePositionActivity.class);
                startActivity(devicePositionActivity);
            }
        });

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                realm.close();
                Realm.deleteRealm(realmConfiguration);
                realm = Realm.getInstance(realmConfiguration);
                Toast.makeText(MainActivity.this,"DB deleted",Toast.LENGTH_SHORT).show();
            }
        });


        if(isInternetConnected())
        {
            fbDevicePosition.setVisibility(View.VISIBLE);
            fbSeeLocation.setVisibility(View.VISIBLE);
        }
        else
        {
            fbDevicePosition.setVisibility(View.GONE);
            fbSeeLocation.setVisibility(View.GONE);
            Snackbar.make(constraintLayout,R.string.noInternet, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
        }
    }

    public boolean isInternetConnected()
    {
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);

        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
}