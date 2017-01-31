package edu.berkeley.cs.amplab.carat.android.models;

import java.io.Serializable;

import edu.berkeley.cs.amplab.carat.android.utils.Range;

/**
 * Created by Jonatan Hamberg on 27.01.2016.
 */
public class Peak implements Serializable {
    private static final long serialVersionUID = -7911168046749271687L;

    private Range<Double> range;
    private Double values[];
    private Double auc;
    private Double inten1;
    private Double inten2;
    private Double length;
    private Double skewness;
    private Double kurtosis;
    private Double mean;
    private Double variance;

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

    public Double[] getValues() {
        return values;
    }

    public Peak setValues(Double[] values) {
        this.values = values;
        return this;
    }

    public Double getAuc() {
        return auc;
    }

    public Peak setAuc(Double auc) {
        this.auc = auc;
        return this;
    }

    public Double getInten1() {
        return inten1;
    }

    public Peak setInten1(Double inten1) {
        this.inten1 = inten1;
        return this;
    }

    public Double getInten2() {
        return inten2;
    }

    public Peak setInten2(Double inten2) {
        this.inten2 = inten2;
        return this;
    }

    public Double getLength() {
        return length;
    }

    public Peak setLength(Double length) {
        this.length = length;
        return this;
    }

    public Double getSkewness() {
        return skewness;
    }

    public Peak setSkewness(Double skewness) {
        this.skewness = skewness;
        return this;
    }

    public Double getKurtosis() {
        return kurtosis;
    }

    public Peak setKurtosis(Double kurtosis) {
        this.kurtosis = kurtosis;
        return this;
    }

    public Double getMean() {
        return mean;
    }

    public Peak setMean(Double mean) {
        this.mean = mean;
        return this;
    }

    public Double getVariance() {
        return variance;
    }

    public Peak setVariance(Double variance) {
        this.variance = variance;
        return this;
    }
}
