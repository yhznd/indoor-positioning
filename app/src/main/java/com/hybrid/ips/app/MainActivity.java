package com.hybrid.ips.app;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity
{

    private FloatingActionButton fbOnline;
    private FloatingActionButton fbOffline;
    private ConstraintLayout constraintLayout;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        constraintLayout = (ConstraintLayout) findViewById(R.id.mainConstraint);
        fbOnline=findViewById(R.id.floatingActionButtonOnline);
        fbOffline=findViewById(R.id.floatingActionButtonOffline);

        fbOnline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent onlineActivity=new Intent(MainActivity.this,BeaconListActivity.class);
                startActivity(onlineActivity);
            }
        });

        fbOffline.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view)
            {
                Intent offlineActivity=new Intent(MainActivity.this,LocationActivity.class);
                startActivity(offlineActivity);
            }
        });

        if(isInternetConnected())
        {
            fbOnline.setVisibility(View.VISIBLE);
            fbOffline.setVisibility(View.VISIBLE);
        }
        else
        {
            fbOnline.setVisibility(View.GONE);
            fbOffline.setVisibility(View.GONE);
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