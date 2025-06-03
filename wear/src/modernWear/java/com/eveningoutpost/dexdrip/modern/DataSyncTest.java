package com.eveningoutpost.dexdrip.modern;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.xdrip;

import java.util.UUID;

/**
 * Helper class to test data synchronization between phone and watch
 */
public class DataSyncTest {
    private static final String TAG = "DataSyncTest";
    
    /**
     * Create a test BG reading to verify database functionality
     * This is only for testing purposes
     */
    public static void createTestReading() {
        try {
            // Make sure we have a sensor
            Sensor sensor = Sensor.currentSensor();
            if (sensor == null) {
                Log.d(TAG, "No active sensor found, creating one");
                sensor = Sensor.create(JoH.tsl());
                if (sensor == null) {
                    Log.e(TAG, "Failed to create sensor");
                    return;
                }
            }
            
            // Create a test reading
            BgReading bgReading = new BgReading();
            bgReading.timestamp = JoH.tsl();
            bgReading.calculated_value = 120; // 120 mg/dL
            bgReading.filtered_calculated_value = 120;
            bgReading.raw_data = 120000;
            bgReading.filtered_data = 120000;
            bgReading.uuid = UUID.randomUUID().toString();
            bgReading.sensor = sensor;
            bgReading.sensor_uuid = sensor.uuid;
            bgReading.save();
            
            Log.e(TAG, "Created test BG reading: " + bgReading.uuid);
            Log.e(TAG, "Total BG readings in database: " + BgReading.getLatestCount());
            
            // Log all readings
            logAllReadings();
        } catch (Exception e) {
            Log.e(TAG, "Error creating test reading: " + e);
        }
    }
    
    /**
     * Log all BG readings in the database
     */
    public static void logAllReadings() {
        try {
            int count = BgReading.getLatestCount();
            Log.e(TAG, "Total BG readings in database: " + count);
            
            if (count > 0) {
                BgReading lastReading = BgReading.last();
                if (lastReading != null) {
                    Log.e(TAG, "Last reading: value=" + lastReading.calculated_value + 
                          ", time=" + JoH.dateTimeText(lastReading.timestamp));
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error logging readings: " + e);
        }
    }
    
    /**
     * Check if the ListenerService is properly registered
     */
    public static void checkListenerService() {
        try {
            // Log the current state
            Log.e(TAG, "Checking ListenerService registration");
            Log.e(TAG, "Current sensor: " + (Sensor.currentSensor() != null ? 
                  Sensor.currentSensor().uuid : "null"));
            
            // Create a test intent to broadcast locally
            Context context = xdrip.getAppContext();
            Intent intent = new Intent();
            intent.setAction("com.eveningoutpost.dexdrip.DATA_TEST");
            intent.putExtra("test_time", JoH.tsl());
            context.sendBroadcast(intent);
            
            Log.e(TAG, "Sent test broadcast");
            
            // Create a test reading
            createTestReading();
        } catch (Exception e) {
            Log.e(TAG, "Error checking ListenerService: " + e);
        }
    }
}