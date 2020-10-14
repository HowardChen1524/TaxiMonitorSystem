package com.example.obd_app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class SendLocation extends Thread{

    private static final String TAG = "SendLocation";

    private String mCar_state;
    private String mLocation_state;
    private String mLocation_latitude;
    private String mLocation_longitude;
    private String mCar_save_time;
    private Handler mHandler;

    SendLocation(Handler handler, String car_state, String location_state,
                 String location_latitude, String location_longitude, String car_save_time){
        mHandler = handler;
        mCar_state = car_state;
        mLocation_state = location_state;
        mLocation_latitude = location_latitude;
        mLocation_longitude = location_longitude;
        mCar_save_time = car_save_time;
    }

    public void run() {
        try {
            sendMessage("Monitor people get into or out car");
            String param_car = "car_state=" + URLEncoder.encode(mCar_state, "UTF-8") + "&"
                    + "location_state=" + URLEncoder.encode(mLocation_state, "UTF-8") + "&"
                    + "location_latitude=" + URLEncoder.encode(mLocation_latitude, "UTF-8") + "&"
                    + "location_longitude=" + URLEncoder.encode(mLocation_longitude, "UTF-8") + "&"
                    + "car_save_time=" + URLEncoder.encode(mCar_save_time, "UTF-8");

            // 建立連線
            URL url = new URL("http://140.138.144.161/vehicle_mis_2/php/SendCarLocation.php");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setUseCaches(false);
            connection.connect();

            // 建立輸入流，向指向的URL傳入引數
            DataOutputStream dos = new DataOutputStream(connection.getOutputStream());
            dos.writeBytes(param_car);
            dos.flush();
            dos.close();

            int resultCode = connection.getResponseCode();
            if (HttpURLConnection.HTTP_OK == resultCode) {
                StringBuffer sb = new StringBuffer();
                String readLine;
                BufferedReader responseReader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(),"UTF-8"));
                while ((readLine = responseReader.readLine()) != null) {
                    sb.append(readLine).append("\n");
                }
                responseReader.close();
                Log.d(TAG, sb.toString());
                sendMessage("Save location success");
            }
            else {
                sendMessage("Save location failed");
            }
        }
        catch (IOException e){
            Log.d(TAG, e.toString() );
        }
    }

    private void sendMessage(String str){
        Message msg = mHandler.obtainMessage(MainActivity.SENDLOCATION_MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, str);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }
}
