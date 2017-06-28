package edu.berkeley.cs.amplab.carat.android.utils;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.TrapezoidIntegrator;

import java.util.List;

/**
 * Created by Jonatan Hamberg on 1.2.2017.
 */
public class MathUtils {
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
        double alpha = Math.max(1/(double)(n+2), 1/window);
        return prev + alpha*(x-prev);
    }

    // Square sum
    public static double ss(double x, double prev, int n, double cma){
        double beta = n/(double)(n+1);
        return prev + beta * Math.pow(x - cma, 2);
    }

    /**
     * Differences of area under curve for two lines.
     * @param start start x-coordinate
     * @param end end x-coordinate
     * @param f1 first line function
     * @param f2 second line function
     * @return difference
     */
    public static double auc(double start, double end, UnivariateFunction f1, UnivariateFunction f2){
        TrapezoidIntegrator trapezoid = new TrapezoidIntegrator();
        double area1 = trapezoid.integrate(Integer.MAX_VALUE, f1, start, end);
        double area2 = trapezoid.integrate(Integer.MAX_VALUE, f2, start, end);
        return area1 - area2;
    }


    /**
     * Intersection between two lines constructed from two pairs of points.
     * Line 1 is constructed between points (x1,y1) and (x2,y2).
     * Line 2 is constructed between points (x3,y3) and (x4,y4).
     * @return intersection point on the x-axis
     */
    public static Double intersection2L(double x1, double y1, double x2, double y2,
                                        double x3, double y3, double x4, double y4){
        return ((x1*y2 - y1*x2)*(x3 - x4) - (x1-x2)*(x3*y4-y3*x4))/
                ((x1-x2)*(y3-y4) - (y1-y2)*(x3-x4));
    }


    public static UnivariateFunction functionFromPoints(List<Double> arr){
        return x -> {
            // Integer points beside the requested point
            int x1 = (int)Math.floor(x);
            int x2 = x1 + 1;

            // If we are beyond the known points just use a simple interpolation
            // between the first and the last known points.
            if(x2 >= arr.size() || x1 < 0){
                x1 = 0;
                x2 = arr.size()-1;
            }
            double y1 = arr.get(x1);
            double y2 = arr.get(x2);
            return ((y2-y1)/(x2-x1))*(x-x1)+y1;
        };
    }
}
