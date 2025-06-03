package com.eveningoutpost.dexdrip.modern;

import android.content.Context;
import android.util.Log;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Sensor;

/**
 * Helper class to initialize data for the modern wear app
 * This addresses the issue where the app shows "No Data" despite being connected to the phone
 */
public class ModernDataInitializer {
    private static final String TAG = "ModernDataInitializer";
    
    /**
     * Initialize the database with necessary data
     */
    public static void initializeData(Context context) {
        Log.d(TAG, "Initializing data for modern wear app");
        
        try {
            // Initialize database
            Sensor.InitDb(context);
            
            // Check if we have an active sensor
            Sensor sensor = Sensor.currentSensor();
            if (sensor == null) {
                Log.e(TAG, "No active sensor found, creating one");
                try {
                    // Create a sensor directly - this is critical for data flow
                    sensor = Sensor.create(JoH.tsl());
                    if (sensor != null) {
                        Log.e(TAG, "Successfully created sensor: " + sensor.uuid);
                    } else {
                        Log.e(TAG, "Failed to create sensor");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error creating sensor: " + e);
                }
            } else {
                Log.d(TAG, "Active sensor found: " + sensor.uuid);
            }
            
            // Log database state
            try {
                int bgCount = BgReading.getLatestCount();
                Log.d(TAG, "BG readings count: " + bgCount);
                
                if (bgCount > 0) {
                    BgReading lastReading = BgReading.last();
                    if (lastReading != null) {
                        Log.d(TAG, "Last reading: value=" + lastReading.calculated_value +
                              ", time=" + JoH.dateTimeText(lastReading.timestamp));
                        
                        // Ensure reading is associated with sensor
                        if (sensor != null && lastReading.sensor == null) {
                            Log.e(TAG, "Reading has no sensor, associating with current sensor");
                            lastReading.sensor = sensor;
                            lastReading.sensor_uuid = sensor.uuid;
                            lastReading.save();
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error checking BG readings: " + e);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error initializing data: " + e);
        }
    }
}