package edu.berkeley.cs.amplab.carat.android.models;

import java.io.Serializable;

/**
 * Created by Jonatan Hamberg on 30.1.2017.
 */
public class ChargingPoint implements Serializable{
    private Double time;
    private Double squareSum;
    private Double average;

    public ChargingPoint(Double time, Double squareSum, Double average){
        this.time = time;
        this.squareSum = squareSum;
        this.average = average;
    }

    public Double getTime() {
        return time;
    }

    public Double getSquareSum() {
        return squareSum;
    }


    public Double getAverage() {
        return average;
    }
}
