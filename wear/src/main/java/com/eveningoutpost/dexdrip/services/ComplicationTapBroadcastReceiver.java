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
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester;

import com.eveningoutpost.dexdrip.KeypadInputActivity;
import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.utilitymodels.PersistentStore;
import com.eveningoutpost.dexdrip.xdrip;

/**
 * BroadcastReceiver that handles complication taps and cycles through different
 * display states for the complication. Also provides a static method to create
 * a PendingIntent that triggers this receiver.
 */
public class ComplicationTapBroadcastReceiver extends BroadcastReceiver {

    private static final String TAG = "ComplicationTapReceiver";
    private static final String EXTRA_PROVIDER_COMPONENT =
            "com.eveningoutpost.dexdrip.wearable.watchface.provider.action.PROVIDER_COMPONENT";
    private static final String EXTRA_COMPLICATION_ID =
            "com.eveningoutpost.dexdrip.wearable.watchface.provider.action.COMPLICATION_ID";
    static final String COMPLICATION_STORE = "complication-state-enum";

    @Override
    public void onReceive(Context context, Intent intent) {
        final Bundle extras = intent.getExtras();
        if (extras == null) {
            Log.e(TAG, "No extras in intent");
            return;
        }
        
        final ComponentName provider = extras.getParcelable(EXTRA_PROVIDER_COMPONENT);
        int complicationId = extras.getInt(EXTRA_COMPLICATION_ID);

        // Cycle through the complication states
        PersistentStore.incrementLong(COMPLICATION_STORE);
        if (PersistentStore.getLong(COMPLICATION_STORE) == CustomComplicationProviderService.COMPLICATION_STATE.RESET.getValue()) {
            PersistentStore.setLong(COMPLICATION_STORE, 0);
        }

        // Request an update for the complication
        if (provider != null) {
            ComplicationDataSourceUpdateRequester requester =
                ComplicationDataSourceUpdateRequester.create(context, provider);
            requester.requestUpdateAll();
            Log.d(TAG, "Complication update requested for id: " + complicationId);
        }

        // Handle double-tap to open keypad input
        if (!JoH.ratelimit("complication-double-tap", 1)) {
            startIntent(KeypadInputActivity.class);
            // If we get more than two states we will need to handle restoring the previous state
            // instead of just incrementing so the complication remains where it was before the double tap
        }
    }

    private void startIntent(Class<?> name) {
        Intent intent = new Intent(xdrip.getAppContext(), name);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        xdrip.getAppContext().startActivity(intent);
    }

    /**
     * Returns a pending intent, suitable for use as a tap intent, that causes a complication to be
     * toggled and updated.
     */
    static PendingIntent getToggleIntent(
            Context context, ComponentName provider, int complicationId) {
        Intent intent = new Intent(context, ComplicationTapBroadcastReceiver.class);
        intent.putExtra(EXTRA_PROVIDER_COMPONENT, provider);
        intent.putExtra(EXTRA_COMPLICATION_ID, complicationId);

        // Pass complicationId as the requestCode to ensure that different complications get
        // different intents.
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }
        
        return PendingIntent.getBroadcast(context, complicationId, intent, flags);
    }

    /**
     * Returns the key for the shared preference used to hold the current state of a given
     * complication.
     */
    static String getPreferenceKey(ComponentName provider, int complicationId) {
        return provider.getClassName() + complicationId;
    }
}
