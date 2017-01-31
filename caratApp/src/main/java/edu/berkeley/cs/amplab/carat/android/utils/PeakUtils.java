package edu.berkeley.cs.amplab.carat.android.utils;

import org.apache.commons.math3.distribution.TDistribution;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

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

        TreeSet<Double> xi = getIntersections(points);
        for(Integer level : points.keySet()){
            ChargingPoint point = points.get(level);
            double time = point.getTime();
            double avg = point.getAverage();
            double ss = point.getSquareSum();
            if(isPeak(time, avg, ss, i)){
                Double lower = xi.lower((double)level);
                Double higher = xi.higher((double)level);
                if(lower != null && higher != null){
                    // TODO: Create peak with properties and add to results.
                }
            }
            i++;
        }
        return peaks;
    }

    public static TreeSet<Double> getIntersections(TreeMap<Integer, ChargingPoint> points){
        TreeSet<Double> intersections = new ArrayList<>();
        Double prevDist = 0.0;
        Integer prevLevel = null;
        Double prevTime = null;
        Double prevAvg = null;
        for(Integer level : points.keySet()){
            ChargingPoint point = points.get(level);
            double time = point.getTime();
            double avg = point.getAverage();
            double dist = time - avg;

            // During first iteration prevDist is always 0 so we don't check intersections.
            // During second iteration prevDist should still be zero since the first points
            // always overlap. During third iteration prevDist should have updated to some
            // value so we can start checking if the lines change order or overlap.
            if(prevDist != 0 && (dist * prevDist <= 0)){
                // Intersection happens exactly at this point so we can just return it.
                if(dist == 0){
                    intersections.add((double)level);
                } else {
                    // Intersection happens between these points, so we need to calculate
                    // the intersection point between the two lines constructed from the
                    // two pairs of points.
                    intersections.add(collide(prevLevel, prevTime, level, time,
                                              prevLevel, prevAvg, level, avg));
                }
            }
            prevDist = dist;
            prevAvg = avg;
            prevTime = time;
            prevLevel = level;
        }
        return intersections;
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

    /**
     * Intersection between two lines constructed from two pairs of points.
     * Line 1 is constructed between points (x1,y1) and (x2,y2).
     * Line 2 is constructed between points (x3,y3) and (x4,y4).
     * @return intersection point on the x-axis
     */
    private static Double collide(double x1, double y1, double x2, double y2,
                                 double x3, double y3, double x4, double y4){
        return ((x1*y2 - y1*x2)*(x3 - x4) - (x1-x2)*(x3*y4-y3*x4))/
                ((x1-x2)*(y3-y4) - (y1-y2)*(x3-x4));
    }
}
