package edu.berkeley.cs.amplab.carat.android.utils;

import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * General utilities for handling different data types.
 * Created by Jonatan Hamberg on 1.2.2017.
 */
public class Util {
    public static double[] toArray(List<Double> list){
        double[] result = new double[list.size()];
        for(int i=0; i<result.length; i++){
            result[i] = list.get(i);
        }
        return result;
    }

    public static <K,V> SortedMap<K, V> firstEntries(int limit, SortedMap<K,V> source){
        TreeMap<K, V> result = new TreeMap<>();
        for(Map.Entry<K,V> entry : source.entrySet()){
            if(result.size() >= limit){
                break;
            }
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static long timeAfterTime(long milliseconds){
        return System.currentTimeMillis() + milliseconds;
    }
}
