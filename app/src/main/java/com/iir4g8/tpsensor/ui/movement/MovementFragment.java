package com.iir4g8.tpsensor.ui.movement;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.iir4g8.tpsensor.R;

public class MovementFragment extends Fragment implements SensorEventListener {

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private Sensor magnetometer;
    private Sensor gyroscope;

    private TextView azimuthText;
    private TextView pitchText;
    private TextView rollText;
    private TextView azimuthDirectionText;
    private TextView movementStatusText;

    // 3D rendering
    private Axis3DView axis3DView;

    private float[] lastAccelerometer = new float[3];
    private float[] lastMagnetometer = new float[3];
    private float[] rotationMatrix = new float[9];
    private float[] orientation = new float[3];

    private boolean hasAccelerometerData = false;
    private boolean hasMagnetometerData = false;

    private float lastAzimuth = 0f;
    private float lastPitch = 0f;
    private float lastRoll = 0f;

    private float movementThreshold = 2.0f; // Degrees
    private boolean isMoving = false;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable movementTimeoutRunnable;

    public MovementFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);

        // Initialize sensors
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        // Create movement timeout runnable
        movementTimeoutRunnable = new Runnable() {
            @Override
            public void run() {
                if (isMoving) {
                    isMoving = false;
                    updateMovementStatus();
                }
            }
        };
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_movement, container, false);

        // Initialize TextViews
        azimuthText = root.findViewById(R.id.text_azimuth);
        pitchText = root.findViewById(R.id.text_pitch);
        rollText = root.findViewById(R.id.text_roll);
        azimuthDirectionText = root.findViewById(R.id.text_azimuth_direction);
        movementStatusText = root.findViewById(R.id.text_movement_status);

        // Initialize 3D renderer
        setupRenderer(root);

        return root;
    }

    private void setupRenderer(View root) {
        // Create the 3D view
        axis3DView = new Axis3DView(getContext());

        // Add to the container
        FrameLayout container = root.findViewById(R.id.device_3d_container);
        container.addView(axis3DView);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Register sensor listeners
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        } else {
            Toast.makeText(getContext(), "Accelerometer not available", Toast.LENGTH_SHORT).show();
        }

        if (magnetometer != null) {
            sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
        } else {
            Toast.makeText(getContext(), "Magnetometer not available", Toast.LENGTH_SHORT).show();
        }

        if (gyroscope != null) {
            sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_GAME);
        }

        // Resume the OpenGL surface
        if (axis3DView != null) {
            axis3DView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        handler.removeCallbacks(movementTimeoutRunnable);

        // Pause the OpenGL surface
        if (axis3DView != null) {
            axis3DView.onPause();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.length);
            hasAccelerometerData = true;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.length);
            hasMagnetometerData = true;
        }

        if (hasAccelerometerData && hasMagnetometerData) {
            boolean success = SensorManager.getRotationMatrix(
                    rotationMatrix, null, lastAccelerometer, lastMagnetometer);

            if (success) {
                SensorManager.getOrientation(rotationMatrix, orientation);

                // Convert from radians to degrees
                float azimuth = (float) Math.toDegrees(orientation[0]); // Z-axis rotation
                float pitch = (float) Math.toDegrees(orientation[1]);   // X-axis rotation
                float roll = (float) Math.toDegrees(orientation[2]);    // Y-axis rotation

                // Normalize azimuth to 0-360
                if (azimuth < 0) {
                    azimuth += 360;
                }

                // Update UI
                updateOrientationDisplay(azimuth, pitch, roll);

                // Check for movement
                checkForMovement(azimuth, pitch, roll);

                // Update 3D model rotation
                if (axis3DView != null) {
                    axis3DView.setOrientation(azimuth, pitch, roll);
                }

                // Save current values for next comparison
                lastAzimuth = azimuth;
                lastPitch = pitch;
                lastRoll = roll;
            }
        }
    }

    private void updateOrientationDisplay(float azimuth, float pitch, float roll) {
        // Update text views with formatted values
        azimuthText.setText(String.format("%.1f°", azimuth));
        pitchText.setText(String.format("%.1f°", pitch));
        rollText.setText(String.format("%.1f°", roll));

        // Update direction text (N, NE, E, SE, S, SW, W, NW)
        String direction = getDirectionFromAzimuth(azimuth);
        azimuthDirectionText.setText("(" + direction + ")");
    }

    private String getDirectionFromAzimuth(float azimuth) {
        if (azimuth >= 337.5 || azimuth < 22.5) {
            return "N";
        } else if (azimuth >= 22.5 && azimuth < 67.5) {
            return "NE";
        } else if (azimuth >= 67.5 && azimuth < 112.5) {
            return "E";
        } else if (azimuth >= 112.5 && azimuth < 157.5) {
            return "SE";
        } else if (azimuth >= 157.5 && azimuth < 202.5) {
            return "S";
        } else if (azimuth >= 202.5 && azimuth < 247.5) {
            return "SW";
        } else if (azimuth >= 247.5 && azimuth < 292.5) {
            return "W";
        } else {
            return "NW";
        }
    }

    private void checkForMovement(float azimuth, float pitch, float roll) {
        // Calculate the absolute difference between current and last values
        float azimuthDiff = Math.abs(azimuth - lastAzimuth);
        // Handle the case when crossing the 0/360 boundary
        if (azimuthDiff > 180) {
            azimuthDiff = 360 - azimuthDiff;
        }

        float pitchDiff = Math.abs(pitch - lastPitch);
        float rollDiff = Math.abs(roll - lastRoll);

        // Check if any of the differences exceed the threshold
        if (azimuthDiff > movementThreshold ||
                pitchDiff > movementThreshold ||
                rollDiff > movementThreshold) {

            // Device is moving
            if (!isMoving) {
                isMoving = true;
                updateMovementStatus();
            }

            // Reset the timeout
            handler.removeCallbacks(movementTimeoutRunnable);
            handler.postDelayed(movementTimeoutRunnable, 1000); // 1 second timeout
        }
    }

    private void updateMovementStatus() {
        if (isMoving) {
            movementStatusText.setText("Device is moving");
            movementStatusText.setTextColor(Color.RED);
        } else {
            movementStatusText.setText("Device is stationary");
            movementStatusText.setTextColor(Color.GREEN);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }
}
