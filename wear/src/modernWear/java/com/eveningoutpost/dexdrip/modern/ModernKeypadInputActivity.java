package com.eveningoutpost.dexdrip.modern;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.eveningoutpost.dexdrip.R;

/**
 * Modern implementation of KeypadInputActivity for Wear OS 5+
 *
 * This activity provides an enhanced interface for the modernWear flavor
 * with Material Design components and improved user experience.
 */
public class ModernKeypadInputActivity extends AppCompatActivity {

    private static final String TAG = "ModernKeypadActivity";
    private static final boolean D = false;

    // Input types
    public static final String INTENT_EXTRA_INPUT_TYPE = "InputType";
    public static final String INTENT_EXTRA_INITIAL_VALUE = "InitialValue";
    public static final String INTENT_EXTRA_UNITS = "Units";
    
    public static final int INPUT_TYPE_CALIBRATION = 1;
    public static final int INPUT_TYPE_TREATMENT = 2;
    
    private static double lastValue = 0;
    private static int lastInputType = INPUT_TYPE_CALIBRATION;
    
    private int inputType;
    private String units = "";
    private StringBuilder inputBuffer = new StringBuilder();
    private TextView valueView;
    private TextView titleView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modern_keypad_input);
        
        // Get input type from intent
        Intent intent = getIntent();
        inputType = intent.getIntExtra(INTENT_EXTRA_INPUT_TYPE, lastInputType);
        lastInputType = inputType;
        
        // Get initial value if provided
        double initialValue = intent.getDoubleExtra(INTENT_EXTRA_INITIAL_VALUE, lastValue);
        if (initialValue > 0) {
            inputBuffer.append(String.valueOf((int)initialValue));
        }
        
        // Get units if provided
        units = intent.getStringExtra(INTENT_EXTRA_UNITS);
        if (units == null) units = "";
        
        // Set up UI
        setupUI();
    }
    
    private void setupUI() {
        // Set title based on input type
        titleView = findViewById(R.id.keypad_title);
        if (titleView != null) {
            if (inputType == INPUT_TYPE_CALIBRATION) {
                titleView.setText(R.string.enter_calibration);
            } else if (inputType == INPUT_TYPE_TREATMENT) {
                titleView.setText(R.string.enter_treatment);
            }
        }
        
        // Set up value display
        valueView = findViewById(R.id.keypad_value);
        updateValueDisplay();
        
        // Set up keypad buttons
        setupKeypadButtons();
        
        // Set up action buttons
        setupActionButtons();
    }
    
    private void setupKeypadButtons() {
        // Set up number buttons 0-9
        for (int i = 0; i <= 9; i++) {
            final int digit = i;
            int buttonId = getResources().getIdentifier("button_" + i, "id", getPackageName());
            MaterialButton button = findViewById(buttonId);
            if (button != null) {
                button.setOnClickListener(v -> onDigitPressed(digit));
            }
        }
        
        // Set up decimal button
        MaterialButton decimalButton = findViewById(R.id.button_decimal);
        if (decimalButton != null) {
            decimalButton.setOnClickListener(v -> onDecimalPressed());
        }
        
        // Set up delete button
        MaterialButton deleteButton = findViewById(R.id.button_delete);
        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> onDeletePressed());
            // Add long press to clear all
            deleteButton.setOnLongClickListener(v -> {
                inputBuffer.setLength(0);
                updateValueDisplay();
                return true;
            });
        }
    }
    
    private void setupActionButtons() {
        // Set up cancel button
        MaterialButton cancelButton = findViewById(R.id.button_cancel);
        if (cancelButton != null) {
            cancelButton.setOnClickListener(v -> onCancelPressed());
        }
        
        // Set up done button
        MaterialButton doneButton = findViewById(R.id.button_done);
        if (doneButton != null) {
            doneButton.setOnClickListener(v -> onDonePressed());
        }
    }
    
    private void onDigitPressed(int digit) {
        // Add digit to input buffer
        inputBuffer.append(digit);
        updateValueDisplay();
        // Add haptic feedback
        performHapticFeedback();
    }
    
    private void onDecimalPressed() {
        // Add decimal point if not already present
        if (!inputBuffer.toString().contains(".")) {
            if (inputBuffer.length() == 0) {
                inputBuffer.append("0");
            }
            inputBuffer.append(".");
            updateValueDisplay();
            // Add haptic feedback
            performHapticFeedback();
        }
    }
    
    private void onDeletePressed() {
        // Remove last character from input buffer
        if (inputBuffer.length() > 0) {
            inputBuffer.deleteCharAt(inputBuffer.length() - 1);
            updateValueDisplay();
            // Add haptic feedback
            performHapticFeedback();
        }
    }
    
    private void onCancelPressed() {
        // Cancel input and finish activity
        setResult(RESULT_CANCELED);
        finish();
    }
    
    private void onDonePressed() {
        // Validate input
        if (inputBuffer.length() == 0) {
            Toast.makeText(this, R.string.please_enter_value, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Parse value
        double value;
        try {
            value = Double.parseDouble(inputBuffer.toString());
        } catch (NumberFormatException e) {
            Toast.makeText(this, R.string.invalid_value, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Validate value range
        if (value <= 0) {
            Toast.makeText(this, R.string.value_must_be_positive, Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Store value for next time
        lastValue = value;
        
        // Return value to caller
        Intent resultIntent = new Intent();
        resultIntent.putExtra("value", value);
        resultIntent.putExtra("input_type", inputType);
        setResult(RESULT_OK, resultIntent);
        
        // Process value based on input type
        processValue(value);
        
        // Finish activity
        finish();
    }
    
    private void processValue(double value) {
        // Process value based on input type
        if (inputType == INPUT_TYPE_CALIBRATION) {
            // Process calibration
            // This would typically call a method to save the calibration
            // and update the glucose values
            Toast.makeText(this, getString(R.string.calibration_saved, value), Toast.LENGTH_SHORT).show();
        } else if (inputType == INPUT_TYPE_TREATMENT) {
            // Process treatment
            // This would typically call a method to save the treatment
            Toast.makeText(this, getString(R.string.treatment_saved, value), Toast.LENGTH_SHORT).show();
        }
    }
    
    private void updateValueDisplay() {
        // Update value display
        if (valueView != null) {
            String displayText = inputBuffer.toString();
            if (displayText.isEmpty()) {
                displayText = "0";
            }
            if (!units.isEmpty()) {
                displayText += " " + units;
            }
            valueView.setText(displayText);
        }
    }
    
    private void performHapticFeedback() {
        // Perform haptic feedback
        View view = findViewById(R.id.keypad_root_layout);
        if (view != null) {
            view.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
        }
    }
    
    /**
     * Reset stored values
     */
    public static void resetValues() {
        lastValue = 0;
        lastInputType = INPUT_TYPE_CALIBRATION;
    }
}