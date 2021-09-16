package com.rioapp.demo.imeiplugin;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.telephony.TelephonyManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.flutter.plugin.common.PluginRegistry.Registrar;

/**
 * ImeiPlugin
 */
@SuppressWarnings("deprecation")
public class ImeiPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.RequestPermissionsResultListener {
    private static final String CHANNEL = "imei_plugin";

    private Activity mActivity;
    private MethodChannel mChannel;
    private ActivityPluginBinding mActivityPluginBinding;
    private Result mResult;

    private static final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE = 1995;
    private static final int MY_PERMISSIONS_REQUEST_READ_PHONE_STATE_IMEI_MULTI = 1997;
    private static final String PREF_UNIQUE_ID = "PREF_UNIQUE_ID_99599";
    private static boolean SSRPR = false;

    private static final String ERCODE_PERMISSIONS_DENIED = "2000";

    public ImeiPlugin() {
    }

    /**
     * Plugin registration.
     * add Listener Request permission
     */
    public static void registerWith(Registrar registrar) {
        final MethodChannel channel = new MethodChannel(registrar.messenger(), CHANNEL);
        ImeiPlugin imeiPlugin = new ImeiPlugin();
        channel.setMethodCallHandler(imeiPlugin);
        registrar.addRequestPermissionsResultListener(imeiPlugin);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        mResult = result;

        try {
            SSRPR = "true".equals(call.<String>argument("ssrpr"));
        } catch (Exception e) {
            SSRPR = false;
        }

        switch (call.method) {
            case "getImei":
                getImei(mActivity, mResult);
                break;
            case "getImeiMulti":
                getImeiMulti(mActivity, result);
                break;
            case "getId":
                getID(mActivity, result);
                break;
            default:
                mResult.notImplemented();
                break;
        }

    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (requestCode == MY_PERMISSIONS_REQUEST_READ_PHONE_STATE || requestCode == MY_PERMISSIONS_REQUEST_READ_PHONE_STATE_IMEI_MULTI) {
            if (results[0] == PackageManager.PERMISSION_GRANTED) {
                if (requestCode == MY_PERMISSIONS_REQUEST_READ_PHONE_STATE) {
                    getImei(mActivity, mResult);
                } else {
                    getImeiMulti(mActivity, mResult);
                }
            } else {
                mResult.error(ERCODE_PERMISSIONS_DENIED, "Permission Denied", null);
            }
            return true;
        }

        return false;
    }

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        mChannel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), CHANNEL);
        mChannel.setMethodCallHandler(this);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        mChannel.setMethodCallHandler(null);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding activityPluginBinding) {
        attachedToActivity(activityPluginBinding);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        detachedFromActivity();
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding activityPluginBinding) {
        attachedToActivity(activityPluginBinding);
    }

    @Override
    public void onDetachedFromActivity() {
        detachedFromActivity();
    }

    private void attachedToActivity(@NonNull ActivityPluginBinding activityPluginBinding) {
        if (mActivityPluginBinding != null) {
            mActivityPluginBinding.removeRequestPermissionsResultListener(this);
        }
        mActivity = activityPluginBinding.getActivity();
        mActivityPluginBinding = activityPluginBinding;
        activityPluginBinding.addRequestPermissionsResultListener(this);
    }

    private void detachedFromActivity() {
        if (mActivityPluginBinding != null) {
            mActivityPluginBinding.removeRequestPermissionsResultListener(this);
        }
        mActivityPluginBinding = null;
    }

    @SuppressLint("HardwareIds")
    private static void getImei(Activity activity, Result result) {
        try {
            if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                result.success(getUUID(activity));
            } else if (ContextCompat.checkSelfPermission((activity), Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                TelephonyManager telephonyManager = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    result.success(telephonyManager.getImei());
                } else {
                    result.success(telephonyManager.getDeviceId());
                }
            } else {
                if (SSRPR && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_PHONE_STATE)) {
                    result.error(ERCODE_PERMISSIONS_DENIED, "Permission Denied", null);
                } else {
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_PHONE_STATE}, MY_PERMISSIONS_REQUEST_READ_PHONE_STATE);
                }
            }
        } catch (Exception ex) {
            result.success("unknown");
        }
    }

    @SuppressLint("HardwareIds")
    private static void getImeiMulti(Activity activity, Result result) {
        try {
            if (android.os.Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                result.success(Collections.singletonList(getUUID(activity)));
            } else if (ContextCompat.checkSelfPermission((activity), Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {
                TelephonyManager telephonyManager = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    int phoneCount = telephonyManager.getPhoneCount();

                    ArrayList<String> imeis = new ArrayList<>();
                    for (int i = 0; i < phoneCount; i++) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            imeis.add(telephonyManager.getImei(i));
                        } else {
                            imeis.add(telephonyManager.getDeviceId(i));
                        }
                    }
                    result.success(imeis);
                } else {
                    result.success(Collections.singletonList(telephonyManager.getDeviceId()));
                }
            } else {
                if (SSRPR && ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.READ_PHONE_STATE)) {
                    result.error(ERCODE_PERMISSIONS_DENIED, "Permission Denied", null);
                } else {
                    ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.READ_PHONE_STATE}, MY_PERMISSIONS_REQUEST_READ_PHONE_STATE_IMEI_MULTI);
                }
            }
        } catch (Exception ex) {
            result.success("unknown");
        }
    }

    private synchronized static String getUUID(Context context) {
        final SharedPreferences sharedPrefs = context.getSharedPreferences(PREF_UNIQUE_ID, Context.MODE_PRIVATE);
        String uniqueID = sharedPrefs.getString(PREF_UNIQUE_ID, null);
        if (uniqueID == null) {
            uniqueID = UUID.randomUUID().toString();
            SharedPreferences.Editor editor = sharedPrefs.edit();
            editor.putString(PREF_UNIQUE_ID, uniqueID);
            editor.apply();
        }

        return uniqueID;
    }

    private static void getID(Context context, Result result) {
        result.success(getUUID(context));
    }
}
