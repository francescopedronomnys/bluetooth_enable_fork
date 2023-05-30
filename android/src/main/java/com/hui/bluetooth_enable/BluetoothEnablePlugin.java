package com.hui.bluetooth_enable;

import android.Manifest;
import android.app.Activity;
import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry.ActivityResultListener;
import io.flutter.plugin.common.PluginRegistry.RequestPermissionsResultListener;

/**
 * FlutterBluePlugin
 */
public class BluetoothEnablePlugin implements FlutterPlugin, ActivityAware, MethodCallHandler, ActivityResultListener, RequestPermissionsResultListener {
    private static final String TAG = "BluetoothEnablePlugin";
    private Activity activity;
    private MethodChannel channel;
    private final Object initializationLock = new Object();
    private static final String NAMESPACE = "bluetooth_enable";
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private FlutterPluginBinding pluginBinding;
    private ActivityPluginBinding activityBinding;

    private Result pendingResult;

    private static final int REQUEST_ENABLE_BLUETOOTH = 1;
    private static final int REQUEST_CODE_SCAN_ACTIVITY = 2777;

    public BluetoothEnablePlugin() {
        this.onDetachedFromEngine(null);
    }

    /* FlutterPlugin implementation */

    @Override
    public void onAttachedToEngine(FlutterPluginBinding flutterPluginBinding) {

        pluginBinding = flutterPluginBinding;
        this.channel = new MethodChannel(pluginBinding.getBinaryMessenger(), "bluetooth_enable");

        setup(pluginBinding.getBinaryMessenger(),
                (Application) pluginBinding.getApplicationContext());

    }

    @Override
    public void onDetachedFromEngine(FlutterPluginBinding binding) {
        this.activity = null;
        this.channel = null;
        this.mBluetoothAdapter = null;
        this.mBluetoothManager = null;
    }

    /* ActivityAware implementation */

    @Override
    public void onAttachedToActivity(ActivityPluginBinding binding) {
        activityBinding = binding;
        this.initPluginFromPluginBinding(activityBinding);
    }

    @Override
    public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {
        activityBinding = binding;
        this.initPluginFromPluginBinding(activityBinding);
    }

    private void initPluginFromPluginBinding(ActivityPluginBinding activityPluginBinding) {
        this.activity = activityPluginBinding.getActivity();
        this.mBluetoothManager = (BluetoothManager) this.activity.getSystemService(Context.BLUETOOTH_SERVICE);
        this.mBluetoothAdapter = mBluetoothManager.getAdapter();

        activityPluginBinding.addActivityResultListener(this);
        activityPluginBinding.addRequestPermissionsResultListener(this);

        this.channel.setMethodCallHandler(this);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        this.releaseResources();
    }

    @Override
    public void onDetachedFromActivity() {
        this.releaseResources();
    }

    private void setup(
            final BinaryMessenger messenger,
            final Application application) {
        synchronized (initializationLock) {
            Log.d(TAG, "setup");
            // channel = new MethodChannel(messenger, NAMESPACE + "/methods");
            this.channel.setMethodCallHandler(this);
            this.mBluetoothManager = (BluetoothManager) application.getSystemService(Context.BLUETOOTH_SERVICE);
            this.mBluetoothAdapter = mBluetoothManager.getAdapter();
        }
    }

    @Override
    public void onMethodCall(MethodCall call, Result result) {
        if (mBluetoothAdapter == null && !"isAvailable".equals(call.method)) {
            result.error("bluetooth_unavailable", "the device does not have bluetooth", null);
            return;
        }

        ActivityCompat.requestPermissions(this.activity,
                new String[]{Manifest.permission.BLUETOOTH_CONNECT},
                REQUEST_ENABLE_BLUETOOTH);

        switch (call.method) {
            case "isAvailable": {
                if (mBluetoothAdapter != null) {
                    result.success(true);
                } else {
                    result.success(false);
                }
                break;
            }
            case "isEnabled": {
                if (mBluetoothAdapter.isEnabled()) {
                    result.success(true);
                } else {
                    result.success(false);
                }
                break;
            }
            case "enableBluetooth": {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BLUETOOTH);
                Log.d(TAG, "rdddesult: " + result);
                pendingResult = result;
                break;
            }
            case "customEnable": {
                try {
                    BluetoothManager bluetoothManager = (BluetoothManager) activity.getSystemService(Context.BLUETOOTH_SERVICE);
                    BluetoothAdapter mBluetoothAdapter = bluetoothManager.getAdapter();
                    if (!mBluetoothAdapter.isEnabled()) {
                        mBluetoothAdapter.disable();
                        Thread.sleep(500); //code for dealing with InterruptedException not shown
                        mBluetoothAdapter.enable();
                    }
                } catch (InterruptedException e) {
                    Log.e(TAG, "customEnable", e);
                }
                result.success("true");
                break;
            }
            default: {
                result.notImplemented();
                break;
            }
        }
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BLUETOOTH) {
            if (pendingResult == null) {
                Log.d(TAG, "onActivityResult: problem: pendingResult is null");
            } else {
                try {
                    if (resultCode == Activity.RESULT_OK) {
                        Log.d(TAG, "onActivityResult: User enabled Bluetooth");
                        pendingResult.success(true);
                    } else {
                        Log.d(TAG, "onActivityResult: User did NOT enabled Bluetooth");
                        pendingResult.success(false);
                    }
                } catch (IllegalStateException | NullPointerException e) {
                    Log.d(TAG, "onActivityResult REQUEST_ENABLE_BLUETOOTH", e);
                }
            }
        }
        return false;
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "BroadcastReceiver onReceive: STATE_OFF");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "BroadcastReceiver onReceive: STATE_TURNING_OFF");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "BroadcastReceiver onReceive: STATE_ON");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "BroadcastReceiver onReceive: STATE_TURNING_ON");
                        break;
                }
            }
        }
    };

    @Override
    public boolean onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult, TWO");

        return false;
    }

    private void releaseResources() {
        this.activity.finish();
    }
}
