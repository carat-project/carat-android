package edu.berkeley.cs.amplab.carat.android.models;

import java.io.Serializable;
import java.util.List;
import java.util.TreeMap;

import edu.berkeley.cs.amplab.carat.android.utils.Logger;
import edu.berkeley.cs.amplab.carat.android.utils.MathUtils;
import edu.berkeley.cs.amplab.carat.android.utils.PeakUtils;

/**
 * Created by Jonatan Hamberg on 30.1.2017.
 */
public class ChargingSession implements Serializable {
    private static final String TAG = ChargingSession.class.getSimpleName();
    private static final long serialVersionUID = 9099497442195235697L;

    private List<Peak> peaks;
    private TreeMap<Integer, ChargingPoint> points;
    private String plugState;
    private long duration;

    private Long timestamp;
    private Boolean replaceOnZigZag = false;

    private ChargingSession(){
        points = new TreeMap<>();
        timestamp = System.currentTimeMillis();
        plugState = "unknown";
        duration = 0;
    }

    public static ChargingSession create(){
        return new ChargingSession();
    }
    public ChargingSession setReplaceOnZigZag(Boolean replaceOnZigZag) {
        this.replaceOnZigZag = replaceOnZigZag;
        return this;
    }

    public void setPlugState(String state){
        plugState = state;
    }

    public String getPlugState(){
        return plugState;
    }

    public List<Peak> getPeaks() {
        return peaks;
    }

    public TreeMap<Integer, ChargingPoint> getPoints() {
        return points;
    }

    public long getDurationInSeconds(){
        return duration;
    }

    public int getPointCount(){
        return points.size();
    }

    public Integer getLastLevel(){
        int lastLevel = points.lastKey();
        Logger.d(TAG, "Last level is " + lastLevel);
        return lastLevel;
    }

    public boolean isNew(){
        return points.size() == 0;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void addPoint(Integer level, Double time) {
        // TODO: Interpolate if we skip a level.

        Logger.d(TAG, "Session " + timestamp + ": " +
                "New level " + level + " took (" + time/1000.0 + " s)");

        // Calculate moving average and square sum
        Double cma = getMovingAverage(level, time);
        Double ss = getSquareSum(level, time, cma);

        duration += time;
        if(points.containsKey(level) && !replaceOnZigZag){
            time += points.get(level).getTime();
            cma += points.get(level).getAverage();
            ss += points.get(level).getSquareSum();
        }

        points.put(level, new ChargingPoint(time, cma, ss));
        peaks = PeakUtils.getPeaks(points);
    }

    public boolean hasPeaks(){
        return peaks.size() > 0;
    }

    private Double getMovingAverage(int level, double value){
        Integer prevKey = points.lowerKey(level);;
        if(prevKey == null) {
            return value;
        }
        Double prevAvg = points.get(prevKey).getAverage();
        int n = points.size();
        return MathUtils.cma(value, prevAvg, n, 40);
    }

    private Double getSquareSum(int level, double value, double cma){
        Integer prevKey = points.lowerKey(level);
        if(prevKey == null){
            return 0.0;
        }
        Double prevSs = points.get(prevKey).getSquareSum();
        int n = points.size();
        return MathUtils.ss(value, prevSs, n, cma);
    }

    @Override
    public String toString() {
        return "ChargingSession{" +
                "peaks=" + peaks +
                ", points=" + points +
                ", timestamp=" + timestamp +
                ", replaceOnZigZag=" + replaceOnZigZag +
                '}';
    }
}
