package com.eveningoutpost.dexdrip;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.PermissionManager;

/**
 * Activity for requesting body sensor permissions with proper rationale
 */
public class SensorPermissionActivity extends AppCompatActivity {

    private static final String TAG = SensorPermissionActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_SENSOR = PermissionManager.PERMISSION_REQUEST_BODY_SENSORS;
    
    private TextView explanationText;
    private boolean requestingBackgroundSensors = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate ENTERING");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_body_permission);
        
        explanationText = findViewById(R.id.explanationText);
        
        // Check if we need to request background body sensors instead
        if (getIntent().getBooleanExtra("background", false) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
            PermissionManager.hasPermission(this, Manifest.permission.BODY_SENSORS)) {
            requestingBackgroundSensors = true;
            updateUIForBackgroundSensors();
        }
        
        JoH.vibrateNotice();
    }
    
    private void updateUIForBackgroundSensors() {
        if (explanationText != null) {
            explanationText.setText(R.string.background_sensors_permission_rationale);
        }
    }

    public void onClickEnablePermission(View view) {
        Log.d(TAG, "onClickEnablePermission()");

        if (requestingBackgroundSensors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            // For background body sensors on Android 14+, we need to send the user to settings
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        } else {
            // Request regular body sensors permission
            PermissionManager.requestPermission(this, Manifest.permission.BODY_SENSORS);
        }
    }

    /*
     * Callback received when a permissions request has been completed.
     */
    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        Log.d(TAG, "onRequestPermissionsResult()");

        if (PermissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)) {
            Log.i(TAG, "onRequestPermissionsResult() granted");
            
            // If we're on Android 14+ and we just got foreground body sensors,
            // we might need background body sensors too
            if (requestCode == PERMISSION_REQUEST_SENSOR &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                JoH.ratelimit("background_sensors_prompt", 10)) {
                
                // Check if the app needs background body sensors
                if (Pref.getBooleanDefaultFalse("use_heart_rate") ||
                    Pref.getBooleanDefaultFalse("use_continuous_heart_rate")) {
                    // Start a new instance of this activity but for background sensors
                    Intent intent = new Intent(this, SensorPermissionActivity.class);
                    intent.putExtra("background", true);
                    startActivity(intent);
                }
            }
            
            finish();
        }
    }
}