package com.eveningoutpost.dexdrip;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utils.PermissionManager;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

/**
 * Activity for requesting notification permission with proper rationale
 * Only used on API 33+ (Android 13+)
 */
public class NotificationPermissionActivity extends AppCompatActivity {

    private static final String TAG = NotificationPermissionActivity.class.getSimpleName();
    private static final int PERMISSION_REQUEST_NOTIFICATION = PermissionManager.PERMISSION_REQUEST_NOTIFICATION;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate ENTERING");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification_permission);
        
        // Only applicable for API 33+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Log.d(TAG, "Notification permission not required for this API level");
            Pref.setBoolean("notification_permission_checked", true);
            finish();
            return;
        }
        
        JoH.vibrateNotice();
    }

    public void onClickEnablePermission(View view) {
        Log.d(TAG, "onClickEnablePermission()");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PermissionManager.requestPermission(this, Manifest.permission.POST_NOTIFICATIONS);
        } else {
            // Not needed for older versions
            Pref.setBoolean("notification_permission_checked", true);
            finish();
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

        if (requestCode == PERMISSION_REQUEST_NOTIFICATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "Notification permission granted");
                JoH.static_toast_long("Notification permission granted");
            } else {
                Log.i(TAG, "Notification permission denied");
                JoH.static_toast_long("Notification permission denied - you may not receive alerts");
            }
            
            // Mark as checked even if denied, to avoid repeated prompts
            Pref.setBoolean("notification_permission_checked", true);
            finish();
        }
    }
}