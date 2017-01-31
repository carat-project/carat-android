package edu.berkeley.cs.amplab.carat.android.models;



import java.io.Serializable;
import java.util.List;
import java.util.TreeMap;

import edu.berkeley.cs.amplab.carat.android.utils.PeakUtils;

/**
 * Created by Jonatan Hamberg on 30.1.2017.
 */
public class ChargingSession implements Serializable {
    private static final long serialVersionUID = 9099497442195235697L;

    private List<Peak> peaks;
    private TreeMap<Integer, ChargingPoint> points;

    private Long timestamp;
    private Boolean replaceOnZigZag = false;

    private ChargingSession(){
        points = new TreeMap<>();
        this.timestamp = System.currentTimeMillis();
    }

    public ChargingSession create(){
        return new ChargingSession();
    }
    public ChargingSession setReplaceOnZigZag(Boolean replaceOnZigZag) {
        this.replaceOnZigZag = replaceOnZigZag;
        return this;
    }

    public List<Peak> getPeaks() {
        return peaks;
    }

    public TreeMap<Integer, ChargingPoint> getPoints() {
        return points;
    }


    public Long getTimestamp() {
        return timestamp;
    }

    public void addPoint(Integer level, Double time) {

        // Calculate moving average and square sum
        Double cma = getMovingAverage(level);
        Double ss = getSquareSum(level, cma);

        if(points.containsKey(level) && !replaceOnZigZag){
            time += points.get(level).getTime();
            cma += points.get(level).getAverage();
            ss += points.get(level).getSquareSum();
        }

        points.put(level, new ChargingPoint(time, cma, ss));
        peaks = PeakUtils.getPeaks(points);
    }

    private Double getMovingAverage(int level){
        Integer prevKey = points.lowerKey(level);
        Double prevAvg = prevKey == null ? 0 : points.get(prevKey).getAverage();
        int n = points.size();
        return PeakUtils.cma(level, prevAvg, n, 40);
    }

    private Double getSquareSum(int level, double cma){
        Integer prevKey = points.lowerKey(level);
        Double prevSs = prevKey == null ? 0 : points.get(prevKey).getSquareSum();
        int n = points.size();
        return PeakUtils.ss(level, prevSs, n, cma);
    }
}
