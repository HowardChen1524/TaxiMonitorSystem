package com.example.obd_app;

import android.app.Application;

import java.util.Arrays;

public class APPSetting extends Application {

    // 設定回傳設定回傳list的position
    private int mOBDAdapter_setting;
    private int mCar_setting;

    // 車種list
    private final static String[] empty = {};
    private String[] mOBDAdapterName_list;
    private String[] mOBDAdapterAddr_list;
    private String[] mCarType_list;
    private String[] mCarDoorID_list;
    private String[] mCarDoorClosed_list;

    // 空車?
    private String mCar_state;

    // 經緯度 & 時間
    private String mLocation_state;
    private String mLocation_latitude;
    private String mLocation_longitude;
    private String mTime;

    @Override
    public void onCreate(){
        super.onCreate();
        setOBDAdapter(0);
        setCar(0);
        setOBDAdapterNameList(empty);
        setOBDAdapterAddrList(empty);
        setCarTypeList(empty);
        setCarDoorIDList(empty);
        setCarDoorClosedList(empty);
        setCarState("Vacant");
        setLocationState("Error");
        setLatitude("");
        setLongitude("");
        setTime("");
    }

    public int getOBDAdapter(){
        return this.mOBDAdapter_setting;
    }
    public void setOBDAdapter(int pos){
        this.mOBDAdapter_setting = pos;
    }
    public int getCar(){
        return this.mCar_setting;
    }
    public void setCar(int pos){
        this.mCar_setting = pos;
    }

    public String[] getOBDAdapterNameList(){
        return this.mOBDAdapterName_list;
    }
    public void setOBDAdapterNameList(String[] list){
        this.mOBDAdapterName_list = Arrays.copyOf(list, list.length);
    }
    public String[] getOBDAdapterAddrList(){
        return this.mOBDAdapterAddr_list;
    }
    public void setOBDAdapterAddrList(String[] list){
        this.mOBDAdapterAddr_list = Arrays.copyOf(list, list.length);
    }
    public String[] getCarTypeList(){
        return this.mCarType_list;
    }
    public void setCarTypeList(String[] list){ this.mCarType_list = Arrays.copyOf(list, list.length); }
    public String[] getCarDoorIDList(){
        return this.mCarDoorID_list;
    }
    public void setCarDoorIDList(String[] list){ this.mCarDoorID_list = Arrays.copyOf(list, list.length); }
    public String[] getCarDoorClosedList(){
        return this.mCarDoorClosed_list;
    }
    public void setCarDoorClosedList(String[] list){ this.mCarDoorClosed_list = Arrays.copyOf(list, list.length); }
    public String getCarState(){
        return this.mCar_state;
    }
    public void setCarState(String state){
        this.mCar_state = state;
    }

    public String getLocationState(){
        return this.mLocation_state;
    }
    public void setLocationState(String state){
        this.mLocation_state = state;
    }
    public String getLatitude(){
        return this.mLocation_latitude;
    }
    public void setLatitude(String latitude){
        this.mLocation_latitude = latitude;
    }
    public String getLongitude(){
        return this.mLocation_longitude;
    }
    public void setLongitude(String longitude){
        this.mLocation_longitude = longitude;
    }
    public String getTime(){
        return this.mTime;
    }
    public void setTime(String time){
        this.mTime = time;
    }
}
