package edu.berkeley.cs.amplab.carat.android.models;

import edu.berkeley.cs.amplab.carat.android.utils.Range;

/**
 * Created by Jonatan Hamberg on 27.01.2016.
 */
public class Peak {
    private Range<Double> range;
    private double values[];
    private double auc;
    private double inten1;
    private double inten2;
    private double length;
    private double skewness;
    private double kurtosis;
    private double mean;
    private double variance;

    public Peak(){
        // Not implemented
    }

    public Range<Double> getRange() {
        return range;
    }

    public Peak setRange(Range<Double> range) {
        this.range = range;
        return this;
    }

    public double[] getValues() {
        return values;
    }

    public Peak setValues(double[] values) {
        this.values = values;
        return this;
    }

    public double getAuc() {
        return auc;
    }

    public Peak setAuc(double auc) {
        this.auc = auc;
        return this;
    }

    public double getInten1() {
        return inten1;
    }

    public Peak setInten1(double inten1) {
        this.inten1 = inten1;
        return this;
    }

    public double getInten2() {
        return inten2;
    }

    public Peak setInten2(double inten2) {
        this.inten2 = inten2;
        return this;
    }

    public double getLength() {
        return length;
    }

    public Peak setLength(double length) {
        this.length = length;
        return this;
    }

    public double getSkewness() {
        return skewness;
    }

    public Peak setSkewness(double skewness) {
        this.skewness = skewness;
        return this;
    }

    public double getKurtosis() {
        return kurtosis;
    }

    public Peak setKurtosis(double kurtosis) {
        this.kurtosis = kurtosis;
        return this;
    }

    public double getMean() {
        return mean;
    }

    public Peak setMean(double mean) {
        this.mean = mean;
        return this;
    }

    public double getVariance() {
        return variance;
    }

    public Peak setVariance(double variance) {
        this.variance = variance;
        return this;
    }
}
