package edu.berkeley.cs.amplab.carat.android.utils;

import org.apache.commons.math3.distribution.TDistribution;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import edu.berkeley.cs.amplab.carat.android.models.ChargingPoint;
import edu.berkeley.cs.amplab.carat.android.models.Peak;

/**
 * Created by Jonatan Hamberg on 30.1.2017.
 */
public class PeakUtils {
    public static List<Peak> getPeaks(TreeMap<Integer, ChargingPoint> points){
        List<Peak> peaks = new ArrayList<>();
        if(points.keySet().size() < 2){
            return peaks;
        }

        int i = 1;

        List<Integer> xi = getIntersections(points);

        for(Integer level : points.keySet()){
            ChargingPoint point = points.get(level);
            double time = point.getTime();
            double avg = point.getAverage();
            double ss = point.getSquareSum();
            if(isPeak(time, avg, ss, i)){
                // TODO: Calculate one intersection before and one after as the peak
                // If there is no intersection after this point, the event is ongoing
                // and we should wait for the intersection. We might also want to inform
                // the user here so they can act and end the peak quicker.
            }
            i++;
        }
        return peaks;
    }

    private static List<Integer> getIntersections(TreeMap<Integer, ChargingPoint> points){
        Boolean prevSign = null;
        for(Integer level : points.keySet()){
            ChargingPoint point = points.get(level);
            double time = point.getTime();
            double avg = point.getAverage();
            boolean sign = time - avg > 0;
            if(prevSign != null && sign != prevSign){
                // Sign changed so there is an intersection
                // Use pythagoras here level as x, time as y
                // calculate intersection of the hypotenuses.
            }
            prevSign = sign;
        }
        return new ArrayList<>();
    }

    private static boolean isPeak(double time, double avg, double ss, double t){
        double va = ss/(t+1);
        double dev = Math.sqrt(va);
        TDistribution distribution = new TDistribution(t);
        double th1 = distribution.inverseCumulativeProbability(0.995);
        double th2 = distribution.inverseCumulativeProbability(1-0.995);
        double z = (time - avg) / dev;
        return (z > th1 || z < th2);
    }

    /**
     * Calculate cumulative moving average for nth addition with value x.
     * @param x new value
     * @param prev previous average
     * @param n item count after adding x
     * @param window window for n
     * @return cumulative moving average
     */
    public static double cma(double x, double prev, int n, double window){
        // This is slightly better known variation of the formula:
        // double d = Math.min(n+1, window);
        // return (x + (d-1) * prev)/d;
        // It is, however, less precise and therefore not used here.
        double alpha = Math.max(1/(double)(n+1), 1/window);
        return prev + alpha*(x-prev);
    }

    public static double ss(double x, double prev, int n, double cma){
        double beta = n/(double)(n-1);
        return prev + beta * Math.pow((x - cma), 2);
    }
}
