package com.example.david.imasam;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.david.imasam.ControllerActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

public class BluetoothChooser extends AppCompatActivity {

    static private boolean isListopen;
    private int REQUEST_ENABLE_BT = 1;
    private boolean found = false;
    ArrayAdapter<String> btArray;
    private ListView devicesfound;

    private static ConnectThread ctThread;
    private static ConnectedThread cTThread;
    public final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    Intent controller = new Intent(this,ControllerActivity.class);

    private TextView chooser;
    private boolean tapped = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Intent controller = new Intent(this, ControllerActivity.class);
        setContentView(R.layout.activity_bluetooth_chooser);
        isListopen = true;
        btArray = new ArrayAdapter<String>(this, R.layout.custom_list_item);
        devicesfound = (ListView)findViewById(R.id.devicesfound);
        devicesfound.setAdapter(btArray);
        chooser = (TextView)findViewById(R.id.choosertitle);

        //int transparent = ContextCompat.getColor(this, android.R.color.transparent);
        //devicesfound.setBackgroundColor(transparent);
        registerReceiver(ActionFoundReceiver, new IntentFilter(BluetoothDevice.ACTION_FOUND));

        new Thread(new Runnable() {
            public void run() {
                int numdots = 0;
                String output;
                mBluetoothAdapter.startDiscovery();
                setText(btArray, "debugmode");
                while (isListopen) {
                    if (mBluetoothAdapter.isDiscovering()){
                        output = "Searching";
                        for (int i = 0; i < numdots; i++ ){
                            output +=".";
                        }
                        if(numdots < 4){
                            numdots++;
                        }else{
                            numdots = 0;
                        }
                        setText(chooser,output);
                    }else{
                        if (tapped == true) {
                            output = "Restarting Search...";
                            mBluetoothAdapter.startDiscovery();
                            clear(btArray);
                            setText(btArray, "debugmode");
                            tapped = false;
                        }else {
                            output = "Searching complete, tap to restart search...";
                        }
                        setText(chooser,output);
                    }

                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();

        chooser.setOnClickListener(new TextView.OnClickListener(){
            @Override
            public void onClick(View v) {
                tapped = true;
            }

        });

        devicesfound.setOnItemClickListener(new AdapterView.OnItemClickListener()
            {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String value = (String)parent.getItemAtPosition(position);
                    valueChecker(value);

                }

                private void valueChecker(String value) {
                    if (value == "debugmode"){
                        ctThread = new ConnectThread(mBluetoothAdapter.getRemoteDevice("00:43:A8:23:10:F0"));

                        startActivity(controller);
                        finish();

                    }
                    value = value.substring(value.length() - 17);
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(value);
                    ctThread = new ConnectThread(device);
                    ctThread.run();
                    cTThread = new ConnectedThread(ctThread.mmSocket);
                    cTThread.start();
                    if (ctThread.mmSocket.isConnected()) {
                        Toast.makeText(getApplicationContext(), "matched success", Toast.LENGTH_SHORT).show();
                        startActivity(controller);
                        finish();
                    }

                    else {
                        Toast.makeText(getApplicationContext(), "Unable to create a connection \n Try again", Toast.LENGTH_LONG).show();
                    }
                }
            }
        );



    }



    private final BroadcastReceiver ActionFoundReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                btArray.add(device.getName() + "\n" + device.getAddress());
                btArray.notifyDataSetChanged();
            }
        }
    };

    private void setText(final ArrayAdapter<String> array, final String text) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                array.add(text);
                array.notifyDataSetChanged();
            }
        });
    }

    private void setText(final TextView textview, final String text) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                textview.setText(text);
            }
        });
    }

    private void clear(final ArrayAdapter<String> array){
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                array.clear();
            }
        });

    }

    @Override
    protected void onPause() {
        // TODO Auto-generated method stub
        super.onPause();
        unregisterReceiver(ActionFoundReceiver);
        isListopen = false;
    }



    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        private final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // Very important - has to be this for SPP to work

        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmp = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = device.createRfcommSocketToServiceRecord(SPP_UUID);
            } catch (IOException e) { }
            mmSocket = tmp;
        }

        public void run() {
            // Cancel discovery because it will slow down the connection
            mBluetoothAdapter.cancelDiscovery();

            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    mmSocket.close();
                } catch (IOException closeException) { }
                return;
            }


        }

        public BluetoothSocket getSocket(){
            return cTThread.mmSocket;
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) { }
        }
    }




    class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        final int MESSAGE_READ = 9999; // its only identifier to tell to handler what to do with data you passed through.

        // Handler in DataTransferActivity
        public Handler mHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                    case MESSAGE_READ:
                        // your code goes here
                }
            }
        };




        public void run() {
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    // Send the obtained bytes to the UI activity
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) { }
        }

        /* Call this from the main activity to shutdown the connection */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {}
        }
    }

    public static void write(byte[] bytes){
        cTThread.write(bytes);
    }
    public static boolean valid(){
        return ctThread.mmSocket.isConnected();
    }
}


