package edu.berkeley.cs.amplab.carat.android.utils;

import android.support.v4.util.Pair;

import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by Jonatan Hamberg on 27.01.2017.
 */
public class ExpiringList<T> implements Iterable<T>{
    private SortedMap<Pair<Long, Integer>, T> items;
    private long expiration;

    public ExpiringList(long expiration){
        items = new TreeMap<>((k1, k2) -> k1.first.compareTo(k2.first));
        this.expiration = expiration;
    }

    public void add(T item){
        invalidate();
        long time = System.currentTimeMillis();
        int hash = item.hashCode();
        Pair<Long, Integer> key = new Pair<>(time, hash);
        items.put(key, item);
    }

    public T latest(){
        invalidate();
        if(items.isEmpty()){
            return null;
        }
        return items.get(items.firstKey());
    }

    public void invalidate(){
        long time = System.currentTimeMillis();
        for(Pair<Long, Integer> key : items.keySet()){
            long timestamp = key.first;
            if(time - timestamp <= expiration){
                items.remove(key);
            }
        }
    }

    @Override
    public Iterator<T> iterator() {
        invalidate();
        return items.values().iterator();
    }
}
