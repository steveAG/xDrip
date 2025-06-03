package com.eveningoutpost.dexdrip;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import androidx.annotation.StringRes;
import android.util.Log;

import com.activeandroid.ActiveAndroid;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.LibreBlock;
import com.eveningoutpost.dexdrip.models.LibreData;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.utilitymodels.PlusAsyncExecutor;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.VersionTracker;
import com.eveningoutpost.dexdrip.utilitymodels.NotificationChannels;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.utils.PermissionManager;


import static com.eveningoutpost.dexdrip.utils.VersionFixer.disableUpdates;


/**
 * Created by Emma Black on 3/21/15.
 */

public class xdrip extends Application {

    private static final String TAG = "xdrip.java";
    @SuppressLint("StaticFieldLeak")
    private static Context context;
    public static PlusAsyncExecutor executor;
    private static boolean fabricInited = false;
    private static Boolean isRunningTestCache;

    @Override
    public void onCreate() {
        xdrip.context = getApplicationContext();
        super.onCreate();
        try {
            if (PreferenceManager.getDefaultSharedPreferences(xdrip.context).getBoolean("enable_crashlytics", true)) {

            }
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
        PreferenceManager.setDefaultValues(this, R.xml.preferences, true);
        ActiveAndroid.initialize(this);
        updateMigrations();
        DemiGod.isPresent();
        JoH.forceBatteryWhitelisting();
        executor = new PlusAsyncExecutor();
        VersionTracker.updateDevice();
        disableUpdates();

        createNotificationChannels();
        
        // Check permissions after a short delay to allow the app to initialize
        new Handler(Looper.getMainLooper()).postDelayed(this::checkPermissions, 3000);
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager == null) {
                Log.e(TAG, "Cannot get NotificationManager");
                return;
            }

            // Ongoing Status Channel
            NotificationChannel ongoingChannel = new NotificationChannel(
                    NotificationChannels.ONGOING_STATUS_CHANNEL_ID,
                    getString(R.string.notification_channel_ongoing_status),
                    NotificationManager.IMPORTANCE_LOW);
            ongoingChannel.setDescription(getString(R.string.notification_channel_ongoing_status_desc));
            ongoingChannel.setShowBadge(false); // No badge for ongoing
            manager.createNotificationChannel(ongoingChannel);

            // Urgent Alerts Channel
            NotificationChannel urgentAlertsChannel = new NotificationChannel(
                    NotificationChannels.URGENT_ALERTS_CHANNEL_ID,
                    getString(R.string.notification_channel_urgent_alerts),
                    NotificationManager.IMPORTANCE_HIGH);
            urgentAlertsChannel.setDescription(getString(R.string.notification_channel_urgent_alerts_desc));
            // Configure sound/vibration based on alert settings later
            manager.createNotificationChannel(urgentAlertsChannel);

            // Info Alerts Channel
            NotificationChannel infoAlertsChannel = new NotificationChannel(
                    NotificationChannels.INFO_ALERTS_CHANNEL_ID,
                    getString(R.string.notification_channel_info_alerts),
                    NotificationManager.IMPORTANCE_DEFAULT);
            infoAlertsChannel.setDescription(getString(R.string.notification_channel_info_alerts_desc));
            manager.createNotificationChannel(infoAlertsChannel);

            // Calibration Reminders Channel
            NotificationChannel calibrationChannel = new NotificationChannel(
                    NotificationChannels.CALIBRATION_REMINDERS_CHANNEL_ID,
                    getString(R.string.notification_channel_calibration),
                    NotificationManager.IMPORTANCE_DEFAULT);
            calibrationChannel.setDescription(getString(R.string.notification_channel_calibration_desc));
            manager.createNotificationChannel(calibrationChannel);

            // Miscellaneous Channel
            NotificationChannel miscChannel = new NotificationChannel(
                    NotificationChannels.MISC_CHANNEL_ID,
                    getString(R.string.notification_channel_misc),
                    NotificationManager.IMPORTANCE_LOW);
            miscChannel.setDescription(getString(R.string.notification_channel_misc_desc));
            manager.createNotificationChannel(miscChannel);
// Low Transmitter Battery Channel
            NotificationChannel lowBatteryChannel = new NotificationChannel(
                    NotificationChannels.LOW_TRANSMITTER_BATTERY_CHANNEL_ID,
                    getString(R.string.notification_channel_low_battery),
                    NotificationManager.IMPORTANCE_DEFAULT); // Or maybe HIGH? Check usage.
            lowBatteryChannel.setDescription(getString(R.string.notification_channel_low_battery_desc));
            manager.createNotificationChannel(lowBatteryChannel);
        }
    }

    public static Context getAppContext() {
        return xdrip.context;
    }

    public static boolean checkAppContext(Context context) {
        if (getAppContext() == null) {
            xdrip.context = context;
            return false;
        } else {
            return true;
        }
    }

    private static void updateMigrations() {
        Sensor.InitDb(context);//ensure database has already been initialized
        BgReading.updateDB();
        LibreBlock.updateDB();
        LibreData.updateDB();
    }

    private static boolean isWear2OrAbove() {
        return Build.VERSION.SDK_INT > 23;
    }

    public static synchronized boolean isRunningTest() {
        android.util.Log.e(TAG, Build.MODEL);
        if (null == isRunningTestCache) {
            boolean test_framework;
            if ("robolectric".equals(Build.FINGERPRINT)) {
                isRunningTestCache = true;
            } else {
                try {
                    Class.forName("android.support.test.espresso.Espresso");
                    test_framework = true;
                } catch (ClassNotFoundException e) {
                    test_framework = false;
                }
                isRunningTestCache = test_framework;
            }
        }
        return isRunningTestCache;
    }

    public static String gs(@StringRes final int id) {
        return getAppContext().getString(id);
    }

    public static String gs(@StringRes final int id, String... args) {
        return getAppContext().getString(id, (Object[]) args);
    }
    
    /**
     * Check and request necessary permissions based on app configuration
     */
    private void checkPermissions() {
        // Only check permissions if we're not in a test environment
        if (isRunningTest()) return;
        
        // Get collection type to determine which permissions are needed
        DexCollectionType collectionType = DexCollectionType.getDexCollectionType();
        
        // Check if we need to show permission activities
        boolean needsPermissionCheck = Pref.getBooleanDefaultFalse("needs_permission_check") ||
                                      !Pref.getBooleanDefaultFalse("permissions_checked");
        
        if (needsPermissionCheck) {
            Log.d(TAG, "Checking permissions for collection type: " + collectionType.name());
            
            // For Bluetooth-based collectors, check Bluetooth permissions
            if (collectionType.hasBluetooth()) {
                boolean hasBluetoothPermissions = true;
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // Android 12+ (API 31+) uses new Bluetooth permissions
                    hasBluetoothPermissions = PermissionManager.hasPermission(this, android.Manifest.permission.BLUETOOTH_SCAN) &&
                                             PermissionManager.hasPermission(this, android.Manifest.permission.BLUETOOTH_CONNECT);
                } else {
                    // Older versions use BLUETOOTH and BLUETOOTH_ADMIN
                    hasBluetoothPermissions = PermissionManager.hasPermission(this, android.Manifest.permission.BLUETOOTH) &&
                                             PermissionManager.hasPermission(this, android.Manifest.permission.BLUETOOTH_ADMIN);
                }
                
                if (!hasBluetoothPermissions) {
                    Intent intent = new Intent(this, BluetoothPermissionActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    return; // Process one permission at a time
                }
            }
            
            // For Bluetooth-based collectors, check location permissions (required for BLE scanning on API 23-30)
            if (collectionType.hasBluetooth() &&
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                
                if (!PermissionManager.hasPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                    Intent intent = new Intent(this, LocationPermissionActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    return; // Process one permission at a time
                }
                
                // For API 29-30 (Android 10-11), background location is needed for BLE scanning
                // For API 31+ (Android 12+), we use the new Bluetooth permissions that don't require location
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                    Build.VERSION.SDK_INT < Build.VERSION_CODES.S &&
                    !PermissionManager.hasPermission(this, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                    
                    Intent intent = new Intent(this, LocationPermissionActivity.class);
                    intent.putExtra("background", true);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    return; // Process one permission at a time
                }
            }
            
            // Check body sensors permission if heart rate monitoring is enabled
            if (Pref.getBooleanDefaultFalse("use_heart_rate") ||
                Pref.getBooleanDefaultFalse("use_continuous_heart_rate")) {
                
                if (!PermissionManager.hasPermission(this, android.Manifest.permission.BODY_SENSORS)) {
                    Intent intent = new Intent(this, SensorPermissionActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    return; // Process one permission at a time
                }
                
                // For API 34+, check background body sensors
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                    !PermissionManager.hasPermission(this, android.Manifest.permission.BODY_SENSORS_BACKGROUND)) {
                    
                    Intent intent = new Intent(this, SensorPermissionActivity.class);
                    intent.putExtra("background", true);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    return; // Process one permission at a time
                }
            }
            
            // Check notification permission for API 33+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !PermissionManager.hasPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) &&
                !Pref.getBooleanDefaultFalse("notification_permission_checked")) {
                
                Intent intent = new Intent(this, NotificationPermissionActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                return; // Process one permission at a time
            }
            
            // If we've reached here, all necessary permissions have been checked
            Pref.setBoolean("permissions_checked", true);
            Pref.setBoolean("needs_permission_check", false);
        }
    }
}
