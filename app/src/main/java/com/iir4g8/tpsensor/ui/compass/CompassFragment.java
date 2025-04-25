package com.iir4g8.tpsensor.ui.compass;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.iir4g8.tpsensor.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class CompassFragment extends Fragment implements SensorEventListener {

    // define the display assembly compass picture
    private ImageView image;

    // record the compass picture angle turned
    private float currentDegree = 0f;

    // device sensor manager
    private SensorManager mSensorManager;

    private Sensor mCompassSensor;

    TextView tvHeading;
    private boolean sensorAvailable = true;

    public CompassFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_compass, container, false);

        // Make sure we're using the correct image resource
        image = (ImageView) root.findViewById(R.id.imageViewCompass);
        image.setImageResource(R.mipmap.compass); // Use compass.png instead of compass1.png if needed

        // TextView that will tell the user what degree is he heading
        tvHeading = (TextView) root.findViewById(R.id.tvHeading);

        if (!sensorAvailable) {
            tvHeading.setText("Compass sensor not available on this device");
        }

        return root;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSensorManager = (SensorManager)getActivity().getSystemService(Context.SENSOR_SERVICE);

        // Try to get the orientation sensor first
        mCompassSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);

        // If orientation sensor is not available, try to use the magnetic field and accelerometer sensors
        if(mCompassSensor == null){
            // Check if we have the required sensors to calculate orientation
            Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            Sensor magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

            if (accelerometer != null && magnetometer != null) {
                // We can calculate orientation using these sensors
                sensorAvailable = true;
            } else {
                sensorAvailable = false;
                Toast.makeText(getContext(), R.string.message_neg, Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (sensorAvailable) {
            if (mCompassSensor != null) {
                mSensorManager.registerListener(this, mCompassSensor, SensorManager.SENSOR_DELAY_GAME);
            } else {
                // Register for both accelerometer and magnetic field sensors
                Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                Sensor magnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

                mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
                mSensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_GAME);
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (sensorAvailable) {
            mSensorManager.unregisterListener(this);
        }
    }

    // Rotation matrix and orientation values for sensor fusion
    float[] rotationMatrix = new float[9];
    float[] orientation = new float[3];
    float[] lastAccelerometer = new float[3];
    float[] lastMagnetometer = new float[3];
    boolean hasAccelerometerData = false;
    boolean hasMagnetometerData = false;

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ORIENTATION) {
            // Direct orientation sensor (deprecated but still works on many devices)
            processOrientationSensor(event);
        } else {
            // Sensor fusion approach
            processSensorFusion(event);
        }
    }

    private void processOrientationSensor(SensorEvent event) {
        // Get the angle around the z-axis rotated
        float degree = Math.round(event.values[0]);
        tvHeading.setText("Heading: " + Float.toString(degree) + " degrees");

        // Create and start the rotation animation
        rotateCompassImage(degree);
    }

    private void processSensorFusion(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, lastAccelerometer, 0, event.values.length);
            hasAccelerometerData = true;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, lastMagnetometer, 0, event.values.length);
            hasMagnetometerData = true;
        }

        if (hasAccelerometerData && hasMagnetometerData) {
            SensorManager.getRotationMatrix(rotationMatrix, null, lastAccelerometer, lastMagnetometer);
            SensorManager.getOrientation(rotationMatrix, orientation);

            // Convert radians to degrees
            float degree = (float) Math.toDegrees(orientation[0]);

            // Normalize to 0-360
            if (degree < 0) {
                degree += 360;
            }

            degree = Math.round(degree);
            tvHeading.setText("Heading: " + Float.toString(degree) + " degrees");

            // Create and start the rotation animation
            rotateCompassImage(degree);
        }
    }

    private void rotateCompassImage(float degree) {
        // Create a rotation animation (reverse turn degree degrees)
        RotateAnimation ra = new RotateAnimation(
                currentDegree,
                -degree,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f);

        // How long the animation will take place
        ra.setDuration(210);

        // Set the animation after the end of the reservation status
        ra.setFillAfter(true);

        // Start the animation
        image.startAnimation(ra);

        // Save current degree for next rotation
        currentDegree = -degree;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }
}
