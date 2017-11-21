package edu.berkeley.cs.amplab.carat.android.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.databinding.DataBindingUtil;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;

import java.util.ArrayList;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import edu.berkeley.cs.amplab.carat.android.CaratActions;
import edu.berkeley.cs.amplab.carat.android.R;
import edu.berkeley.cs.amplab.carat.android.databinding.FragmentChargingBinding;
import edu.berkeley.cs.amplab.carat.android.models.ChargingPoint;
import edu.berkeley.cs.amplab.carat.android.models.ChargingSession;
import edu.berkeley.cs.amplab.carat.android.models.Peak;
import edu.berkeley.cs.amplab.carat.android.sampling.ChargingSessionManager;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;
import edu.berkeley.cs.amplab.carat.android.utils.Range;
import edu.berkeley.cs.amplab.carat.android.utils.Util;

/**
 * Created by Jonatan on 21.11.2017.
 */
public class ChargingFragment extends Fragment {
    private static final String TAG = ChargingFragment.class.getSimpleName();
    private FragmentChargingBinding binding;
    private ChargingSessionManager sessionManager;
    private LineDataSet defaultSet;
    private LineDataSet abnormalSet;
    private ArrayList<Entry> defaultEntries;
    private ArrayList<Entry> abnormalEntries;

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
              switch(intent.getAction()){
                  case CaratActions.CHARGING_NEW:
                  case CaratActions.CHARGING_UPDATE:
                      setChargingSessions(sessionManager.getSavedSessions());
              }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_charging, container, false);
        sessionManager = ChargingSessionManager.getInstance();
        defaultEntries = new ArrayList<>();
        abnormalEntries = new ArrayList<>();
        getContext().registerReceiver(receiver, new IntentFilter(CaratActions.CHARGING_NEW));
        getContext().registerReceiver(receiver, new IntentFilter(CaratActions.CHARGING_UPDATE));
        setupChart();
        new Handler().post(() -> {
            setChargingSessions(sessionManager.getSavedSessions());
        });
        return binding.getRoot();
    }

    private void setChargingSessions(SortedMap<Long, ChargingSession> sessions){
        Logger.d(TAG, "Got " + sessions.size() + " sessions");
        if(!Util.isNullOrEmpty(sessions)){
            ChargingSession last = sessions.get(sessions.firstKey());
            updateChart(last);
            StringBuilder sb = new StringBuilder();
            for(Map.Entry<Long, ChargingSession> entry : sessions.entrySet()){
                sb.append(entry.getValue()).append("\n");
            }
            binding.chargingDebug.setText(sb.toString());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    public void updateChart(ChargingSession session){
        TreeMap<Integer, ChargingPoint> points = session.getPoints();
        if(points.size() == 0){
            return;
        }
        binding.chargingChart.getXAxis().setAxisMinimum(points.firstKey());
        int last = points.lastKey();
        if(100-last > 5){
            last += 5;
        } else {
            last += (100-last);
        }

        binding.chargingChart.getXAxis().setAxisMaximum(last);
        defaultEntries.clear();
        double maxValue = 0.0;
        for(Integer level : points.keySet()){
            double time = points.get(level).getTime();
            maxValue = Math.max(time, maxValue);
            defaultEntries.add(new Entry(level, (float)time));
        }

        binding.chargingChart.getAxisLeft().setAxisMaximum((float)maxValue);

        abnormalEntries.clear();
        for(Peak peak : session.getPeaks()){
            Range<Double> range = peak.getRange();
            int start = (int)Math.round(range.getMin());
            int end = (int)Math.round(range.getMax());
            for(int i=start; start <= end; i++){
                double time = points.get(i).getTime();
                abnormalEntries.add(new Entry(i, (float)time));
            }
        }
        defaultSet.setValues(defaultEntries);
        abnormalSet.setValues(abnormalEntries);
        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(defaultSet);
        dataSets.add(abnormalSet);
        LineData data = new LineData(dataSets);
        binding.chargingChart.setData(data);
        binding.chargingChart.invalidate();
    }

    public void setupChart(){
        Legend legend = binding.chargingChart.getLegend();
        legend.setForm(Legend.LegendForm.SQUARE);
        binding.chargingChart.setDescription(null);

        XAxis xAxis = binding.chargingChart.getXAxis();
        xAxis.setAxisMaximum(100);
        xAxis.setAxisMinimum(0);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setGranularity(1.0f);
        xAxis.setGranularityEnabled(true);

        YAxis yAxis = binding.chargingChart.getAxisLeft();
        yAxis.setAxisMaximum(100);
        yAxis.setAxisMinimum(0);

        YAxis rightAxis = binding.chargingChart.getAxisRight();
        rightAxis.setDrawLabels(false);
        rightAxis.setEnabled(false);

        defaultSet = new LineDataSet(defaultEntries, "Normal charging");
        defaultSet.setColor(Color.rgb(0, 102, 0));
        defaultSet.setDrawCircles(false);
        defaultSet.setLineWidth(2f);
        defaultSet.setDrawValues(false);
        defaultSet.setCircleRadius(3f);
        defaultSet.setDrawCircleHole(false);
        defaultSet.setDrawFilled(true);
        defaultSet.setFormLineWidth(1f);
        defaultSet.setFormSize(15.f);
        defaultSet.setFillColor(Color.rgb(0, 102, 0));

        abnormalSet = new LineDataSet(abnormalEntries, "Abnormal charging");
        abnormalSet.setColor(Color.RED);
        abnormalSet.setDrawCircles(false);
        abnormalSet.setLineWidth(3f);
        abnormalSet.setDrawValues(false);
        abnormalSet.setCircleRadius(3f);
        abnormalSet.setDrawCircleHole(false);
        abnormalSet.setDrawFilled(true);
        abnormalSet.setFormLineWidth(1f);
        abnormalSet.setFormSize(15.f);
        abnormalSet.setFillColor(Color.RED);

        ArrayList<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(defaultSet);
        dataSets.add(abnormalSet);
        LineData data = new LineData(dataSets);
        binding.chargingChart.setData(data);
        binding.chargingChart.invalidate();
    }
}
