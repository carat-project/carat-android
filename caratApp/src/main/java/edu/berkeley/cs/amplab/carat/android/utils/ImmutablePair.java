package edu.berkeley.cs.amplab.carat.android.utils;

import java.io.Serializable;

/**
 * Created by Jonatan on 30.1.2017.
 */
public class ImmutablePair<F extends Serializable, S extends Serializable> implements Serializable {
    public F getFirst() {
        return first;
    }

    private F first;
    private S second;

    public ImmutablePair(F first, S second){
        this.first = first;
        this.second = second;
    }

    public void setFirst(F first) {
        this.first = first;
    }

    public S getSecond() {
        return second;
    }

    public void setSecond(S second) {
        this.second = second;
    }

}
