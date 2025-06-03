package com.eveningoutpost.dexdrip.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;

import com.eveningoutpost.dexdrip.LocationPermissionActivity;
import com.eveningoutpost.dexdrip.SensorPermissionActivity;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Centralized permission management for handling runtime permissions across different API levels
 */
public class PermissionManager {
    private static final String TAG = PermissionManager.class.getSimpleName();
    
    // Permission request codes
    public static final int PERMISSION_REQUEST_LOCATION = 1;
    public static final int PERMISSION_REQUEST_BODY_SENSORS = 2;
    public static final int PERMISSION_REQUEST_BLUETOOTH = 3;
    public static final int PERMISSION_REQUEST_STORAGE = 4;
    public static final int PERMISSION_REQUEST_NOTIFICATION = 5;
    public static final int PERMISSION_REQUEST_BACKGROUND_LOCATION = 6;
    
    // Permission groups
    private static final Map<String, Integer> PERMISSION_GROUPS = new HashMap<>();
    static {
        PERMISSION_GROUPS.put(Manifest.permission.ACCESS_FINE_LOCATION, PERMISSION_REQUEST_LOCATION);
        PERMISSION_GROUPS.put(Manifest.permission.ACCESS_COARSE_LOCATION, PERMISSION_REQUEST_LOCATION);
        PERMISSION_GROUPS.put(Manifest.permission.BODY_SENSORS, PERMISSION_REQUEST_BODY_SENSORS);
        PERMISSION_GROUPS.put(Manifest.permission.BLUETOOTH, PERMISSION_REQUEST_BLUETOOTH);
        PERMISSION_GROUPS.put(Manifest.permission.BLUETOOTH_ADMIN, PERMISSION_REQUEST_BLUETOOTH);
        PERMISSION_GROUPS.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PERMISSION_REQUEST_STORAGE);
        PERMISSION_GROUPS.put(Manifest.permission.READ_EXTERNAL_STORAGE, PERMISSION_REQUEST_STORAGE);
    }
    
    // Add API 31+ Bluetooth permissions
    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PERMISSION_GROUPS.put(Manifest.permission.BLUETOOTH_SCAN, PERMISSION_REQUEST_BLUETOOTH);
            PERMISSION_GROUPS.put(Manifest.permission.BLUETOOTH_CONNECT, PERMISSION_REQUEST_BLUETOOTH);
        }
    }
    
    // Add API 33+ Notification permission
    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            PERMISSION_GROUPS.put(Manifest.permission.POST_NOTIFICATIONS, PERMISSION_REQUEST_NOTIFICATION);
        }
    }
    
    // Add API 34+ Body Sensors Background permission
    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            PERMISSION_GROUPS.put(Manifest.permission.BODY_SENSORS_BACKGROUND, PERMISSION_REQUEST_BODY_SENSORS);
        }
    }
    
    /**
     * Check if a permission is granted
     * @param context The context
     * @param permission The permission to check
     * @return True if the permission is granted, false otherwise
     */
    public static boolean hasPermission(Context context, String permission) {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED;
    }
    
    /**
     * Check if all permissions in a list are granted
     * @param context The context
     * @param permissions The permissions to check
     * @return True if all permissions are granted, false otherwise
     */
    public static boolean hasAllPermissions(Context context, String... permissions) {
        for (String permission : permissions) {
            if (!hasPermission(context, permission)) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Request a permission
     * @param activity The activity
     * @param permission The permission to request
     */
    public static void requestPermission(Activity activity, String permission) {
        Integer requestCode = PERMISSION_GROUPS.get(permission);
        if (requestCode == null) {
            requestCode = 0;
        }
        ActivityCompat.requestPermissions(activity, new String[]{permission}, requestCode);
    }
    
    /**
     * Request multiple permissions
     * @param activity The activity
     * @param permissions The permissions to request
     */
    public static void requestPermissions(Activity activity, String... permissions) {
        ActivityCompat.requestPermissions(activity, permissions, 0);
    }
    
    /**
     * Check and request location permissions
     * @param activity The activity
     * @return True if permission is already granted, false if it needs to be requested
     */
    public static boolean checkAndRequestLocationPermission(Activity activity) {
        if (!hasPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
            Intent intent = new Intent(activity, LocationPermissionActivity.class);
            activity.startActivity(intent);
            return false;
        }
        return true;
    }
    
    /**
     * Check and request body sensors permission
     * @param activity The activity
     * @return True if permission is already granted, false if it needs to be requested
     */
    public static boolean checkAndRequestBodySensorsPermission(Activity activity) {
        if (!hasPermission(activity, Manifest.permission.BODY_SENSORS)) {
            Intent intent = new Intent(activity, SensorPermissionActivity.class);
            activity.startActivity(intent);
            return false;
        }
        
        // For API 34+, also check for background body sensors permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (!hasPermission(activity, Manifest.permission.BODY_SENSORS_BACKGROUND)) {
                // For background permissions, we need to direct users to settings
                // as they can only be granted via settings
                JoH.showNotification("Background Sensor Permission Required", 
                        "Please grant background body sensors permission in settings", 
                        null, 778, true, true, false);
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                intent.setData(uri);
                activity.startActivity(intent);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Check and request Bluetooth permissions based on API level
     * @param activity The activity
     * @return True if all required permissions are granted, false otherwise
     */
    public static boolean checkAndRequestBluetoothPermissions(Activity activity) {
        List<String> permissionsNeeded = new ArrayList<>();
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (API 31+) uses new Bluetooth permissions
            if (!hasPermission(activity, Manifest.permission.BLUETOOTH_SCAN)) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN);
            }
            if (!hasPermission(activity, Manifest.permission.BLUETOOTH_CONNECT)) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT);
            }
        } else {
            // Older versions use BLUETOOTH and BLUETOOTH_ADMIN
            if (!hasPermission(activity, Manifest.permission.BLUETOOTH)) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH);
            }
            if (!hasPermission(activity, Manifest.permission.BLUETOOTH_ADMIN)) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_ADMIN);
            }
            
            // Location permission is required for Bluetooth scanning on API 23-30
            if (!hasPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
                return checkAndRequestLocationPermission(activity);
            }
        }
        
        if (!permissionsNeeded.isEmpty()) {
            requestPermissions(activity, permissionsNeeded.toArray(new String[0]));
            return false;
        }
        
        return true;
    }
    
    /**
     * Check and request notification permission for API 33+
     * @param activity The activity
     * @return True if permission is granted or not needed, false if it needs to be requested
     */
    public static boolean checkAndRequestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasPermission(activity, Manifest.permission.POST_NOTIFICATIONS)) {
                requestPermission(activity, Manifest.permission.POST_NOTIFICATIONS);
                return false;
            }
        }
        return true;
    }
    
    /**
     * Check and request storage permissions
     * @param activity The activity
     * @return True if permissions are granted, false if they need to be requested
     */
    public static boolean checkAndRequestStoragePermissions(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // For API 29+, we should use scoped storage instead of requesting permissions
            // But for backward compatibility, we still request them
            if (!hasPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                requestPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
                return false;
            }
        } else {
            if (!hasPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) ||
                !hasPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)) {
                String[] permissions = {
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                };
                requestPermissions(activity, permissions);
                return false;
            }
        }
        return true;
    }
    
    /**
     * Check and request background location permission for API 29+
     * @param activity The activity
     * @return True if permission is granted or not needed, false otherwise
     */
    public static boolean checkAndRequestBackgroundLocationPermission(Activity activity) {
        // First ensure we have foreground location permission
        if (!hasPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)) {
            return checkAndRequestLocationPermission(activity);
        }
        
        // Then check for background location permission on API 29+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (!hasPermission(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                // Background location permission must be requested in settings
                JoH.showNotification("Background Location Required", 
                        "Please grant background location permission in settings", 
                        null, 779, true, true, false);
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
                intent.setData(uri);
                activity.startActivity(intent);
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Process permission request results
     * @param requestCode The request code
     * @param permissions The permissions that were requested
     * @param grantResults The results of the permission requests
     * @return True if all permissions were granted, false otherwise
     */
    public static boolean onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: " + requestCode);
        
        if (grantResults.length > 0) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Permission denied: " + permissions[i]);
                    return false;
                }
            }
            return true;
        }
        
        return false;
    }
}