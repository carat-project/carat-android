package edu.berkeley.cs.amplab.carat.android.utils;

import java.io.RandomAccessFile;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * General utilities for handling different data types.
 * Created by Jonatan Hamberg on 1.2.2017.
 */
public class Util {
    private final static String TAG = Util.class.getSimpleName();

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

    public static Integer[][] readRAF(String stream, int maxRows, int maxColumns, String delim){
        Integer[][] result = new Integer[maxRows+1][maxColumns+1];
        try{
            RandomAccessFile reader = new RandomAccessFile(stream, "/r");
            for (int row = 0; row < maxRows; row++) {
                String line = reader.readLine();
                if (line == null) break; // EOF
                String[] tokens = line.split(delim);
                for (int column = 0; column < maxColumns; column++) {
                    if(maxColumns < tokens.length && isInteger(tokens[column])){
                        result[row][column] = Integer.parseInt(tokens[column]);
                    } else {
                        result[row][column] = 0;
                    }
                }
            }
            reader.close();
        } catch(Throwable th){
            Logger.e(TAG, "Failed reading " + stream + ": " + th);
            return null;
        }
        return result;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean isInteger(String value){
        try {
            Integer.parseInt(value);
        } catch(Throwable th){
            return false;
        }
        return true;
    }

    public static long timeAfterTime(long milliseconds){
        return System.currentTimeMillis() + milliseconds;
    }
}
