package com.eveningoutpost.dexdrip;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.PermissionManager;
import com.eveningoutpost.dexdrip.utilitymodels.CollectionServiceStarter;

/**
 * Activity for requesting Bluetooth permissions with proper rationale
 */
public class BluetoothPermissionActivity extends AppCompatActivity {

    private static final String TAG = BluetoothPermissionActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_BLUETOOTH = PermissionManager.PERMISSION_REQUEST_BLUETOOTH;
    
    private TextView explanationText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate ENTERING");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_permission);
        
        explanationText = findViewById(R.id.explanationText);
        
        // Update explanation text based on API level
        updateExplanationText();
        
        JoH.vibrateNotice();
    }
    
    private void updateExplanationText() {
        if (explanationText != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // Android 12+ uses new Bluetooth permissions
                explanationText.setText(R.string.bluetooth_permission_text_android12);
            } else {
                // Older versions use BLUETOOTH and BLUETOOTH_ADMIN
                explanationText.setText(R.string.bluetooth_permission_text);
            }
        }
    }

    public void onClickEnablePermission(View view) {
        Log.d(TAG, "onClickEnablePermission()");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+) uses new Bluetooth permissions
            String[] permissions = {
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            };
            PermissionManager.requestPermissions(this, permissions);
        } else {
            // Older versions use BLUETOOTH and BLUETOOTH_ADMIN
            String[] permissions = {
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN
            };
            PermissionManager.requestPermissions(this, permissions);
            
            // For API 23-30, location permission is also required for Bluetooth scanning
            if (!PermissionManager.hasPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)) {
                Intent intent = new Intent(this, LocationPermissionActivity.class);
                startActivity(intent);
            }
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
            
            // If we're using a Bluetooth collector, restart it to apply the new permissions
            if (DexCollectionType.hasBluetooth()) {
                JoH.static_toast_long("Bluetooth permissions granted, restarting collector");
                CollectionServiceStarter.restartCollectionServiceBackground();
            }
            
            finish();
        }
    }
}