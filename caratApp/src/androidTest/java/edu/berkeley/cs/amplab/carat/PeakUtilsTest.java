package edu.berkeley.cs.amplab.carat;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

import edu.berkeley.cs.amplab.carat.android.models.ChargingPoint;
import edu.berkeley.cs.amplab.carat.android.models.ChargingSession;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;
import edu.berkeley.cs.amplab.carat.android.utils.PeakUtils;
import static org.junit.Assert.assertTrue;

/**
 * Created by Jonatan Hamberg on 31.1.2017.
 */
public class PeakUtilsTest {
    private static final String TAG = PeakUtilsTest.class.getSimpleName();
    String line1 = "0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;0.0;9.0;57.0;23.0;25.0;37.0;27.0;28.0;34.0;49.0;43.0;22.0;55.0;13.0;41.0;27.0;54.0;27.0;27.0;47.0;52.0;38.0;15.0;35.0;56.0;26.0;38.0;39.0;60.0;59.0;6.0;56.0;55.0;19.0;49.0;54.0;46.0;70.0;39.0;43.0;55.0;55.0;84.0;39.0;53.0;65.0;70.0;78.0;66.0;70.0;124.0;41.0;82.0;70.0;106.0;64.0;89.0;119.0;90.0;144.0;128.0;99.0;165.0;180.0;0.0;0.0;0.0;0.0;0.0";

    @Test
    public void testPeakDetection(){
        ChargingSession session = ChargingSession.create();
        TreeMap<Integer, ChargingPoint> points;
        String ss[] = line1.split(";");
        List<Double> times = new ArrayList<>();
        for(int i=0; i<ss.length; i++){
            Double number = Double.parseDouble(ss[i]);
            if(number != 0.0){
                session.addPoint(i, number);
                Logger.d(TAG, "Peaks are: " + session.getPeaks().toString());
            }
        }
    }

    @Test
    public void testChargingPointIntersections(){
        double D = 2.0;
        TreeMap<Integer, ChargingPoint> map = new TreeMap<>();
        TreeSet<Double> intersections;
        map.put(1, new ChargingPoint(2.0, D, 2.0));
        map.put(2, new ChargingPoint(2.0, D, 2.0));
        map.put(3, new ChargingPoint(2.0, D, 2.0));
        map.put(4, new ChargingPoint(10.0, D, 5.0));
        intersections = PeakUtils.getIntersections(map);
        assertTrue(intersections.isEmpty());

        map.clear();
        map.put(1, new ChargingPoint(2.0, D, 2.0));
        map.put(2, new ChargingPoint(3.0, D, 2.0));
        map.put(3, new ChargingPoint(2.0, D, 3.0));
        map.put(4, new ChargingPoint(3.0, D, 4.0));
        map.put(5, new ChargingPoint(3.0, D, 3.5));
        map.put(6, new ChargingPoint(2.0, D, 3.0));
        map.put(7, new ChargingPoint(6.0, D, 7.0));
        map.put(8, new ChargingPoint(6.0, D, 5.0));
        intersections = PeakUtils.getIntersections(map);
        assertTrue(intersections.contains(2.5));
        assertTrue(intersections.contains(7.5));

        map.clear();
        map.put(1, new ChargingPoint(2.0, D, 2.0));
        map.put(2, new ChargingPoint(3.0, D, 4.0));
        map.put(3, new ChargingPoint(4.0, D, 6.0));
        map.put(4, new ChargingPoint(5.0, D, 3.0));
        map.put(5, new ChargingPoint(6.0, D, 3.0));
        map.put(6, new ChargingPoint(5.0, D, 5.0));
        map.put(7, new ChargingPoint(5.0, D, 5.0));
        map.put(8, new ChargingPoint(5.0, D, 5.0));
        map.put(9, new ChargingPoint(4.0, D, 6.0));
        intersections = PeakUtils.getIntersections(map);
        assertTrue(intersections.contains(3.5));
        assertTrue(intersections.contains(6.0));

    }
}
