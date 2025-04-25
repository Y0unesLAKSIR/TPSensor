package com.iir4g8.tpsensor.ui.thermometer;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import android.os.Handler;
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
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import com.iir4g8.tpsensor.R;

public class ThermoFragment extends Fragment implements SensorEventListener {

    private LineChart chart;
    private SensorManager mSensorManager;
    private Sensor mTempSensor;
    static ArrayList<Entry> entries = new ArrayList<>();
    private boolean useMockData = false;
    private Handler mockDataHandler;
    private Random random = new Random();
    private TextView statusText;
    private TextView currentTempText;

    // Constants for chart scaling
    private static final float MIN_TEMP = -50f;  // Minimum temperature to display
    private static final float MAX_TEMP = 100f;  // Maximum temperature to display
    private static final int MAX_VISIBLE_ENTRIES = 50; // Maximum number of visible entries

    // For tracking extreme values
    private float minRecordedTemp = Float.MAX_VALUE;
    private float maxRecordedTemp = Float.MIN_VALUE;

    public ThermoFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSensorManager = (SensorManager)getActivity().getSystemService(Context.SENSOR_SERVICE);
        mTempSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);

        if(mTempSensor == null){
            useMockData = true;
            mockDataHandler = new Handler();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_thermo, container, false);
        chart = (LineChart) root.findViewById(R.id.chart);

        // Configure the chart
        setupChart();

        // Add a status text view to show if we're using mock data
        statusText = root.findViewById(R.id.status_text);

        // Add current temperature display
        currentTempText = root.findViewById(R.id.current_temp_text);

        if (useMockData) {
            statusText.setText("Temperature sensor not available. Showing simulated data.");
            statusText.setTextColor(Color.RED);
        } else {
            statusText.setText("Using device temperature sensor");
            statusText.setTextColor(Color.GREEN);
        }

        return root;
    }

    private void setupChart() {
        // Style the chart
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setScaleEnabled(true);
        chart.setPinchZoom(true);
        chart.setDrawGridBackground(false);
        chart.setHighlightPerDragEnabled(false);

        // Set background color
        chart.setBackgroundColor(Color.WHITE);

        // Enable auto scaling
        chart.setAutoScaleMinMaxEnabled(true);

        // Configure X axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(true);
        xAxis.setGranularity(1f); // Only intervals of 1
        xAxis.setLabelCount(10);

        // Configure Y axis
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(MIN_TEMP);
        leftAxis.setAxisMaximum(MAX_TEMP);
        leftAxis.setDrawZeroLine(true);

        // Disable right Y axis
        chart.getAxisRight().setEnabled(false);

        // Create empty data
        LineData data = new LineData();
        data.setValueTextColor(Color.BLACK);
        chart.setData(data);

        // Refresh
        chart.invalidate();
    }

    private void addEntry(float value) {
        // Validate temperature value - ignore extreme values that are likely errors
        if (value < -300 || value > 300) {
            Log.w("ThermoFragment", "Ignoring extreme temperature value: " + value);
            return;
        }

        // Update min/max recorded temperatures
        minRecordedTemp = Math.min(minRecordedTemp, value);
        maxRecordedTemp = Math.max(maxRecordedTemp, value);

        // Update current temperature display
        updateCurrentTemperature(value);

        // Add entry to the chart
        LineData data = chart.getData();

        if (data == null) {
            // Create new dataset if none exists
            LineDataSet set = new LineDataSet(null, "Temperature (°C)");
            configureDataSet(set);
            data = new LineData(set);
            chart.setData(data);
        }

        // Get the dataset
        ILineDataSet set = data.getDataSetByIndex(0);
        if (set == null) {
            set = createSet();
            data.addDataSet(set);
        }

        // Add the new data point
        data.addEntry(new Entry(set.getEntryCount(), value), 0);

        // Limit the number of visible entries to prevent overcrowding
        if (set.getEntryCount() > MAX_VISIBLE_ENTRIES) {
            chart.setVisibleXRangeMaximum(MAX_VISIBLE_ENTRIES);
            chart.moveViewToX(data.getEntryCount() - MAX_VISIBLE_ENTRIES);
        }

        // Notify chart of data change
        data.notifyDataChanged();
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    private LineDataSet createSet() {
        LineDataSet set = new LineDataSet(null, "Temperature (°C)");
        configureDataSet(set);
        return set;
    }

    private void configureDataSet(LineDataSet set) {
        set.setColor(Color.RED);
        set.setLineWidth(2f);
        set.setCircleColor(Color.RED);
        set.setCircleRadius(3f);
        set.setDrawCircleHole(false);
        set.setValueTextSize(10f);
        set.setDrawFilled(true);
        set.setFillColor(Color.RED);
        set.setFillAlpha(50);
        set.setDrawValues(false);
        set.setHighlightEnabled(true);
        set.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Smooth curve
    }

    private void updateCurrentTemperature(float temperature) {
        if (currentTempText != null) {
            currentTempText.setText(String.format("Current: %.1f°C (Min: %.1f°C, Max: %.1f°C)",
                    temperature, minRecordedTemp, maxRecordedTemp));
        }
    }

    private void generateMockData() {
        // Generate temperature between 20-30°C
        float mockTemp = 20 + random.nextFloat() * 10;
        addEntry(mockTemp);

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

        // Reset min/max values
        minRecordedTemp = Float.MAX_VALUE;
        maxRecordedTemp = Float.MIN_VALUE;

        // Clear existing data
        if (chart.getData() != null) {
            chart.getData().clearValues();
            chart.invalidate();
        }

        if (useMockData) {
            generateMockData();
        } else {
            mSensorManager.registerListener(this, mTempSensor, SensorManager.SENSOR_DELAY_NORMAL);
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
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    public void onSensorChanged(SensorEvent event) {
        addEntry(event.values[0]);
    }
}
