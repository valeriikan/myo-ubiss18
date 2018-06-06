package com.aware.app.myoubiss18;

import android.Manifest;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.aware.ui.PermissionsHandler;

import java.util.ArrayList;
import java.util.List;

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
        ImuProcessor.ImuDataListener{

    // UI views
     ToggleButton connectBtn;
     TextView tvCollecting;
     ProgressBar progress;

    // Myo variables
    private MyoConnector connector = null;
    private Myo myo = null;
    private EmgProcessor emgProcessor = null;
    private ImuProcessor imuProcessor = null;

    public static final String MYO_TAG = "MYO_TAG";
    public static final String SAMPLE_TAG = "SAMPLE_TAG";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvCollecting = findViewById(R.id.tvCollecting);
        progress = findViewById(R.id.progress);
        connectBtn = findViewById(R.id.connectBtn);
    }

    @Override
    protected void onResume() {
        super.onResume();

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

        //Removing values
        removeValues();
    }

    // Initializing Connector and connecting to Myo
    private void connectMyo() {
        if (connector == null) connector = new MyoConnector(this);
        connector.scan(5000, new MyoConnector.ScannerCallback() {
            @Override
            public void onScanFinished(List<Myo> scannedMyos) {

                Log.d(MYO_TAG, "Found " + scannedMyos.size() + " Myo: " + scannedMyos.toString());

                if (scannedMyos.size() == 0) {
                    Log.d(MYO_TAG, "Connection failed, cannot find adjacent Myo");

                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, "Connection failed, cannot find any Myo devices",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });

                    uiConnected(false);

                } else {
                    myo = scannedMyos.get(0);
                    myo.addConnectionListener(MainActivity.this);
                    myo.connect();

                }
            }
        });
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

        if (connector != null) {
            connector = null;
        }
    }

    @Override
    public void onConnectionStateChanged(final BaseMyo baseMyo, BaseMyo.ConnectionState state) {

        if (state == BaseMyo.ConnectionState.CONNECTED) {
            Log.d(MYO_TAG, "STATE CONNECTED");

            //Applying settings to connected Myo
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
