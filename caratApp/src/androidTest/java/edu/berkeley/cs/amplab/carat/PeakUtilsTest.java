package edu.berkeley.cs.amplab.carat;

import org.junit.Test;

import java.util.List;
import java.util.TreeMap;
import edu.berkeley.cs.amplab.carat.android.models.ChargingPoint;
import edu.berkeley.cs.amplab.carat.android.utils.PeakUtils;
import static org.junit.Assert.assertTrue;

/**
 * Created by Jonatan Hamberg on 31.1.2017.
 */
public class PeakUtilsTest {

    @Test
    public void testChargingPointIntersections(){
        double D = 2.0;
        TreeMap<Integer, ChargingPoint> map = new TreeMap<>();
        List<Double> intersections;
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
