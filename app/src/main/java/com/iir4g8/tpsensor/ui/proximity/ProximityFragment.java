package com.iir4g8.tpsensor.ui.proximity;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import com.iir4g8.tpsensor.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class ProximityFragment extends Fragment implements SensorEventListener {
    private LineChart chart;
    private SensorManager mSensorManager;
    private Sensor mProximitySensor;
    static ArrayList<Entry> entries = new ArrayList<>();
    private boolean useMockData = false;
    private Handler mockDataHandler;
    private Random random = new Random();
    private TextView statusText;
    private TextView currentValueText;
    private float maxRange = 10.0f; // Default max range

    public ProximityFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSensorManager = (SensorManager)getActivity().getSystemService(Context.SENSOR_SERVICE);
        mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);

        if(mProximitySensor == null){
            useMockData = true;
            mockDataHandler = new Handler();
        } else {
            // Get the maximum range of the proximity sensor
            maxRange = mProximitySensor.getMaximumRange();
            Log.d("ProximityFragment", "Proximity sensor max range: " + maxRange);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_proximity, container, false);
        chart = (LineChart) root.findViewById(R.id.chart);

        // Configure chart
        chart.getDescription().setText("Proximity Sensor Data");
        chart.getDescription().setTextSize(12f);

        // Set Y-axis range based on sensor's maximum range
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setAxisMaximum(maxRange);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setDrawGridLines(true);

        YAxis rightAxis = chart.getAxisRight();
        rightAxis.setEnabled(false);

        // Configure X-axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);

        // Add status text view
        statusText = new TextView(getContext());
        statusText.setTextSize(16);
        statusText.setPadding(20, 20, 20, 0);

        // Add current value text view
        currentValueText = new TextView(getContext());
        currentValueText.setTextSize(24);
        currentValueText.setPadding(20, 0, 20, 20);
        currentValueText.setTextColor(Color.BLACK);

        if (useMockData) {
            statusText.setText("Proximity sensor not available. Showing simulated data.");
            statusText.setTextColor(Color.RED);
        } else {
            statusText.setText("Using device proximity sensor");
            statusText.setTextColor(Color.GREEN);
        }

        // Add the text views to the layout
        ViewGroup parent = (ViewGroup) chart.getParent();
        parent.addView(statusText, 0);
        parent.addView(currentValueText, 1);

        return root;
    }

    private void addEntry(float value) {
        // Update the current value text
        currentValueText.setText("Current distance: " + value + " cm");

        entries.add(new Entry(entries.size(), value));

        LineDataSet dataSet;
        if (chart.getData() != null &&
                chart.getData().getDataSetCount() > 0) {
            dataSet = (LineDataSet) chart.getData().getDataSetByIndex(0);
            dataSet.addEntry(new Entry(entries.size() - 1, value));
            chart.getData().notifyDataChanged();
            chart.notifyDataSetChanged();
        } else {
            dataSet = new LineDataSet(entries, "Proximity - Distance (cm)");
            dataSet.setDrawCircles(true);
            dataSet.setCircleRadius(4f);
            dataSet.setDrawValues(false);
            dataSet.setLineWidth(2f);
            dataSet.setColor(Color.BLUE);
            dataSet.setCircleColor(Color.BLUE);
            dataSet.setHighLightColor(Color.rgb(244, 117, 117));
            dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

            LineData data = new LineData(dataSet);
            chart.setData(data);
        }

        // Limit visible entries
        chart.setVisibleXRangeMaximum(20);
        if (entries.size() > 20)
            chart.moveViewToX(entries.size() - 21);

        chart.invalidate();
    }

    private void generateMockData() {
        // Generate random proximity values (0 = near, maxRange = far)
        // Occasionally show 0 to simulate object detection
        float mockValue;
        if (random.nextInt(10) < 2) { // 20% chance of detecting something close
            mockValue = 0f;
        } else {
            mockValue = random.nextFloat() * maxRange;
        }

        addEntry(mockValue);

        // Schedule next update
        mockDataHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                generateMockData();
            }
        }, 1000); // Update every second
    }

    @Override
    public void onResume() {
        super.onResume();
        entries.clear();

        if (useMockData) {
            generateMockData();
        } else {
            mSensorManager.registerListener(this, mProximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (useMockData) {
            mockDataHandler.removeCallbacksAndMessages(null);
        } else {
            mSensorManager.unregisterListener(this);
        }
        entries.clear();
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {
            float distance = event.values[0];
            addEntry(distance);
        }
    }
}
