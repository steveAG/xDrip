package com.eveningoutpost.dexdrip.modern;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.util.Log;

import com.activeandroid.query.Select;
import com.eveningoutpost.dexdrip.Home;
import com.eveningoutpost.dexdrip.R;
import com.eveningoutpost.dexdrip.calibrations.CalibrationAbstract;
import com.eveningoutpost.dexdrip.models.ActiveBluetoothDevice;
import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.Sensor;
import com.eveningoutpost.dexdrip.models.SensorSanity;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utils.DexCollectionType;
import com.eveningoutpost.dexdrip.xdrip;
import com.eveningoutpost.dexdrip.modern.ModernDataInitializer;

import java.util.List;

import static com.eveningoutpost.dexdrip.calibrations.PluggableCalibration.getCalibrationPluginFromPreferences;

/**
 * BestGlucose implementation for modernWear flavor
 *
 * Simplified implementation that returns a default "No Data" message
 * to avoid database access issues in the tile services.
 */
public class ModernBestGlucose {

    final static String TAG = "BestGlucose";
    final static boolean d = true; // debug flag
    private static SharedPreferences prefs;

    public static class DisplayGlucose {
        private Boolean stale = null;
        private Double highMark = null;
        private Double lowMark = null;
        public boolean doMgDl = true; // mgdl/mmol
        public double mgdl = -1;    // displayable mgdl figure
        public double unitized_value = -1; // in local units
        public double delta_mgdl = 0; // displayable delta mgdl figure
        public double slope = 0; // slope metric mgdl/ms
        public double noise = -1; // noise value
        public int warning = -1;  // warning level
        public long mssince = -1;
        public long timestamp = -1; // timestamp of reading
        public String unitized = "---";
        public String unitized_delta = "";
        public String unitized_delta_no_units = "";
        public String delta_arrow = ""; // unicode delta arrow
        public String delta_name = "";
        public String extra_string = "";
        public String plugin_name = ""; // plugin which generated this data
        public boolean from_plugin = false; // whether a plugin was used

        // Display getters - built in caching where appropriate

        public String minutesAgo() {
            return minutesAgo(false);
        }

        public String minutesAgo(boolean include_words) {
            final int minutes = ((int) (this.mssince / 60000));
            return Integer.toString(minutes) + (include_words ? (((minutes == 1) ? xdrip.getAppContext().getString(R.string.space_minute_ago) : xdrip.getAppContext().getString(R.string.space_minutes_ago))) : "");
        }

        // return boolean if data would be considered stale
        public boolean isStale() {
            if (this.stale == null) {
                this.stale = this.mssince > Home.stale_data_millis();
            }
            return this.stale;
        }

        // return boolean if data would be considered stale
        public boolean isReallyStale() {
            return this.mssince > (Home.stale_data_millis()*3);
        }

        // is this value above the "High" preference value
        public boolean isHigh() {
            if (this.highMark == null)
                this.highMark = JoH.tolerantParseDouble(prefs.getString("highValue", "170"), 170d);
            return this.unitized_value >= this.highMark;
        }

        // is this value below the "Low" preference value
        public boolean isLow() {
            if (this.lowMark == null)
                this.lowMark = JoH.tolerantParseDouble(prefs.getString("lowValue", "70"), 70d);
            return this.unitized_value <= this.lowMark;
        }

        // return strikeout string if data is high/low / stale
        public SpannableString spannableString(String str) {
            return spannableString(str, false);
        }

        // return a coloured strikeout string based on boolean
        public SpannableString spannableString(String str, boolean color) {
            final SpannableString ret = new SpannableString((str != null) ? str : "");
            if (isStale()) wholeSpan(ret, new StrikethroughSpan());
            if (color) {
                if (isLow()) {
                    wholeSpan(ret, new ForegroundColorSpan(0xC30909));
                } else if (isHigh()) {
                    wholeSpan(ret, new ForegroundColorSpan(0xFFBB33));
                }
            }
            return ret;
        }

        // set the whole spannable string to whatever this span is
        private void wholeSpan(SpannableString ret, Object what) {
            ret.setSpan(what, 0, ret.length(), 0);
        }

        public String humanSummary() {
            return unitized + " " + (doMgDl ? "mg/dl" : "mmol/l") + (isStale() ? ", " + minutesAgo(true).toLowerCase() : "");
        }
    }

    /**
     * Get a DisplayGlucose object with default "No Data" values
     * This simplified implementation avoids database access issues
     */
    public static DisplayGlucose getDisplayGlucose() {
        try {
            Log.d(TAG, "getDisplayGlucose: Creating default DisplayGlucose");
            
            if (prefs == null) {
                prefs = PreferenceManager.getDefaultSharedPreferences(xdrip.getAppContext());
            }
            
            final DisplayGlucose dg = new DisplayGlucose();
            final boolean doMgdl = (prefs.getString("units", "mgdl").equals("mgdl"));
            
            dg.doMgDl = doMgdl;
            dg.unitized = "No Data";
            dg.delta_name = "Check App";
            dg.mssince = 0;
            dg.timestamp = JoH.tsl();
            
            // Initialize data if needed
            try {
                // Check if we need to initialize data
                Sensor sensor = Sensor.currentSensor();
                
                // Try to get ActiveBluetoothDevice but don't fail if it causes a security exception
                ActiveBluetoothDevice activeDevice = null;
                try {
                    activeDevice = ActiveBluetoothDevice.first();
                } catch (SecurityException e) {
                    Log.e(TAG, "Security exception when checking ActiveBluetoothDevice: " + e);
                    // Continue without checking the device - the ListenerService will handle data reception
                }
                
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
                    
                    // Initialize database
                    ModernDataInitializer.initializeData(xdrip.getAppContext());
                }
                
                // Log database state for debugging
                int readingCount = BgReading.getLatestCount();
                Log.d(TAG, "Database state: sensor=" + (sensor != null ? sensor.uuid : "null") +
                      ", readings=" + readingCount);
            } catch (Exception e) {
                Log.e(TAG, "Error initializing data: " + e);
            }
            
            // Try to get actual glucose readings without using ListenerService
            try {
                Log.d(TAG, "getDisplayGlucose: Trying to get actual readings directly");
                
                final boolean is_follower = Home.get_follower();
                Log.d(TAG, "getDisplayGlucose: is_follower=" + is_follower);
                
                // Check if we have a current sensor
                Sensor sensor = Sensor.currentSensor();
                Log.d(TAG, "getDisplayGlucose: currentSensor=" + (sensor != null ? sensor.uuid : "null"));
                
                // Check database state
                int bgReadingCount = 0;
                try {
                    bgReadingCount = new Select().from(BgReading.class).count();
                    Log.d(TAG, "getDisplayGlucose: Total BgReadings in database: " + bgReadingCount);
                } catch (Exception e) {
                    Log.e(TAG, "getDisplayGlucose: Error counting BgReadings: " + e);
                }
                
                // Get latest readings
                List<BgReading> last_2 = BgReading.latest(2);
                Log.d(TAG, "getDisplayGlucose: Got " + (last_2 != null ? last_2.size() : 0) + " readings");
                
                // Try getting readings without sensor constraint
                List<BgReading> anyReadings = null;
                try {
                    anyReadings = new Select()
                        .from(BgReading.class)
                        .where("calculated_value != 0")
                        .where("raw_data != 0")
                        .orderBy("timestamp desc")
                        .limit(2)
                        .execute();
                    Log.d(TAG, "getDisplayGlucose: Got " + (anyReadings != null ? anyReadings.size() : 0) + " readings without sensor constraint");
                } catch (Exception e) {
                    Log.e(TAG, "getDisplayGlucose: Error getting readings without sensor: " + e);
                }
                
                // Get last reading
                final BgReading lastBgReading = BgReading.last(is_follower);
                Log.e(TAG, "DEBUG: getDisplayGlucose: lastBgReading=" + (lastBgReading != null ?
                    "value=" + lastBgReading.calculated_value +
                    ", time=" + JoH.dateTimeText(lastBgReading.timestamp) +
                    ", uuid=" + lastBgReading.uuid +
                    ", sensor=" + (lastBgReading.sensor != null ? lastBgReading.sensor.uuid : "null") :
                    "null"));
                
                // If no readings available, return the default DisplayGlucose
                if (lastBgReading == null) {
                    Log.d(TAG, "getDisplayGlucose: No recent BG readings available");
                    
                    // Check if we have any data in the database at all
                    BgReading anyReading = BgReading.lastNoSenssor();
                    Log.d(TAG, "getDisplayGlucose: lastNoSensor=" + (anyReading != null ?
                        "found (value: " + anyReading.calculated_value + ", time: " + JoH.dateTimeText(anyReading.timestamp) + ")" :
                        "null"));
                    
                    return dg;
                }
                
                // Process the glucose data
                Log.d(TAG, "getDisplayGlucose: Processing glucose data");
                
                double estimate = lastBgReading.calculated_value;
                double filtered = lastBgReading.filtered_calculated_value;
                double previous_estimate = -1;
                double previous_filtered = -1;
                long previous_timestamp = -1;
                
                if (last_2 != null && last_2.size() == 2) {
                    previous_estimate = last_2.get(1).calculated_value;
                    previous_filtered = last_2.get(1).filtered_calculated_value;
                    previous_timestamp = last_2.get(1).timestamp;
                }
                
                dg.mssince = JoH.msSince(lastBgReading.timestamp);
                dg.timestamp = lastBgReading.timestamp;
                
                // Calculate slope and delta
                double slope = 0;
                double estimated_delta = 0;
                
                if (previous_timestamp > 0) {
                    slope = calculateSlope(estimate, lastBgReading.timestamp, previous_estimate, previous_timestamp);
                    estimated_delta = estimate - previous_estimate;
                    dg.slope = slope;
                }
                
                // Set values in DisplayGlucose
                dg.mgdl = estimate;
                dg.delta_mgdl = estimated_delta;
                dg.unitized_value = BgGraphBuilder.unitized(estimate, doMgdl);
                dg.unitized = BgGraphBuilder.unitized_string(estimate, doMgdl);
                
                // Set delta arrow and name
                if (slope != 0) {
                    dg.delta_arrow = BgReading.slopeToArrowSymbol(slope * 60000);
                    dg.delta_name = BgReading.slopeName(slope * 60000);
                }
                
                // Set delta string
                if (previous_timestamp > 0) {
                    dg.unitized_delta = unitizedDeltaString(true, true, doMgdl, estimate, lastBgReading.timestamp, previous_estimate, previous_timestamp);
                    dg.unitized_delta_no_units = unitizedDeltaString(false, true, doMgdl, estimate, lastBgReading.timestamp, previous_estimate, previous_timestamp);
                }
                
                Log.d(TAG, "getDisplayGlucose: Successfully processed glucose data: " + dg.unitized);
                return dg;
                
            } catch (Exception e) {
                Log.e(TAG, "Error getting actual glucose data: " + e);
                return dg; // Return the default DisplayGlucose
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating DisplayGlucose: " + e);
            
            // If we can't even create a default DisplayGlucose, return a hardcoded one
            DisplayGlucose fallback = new DisplayGlucose();
            fallback.unitized = "No Data";
            fallback.delta_name = "Error";
            
            return fallback;
        }
    }
    
    private static double calculateSlope(double value1, long timestamp1, double value2, long timestamp2) {
        if (timestamp1 == timestamp2 || value1 == value2) {
            return 0;
        } else {
            return (value2 - value1) / (timestamp2 - timestamp1);
        }
    }
    
    public static String unitizedDeltaString(boolean showUnit, boolean highGranularity, boolean doMgdl, double value1, long timestamp1, double value2, long timestamp2) {
        // timestamps invalid or too far apart return ???
        if ((timestamp1 < 0)
                || (timestamp2 < 0)
                || (value1 < 0)
                || (value2 < 0)
                || (timestamp2 > timestamp1)
                || (timestamp1 - timestamp2 > (20 * 60 * 1000)))
            return "???";

        double value = calculateSlope(value1, timestamp1, value2, timestamp2) * 5 * 60 * 1000;

        return BgGraphBuilder.unitizedDeltaStringRaw(showUnit, highGranularity, value, doMgdl);
    }
}