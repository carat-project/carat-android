package edu.berkeley.cs.amplab.carat;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.berkeley.cs.amplab.carat.android.models.ChargingPoint;
import edu.berkeley.cs.amplab.carat.android.models.ChargingSession;
import edu.berkeley.cs.amplab.carat.android.models.Peak;
import edu.berkeley.cs.amplab.carat.android.utils.MathUtils;
import edu.berkeley.cs.amplab.carat.android.utils.PeakUtils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by Jonatan Hamberg on 31.1.2017.
 */
public class PeakUtilsTest {
    private static final String TAG = PeakUtilsTest.class.getSimpleName();
    String line1 = "0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;9.0;57.0;23.0;25.0;37.0;27.0;28.0;34.0;49.0;43.0;22.0;55.0;13.0;41.0;27.0;54.0;27.0;27.0;47.0;52.0;38.0;15.0;35.0;56.0;26.0;38.0;39.0;60.0;59.0;6.0;56.0;55.0;19.0;49.0;54.0;46.0;70.0;39.0;43.0;55.0;55.0;84.0;39.0;53.0;65.0;70.0;78.0;66.0;70.0;124.0;41.0;82.0;70.0;106.0;64.0;89.0;119.0;90.0;144.0;128.0;99.0;165.0;180.0;0.0;0.0;0.0;0.0;0.0";
    String line2 = "0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;32;61;112;51;51;92;50;41;92;30;82;31;197;41;81;72;61;61;51;61;51;50;71;61;51;61;51;72;50;61;68;51;72;61;61;61;61;50;72;61;71;61;112;132;153;163;224;214;264;254;233;275;274;285;274;503;284;285;299;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0;0";

    private List<Double> positiveSpan(String line){
        String[] ss = line.split(";");
        List<Double> span = new ArrayList<>();
        for (String s : ss) {
            Double number = Double.parseDouble(s);
            if (number != 0.0) {
                span.add(number);
            }
        }
        return span;
    }

    @Test
    public void testPeakDetection(){
        List<Double> peak2 = Arrays.asList(39.0, 53.0, 65.0, 70.0, 78.0, 66.0, 70.0, 124.0, 41.0);
        ChargingSession session = ChargingSession.create();
        String ss[] = line1.split(";");
        for(int i=0; i<ss.length; i++){
            Double number = Double.parseDouble(ss[i]);
            if(number != 0.0){
                session.addPoint(i, number);
            }
        }
        boolean flag = true;
        List<Peak> peaks = session.getPeaks();
        assertTrue(peaks != null && peaks.size() == 2);

        Peak peak = session.getPeaks().get(1);
        List<Double> values = peak.getValues();

        flag = true;
        for(int i=0; i<values.size(); i++){
            if(!peak2.get(i).equals(values.get(i))){
                flag = false;
            }
        }
        assertTrue(flag);

        // Not precise
        assertEquals(227.0014, peak.getAuc(), 5);
        assertEquals(8.0, peak.getLength(), 0.5);

        // Precise
        assertEquals(84.3043, peak.getInten1(), 0.001);
        assertEquals(-0.6957, peak.getInten2(), 0.001);
        assertEquals(1.4119, peak.getSkewness(), 0.001);
        assertEquals(3.1595, peak.getKurtosis(), 0.001);
        assertEquals(67.3333, peak.getMean(), 0.001);
        assertEquals(631.0, peak.getVariance(), 0.001);
    }

    @Test
    public void testCumulativeRunningAverage(){
        List<Double> numbers = positiveSpan(line1);
        List<Double> cmas = new ArrayList<>();
        Double prev = null;
        for(int i=0; i< numbers.size(); i++){
            Double cma;
            if(prev == null){
                cma = numbers.get(i);
            } else {
                cma = MathUtils.cma(numbers.get(i), prev, i, 40);
            }
            cmas.add(cma);
            prev = cma;
        }

        double firstCma = cmas.get(0);
        double lastCma = cmas.get(cmas.size()-1);
        assertEquals(9.0, firstCma, 0.0);
        assertEquals(63.378, lastCma, 0.001);
    }

    @Test
    public void testSquareSum(){
        List<Double> numbers = positiveSpan(line1);
        List<Double> sums = new ArrayList<>();
        Double prev = null;
        Double prevCma = null;
        for(int i=0; i< numbers.size(); i++){
            Double ss, cma = null;
            if(prev == null){
                cma = numbers.get(i);
                ss = 0.0;
            } else {
                cma = MathUtils.cma(numbers.get(i), prevCma, i, 40);
                ss = MathUtils.ss(numbers.get(i), prev, i, cma);
            }
            sums.add(ss);
            prev = ss;
            prevCma = cma;
        }

        double firstSs = sums.get(0);
        double lastSs = sums.get(sums.size()-1);
        assertEquals(0.0, firstSs, 0);
        assertEquals(72216.922, lastSs, 0.001);
    }

    @Test
    public void testChargingPointIntersections(){
        double D = 2.0;
        TreeMap<Integer, ChargingPoint> map = new TreeMap<>();
        TreeSet<Double> intersections;
        map.put(1, new ChargingPoint(2.0, 2.0, D));
        map.put(2, new ChargingPoint(2.0, 2.0, D));
        map.put(3, new ChargingPoint(2.0, 2.0, D));
        map.put(4, new ChargingPoint(10.0, 5.0, D));
        intersections = PeakUtils.getIntersections(map);
        assertTrue(intersections.isEmpty());

        map.clear();
        map.put(1, new ChargingPoint(2.0, 2.0, D));
        map.put(2, new ChargingPoint(3.0, 2.0, D));
        map.put(3, new ChargingPoint(2.0, 3.0, D));
        map.put(4, new ChargingPoint(3.0, 4.0, D));
        map.put(5, new ChargingPoint(3.0, 3.5, D));
        map.put(6, new ChargingPoint(2.0, 3.0, D));
        map.put(7, new ChargingPoint(6.0, 7.0, D));
        map.put(8, new ChargingPoint(6.0, 5.0, D));
        intersections = PeakUtils.getIntersections(map);
        assertTrue(intersections.contains(2.5));
        assertTrue(intersections.contains(7.5));

        map.clear();
        map.put(1, new ChargingPoint(2.0, 2.0, D));
        map.put(2, new ChargingPoint(3.0, 4.0, D));
        map.put(3, new ChargingPoint(4.0, 6.0, D));
        map.put(4, new ChargingPoint(5.0, 3.0, D));
        map.put(5, new ChargingPoint(6.0, 3.0, D));
        map.put(6, new ChargingPoint(5.0, 5.0, D));
        map.put(7, new ChargingPoint(5.0, 5.0, D));
        map.put(8, new ChargingPoint(5.0, 5.0, D));
        map.put(9, new ChargingPoint(4.0, 6.0, D));
        intersections = PeakUtils.getIntersections(map);
        assertTrue(intersections.contains(3.5));
        assertTrue(intersections.contains(6.0));
    }
}
