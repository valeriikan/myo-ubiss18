package com.aware.app.myoubiss18;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ContentValues;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.aware.ui.PermissionsHandler;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.internal.bind.DateTypeAdapter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.HttpsURLConnection;

import eu.darken.myolib.BaseMyo;
import eu.darken.myolib.Myo;
import eu.darken.myolib.MyoCmds;
import eu.darken.myolib.MyoConnector;
import eu.darken.myolib.msgs.MyoMsg;
import eu.darken.myolib.processor.emg.EmgData;
import eu.darken.myolib.processor.emg.EmgProcessor;
import eu.darken.myolib.processor.imu.ImuData;
import eu.darken.myolib.processor.imu.ImuProcessor;

public class MainActivity extends AppCompatActivity implements
        BaseMyo.ConnectionListener,
        EmgProcessor.EmgDataListener,
        ImuProcessor.ImuDataListener,
        SensorEventListener {

    // UI views
     ToggleButton connectBtn;
     Button endTrailButton;
     TextView tvCollecting;
     ProgressBar progress;

    // Myo variables
    private MyoConnector connector = null;
    private Myo myo = null;
    private EmgProcessor emgProcessor = null;
    private ImuProcessor imuProcessor = null;

    public static final String MYO_TAG = "MYO_TAG";
    public static final String SAMPLE_TAG = "SAMPLE_TAG";
    public String EXTRA_DATA;


    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyro;

    private ArrayList<UBISSD> acc = new ArrayList<>();
    private ArrayList<UBISSD> gyro = new ArrayList<>();

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) { }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvCollecting = findViewById(R.id.tvCollecting);
        progress = findViewById(R.id.progress);
        connectBtn = findViewById(R.id.connectBtn);

        endTrailButton=findViewById(R.id.endTrailButton);
        Intent intent = getIntent();
        Bundle bd = intent.getExtras();
        if(bd != null)
        {
             EXTRA_DATA = (String) bd.get("EXTRA_DATA");

        }

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);


        Log.d("Walk_the_line","MainActivity");
        endTrailButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JsonObject data=new JsonObject();
                JsonArray acc_data=new JsonArray();
                JsonArray gyro_data=new JsonArray();
                for(UBISSD d : acc){
                    JsonObject tmp = new JsonObject();

                    tmp.addProperty("x",d.x);
                    tmp.addProperty("y",d.y);
                    tmp.addProperty("z",d.z);
                    tmp.addProperty("t",d.t);

                    acc_data.add( tmp);
                }
                for(UBISSD d : gyro){
                    JsonObject tmp = new JsonObject();

                    tmp.addProperty("x",d.x);
                    tmp.addProperty("y",d.y);
                    tmp.addProperty("z",d.z);
                    tmp.addProperty("t",d.t);

                    gyro_data.add( tmp);
                }
                data.addProperty("participant",EXTRA_DATA);
                data.addProperty("acc",acc_data.getAsJsonArray().toString() );
                data.addProperty("gyro",gyro_data.getAsJsonArray().toString() );

                data.addProperty("trial_end_time",System.currentTimeMillis());
                acc = new ArrayList<>();
                gyro = new ArrayList<>();
                new NetworkCallHandler().execute(data);

            }
        });
    }
    public class NetworkCallHandler extends AsyncTask<JsonObject, Void, Void> {
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            finish();
        }

        @Override
        protected Void doInBackground(JsonObject... jsonObjects) {
            URL url;
            String response = "";
            try {
                url = new URL("https://ubiss-206410.appspot.com/entry");

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setReadTimeout(15000);
                conn.setConnectTimeout(15000);
                conn.setRequestMethod("POST");

                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoInput(true);
                conn.setDoOutput(true);


                OutputStream os = conn.getOutputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(os, "UTF-8"));

                JsonObject content=jsonObjects[0];
                Log.d("Walk_the_Line",content.toString());
                writer.write(content.toString());

                writer.flush();
                writer.close();
                os.close();
                int responseCode=conn.getResponseCode();

                if (responseCode == HttpsURLConnection.HTTP_OK) {
                    String line;
                    BufferedReader br=new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    while ((line=br.readLine()) != null) {
                        response+=line;
                    }

                }
                else {
                    response="";

                }

                Log.d("Walk_the_Line",response);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;

        }
    }

    @Override
    public void onSensorChanged (SensorEvent event) {

        Log.d("Walk_the_line","new sensor event");

        switch (event.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                UBISSD d = new UBISSD();
                d.x = event.values[0];
                d.y = event.values[1];
                d.z = event.values[2];
                d.t =  System.currentTimeMillis();
                acc.add(d);

                break;

            case Sensor.TYPE_GYROSCOPE:
                UBISSD d2 = new UBISSD();
                d2.x = event.values[0];
                d2.y = event.values[1];
                d2.z = event.values[2];
                d2.t =  System.currentTimeMillis();
                gyro.add(d2);

                break;

            default:
                break;
        }

    }

    @Override
    protected void onResume() {

        super.onResume();

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mGyro, SensorManager.SENSOR_DELAY_GAME);

        // List of required permission
        ArrayList<String> REQUIRED_PERMISSIONS = new ArrayList<>();
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_FINE_LOCATION);
        REQUIRED_PERMISSIONS.add(Manifest.permission.ACCESS_COARSE_LOCATION);


        boolean permissions_ok = true;
        for (String p : REQUIRED_PERMISSIONS) {
            if (PermissionChecker.checkSelfPermission(this, p) != PermissionChecker.PERMISSION_GRANTED) {
                permissions_ok = false;
                break;
            }
        }

        if (permissions_ok) {


            connectBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (connectBtn.isChecked()) {
                        Log.d(MYO_TAG, "checked");

                        connectBtn.setEnabled(false);
                        connectBtn.setVisibility(View.INVISIBLE);
                        progress.setVisibility(View.VISIBLE);
                        tvCollecting.setVisibility(View.VISIBLE);
                        tvCollecting.setText("Connecting...");

                        connectMyo();

                    } else {
                        Log.d(MYO_TAG, "UNchecked");

                        connectBtn.setEnabled(false);
                        connectBtn.setVisibility(View.INVISIBLE);
                        progress.setVisibility(View.VISIBLE);
                        tvCollecting.setVisibility(View.VISIBLE);
                        tvCollecting.setText("Disconnecting...");

                        disconnectMyo();
                    }

                }
            });

        } else {

            Intent permissions = new Intent(MainActivity.this, PermissionsHandler.class);
            permissions.putExtra(PermissionsHandler.EXTRA_REQUIRED_PERMISSIONS, REQUIRED_PERMISSIONS);
            permissions.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(permissions);

        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(this);
        //Removing values
        removeValues();
    }

    // Initializing Connector and connecting to Myo
    private void connectMyo() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice bt = bluetoothAdapter.getRemoteDevice("C4:EF:50:4D:29:BD");

        myo = new Myo(getApplicationContext(), bt);
        myo.addConnectionListener(MainActivity.this);
        myo.connect();
    }

    // Disconnecting from Myo
    private void disconnectMyo() {
        if (myo != null) {
            //Applying settings to disconnected Myo
            myo.setConnectionSpeed(BaseMyo.ConnectionSpeed.BALANCED);
            myo.writeSleepMode(MyoCmds.SleepMode.NORMAL, new Myo.MyoCommandCallback() {
                @Override
                public void onCommandDone(Myo myo, MyoMsg msg) {
                    Log.d(MYO_TAG, "Sleep mode: -");
                }
            });
            myo.writeMode(MyoCmds.EmgMode.NONE, MyoCmds.ImuMode.NONE, MyoCmds.ClassifierMode.DISABLED, new Myo.MyoCommandCallback() {
                @Override
                public void onCommandDone(Myo myo, MyoMsg msg) {
                    Log.d(MYO_TAG, "EMG and Imu: -");

                    // Disconnecting from Myo and aplying UI changes
                    Log.d(MYO_TAG, "Disconnected");
                    myo.disconnect();
                    removeValues();

                    uiConnected(false);
                }
            });
        }
    }

    // Removing values when Myo is detached
    private void removeValues() {
        if (myo != null) {
            if (emgProcessor != null) {
                myo.removeProcessor(emgProcessor);
                emgProcessor = null;
            }
            if (imuProcessor != null) {
                myo.removeProcessor(imuProcessor);
                imuProcessor = null;
            }
            myo.removeConnectionListener(this);
            myo = null;
        }
    }

    @Override
    public void onConnectionStateChanged(final BaseMyo baseMyo, BaseMyo.ConnectionState state) {

        if (state == BaseMyo.ConnectionState.CONNECTED) {
            Log.d(MYO_TAG, "STATE CONNECTED");

            // Applying settings to connected Myo
            // First run does not work
            myo.setConnectionSpeed(BaseMyo.ConnectionSpeed.HIGH);
            myo.writeSleepMode(MyoCmds.SleepMode.NEVER,null);
            myo.writeUnlock(MyoCmds.UnlockType.HOLD, null);
            myo.writeMode(MyoCmds.EmgMode.FILTERED, MyoCmds.ImuMode.RAW, MyoCmds.ClassifierMode.DISABLED,null);

            // Second run makes actual changes
            myo.setConnectionSpeed(BaseMyo.ConnectionSpeed.HIGH);
            myo.writeSleepMode(MyoCmds.SleepMode.NEVER, new Myo.MyoCommandCallback() {
                @Override
                public void onCommandDone(Myo myo, MyoMsg msg) {
                    Log.d(MYO_TAG, "Sleep mode: +");
                }
            });
            myo.writeUnlock(MyoCmds.UnlockType.HOLD, new Myo.MyoCommandCallback() {
                @Override
                public void onCommandDone(Myo myo, MyoMsg msg) {
                    Log.d(MYO_TAG, "Unlock: +");
                    myo.writeVibrate(MyoCmds.VibrateType.LONG, null);
                }
            });
            myo.writeMode(MyoCmds.EmgMode.FILTERED, MyoCmds.ImuMode.RAW, MyoCmds.ClassifierMode.DISABLED, new Myo.MyoCommandCallback() {
                @Override
                public void onCommandDone(Myo myo, MyoMsg msg) {
                    // Setting up Imu and EMG sensors
                    Log.d(MYO_TAG, "EMG and Imu: +");
                    imuProcessor = new ImuProcessor();
                    emgProcessor = new EmgProcessor();
                    imuProcessor.addListener(MainActivity.this);
                    emgProcessor.addListener(MainActivity.this);
                    myo.addProcessor(imuProcessor);
                    myo.addProcessor(emgProcessor);

                    // Applying UI updates
                    Log.d(MYO_TAG, "Connected to: " + baseMyo.toString());
                    uiConnected(true);
                }
            });

        }

        if (state == BaseMyo.ConnectionState.DISCONNECTED) {
            Log.d(MYO_TAG, "STATE DISCONNECTED");
            Log.d(MYO_TAG, "Disconnected from Myo: " + baseMyo.toString());

            uiConnected(false);
            removeValues();
        }

    }

    private void uiConnected(final boolean connected) {

        Activity act = this;
        act.runOnUiThread(new Runnable(){
            @Override
            public void run() {

                if (connected) {
                    connectBtn.setEnabled(true);
                    connectBtn.setChecked(true);
                    connectBtn.setVisibility(View.VISIBLE);
                    tvCollecting.setText("Collecting data...");
                    progress.setVisibility(View.INVISIBLE);

                } else {
                    connectBtn.setChecked(false);
                    connectBtn.setEnabled(true);
                    connectBtn.setVisibility(View.VISIBLE);
                    tvCollecting.setVisibility(View.INVISIBLE);
                    progress.setVisibility(View.INVISIBLE);
                }
            } });
    }

    //Myo EMG data listener
    private long mLastEmgUpdate = 0;
    @Override
    public void onNewEmgData(EmgData emgData) {
        // Check for Emg updates twice per second
        if (System.currentTimeMillis() - mLastEmgUpdate > 500) {
            byte[] data = emgData.getData();
            ContentValues emg = new ContentValues();
            emg.put("emg0", String.valueOf(data[0]));
            emg.put("emg1", String.valueOf(data[1]));
            emg.put("emg2", String.valueOf(data[2]));
            emg.put("emg3", String.valueOf(data[3]));
            emg.put("emg4", String.valueOf(data[4]));
            emg.put("emg5", String.valueOf(data[5]));
            emg.put("emg6", String.valueOf(data[6]));
            emg.put("emg7", String.valueOf(data[7]));

            Log.d(SAMPLE_TAG, "EMG SAMPLE: " + emg.toString());

            mLastEmgUpdate = System.currentTimeMillis();
        }
    }

    //Myo Imu data (accelerometer, gyroscope, orientation) listener
    private long mLastImuUpdate = 0;
    @Override
    public void onNewImuData(ImuData imuData) {
        // Check for Gyro updates twice per second
        if (System.currentTimeMillis() - mLastImuUpdate > 500) {
            ContentValues gyroData = new ContentValues();
            gyroData.put("gyroX", imuData.getGyroData()[0]);
            gyroData.put("gyroY", imuData.getGyroData()[1]);
            gyroData.put("gyroZ", imuData.getGyroData()[2]);

            ContentValues accData = new ContentValues();
            accData.put("accX", imuData.getAccelerometerData()[0]);
            accData.put("accY", imuData.getAccelerometerData()[1]);
            accData.put("accZ", imuData.getAccelerometerData()[2]);

            ContentValues orData = new ContentValues();
            orData.put("accX", imuData.getOrientationData()[0]);
            orData.put("accY", imuData.getOrientationData()[1]);
            orData.put("accZ", imuData.getOrientationData()[2]);

            Log.d(SAMPLE_TAG, "GYRO SAMPLE: " + gyroData.toString());
            Log.d(SAMPLE_TAG, "ACC SAMPLE: " + accData.toString());
            Log.d(SAMPLE_TAG, "ORIENT SAMPLE: " + orData.toString());

            mLastImuUpdate = System.currentTimeMillis();
        }
    }

}
