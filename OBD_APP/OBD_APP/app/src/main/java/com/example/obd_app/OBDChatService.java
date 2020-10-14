package com.example.obd_app;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.text.DecimalFormat;
import java.util.Timer;
import java.util.TimerTask;

public class OBDChatService {

    //Log
    private static final String TAG = "OBDChatService";
    private static final boolean D = true;

    //資料格式設定
    private DecimalFormat format_rpm = new DecimalFormat("#####.##");
    private DecimalFormat format_O2Sensor = new DecimalFormat("#.###");
    private DecimalFormat format_maf = new DecimalFormat("###.##");

    // Car state
    public final static String CAR_VACANT = "Vacant";
    public final static String CAR_HIRED = "Hired";
    private String mCarState = CAR_VACANT;

    //OBD PIDs & AT Commands
    public static final String AT_Z = "atz\r";
    public static final String AT_D = "atd\r";
    public static final String AT_SP0 = "atsp0\r";//test sp0
    public static final String AT_E0 = "ate0\r";
    public static final String AT_CF = "atcf";
    private String CAR_DOOR_ID;
    private String CAR_DOOR_CLOSED;
    public static final String AT_H1 = "ath1\r";
    public static final String AT_MA = "atma\r";
    public static final String PID_TEMPERATURE = "0105\r";
    public static final String PID_SPEED = "010d\r";
    public static final String PID_RPM = "010c\r";
    public static final String PID_O2SENSOR = "0114c\r";
    public static final String PID_MAF = "0110\r";

    // Bluetooth
    private BluetoothChatService mBluetoothChatService;

    // Handler
    private Handler mHandler;

    // Thread
    private AmodeThread mAmodeThread;
    private BmodeThread mBmodeThread;

    public OBDChatService(BluetoothChatService bluetoothChatService, Handler handler, String car_door_id, String car_door_closed) {
        mBluetoothChatService = bluetoothChatService;
        mHandler = handler;
        CAR_DOOR_ID = car_door_id;
        CAR_DOOR_CLOSED = car_door_closed;
    }

    public void changeCarState() {
        if (mCarState.equals(CAR_VACANT)) {
            mCarState = CAR_HIRED;
            sendState();
        }
        else {
            mCarState = CAR_VACANT;
            sendState();
        }
    }

    public void setOBDData() {
        mAmodeThread = new AmodeThread();
        mAmodeThread.start();
    }

    private void setDefaultOBD(){
        try {
            mBluetoothChatService.writeCommand(AT_D);
            Thread.sleep(50);
            if (!checkResponse())
                if (D) Log.d(TAG, "response error");

            mBluetoothChatService.writeCommand(AT_Z);
            Thread.sleep(1000);
            if (!checkResponse())
                if (D) Log.d(TAG, "response error");

            mBluetoothChatService.writeCommand(AT_SP0);
            Thread.sleep(50);
            if (!checkResponse())
                if (D) Log.d(TAG, "response error");

            mBluetoothChatService.writeCommand(AT_E0);
            Thread.sleep(50);
            if (!checkResponse())
                if (D) Log.d(TAG, "response error");

            mBluetoothChatService.writeCommand(AT_H1);
            Thread.sleep(50);
            if (!checkResponse())
                if (D) Log.d(TAG, "response error");

        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean checkResponse(){
        String re_str = mBluetoothChatService.getOBDResponse();
        return re_str.contains(">");
    }

    public void stop() {
        if(mAmodeThread != null) mAmodeThread = null;
        if(mBmodeThread != null) mBmodeThread = null;
    }

    private void sendState(){
        Message msg = mHandler.obtainMessage(MainActivity.OBD_STATE_CHANGE);
        Bundle bundle = new Bundle();
        bundle.putString("state", mCarState);
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    //A mode: 監測汽車State
    private class AmodeThread extends Thread {

        private final String empty = "NO DATA";
        private StringBuffer temperature = new StringBuffer("NO DATA");
        private StringBuffer speed = new StringBuffer("NO DATA");
        private StringBuffer rpm = new StringBuffer("NO DATA");
        private StringBuffer O2Sensor = new StringBuffer("NO DATA");
        private StringBuffer maf = new StringBuffer("NO DATA");
        private int count = 0;

        public void run() {
            try {
                setDefaultOBD();
                while (true) {
                    mBluetoothChatService.writeCommand(PID_TEMPERATURE);
                    Thread.sleep(50);
                    countTemperature();
                    mBluetoothChatService.writeCommand(PID_RPM);
                    Thread.sleep(50);
                    countRpm();
                    mBluetoothChatService.writeCommand(PID_SPEED);
                    Thread.sleep(50);
                    countSpeed();
                    mBluetoothChatService.writeCommand(PID_O2SENSOR);
                    Thread.sleep(50);
                    countO2Sensor();
                    mBluetoothChatService.writeCommand(PID_MAF);
                    Thread.sleep(50);
                    countMaf();
                    if (!checkSpeedAndRpm())
                        sendData();
                    else {
                        AtoB();
                        break;
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if(D) Log.d(TAG,"Amode is over");
        }

        private void countTemperature() {
            String re_str = mBluetoothChatService.getOBDResponse();
            while (!re_str.contains(">")){
                String temp = mBluetoothChatService.getOBDResponse();
                re_str += temp;
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            int pos = re_str.indexOf("41 05");
            if(pos != -1) {
                String A = re_str.substring(pos + 6, pos + 8);
                int value = Integer.valueOf(A, 16) - 40;
                temperature.replace(0, temperature.length(), Integer.toString(value));
                if (D) Log.d(TAG, "temp: " + temperature);
            }
            else {
                temperature.replace(0, empty.length(), empty);
                if (D) Log.d(TAG, "temp: " + empty);
            }
        }

        private void countSpeed() {
            String re_str = mBluetoothChatService.getOBDResponse();
            while (!re_str.contains(">")){
                String temp = mBluetoothChatService.getOBDResponse();
                re_str += temp;
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            int pos = re_str.indexOf("41 0D");
            if(pos != -1) {
                String A = re_str.substring(pos + 6, pos + 8);
                Integer value = Integer.valueOf(A, 16);
                speed.replace(0, speed.length(), value.toString());
                if (D) Log.d(TAG, "speed: " + speed);
            }
            else {
                speed.replace(0, empty.length(), empty);
                if (D) Log.d(TAG, "speed: " + empty);
            }
        }

        private void countRpm() {
            String re_str = mBluetoothChatService.getOBDResponse();
            while (!re_str.contains(">")){
                String temp = mBluetoothChatService.getOBDResponse();
                re_str += temp;
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            int pos = re_str.indexOf("41 0C");
            if(pos != -1) {
                String A = re_str.substring(pos + 6, pos + 8);
                String B = re_str.substring(pos + 9, pos + 11);
                Integer value_1 = Integer.valueOf(A, 16);
                Integer value_2 = Integer.valueOf(B, 16);
                double value = Double.parseDouble(
                        format_rpm.format((256 * value_1 + value_2) / 4.0D));
                rpm.replace(0, rpm.length(), Double.toString(value));
                if (D) Log.d(TAG, "rpm: " + rpm);
            }
            else {
                rpm.replace(0, empty.length(), empty);
                if (D) Log.d(TAG, "rpm: " + empty);
            }
        }

        private void countO2Sensor() {
            String re_str = mBluetoothChatService.getOBDResponse();
            while (!re_str.contains(">")){
                String temp = mBluetoothChatService.getOBDResponse();
                re_str += temp;
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            int pos = re_str.indexOf("41 14");
            if(pos != -1) {
                String A = re_str.substring(pos + 6, pos + 8);
                Integer value_1 = Integer.valueOf(A, 16);
                double value = Double.parseDouble(format_O2Sensor.format(value_1 / 200.0D));
                O2Sensor.replace(0, O2Sensor.length(), Double.toString(value));
                if (D) Log.d(TAG, "O2Sensor: " + O2Sensor);
            }
            else {
                O2Sensor.replace(0, empty.length(), empty);
                if (D) Log.d(TAG, "O2Sensor: " + empty);
            }
        }

        private void countMaf() {
            String re_str = mBluetoothChatService.getOBDResponse();
            while (!re_str.contains(">")){
                String temp = mBluetoothChatService.getOBDResponse();
                re_str += temp;
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            int pos = re_str.indexOf("41 10");
            if(pos != -1) {
                String A = re_str.substring(pos + 6, pos + 8);
                String B = re_str.substring(pos + 9, pos + 11);
                Integer value_1 = Integer.valueOf(A, 16);
                Integer value_2 = Integer.valueOf(B, 16);
                double value = Double.parseDouble(format_maf.format((256 * value_1 + value_2) / 100.0D));
                maf.replace(0, maf.length(), Double.toString(value));
                if (D) Log.d(TAG, "maf: " + maf);
            }
            else {
                maf.replace(0, empty.length(), empty);
                if (D) Log.d(TAG, "maf: " + empty);
            }
        }

        private void sendData() {
            Message msg = mHandler.obtainMessage(MainActivity.OBD_DATA_CHANGE);
            Bundle bundle = new Bundle();
            bundle.putString("Temperature", String.valueOf(temperature));
            bundle.putString("Speed", String.valueOf(speed));
            bundle.putString("Rpm", String.valueOf(rpm));
            bundle.putString("O2Sensor", String.valueOf(O2Sensor));
            bundle.putString("Maf", String.valueOf(maf));
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }

        private boolean checkSpeedAndRpm() {
            if (speed.toString() != empty && rpm.toString() != empty) {
                int speed_value = Integer.parseInt(String.valueOf(speed));
                double rpm_value = Double.parseDouble(String.valueOf(rpm));
                if (speed_value <= 5 && rpm_value <= 900) {
                    if (count == 12) {
                        count = 0;
                        if (D) Log.d(TAG, "Stop monitor car data and go to B mode");
                        return true;
                    } else {
                        count += 1;
                        return false;
                    }
                } else
                    return false;
            }
            else
                return false;
        }

        private void AtoB() {
            if(mAmodeThread != null) mAmodeThread = null;
            mHandler.obtainMessage(MainActivity.OBD_DOOR_MONITOR).sendToTarget();
            mBmodeThread = new BmodeThread();
            mBmodeThread.start();
        }
    }

    //B mode: 監測汽車車門打開訊號
    private class BmodeThread extends Thread {
        //計時器
        private Timer mTimer;
        private TimerTask mTimerTask;

        //車門狀態
        private int doorState = 0;
        private static final int STATE_CLOSED = 0;
        private static final int STATE_OPENED = 1;

        public void run() {
            try {
                mBluetoothChatService.writeCommand(AT_CF + CAR_DOOR_ID + "\r");
                Thread.sleep(50);
                if (checkResponse()) {
                    mBluetoothChatService.writeCommand(AT_MA);
                    mTimer = new Timer();
                    mTimerTask = new TimerTask (){
                        int count = 10; // 設定時間
                        public void run() {
                            if(count > 0){
                                checkDoorOpen();
                                if(doorState == STATE_CLOSED) {
                                    if (D) Log.d(TAG, "door is close, timer continue");
                                    count--;
                                }
                                else {
                                    if (D) Log.d(TAG, "door is open, timer stop, jump to Amode");
                                    monitorStop();
                                    stopTimer();
                                    sendLocation();
                                    BtoA();
                                }
                                /*monitorStop();
                                stopTimer();
                                sendLocation();
                                BtoA();*/
                            }
                            else{
                                if (D) Log.d(TAG, "1 min no door open, timer stop, jump to Amode");
                                monitorStop();
                                stopTimer();
                                BtoA();
                            }
                        }
                    };
                    mTimer.scheduleAtFixedRate(mTimerTask, 1000L, 1000L);
                }
                else
                if (D) Log.d(TAG, "OBD didn't return OK");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void checkDoorOpen() {
            String re_str = mBluetoothChatService.getOBDResponse();
            if (re_str.contains(CAR_DOOR_CLOSED))
                doorState = STATE_CLOSED;
            else
                doorState = STATE_OPENED;
        }

        private void monitorStop() {
            try {
                mBluetoothChatService.writeCommand("x");
                Thread.sleep(50);
                String re_str = mBluetoothChatService.getOBDResponse();
                while (!re_str.contains(">")){
                    String temp = mBluetoothChatService.getOBDResponse();
                    re_str += temp;
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if(D) Log.d(TAG,re_str + " Bmode is over");
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        private void stopTimer() {
            if (mTimer != null) {
                mTimer.cancel();
                mTimer = null;
            }
            if (mTimerTask != null) {
                mTimerTask.cancel();
                mTimerTask = null;
            }
        }

        private void sendLocation(){
            if(D) Log.d(TAG,"Start to send location");
            mHandler.obtainMessage(MainActivity.OBD_SEND_LOCATION).sendToTarget();
        }

        private void BtoA() {
            if(mBmodeThread != null) mBmodeThread = null;
            mAmodeThread = new AmodeThread();
            mAmodeThread.start();
        }
    }



}
