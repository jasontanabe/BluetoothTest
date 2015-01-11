package com.tanabe.jason.bluetoothtest;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

/**
 * Created by jason on 1/9/2015.
 */
public class Bluetooth {
    public enum BluetoothState { NO_BLUETOOTH, BLUETOOTH_OFF, DISCONNECTED, CONNECTING, CONNECTED };

    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    public static final int SCANNED_BLUETOOTH_DEVICE = 0;
    public static final int START_DISCOVYERY = 1;
    public static final int FINISH_DISCOVERY = 2;
    public static final int CONNECTING = 3;
    public static final int CONNECTION_FAILED = 4;
    public static final int CONNECTED = 5;
    public static final int DISCONNECTED = 6;

    private BluetoothAdapter mAdapter;
    private BluetoothState mState;
    private Context mContext;
    private BroadcastReceiver mReceiver;
    private Set<BluetoothDevice> mPairedDevices;
    private BluetoothDevice mConnectedDevice;
    private Handler mHandler;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    public Bluetooth(Context context, Handler handler) {
        mContext = context;
        mHandler = handler;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mAdapter == null) {
            mState = BluetoothState.NO_BLUETOOTH;
        } else if (!mAdapter.isEnabled()) {
            mState = BluetoothState.BLUETOOTH_OFF;
        } else {
            mState = BluetoothState.DISCONNECTED;
        }
        mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    mHandler.obtainMessage(SCANNED_BLUETOOTH_DEVICE, device).sendToTarget();
                } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                    mHandler.obtainMessage(FINISH_DISCOVERY).sendToTarget();
                } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                    mHandler.obtainMessage(START_DISCOVYERY).sendToTarget();
                } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                    disconnected();
                } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(action)) {
                    connected();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);
        mContext.registerReceiver(mReceiver, filter);
    }

    public BluetoothState getState() {
        return mState;
    }

    public Set<BluetoothDevice> getPairedDevices() {
        return mPairedDevices;
    }

    public boolean arePairedDevices() {
        if (mPairedDevices != null) {
            return mPairedDevices.size() > 0;
        } else {
            return false;
        }
    }

    public void enableBT() {
        if (mAdapter != null) {
            mAdapter.enable();
        }
    }

    public boolean queryPairedDevices() {
        mPairedDevices = mAdapter.getBondedDevices();
        return arePairedDevices();
    }

    public void startDiscovery() {
        if (mAdapter.isDiscovering()) {
            mAdapter.cancelDiscovery();
        }
        mAdapter.startDiscovery();
    }

    public void cancelDiscovery() {
        if (mAdapter.isDiscovering()) {
            mAdapter.cancelDiscovery();
        }
    }

    public void connect(BluetoothDevice device) {
        // turn off discovery b/c it takes up a lot of resources
        if (mAdapter.isDiscovering()) {
            mAdapter.cancelDiscovery();
        }
        // if we're connecting to something, cancel it
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        // if we're connected to to something, disconnect
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        // start connecting to device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        mState = BluetoothState.CONNECTING;
        mHandler.obtainMessage(CONNECTING).sendToTarget();
        mConnectedDevice = device;
    }

    public void disconnect() {
        // cancel any current connections
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        mContext.unregisterReceiver(mReceiver);
        mReceiver = null;
        mState = BluetoothState.DISCONNECTED;
    }

    public void disableBT() {
        if (mAdapter != null) {
            mAdapter.disable();
        }
    }


    private void handleConnectedThread(BluetoothSocket socket) {
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();
    }

    private void connected() {
        mState = BluetoothState.CONNECTED;
        mHandler.obtainMessage(CONNECTED).sendToTarget();
    }

    private void connectionFailed() {
        mState = BluetoothState.DISCONNECTED;
        mConnectedDevice = null;
        mHandler.obtainMessage(CONNECTION_FAILED).sendToTarget();
    }

    private void disconnected() {
        mHandler.obtainMessage(DISCONNECTED).sendToTarget();
        mState = BluetoothState.DISCONNECTED;
        mConnectedDevice = null;
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                connectionFailed();
                return;
            }

            // Do work to manage the connection (in a separate thread)
            handleConnectedThread(mmSocket);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }

    private class ConnectedThread extends Thread
    {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;

        public ConnectedThread(BluetoothSocket socket)
        {
            mmSocket = socket;
            InputStream tmpIn = null;

            // Get the BluetoothSocket input stream
            try
            {
                tmpIn = socket.getInputStream();
            }
            catch (IOException e)
            {
            }

            mmInStream = tmpIn;
        }

        public void run()
        {
            // implement how to read data and what to send to
            /*
            Scanner scan = new Scanner(new InputStreamReader(mmInStream));
            scan.useDelimiter("d");
            double[] values = new double[512];
            String data = "";

            // Keep listening to the InputStream while connected
            while (true)
            {
                try
                {
                    data = scan.next();
                    String[] tokens = data.split("p|m|b|h");

                    if(tokens.length == 4)
                    {
                        for (int i = 0; i < values.length-1; ++i)
                        {
                            values[i] = values[i+1];
                        }
                        try
                        {
                            values[values.length-1] = Double.parseDouble(tokens[0]);
                        }
                        catch(NumberFormatException nfe) { }
                        catch(NullPointerException npe) { }

                        mHandler.obtainMessage(MESSAGE_PHASE, values).sendToTarget();
                        mHandler.obtainMessage(MESSAGE_MAG, tokens[1]).sendToTarget();
                        mHandler.obtainMessage(MESSAGE_BR, tokens[2]).sendToTarget();
                        mHandler.obtainMessage(MESSAGE_HR, tokens[3]).sendToTarget();
                    }
                }
                catch (IllegalStateException ise)  { }
                catch (NoSuchElementException nsee) { }
            }
            */
        }

        public void cancel()
        {
            try
            {
                mmInStream.close();
                mmSocket.close();
                mState = BluetoothState.DISCONNECTED;
                mConnectedDevice = null;
            }
            catch (IOException e){ }
            catch (Exception e) { }
        }
    }

}
