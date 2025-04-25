package com.iir4g8.tpsensor.ui.humidity;

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
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

import com.iir4g8.tpsensor.R;

public class HumidityFragment extends Fragment implements SensorEventListener {
    private LineChart chart;
    private SensorManager mSensorManager;
    private Sensor mHumidSensor;
    static ArrayList<Entry> entries = new ArrayList<>();
    private boolean useMockData = false;
    private Handler mockDataHandler;
    private Random random = new Random();
    private TextView statusText;

    public HumidityFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSensorManager = (SensorManager)getActivity().getSystemService(Context.SENSOR_SERVICE);
        mHumidSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_RELATIVE_HUMIDITY);

        if(mHumidSensor == null){
            useMockData = true;
            mockDataHandler = new Handler();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root = inflater.inflate(R.layout.fragment_humidity, container, false);
        chart = (LineChart) root.findViewById(R.id.chart);

        // Add a status text view to show if we're using mock data
        statusText = new TextView(getContext());
        statusText.setTextSize(16);
        statusText.setPadding(20, 20, 20, 20);

        if (useMockData) {
            statusText.setText("Humidity sensor not available. Showing simulated data.");
            statusText.setTextColor(Color.RED);
        } else {
            statusText.setText("Using device humidity sensor");
            statusText.setTextColor(Color.GREEN);
        }

        // Add the status text to the layout
        ViewGroup parent = (ViewGroup) chart.getParent();
        parent.addView(statusText, 0);

        return root;
    }

    private void addEntry(float value) {
        entries.add(new Entry(entries.size(), value));

        LineDataSet dataSet = new LineDataSet(entries, "Humidity - Time series");
        dataSet.setColor(Color.BLUE);
        dataSet.setValueTextColor(Color.BLACK);

        LineData data = new LineData(dataSet);
        Log.d("size", entries.size()+"");

        chart.setData(data);
        chart.notifyDataSetChanged();
        chart.invalidate();
    }

    private void generateMockData() {
        // Generate humidity between 30-70%
        float mockHumidity = 30 + random.nextFloat() * 40;
        addEntry(mockHumidity);

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
            mSensorManager.registerListener(this, mHumidSensor, SensorManager.SENSOR_DELAY_NORMAL);
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
        addEntry(event.values[0]);
    }
}