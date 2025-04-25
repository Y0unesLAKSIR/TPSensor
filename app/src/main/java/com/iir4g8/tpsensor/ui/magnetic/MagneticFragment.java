package com.iir4g8.tpsensor.ui.magnetic;


import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;

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

import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import com.iir4g8.tpsensor.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class MagneticFragment extends Fragment implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor mMagneticSensor;
    public static DecimalFormat DECIMAL_FORMATTER;
    static ArrayList<Entry> entries = new ArrayList<>();
    private LineChart chart;


    private TextView value;

    public MagneticFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSensorManager = (SensorManager)getActivity().getSystemService(Context.SENSOR_SERVICE);
        mMagneticSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if(mMagneticSensor == null){
            Toast.makeText(getContext(), R.string.message_neg, Toast.LENGTH_LONG).show();
        }
    }

    private void addEntry(double value) {
        Date d = new Date();
        entries.add(new Entry(entries.size(), (float) value));
        LineDataSet dataSet = new LineDataSet(entries, "Magnetic - Time series");
        LineData data = new LineData(dataSet);
        Log.d("size", entries.size()+"");
        XAxis xAxis = chart.getXAxis();
        chart.setData(data);
        chart.notifyDataSetChanged();
        // chart.animateX(900000000);
        //refresh
        chart.invalidate();

    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View root =  inflater.inflate(R.layout.fragment_magnetic, container, false);
        value = (TextView) root.findViewById(R.id.value);
        chart = (LineChart) root.findViewById(R.id.chart);
        // define decimal formatter
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        symbols.setDecimalSeparator('.');
        DECIMAL_FORMATTER = new DecimalFormat("#.000", symbols);
        return root;
    }

    @Override
    public void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mMagneticSensor, SensorManager.SENSOR_DELAY_NORMAL);
        entries.clear();
    }

    @Override
    public void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
        entries.clear();
    }

    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            float magX = event.values[0];
            float magY = event.values[1];
            float magZ = event.values[2];
            double magnitude = Math.sqrt((magX * magX) + (magY * magY) + (magZ * magZ));
            // set value on the screen
            value.setText(DECIMAL_FORMATTER.format(magnitude) + " \u00B5Tesla");
            addEntry(magnitude);
        }
    }
}
