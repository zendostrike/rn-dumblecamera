package com.scanner.demo;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.scanlibrary.ScanActivity;
import com.scanlibrary.ScanConstants;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DumbleCameraModule extends ReactContextBaseJavaModule implements ActivityEventListener{

    private WritableMap scannedResult;
    private static final int REQUEST_CODE = 99;
    private static final String SCANNED_RESULT = "DumbleCamera:scannedResult";

    public DumbleCameraModule(ReactApplicationContext reactContext) {
        super(reactContext);
        reactContext.addActivityEventListener(this);
    }

    @Override
    public String getName() {
        return "DumbleCamera";
    }

    @Override
    public Map<String, Object> getConstants() {
        final Map<String, Object> constants = new HashMap<>();
        constants.put("SCANNED_RESULT", SCANNED_RESULT);
        return constants;
    }

    private void emitMessageToRN(ReactContext reactContext, String eventName, @Nullable WritableMap params) {
        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    @ReactMethod
    public void openCamera(){
        Activity currentActivity = getCurrentActivity();
        Intent intent = new Intent(getReactApplicationContext(), ScanActivity.class);
        currentActivity.startActivityForResult(intent, REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Uri uri = data.getExtras().getParcelable(ScanConstants.SCANNED_RESULT);
        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(getReactApplicationContext().getContentResolver(), uri);;

            scannedResult = Arguments.createMap();
            scannedResult.putString ("uri", uri.toString());


            emitMessageToRN(getReactApplicationContext(), SCANNED_RESULT, scannedResult);
            // scannedImageView.setImageBitmap(bitmap);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
