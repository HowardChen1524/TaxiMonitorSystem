package com.example.obd_app;

import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.text.SimpleDateFormat;
import java.util.Date;

public class LocationService extends Thread{

    // Log
    private static final String TAG = "LocationService";
    private static final boolean D = true;

    // Location state
    public final static String LOCATION_NORMAL = "Normal";
    public final static String LOCATION_ERROR = "Error";
    private String mLocationState = LOCATION_ERROR;

    // Location
    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;
    private String mLatitude;
    private String mLongitude;

    // Time
    private SimpleDateFormat formatter;
    private Date curDate;
    private String time;

    // Handler
    private Handler mHandler;

    LocationService(GoogleApiClient googleApiClient, Handler locationHandler){
        mGoogleApiClient = googleApiClient;
        mHandler = locationHandler;
    }

    @SuppressLint("SimpleDateFormat")
    @Override
    public void run() {
        while(true) {
            try {
                // 位置取得
                mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
                if (mLastLocation != null) {
                    mLocationState = LOCATION_NORMAL;
                    sendState();
                    mLatitude = String.valueOf(mLastLocation.getLatitude());
                    mLongitude = String.valueOf(mLastLocation.getLongitude());
                } else {
                    mLocationState = LOCATION_ERROR;
                    sendState();
                    Log.d(TAG, "mLastLocation == null");
                }
                // 時間取得
                formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                curDate = new Date(System.currentTimeMillis());
                time = formatter.format(curDate);

                if(D) Log.d(TAG,"Location: " + mLatitude + ", " + mLongitude + " Time: " + time);
                Message msg = mHandler.obtainMessage(MainActivity.SAVE_LOCATION_TIME);
                Bundle bundle = new Bundle();
                bundle.putString("latitude", mLatitude);
                bundle.putString("longitude", mLongitude);
                bundle.putString("time", time);
                msg.setData(bundle);
                mHandler.sendMessage(msg);
                Thread.sleep(1000);
            } catch (SecurityException | InterruptedException e) {
                Log.e(TAG, e.toString());
            }
        }
    }

    private void sendState(){
        Message msg = mHandler.obtainMessage(MainActivity.LOCATION_STATE_CHANGE);
        Bundle bundle = new Bundle();
        bundle.putString("state", mLocationState);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }
}
