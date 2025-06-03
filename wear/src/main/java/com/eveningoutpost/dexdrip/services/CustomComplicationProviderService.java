/*
 * Copyright (C) 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// provided by lurosys

package com.eveningoutpost.dexdrip.services;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.wear.watchface.complications.data.ComplicationData;
import androidx.wear.watchface.complications.data.ComplicationType;
import androidx.wear.watchface.complications.data.LongTextComplicationData;
import androidx.wear.watchface.complications.data.PlainComplicationText;
import androidx.wear.watchface.complications.data.ShortTextComplicationData;
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService;
import androidx.wear.watchface.complications.datasource.ComplicationRequest;

import com.eveningoutpost.dexdrip.models.BgReading;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Pref;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;

import static com.eveningoutpost.dexdrip.utilitymodels.BgGraphBuilder.unitizedDeltaString;

/**
 * Complication data provider for xDrip+ glucose data.
 * Provides glucose readings, delta, and time since last reading.
 */
// This is a placeholder implementation that will be replaced in the modernWear flavor
// The ComplicationDataSourceService API has changed between versions
public abstract class CustomComplicationProviderService {

    private static final String TAG = "ComplicationProvider";
    private static final long STALE_MS = Constants.MINUTE_IN_MS * 15;
    private static final long FRESH_MS = Constants.MINUTE_IN_MS * 5;

    public enum COMPLICATION_STATE {
        DELTA(0),
        AGO(1),
        RESET(2);

        private int enum_value;

        COMPLICATION_STATE(int value) {
            this.enum_value = value;
        }

        public int getValue() {
            return enum_value;
        }
    }

    // This method will be implemented in the modernWear flavor
    public ComplicationData onComplicationRequest(
            @NonNull ComplicationRequest request) {
        
        Log.d(TAG, "onComplicationRequest() id: " + request.getComplicationInstanceId());
        
        // This is a placeholder implementation
        return null;
    }

    // Placeholder method - will be implemented in the modernWear flavor
    private ComplicationData createComplicationData(
            ComplicationType type,
            BgReading bgReading,
            COMPLICATION_STATE state) {
        return null;
    }
    
    // Placeholder methods - will be implemented in the modernWear flavor
    private ShortTextComplicationData createShortTextComplicationData(
            BgReading bgReading,
            COMPLICATION_STATE state,
            PendingIntent tapAction) {
        return null;
    }
    
    private LongTextComplicationData createLongTextComplicationData(
            BgReading bgReading,
            COMPLICATION_STATE state,
            PendingIntent tapAction) {
        return null;
    }

    // This method will be implemented in the modernWear flavor
    public ComplicationData getPreviewData(
            @NonNull ComplicationType type) {
        // This is a placeholder implementation
        return null;
    }

    public static void refresh() {
        Log.d(TAG, "Refresh requested");
        // Request a complication update
        // This will be handled by the ComplicationDataSourceUpdateRequester in a future update
    }
}
