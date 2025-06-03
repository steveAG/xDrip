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
import com.eveningoutpost.dexdrip.utils.PermissionManager;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;

/**
 * Activity for requesting location permissions with proper rationale
 */
public class LocationPermissionActivity extends AppCompatActivity {

    private static final String TAG = LocationPermissionActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_FINE_LOCATION = PermissionManager.PERMISSION_REQUEST_LOCATION;
    private static final int PERMISSION_REQUEST_BACKGROUND_LOCATION = PermissionManager.PERMISSION_REQUEST_BACKGROUND_LOCATION;
    
    private TextView explanationText;
    private boolean requestingBackgroundLocation = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate ENTERING");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_permission);
        
        explanationText = findViewById(R.id.explanationText);
        
        // Check if we need to request background location instead
        if (getIntent().getBooleanExtra("background", false) &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            PermissionManager.hasPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
            requestingBackgroundLocation = true;
            updateUIForBackgroundLocation();
        }
        
        JoH.vibrateNotice();
    }
    
    private void updateUIForBackgroundLocation() {
        if (explanationText != null) {
            explanationText.setText(R.string.background_location_permission_rationale);
        }
    }

    public void onClickEnablePermission(View view) {
        Log.d(TAG, "onClickEnablePermission()");

        if (requestingBackgroundLocation && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For background location on Android 10+, we need to send the user to settings
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            Uri uri = Uri.fromParts("package", getPackageName(), null);
            intent.setData(uri);
            startActivity(intent);
        } else {
            // Request regular location permission
            PermissionManager.requestPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);
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
            
            // If we're on Android 10+ and we just got foreground location,
            // we might need background location too
            if (requestCode == PERMISSION_REQUEST_FINE_LOCATION &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                JoH.ratelimit("background_location_prompt", 10)) {
                
                // For API 29-30 (Android 10-11), background location is needed for BLE scanning
                // For API 31+ (Android 12+), we use the new Bluetooth permissions that don't require location
                if ((DexCollectionType.hasBluetooth() || DexCollectionType.hasWifi()) && 
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    // Start a new instance of this activity but for background location
                    Intent intent = new Intent(this, LocationPermissionActivity.class);
                    intent.putExtra("background", true);
                    startActivity(intent);
                }
            }
            
            finish();
        }
    }
}