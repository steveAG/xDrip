package com.eveningoutpost.dexdrip;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.eveningoutpost.dexdrip.models.UserError;

/**
 * Simple activity to display information about the watch faces
 */
public class WatchFaceLauncherActivity extends Activity {
    private static final String TAG = "WatchFaceLauncher";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Set the content view from the layout file
        setContentView(R.layout.activity_watch_face_launcher);
        
        // Set up the button click listener
        Button openPickerButton = findViewById(R.id.open_picker_button);
        openPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    // Log available watch faces
                    UserError.Log.e(TAG, "Attempting to launch watch face picker");
                    UserError.Log.e(TAG, "Package name: " + getPackageName());
                    
                    // Try to launch the watch face picker using the standard intent
                    Intent intent = new Intent();
                    intent.setAction("android.settings.WATCHFACE_PICKER");
                    UserError.Log.e(TAG, "Launching intent: " + intent.toString());
                    startActivity(intent);
                } catch (Exception e) {
                    UserError.Log.e(TAG, "Error launching watch face picker: " + e);
                    
                    // Try an alternative approach
                    try {
                        UserError.Log.e(TAG, "Trying alternative approach");
                        Intent intent = new Intent();
                        intent.setAction("com.google.android.wearable.action.WATCHFACE_CONFIG");
                        UserError.Log.e(TAG, "Launching alternative intent: " + intent.toString());
                        startActivity(intent);
                    } catch (Exception e2) {
                        UserError.Log.e(TAG, "Error launching alternative watch face picker: " + e2);
                        Toast.makeText(WatchFaceLauncherActivity.this,
                                "Could not open watch face picker: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }
}