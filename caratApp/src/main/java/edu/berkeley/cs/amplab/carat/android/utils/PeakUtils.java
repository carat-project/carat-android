package edu.berkeley.cs.amplab.carat.android.utils;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.distribution.TDistribution;
import org.apache.commons.math3.stat.descriptive.moment.Kurtosis;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.apache.commons.math3.stat.descriptive.moment.Skewness;
import org.apache.commons.math3.stat.descriptive.moment.Variance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.berkeley.cs.amplab.carat.android.models.ChargingPoint;
import edu.berkeley.cs.amplab.carat.android.models.Peak;

/**
 * Created by Jonatan Hamberg on 30.1.2017.
 */
public class PeakUtils {
    private static final String TAG = PeakUtils.class.getSimpleName();

    public static List<Peak> getPeaks(TreeMap<Integer, ChargingPoint> points){
        List<Peak> peaks = new ArrayList<>();
        if(points.keySet().size() < 2){
            return peaks;
        }

        TreeSet<Double> xi = getIntersections(points);
        List<Double> times = new ArrayList<>();
        List<Double> averages = new ArrayList<>();
        for(ChargingPoint point : points.values()){
            times.add(point.getTime());
            averages.add(point.getAverage());
        }

        UnivariateFunction timeFunction = MathUtils.functionFromPoints(times);
        UnivariateFunction averageFunction = MathUtils.functionFromPoints(averages);

        int offset = 1;
        for(Integer level : points.keySet()){
            ChargingPoint point = points.get(level);
            double time = point.getTime();
            double avg = point.getAverage();
            double ss = point.getSquareSum();
            if(isPeak(time, avg, ss, offset+1)){
                Double lower = xi.lower((double)level);
                Double higher = xi.higher((double)level);
                if(lower != null && higher != null){
                    Peak peak = constructPeak(lower, higher, points, timeFunction, averageFunction);
                    Logger.d(TAG, "Detected a peak: " + peak);
                    peaks.add(peak);
                }
            }
            offset++;
        }
        return peaks;
    }

    private static Peak constructPeak(double lower, double higher,
                                      TreeMap<Integer, ChargingPoint> points,
                                      UnivariateFunction timeFunction,
                                      UnivariateFunction averageFunction){
        Skewness skewness = new Skewness();
        Kurtosis kurtosis = new Kurtosis();
        Variance variance = new Variance();
        Mean mean = new Mean();

        int lowerKey = (int)Math.floor(lower);
        int higherKey = (int)Math.ceil(higher);
        List<Double> peakTimes = getValues(points, lowerKey, higherKey);

        int offset = points.firstKey();
        double[] timeArray = toArray(peakTimes);

        return new Peak()
                .setValues(peakTimes)
                .setRange(new Range<>(lower, higher))
                .setAuc(MathUtils.auc(lower-offset, higher-offset, timeFunction, averageFunction))
                .setInten1(Collections.max(peakTimes) - points.get(lowerKey).getAverage())
                .setInten2(Collections.min(peakTimes) - points.get(lowerKey).getAverage())
                .setSkewness(skewness.evaluate(timeArray))
                .setKurtosis(kurtosis.evaluate(timeArray))
                .setVariance(variance.evaluate(timeArray))
                .setMean(mean.evaluate(timeArray));
    }

    public static TreeSet<Double> getIntersections(TreeMap<Integer, ChargingPoint> points){
        TreeSet<Double> intersections = new TreeSet<>();
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
                    intersections.add(MathUtils.intersection2L(prevLevel, prevTime, level, time,
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
        double va = ss/t;
        double dev = Math.sqrt(va);
        TDistribution distribution = new TDistribution(t);
        double th1 = distribution.inverseCumulativeProbability(0.995);
        double th2 = distribution.inverseCumulativeProbability(1-0.995);
        double z = (time - avg) / dev;
        return (z > th1 || z < th2);
    }

    private static List<Double> getValues(TreeMap<Integer, ChargingPoint> map, int start, int end){
        List<Double> values = new ArrayList<>();
        SortedMap<Integer, ChargingPoint> points = map.subMap(start, end+1);
        for(ChargingPoint value : points.values()){
            values.add(value.getTime());
        }
        return values;
    }

    private static double[] toArray(List<Double> list){
        double[] result = new double[list.size()];
        for(int i=0; i<result.length; i++){
            result[i] = list.get(i);
        }
        return result;
    }
}
