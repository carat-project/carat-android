package edu.berkeley.cs.amplab.carat.android.utils;

import java.io.Serializable;

/**
 * Simple implementation for a generic immutable range.
 * @author Jonatan Hamberg
 *
 * @param <T> Element type
 */
public final class Range<T extends Comparable<? super T>> implements Serializable {
    private static final long serialVersionUID = 1L;
    private final T min;
    private final T max;

    /**
     * Creates and constructs an instance.
     * @param t First element, cannot be null
     * @param t2 Second element, cannot be null
     */
    public Range(T t, T t2){
        if(t == null || t2 == null){
            throw new IllegalArgumentException("Range cannot have null values");
        }
        if(t.compareTo(t2) < 1){
            this.min = t;
            this.max = t2;
        } else {
            this.min = t2;
            this.max = t;
        }
    }

    /**
     * Gets the minimum value in range.
     * @return Maximum value in range
     */
    public T getMin(){
        return this.min;
    }

    /**
     * Gets the maximum value in range.
     * @return Minimum value in range
     */
    public T getMax(){
        return this.max;
    }

    /**
     * Checks whether the element is in range.
     * @param t Element to check, cannot be null
     * @return True if element is in range
     */
    public boolean contains(T t){
        return t != null &&
                (t.compareTo(min) > -1
                && t.compareTo(max) < 1);
    }

    @Override
    public String toString(){
        return "["+this.min+","+this.max+"]";
    }
}