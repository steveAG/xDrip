package com.eveningoutpost.dexdrip;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;

/**
 * Activity for requesting exact alarm permission with proper rationale
 * Only used on API 31+ (Android 12+)
 */
public class ExactAlarmPermissionActivity extends AppCompatActivity {

    private static final String TAG = ExactAlarmPermissionActivity.class.getSimpleName();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate ENTERING");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exact_alarm_permission);
        
        // Only applicable for API 31+
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            Log.d(TAG, "Exact alarm permission not required for this API level");
            Pref.setBoolean("exact_alarm_permission_checked", true);
            finish();
            return;
        }
        
        JoH.vibrateNotice();
    }

    public void onClickEnablePermission(View view) {
        Log.d(TAG, "onClickEnablePermission()");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // For Android 12+, we need to send the user to settings
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            } else {
                // Permission already granted
                Pref.setBoolean("exact_alarm_permission_checked", true);
                finish();
            }
        } else {
            // Not needed for older versions
            Pref.setBoolean("exact_alarm_permission_checked", true);
            finish();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Check if permission was granted when returning from settings
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            if (alarmManager.canScheduleExactAlarms()) {
                Log.i(TAG, "Exact alarm permission granted");
                JoH.static_toast_long("Exact alarm permission granted");
                Pref.setBoolean("exact_alarm_permission_checked", true);
                finish();
            }
        }
    }
}