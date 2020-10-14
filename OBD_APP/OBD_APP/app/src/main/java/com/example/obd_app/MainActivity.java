package com.example.obd_app;

import android.Manifest;
import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener{

    // Log
    private static final String TAG = "Main";
    private static final boolean D = true; // 設為false能清除所有Log

    // UI
    Button mCarState_Btn;
    TextView mOBDConnectState_Tv;
    TextView mOBDTemperature_Tv;
    TextView mOBDSpeed_Tv;
    TextView mOBDRpm_Tv;
    TextView mOBDO2_Tv;
    TextView mOBDMaf_Tv;

    // Global
    private APPSetting mAPPSetting;

    // Bluetooth
    public static final String TOAST = "toast"; // BluetoothChatService Toast時需傳送的參數
    public static final int REQUEST_ENABLE_BT = 1; // Intent request codes
    private BluetoothChatService mBluetoothChatService;

    // OBD
    private OBDChatService mOBDChatService;
    private AlertDialog mOBDMonitorDialog;

    // Location
    public static final int REQUEST_FINE_LOCATION_PERMISSION = 102;
    private GoogleApiClient mGoogleApiClient;
    private LocationService mLocationService;

    // Handler
    private Handler mBluetoothHandler = new BluetoothHandler();
    public static final int BLUETOOTH_STATE_CHANGE = 0;
    public static final int BLUETOOTH_OBD_START = 1;
    public static final int BLUETOOTH_MESSAGE_TOAST = 2;

    private Handler mOBDHandler = new OBDHandler();
    public static final int OBD_STATE_CHANGE = 0;
    public static final int OBD_DATA_CHANGE = 1;
    public static final int OBD_DOOR_MONITOR = 2;
    public static final int OBD_SEND_LOCATION = 3;
    public static final int OBD_MESSAGE_TOAST = 4;

    private Handler mLocationHandler = new LocationHandler();
    public static final int LOCATION_STATE_CHANGE = 0;
    public static final int SAVE_LOCATION_TIME = 1;

    private Handler mGetCarTypeHandler = new GetCarTypeHandler();
    public static final int GET_CAR_DATA = 0;

    private Handler mSendLocationHandler = new SendLocationHandler();
    public static final int SENDLOCATION_MESSAGE_TOAST = 0;

    // MySQL
    private GetCarType mGetCarType;

    /*-----Life Cycle-----------------------------------------------------------------------------*/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (D) Log.d(TAG, "+++ ON CREATE +++");

        //UI連接
        mCarState_Btn       = findViewById(R.id.carState);
        mOBDConnectState_Tv = findViewById(R.id.connectStateTv);
        mOBDTemperature_Tv  = findViewById(R.id.tempTv);
        mOBDSpeed_Tv        = findViewById(R.id.speedTv);
        mOBDRpm_Tv          = findViewById(R.id.rpmTv);
        mOBDO2_Tv           = findViewById(R.id.O2Tv);
        mOBDMaf_Tv          = findViewById(R.id.mafTv);

        mCarState_Btn.setOnClickListener(carStateListener);

        // 取得全域變數
        mAPPSetting = (APPSetting)getApplication();

        // 實例化BluetoothChatService & 確認藍牙是否支援
        mBluetoothChatService = new BluetoothChatService(mBluetoothHandler);
        Log.d(TAG, "Setup BluetoothChatService()");
        if(!checkBluetoothSupport()) {
            if (D) Log.e(TAG, "This device not support Bluetooth...");
            finish();
            return;
        }

        // MySQL抓取車種資料
        if(mGetCarType == null) {
            mGetCarType = new GetCarType(mGetCarTypeHandler);
            mGetCarType.start();
        }

        //確認位置權限狀態，如果沒有授權使用定位則詢問使用者開啟權限，首次安裝會閃退
        requestLocationPermission();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (D) Log.d(TAG, "++ ON START ++");

        // 藍牙未開啟則彈出視窗確認
        while (!mBluetoothChatService.getBluetoothAdapter().isEnabled())
            startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);

        // 取得藍牙已配對裝置
        mBluetoothChatService.setBluetoothBondedDevices();
        mAPPSetting.setOBDAdapterNameList(mBluetoothChatService.getOBDAdapterName_list());
        mAPPSetting.setOBDAdapterAddrList(mBluetoothChatService.getOBDAdapterAddr_list());

        // 連線到Google Play services
        mGoogleApiClient.connect();
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (D) Log.d(TAG, "+ ON RESUME +");
        // 開始進行Listen
        if (mBluetoothChatService.getState() == BluetoothChatService.STATE_NONE) {
            mBluetoothChatService.start();
        }
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if (D) Log.d(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if (D) Log.d(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mGetCarType != null) mGetCarType = null;
        if (mOBDChatService != null) mOBDChatService.stop();
        if (mBluetoothChatService != null) mBluetoothChatService.stop();
        if (mLocationService != null) mLocationService = null; mGoogleApiClient.disconnect();
        if (D) Log.d(TAG, "--- ON DESTROY ---");
    }

    @Override
    //藍牙未開啟時，會跳到Second Activity(選擇框)，選擇之後會跳回First Activity
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (D) Log.d(TAG, "onActivityResult " + resultCode);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                showToast("Bluetooth is open！");
            } else
                showToast("Could't open Bluetooth...");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // 設置要用哪個menu檔做為選單
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // 取得點選項目的id
        int id = item.getItemId();

        // 依照id判斷點了哪個項目並做相應事件
        if (id == R.id.action_connect) {
            BluetoothDevice mBluetoothDevice;
            if (!mBluetoothChatService.getBluetoothAdapter().isEnabled())
                startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE), REQUEST_ENABLE_BT);
            else {
                mBluetoothDevice = mBluetoothChatService.getBluetoothAdapter().
                        getRemoteDevice(mAPPSetting.getOBDAdapterAddrList()[mAPPSetting.getOBDAdapter()]);
                mBluetoothChatService.connect(mBluetoothDevice);
                showToast("Connecting to OBDII device......");
            }
            return true;
        }
        if (id == R.id.action_settings) {
            startActivity(new Intent().setClass(MainActivity.this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /*-----Button Event---------------------------------------------------------------------------*/

    private Button.OnClickListener carStateListener= new Button.OnClickListener() {
        @Override
        public void onClick(View view) {
            if (mOBDChatService != null) {
              mOBDChatService.changeCarState();
            }
        }
    };

    /*-----Bluetooth------------------------------------------------------------------------------*/
    private boolean checkBluetoothSupport(){
        return mBluetoothChatService.getBluetoothAdapter() != null;
    }

    @SuppressLint("HandlerLeak")
    class BluetoothHandler extends Handler{
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage (Message msg){
            switch (msg.what) {
                case BLUETOOTH_STATE_CHANGE:
                    if (D) Log.i(TAG, "STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothChatService.STATE_NONE:
                            mOBDConnectState_Tv.setText(R.string.disconnected);
                            break;
                        case BluetoothChatService.STATE_LISTEN:
                            mOBDConnectState_Tv.setText(R.string.listening);
                            break;
                        case BluetoothChatService.STATE_CONNECTING:
                            mOBDConnectState_Tv.setText(R.string.connecting);
                            break;
                        case BluetoothChatService.STATE_CONNECTED:
                            mOBDConnectState_Tv.setText(R.string.connected);
                            break;
                    }
                    break;
                case BLUETOOTH_MESSAGE_TOAST:
                    showToast(msg.getData().getString(TOAST));
                    break;
                case BLUETOOTH_OBD_START:
                    setupOBDChatService();
                    mOBDChatService.setOBDData();
                    if (mLocationService == null) {
                        mLocationService = new LocationService(mGoogleApiClient, mLocationHandler);
                        mLocationService.start();
                    }
                    break;
                default:
                    break;
            }
        }
    }

    /*-----OBD------------------------------------------------------------------------------------*/
    private void setupOBDChatService() {
        Log.d(TAG, "Setup OBDChatService()");
        mOBDChatService = new OBDChatService(
                        mBluetoothChatService,
                        mOBDHandler,
                        mAPPSetting.getCarDoorIDList()[mAPPSetting.getCar()],
                        mAPPSetting.getCarDoorClosedList()[mAPPSetting.getCar()]);
        createMonitorDialog();
    }

    private void createMonitorDialog(){
        ImageView image = new ImageView(MainActivity.this);
        image.setImageResource(R.drawable.icon_search);
        AlertDialog.Builder mOBDAdapter_builder = new AlertDialog.Builder(MainActivity.this)
                .setTitle("Monitoring CAN mode")
                .setIcon(R.drawable.icon_warning)
                .setView(image);
        mOBDAdapter_builder.setPositiveButton("Close", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        mOBDMonitorDialog = mOBDAdapter_builder.create();
    }

    @SuppressLint("HandlerLeak")
    class OBDHandler extends Handler{
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage (Message msg){
            switch (msg.what) {
                case OBD_STATE_CHANGE:
                    mAPPSetting.setCarState(msg.getData().getString("state"));
                    mCarState_Btn.setText(msg.getData().getString("state"));
                    break;
                case OBD_DATA_CHANGE:
                    mOBDTemperature_Tv.setText(msg.getData().getString("Temperature"));
                    mOBDSpeed_Tv.setText(msg.getData().getString("Speed"));
                    mOBDRpm_Tv.setText(msg.getData().getString("Rpm"));
                    mOBDO2_Tv.setText(msg.getData().getString("O2Sensor"));
                    mOBDMaf_Tv.setText(msg.getData().getString("Maf"));
                    break;
                case OBD_DOOR_MONITOR:
                    mOBDMonitorDialog.show();
                    showToast("Start to monitor CAN");
                    break;
                case OBD_SEND_LOCATION:
                    SendLocation mSendLocation = new SendLocation(
                            mSendLocationHandler,
                            mAPPSetting.getCarState(),
                            mAPPSetting.getLocationState(),
                            mAPPSetting.getLatitude(),
                            mAPPSetting.getLongitude(),
                            mAPPSetting.getTime());
                    mSendLocation.start();
                    break;
                case OBD_MESSAGE_TOAST:
                    showToast("OBD: " + msg.getData().getString(TOAST));
                    break;
                default:
                    break;
            }
        }
    }

    /*-----Location-------------------------------------------------------------------------------*/
    private void createGoogleApiClient(){
        if (mGoogleApiClient == null) {
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .addConnectionCallbacks(this)
                    .addOnConnectionFailedListener(this)
                    .addApi(LocationServices.API)
                    .build();
        }
    }

    private void requestLocationPermission(){
        //如果裝置版本是6.0（包含）以上
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //取得授權狀態，參數是請求授權的名稱
            int hasPermission = checkSelfPermission(
                    android.Manifest.permission.ACCESS_FINE_LOCATION);

            //如果未授權
            if (hasPermission != PackageManager.PERMISSION_GRANTED) {
                //請求授權，第一個參數是請求授權的名稱，第二個參數是請求代碼
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        REQUEST_FINE_LOCATION_PERMISSION);
            }
            else {
                //實體化一個GoogleAPIClient.
                createGoogleApiClient();
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "onConnected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended: " + i);
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed: \n" + connectionResult.toString());
    }

    @SuppressLint("HandlerLeak")
    class LocationHandler extends Handler{
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage (Message msg) {
            switch (msg.what) {
                case LOCATION_STATE_CHANGE:
                    mAPPSetting.setLocationState(msg.getData().getString("state"));
                    break;
                case SAVE_LOCATION_TIME:
                    if(mAPPSetting.getLocationState().equals(LocationService.LOCATION_NORMAL)) {
                        mAPPSetting.setLatitude(msg.getData().getString("latitude"));
                        mAPPSetting.setLongitude(msg.getData().getString("longitude"));
                        mAPPSetting.setTime(msg.getData().getString("time"));
                        if(D) Log.d(TAG, "Set location & time success");
                    }
                    else
                        if(D) Log.d(TAG, "Set location & time error");
                    break;
                default:
                    break;
            }
        }
    }

    /*-----MySQL----------------------------------------------------------------------------------*/
    @SuppressLint("HandlerLeak")
    class GetCarTypeHandler extends Handler{
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage (Message msg){
            if (msg.what == GET_CAR_DATA) {
                mAPPSetting.setCarTypeList(msg.getData().getStringArray("car_type"));
                mAPPSetting.setCarDoorIDList(msg.getData().getStringArray("car_door_id"));
                mAPPSetting.setCarDoorClosedList(msg.getData().getStringArray("car_door_closed"));
                Log.d(TAG, "Get Car type success");
            }
        }
    }

    @SuppressLint("HandlerLeak")
    class SendLocationHandler extends Handler {
        @SuppressLint("SetTextI18n")
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == SENDLOCATION_MESSAGE_TOAST) {
                showToast(msg.getData().getString(TOAST));
            }
        }
    }

    /*-----Other----------------------------------------------------------------------------------*/
    private void showToast(String msg){
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

}
