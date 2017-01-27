package edu.berkeley.cs.amplab.carat.android.components;

import com.github.mikephil.charting.charts.LineChart;

import edu.berkeley.cs.amplab.carat.android.utils.Range;

/**
 * Created by Jonatan Hamberg on 27.1.2017.
 */

public class ChargingChart {
    private LineChart view;

    private ChargingChart(){
        // Not implemented
    }

    public static ChargingChart forView(LineChart view){
        ChargingChart instance = new ChargingChart();
        instance.view = view;
        return instance;
    }

    public static void reset(){
        // To be done
    }

    public static void update(double point, Range<Integer> peak){
        // To be done
    }
}
