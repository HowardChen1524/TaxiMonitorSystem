package com.example.obd_app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

public class GetCarType extends Thread {

    // Log
    private static final String TAG = "GetCarType";
    private static final boolean D = true;

    // Bluetooth device set & MySQL Car type set
    private ArrayList<String> mCarTypeData_type = new ArrayList<>();
    private ArrayList<String> mCarTypeData_doorID = new ArrayList<>();
    private ArrayList<String> mCarTypeData_doorClosed = new ArrayList<>();
    private Handler mHandler;

    GetCarType(Handler handler){
        mHandler = handler;
    }

    public void run() {
        try {
            mCarTypeData_type.clear();
            mCarTypeData_doorID.clear();
            mCarTypeData_doorClosed.clear();
            URL url = new URL("http://140.138.144.161/vehicle_mis_2/php/GetCarType.php");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true); // 允許輸出
            connection.setDoInput(true); // 允許讀入
            connection.setUseCaches(false); // 不使用快取
            connection.connect(); // 開始連線

            int responseCode = connection.getResponseCode();
            if(responseCode == HttpURLConnection.HTTP_OK){
                BufferedReader bufReader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), "utf-8"));
                String line;
                while(true) {
                    line = bufReader.readLine();
                    if(line == null) break;
                    mCarTypeData_type.add(line);
                    line = bufReader.readLine();
                    if(line == null) break;
                    mCarTypeData_doorID.add(line);
                    line = bufReader.readLine();
                    if(line == null) break;
                    mCarTypeData_doorClosed.add(line);
                }

                Message msg = mHandler.obtainMessage(MainActivity.GET_CAR_DATA);
                Bundle bundle = new Bundle();
                bundle.putStringArray("car_type", mCarTypeData_type.toArray(new String[0]));
                bundle.putStringArray("car_door_id", mCarTypeData_doorID.toArray(new String[0]));
                bundle.putStringArray("car_door_closed", mCarTypeData_doorClosed.toArray(new String[0]));
                msg.setData(bundle);
                mHandler.sendMessage(msg);
            }
        } catch(Exception e) {
            Log.e(TAG, e.toString()); // 回傳錯誤訊息
        }
    }
}
