package com.eveningoutpost.dexdrip;

import android.content.Intent;
import android.os.PowerManager;
import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;


import com.eveningoutpost.dexdrip.models.JoH;
import com.eveningoutpost.dexdrip.models.UserError;
import com.eveningoutpost.dexdrip.utilitymodels.Constants;
import com.eveningoutpost.dexdrip.utilitymodels.Inevitable;
import com.eveningoutpost.dexdrip.utils.AdbInstaller;
import com.eveningoutpost.dexdrip.utils.CipherUtils;
import com.eveningoutpost.dexdrip.utils.VersionFixer;
// Removed: import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.tasks.Task; // Add Task import
import com.google.android.gms.wearable.ChannelClient; // Add ChannelClient import
import com.google.android.gms.wearable.Wearable; // Add Wearable import
import android.content.Context; // Add Context import

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import static com.eveningoutpost.dexdrip.ListenerService.apkBytesRead;
import static com.eveningoutpost.dexdrip.ListenerService.apkBytesVersion;

// jamorham

// work around Oreo bugs

public class ProcessAPKChannelDownload extends JobIntentService {

    private static final String TAG = ProcessAPKChannelDownload.class.getSimpleName();
    private static volatile byte[] apkBytesOutput = new byte[0];
    private static volatile ChannelClient.Channel channel;
    // Removed: private static volatile GoogleApiClient googleApiClient;

    public synchronized void process() {

        if (channel == null) { // Removed googleApiClient check
            if (JoH.ratelimit("channel-error-msg", 10)) {
                UserError.Log.wtf(TAG, "Could not process as channel is null!");
            }
            return;
        }

        // Use ChannelClient to get InputStream
        Task<InputStream> inputStreamTask = Wearable.getChannelClient(this).getInputStream(channel);
        inputStreamTask.addOnSuccessListener(inputStream -> {
            // Existing logic was wrapped in ResultCallback and new Thread
            // Keep the new Thread for background processing
            new Thread(() -> { // Start background thread for processing
                final PowerManager.WakeLock wl = JoH.getWakeLock("receive-apk-update", 300000);
                BufferedReader reader = null; // Declare reader outside try-catch for finally block
                try { // Outer try: Covers entire background processing including stream handling
                    android.util.Log.d(TAG, "Channel opened: Got InputStream");
                    if (inputStream == null) {
                        UserError.Log.e(TAG, "Channel input stream is NULL!");
                        return; // Exit thread if stream is null
                    }

                    try { // Inner try: Covers stream reading
                        reader = new BufferedReader(new InputStreamReader(inputStream));

                        // Protocol reading logic (unchanged)
                        final String versionId = reader.readLine();
                        UserError.Log.d(TAG, "Source version identifier: " + versionId);
                        final String sizeText = reader.readLine();
                        final int size = Integer.parseInt(sizeText);
                        final String startText = reader.readLine();
                        final int startAt = Integer.parseInt(startText);
                        if (!versionId.equals(apkBytesVersion)) {
                            UserError.Log.d(TAG, "New UUID to buffer: " + apkBytesVersion + " vs " + versionId);
                            apkBytesOutput = new byte[size];
                            apkBytesRead = 0;
                            apkBytesVersion = versionId;
                        }

                        if (apkBytesOutput.length != size) {
                            UserError.Log.d(TAG, "Buffer size wrong! us:" + apkBytesOutput.length + " vs " + size);
                            return;
                        }

                        if (startAt > apkBytesRead) {
                            UserError.Log.e(TAG, "Cannot start at position: " + startAt + " vs " + apkBytesRead);
                            return;
                        }

                        if (startAt != apkBytesRead) {
                            UserError.Log.d(TAG, "Setting start position to: " + startAt);
                            apkBytesRead = startAt;
                        }

                        // Data reading loop (unchanged)
                        while (apkBytesRead < apkBytesOutput.length) {
                            final int complete = (apkBytesRead * 100 / apkBytesOutput.length);
                            android.util.Log.d(TAG, "Preparing to read, total: " + apkBytesRead + " out of " + apkBytesOutput.length + " complete " + complete + "%");
                            if (JoH.quietratelimit("wear-update-notice", 5)) {
                                JoH.static_toast_long("Updating xDrip " + complete + "%");
                                if (JoH.quietratelimit("adb ping", 30)) {
                                    AdbInstaller.pingIfNoDemigod(null);
                                }
                            }

                            final long startedWaiting = JoH.tsl();
                            while (apkBytesRead < apkBytesOutput.length && inputStream.available() == 0) {
                                if (JoH.msSince(startedWaiting) > Constants.SECOND_IN_MS * 30) {
                                    UserError.Log.e(TAG, "Timed out waiting for new APK data!");
                                    Inevitable.task("re-request-apk", 5000, () -> {
                                        UserError.Log.d(TAG, "Asking to resume apk from: " + apkBytesRead);
                                        ListenerService.requestAPK(apkBytesRead);
                                    });
                                    return; // Exit thread on timeout
                                }
                                android.util.Log.d(TAG, "Pausing for new data");
                                JoH.threadSleep(1000);
                            }
                            final int bytesToRead = Math.min(inputStream.available(), apkBytesOutput.length - apkBytesRead);
                            UserError.Log.d(TAG, "Before read: " + bytesToRead);
                            if (bytesToRead > 0) {
                                apkBytesRead += inputStream.read(apkBytesOutput, apkBytesRead, bytesToRead);
                            }
                            UserError.Log.d(TAG, "After read");
                        }

                        // Processing after successful read (unchanged)
                        android.util.Log.d(TAG, "Received the following COMPLETE message: " + apkBytesRead);
                        UserError.Log.d(TAG, "APK sha256: " + CipherUtils.getSHA256(apkBytesOutput));
                        VersionFixer.runPackageInstaller(apkBytesOutput);
                        apkBytesOutput = new byte[0];
                        apkBytesRead = 0;
                        apkBytesVersion = "";

                    } catch (final IOException e) { // Catch exceptions during stream reading
                        if (channel != null) {
                            android.util.Log.w(TAG, "Could not read channel message Node ID: " + channel.getNodeId() + " Path: " + channel.getPath() + " Error message: " + e.getMessage() + " Error cause: " + e.getCause());
                        } else {
                            android.util.Log.w(TAG, "channel is null in ioexception: " + e);
                        }
                        // Consider re-requesting APK here as well?
                    } finally { // Inner finally: Close reader
                        try {
                            if (reader != null) {
                                reader.close();
                            }
                        } catch (final IOException e) {
                             android.util.Log.e(TAG, "Error closing reader: " + e.getMessage());
                        }
                    }

                } finally { // Outer finally: Release wakelock, close channel
                    android.util.Log.d(TAG, "Finally block before exit");
                    try {
                        // Use ChannelClient to close
                        if (channel != null) {
                             Wearable.getChannelClient(ProcessAPKChannelDownload.this).close(channel);
                             android.util.Log.d(TAG, "Channel closed in finally block.");
                        }
                    } catch (Exception e) {
                        android.util.Log.e(TAG, "Error closing channel in finally block: " + e.getMessage());
                    }
                    channel = null; // Clear static reference
                    JoH.releaseWakeLock(wl);
                }
            }).start(); // Start the background thread
        }).addOnFailureListener(e -> { // Handle failure to get InputStream
            UserError.Log.e(TAG, "Failed to get input stream: " + e.getMessage());
            // Close channel on failure to get stream
             if (channel != null) {
                 Wearable.getChannelClient(this).close(channel);
                 channel = null; // Clear static reference
                 android.util.Log.d(TAG, "Channel closed in onFailureListener.");
             }
        });
        UserError.Log.d(TAG, "Process exit with channel callback scheduled"); // Moved log statement inside method
        // Removed duplicated code block from original ResultCallback
    } // This is the correct closing brace for the process() method

        // Removed misplaced log statement (moved inside process method)
        // Removed misplaced closing brace below


    @Override
    protected void onHandleWork(@NonNull Intent intent) {
        UserError.Log.d(TAG, "onHandleWork enter");
        process();
        UserError.Log.d(TAG, "onHandleWork exit");

    }

    // Updated enqueueWork to accept Context instead of GoogleApiClient
    static synchronized void enqueueWork(final Context context, final ChannelClient.Channel current_channel) {
        UserError.Log.d(TAG, "EnqueueWork enter");
        if (context == null || current_channel == null) {
            UserError.Log.d(TAG, "Enqueue Work: Null input data!!");
            return;
        }
        // Removed: googleApiClient = client;
        channel = current_channel; // Keep static channel reference for now
        // Use the provided context to get the application context
        enqueueWork(context.getApplicationContext(), ProcessAPKChannelDownload.class, Constants.APK_DOWNLOAD_JOB_ID, new Intent());
    }

}
