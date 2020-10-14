package com.example.obd_app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
public class BluetoothChatService {

    //Log
    private static final String TAG = "BluetoothChatService";
    private static final boolean D = true;

    //UUID
    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String NAME = "Bluetooth";

    //Bluetooth連線狀態
    public static final int STATE_NONE = 0;
    public static final int STATE_LISTEN = 1;
    public static final int STATE_CONNECTING = 2;
    public static final int STATE_CONNECTED = 3;
    private int mState;

    //Bluetooth member
    private BluetoothAdapter mAdapter;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private ArrayList<String> mOBDAdapterName_list = new ArrayList<>();
    private ArrayList<String> mOBDAdapterAddr_list = new ArrayList<>();

    // Handler
    private Handler mHandler;

    BluetoothChatService(Handler handler) {
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        mHandler = handler;
        mState = STATE_NONE;
    }

    /*-----狀態相關--------------------------------------------------------------------------------*/
    public synchronized BluetoothAdapter getBluetoothAdapter(){
        return mAdapter;
    }

    private synchronized void setState(int state) {
        if (D) Log.i(TAG, "State change:  " + mState + " -> " + state);
        mState = state;
        mHandler.obtainMessage(MainActivity.BLUETOOTH_STATE_CHANGE, state, -1).sendToTarget();
    }

    public synchronized int getState() {
        return mState;
    }

    /*-----連線相關--------------------------------------------------------------------------------*/
    public synchronized void start() {
        if (D) Log.d(TAG, "Start listen");

        if (mConnectThread != null) { mConnectThread.cancel(); mConnectThread = null; }
        if (mConnectedThread != null) { mConnectedThread.cancel(); mConnectedThread = null; }
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
        }

        setState(STATE_LISTEN);
    }

    public synchronized void connect(BluetoothDevice device) {
        if (D) Log.d(TAG, "Start connect to: " + device);

        if (mState == STATE_CONNECTING)
            if (mConnectThread != null) { mConnectThread.cancel(); mConnectThread = null; }
        if (mConnectedThread != null) { mConnectedThread.cancel(); mConnectedThread = null; }
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();

        setState(STATE_CONNECTING);
    }

    public synchronized void connected(BluetoothSocket socket) {
        if (D) Log.d(TAG, "Connected");

        if (mConnectThread != null) { mConnectThread.cancel(); mConnectThread = null; }
        if (mConnectedThread != null) { mConnectedThread.cancel(); mConnectedThread = null; }
        if (mAcceptThread != null) { mAcceptThread.cancel(); mAcceptThread = null; }
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        setState(STATE_CONNECTED);
    }

    public synchronized void stop() {
        if (D) Log.d(TAG, "Stop, all threads are close");

        if (mConnectThread != null) {mConnectThread.cancel(); mConnectThread = null;}
        if (mConnectedThread != null) {mConnectedThread.cancel(); mConnectedThread = null;}
        if (mAcceptThread != null) {mAcceptThread.cancel(); mAcceptThread = null;}

        setState(STATE_NONE);
    }

    private void connectionFailed() {
        setState(STATE_LISTEN);
        Message msg = mHandler.obtainMessage(MainActivity.BLUETOOTH_MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    private void connectionLost() {
        setState(STATE_LISTEN);
        Message msg = mHandler.obtainMessage(MainActivity.BLUETOOTH_MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(MainActivity.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
    }

    private class AcceptThread extends Thread {
        private final BluetoothServerSocket mmServerSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            try {
                tmp = mAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
            } catch (IOException e) {
                Log.e(TAG, "listen() failed", e);
            }
            mmServerSocket = tmp;
        }

        public void run() {
            if (D) Log.d(TAG, "BEGIN mAcceptThread" + this);
            setName("AcceptThread");
            BluetoothSocket socket;

            while (mState != STATE_CONNECTED) {
                try {
                    socket = mmServerSocket.accept(); //是個blocking call，如果沒有主動連線就被動聆聽連線
                } catch (IOException e) {
                    Log.e(TAG, "accept() failed", e);
                    break;
                }
                if (socket != null) {
                    synchronized (BluetoothChatService.this) {
                        switch (mState) {
                            case STATE_NONE:
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                connected(socket);
                                break;
                            case STATE_CONNECTED:
                                try {
                                    socket.close(); // 尚未準備就緒或已連接，終止Socket
                                } catch (IOException e) {
                                    Log.e(TAG, "Could not close unwanted socket", e);
                                }
                                break;
                        }
                    }
                }
            }
            if (D) Log.d(TAG, "END mAcceptThread");
        }

        public void cancel() {
            if (D) Log.d(TAG, "cancel " + this);
            try {
                mmServerSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of server failed", e);
            }
        }
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID); // 取得該BluetoothDevice的BluetoothSocket
            } catch (IOException e) {
                Log.e(TAG, "Create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");
            setName("ConnectThread");

            mAdapter.cancelDiscovery(); //Always取消discovery因為它會降低連線速度
            try {
                mmSocket.connect(); //是個blocking call，只有連接成功或出現異常才會return
            } catch (IOException e) {
                connectionFailed();
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                BluetoothChatService.this.start(); // 回到listen mode
                return;
            }

            synchronized (BluetoothChatService.this) {
                mConnectThread = null; // 做完砍掉ConnectThread
            }
            connected(mmSocket);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            mHandler.obtainMessage(MainActivity.BLUETOOTH_OBD_START).sendToTarget();// 連線中，開始下PIDs和AT Commands
        }

        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /*-----OBD相關--------------------------------------------------------------------------------*/
    public synchronized void writeCommand(String command){
        mConnectedThread.write(command.getBytes());
    }

    public synchronized String getOBDResponse(){
        byte[] buffer = new byte[512];
        int bytesNum;
        String str = "";
        try {
            bytesNum = mConnectedThread.mmInStream.read(buffer);
            str = new String(buffer, 0, bytesNum);
            if (D) Log.d(TAG, str);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return str;
    }

    /*-----其他相關--------------------------------------------------------------------------------*/
    public void setBluetoothBondedDevices() {
        mOBDAdapterName_list.clear();
        mOBDAdapterAddr_list.clear();
        Set<BluetoothDevice> mBluetoothDeviceList = mAdapter.getBondedDevices();
        if (mBluetoothDeviceList.size() > 0)
            for (BluetoothDevice device : mBluetoothDeviceList) {
                mOBDAdapterName_list.add(device.getName());
                mOBDAdapterAddr_list.add(device.getAddress());
            }
    }

    public synchronized String[] getOBDAdapterName_list() {
        return mOBDAdapterName_list.toArray(new String[0]);
    }

    public synchronized String[] getOBDAdapterAddr_list() {
        return mOBDAdapterAddr_list.toArray(new String[0]);
    }

}
