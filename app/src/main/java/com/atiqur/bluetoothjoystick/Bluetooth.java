package com.atiqur.bluetoothjoystick;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class Bluetooth {
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
    public boolean disconnecting = false;
    public final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    public ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    public final Handler mHandler;
    private int mState = 0;

    public Bluetooth(Handler handler) {
        mHandler = handler;
    }

    public synchronized void disconnect() {
        disconnecting = true;
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
            Message msg = this.mHandler.obtainMessage(3);
            Bundle bundle = new Bundle();
            bundle.putString("toast", "Disconnected from device");
            msg.setData(bundle);
            mHandler.sendMessage(msg);
        }
        disconnecting = false;
        start();
    }

    public synchronized void start() {
        stop();
    }

    public synchronized void stop() {
        cancelThreads();
        setState(0);
    }

    /* access modifiers changed from: private */
    public void connectionFailed() {
        Message msg = mHandler.obtainMessage(3);
        Bundle bundle = new Bundle();
        bundle.putString("toast", "Unable to connect device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        start();
    }

    /* access modifiers changed from: private */
    public void connectionLost() {
        Message msg = mHandler.obtainMessage(3);
        Bundle bundle = new Bundle();
        bundle.putString("toast", "Disconnected from device");
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        start();
    }

    public synchronized void connect(BluetoothDevice device) {
        if (mState == 1 && mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(1);
    }

    private class ConnectThread extends Thread {
        private final BluetoothDevice mmDevice;
        private final BluetoothSocket mmSocket;

        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            this.mmDevice = device;
            try {
                tmp = device.createRfcommSocketToServiceRecord(Bluetooth.MY_UUID);
            } catch (IOException e) {
                setState(0);
            }
            this.mmSocket = tmp;
        }

        public void run() {
            if (mBluetoothAdapter.isDiscovering()) {
                mBluetoothAdapter.cancelDiscovery();
            }
            try {
                this.mmSocket.connect();
                synchronized (this) {
                    ConnectThread unused = mConnectThread = null;
                }
                connected(this.mmSocket, this.mmDevice);
            } catch (IOException e) {
                try {
                    this.mmSocket.close();
                } catch (IOException e2) {
                    Log.d("Error", e2 + "");
                }
                connectionFailed();
            }
        }

        public void cancel() {
            try {
                this.mmSocket.close();
            } catch (IOException e) {
                Log.d("Error", e + "");
            }
        }
    }

    public synchronized void connected(BluetoothSocket socket, BluetoothDevice device) {
        cancelThreads();
        this.mConnectedThread = new ConnectedThread(socket);
        this.mConnectedThread.start();
        Message msg = this.mHandler.obtainMessage(2);
        Bundle bundle = new Bundle();
        bundle.putString("device_name", device.getName());
        msg.setData(bundle);
        this.mHandler.sendMessage(msg);
        setState(2);
    }

    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final BluetoothSocket mmSocket;

        public ConnectedThread(BluetoothSocket socket) {
            this.mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.d("Error", e + "");
            }
            this.mmInStream = tmpIn;
            this.mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[1024];
            while (true) {
                try {
                    int bytes = this.mmInStream.read(buffer);
                    String receivedMessage = new String(buffer).substring(0, bytes);
                    Message msg = Bluetooth.this.mHandler.obtainMessage(4);
                    Bundle bundle = new Bundle();
                    bundle.putString("bluetooth_message", receivedMessage);
                    bundle.putInt("message_length", bytes);
                    msg.setData(bundle);
                    mHandler.sendMessage(msg);
                } catch (IOException e) {
                    if (disconnecting) {
                        connectionLost();
                    }
                    Bluetooth.this.start();
                    return;
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                this.mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.d("Error", e + "");
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.d("Error", e + "");
            }
        }
    }

    private void cancelThreads() {
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
    }

    /* access modifiers changed from: private */
    public synchronized void setState(int state) {
        mState = state;
        mHandler.obtainMessage(1, state, -1).sendToTarget();
    }

    public synchronized int getState() {
        return mState;
    }

    public void write(byte[] out) {
        synchronized (this) {
            if (mState == 2) {
                ConnectedThread r = this.mConnectedThread;
                r.write(out);
            }
        }
    }
}
