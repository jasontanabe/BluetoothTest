package com.tanabe.jason.bluetoothtest;

import android.bluetooth.BluetoothDevice;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.HashMap;


public class MainActivity extends ActionBarActivity {

    private Bluetooth mBluetooth;
    private ArrayList<String> mDeviceNames;
    private HashMap<String, BluetoothDevice> mDevices;
    private ListView mBluetoothListView;
    private ArrayAdapter<String> mListAdapter;
    private Button mScanButton;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what) {
                case Bluetooth.SCANNED_BLUETOOTH_DEVICE:
                    BluetoothDevice device = (BluetoothDevice)msg.obj;
                    if (!mBluetooth.getPairedDevices().contains(device)) {
                        Log.d("Scanned Device", bluetoothName(device));
                        mDevices.put(bluetoothName(device), device);
                        mDeviceNames.add(bluetoothName(device));
                        mListAdapter.notifyDataSetChanged();
                    }
                    break;
                case Bluetooth.FINISH_DISCOVERY:
                    Log.d("Finish Discovery", "DONE");
                    break;
                case Bluetooth.START_DISCOVYERY:
                    Log.d("Start Discovery", "DONE");
                    break;
                case Bluetooth.CONNECTING:
                    Log.d("Connecting", "DONE");
                    break;
                case Bluetooth.CONNECTED:
                    Log.d("Connected", "DONE");
                    break;
                case Bluetooth.CONNECTION_FAILED:
                    Log.d("Connection Failed", "DONE");
                    break;
                case Bluetooth.DISCONNECTED:
                    Log.d("Disconnected", "DONE");
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mBluetooth = new Bluetooth(this, mHandler);
        mDeviceNames = new ArrayList<String>();
        mScanButton = (Button) findViewById(R.id.scanButton);
        mScanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mBluetooth.startDiscovery();
            }
        });
        mBluetoothListView = (ListView) findViewById(R.id.bluetoothList);
        mListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mDeviceNames);
        mBluetoothListView.setAdapter(mListAdapter);
        mBluetoothListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final String deviceName = (String) parent.getItemAtPosition(position);
                mBluetooth.connect(mDevices.get(deviceName));
            }
        });
        mDevices = new HashMap<String, BluetoothDevice>();
        Log.d("Bluetooth State", mBluetooth.getState().toString());
        if (mBluetooth.getState().equals(Bluetooth.BluetoothState.DISCONNECTED)) {
            mBluetooth.queryPairedDevices();
            for (BluetoothDevice device : mBluetooth.getPairedDevices()) {
                Log.d("Paired Devices", bluetoothName(device) + "(Paired)");
                mDeviceNames.add(bluetoothName(device) + "(Paired)");
                mDevices.put(bluetoothName(device) + "(Paired)", device);
            }
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBluetooth != null) {
            mBluetooth.disconnect();
        }
    }

    private String bluetoothName(BluetoothDevice device) {
        if (device == null) return null;
        if (device.getName() == null) {
            return device.getAddress();
        } else {
            return device.getName();
        }
    }
}
