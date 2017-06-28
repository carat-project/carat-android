package edu.berkeley.cs.amplab.carat.android.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Created by Jonatan Hamberg on 27.01.2017.
 */
public class ExpiringMap<K, V> implements Iterable<V>{
    /**
     * Pair whose hashcode is determined only by the second element.
     * This is necessary to make sure keys with different timestamps
     * override each other.
     * @param <F> first element
     * @param <S> second element
     */
    private class Pair<F, S>{
        F first; S second;

        Pair(F first, S second){
            this.first = first;
            this.second = second;
        }

        @Override
        public int hashCode() {
            return second.hashCode();
        }
    }

    private SortedMap<Pair<Long, K>, V> items;
    private long expiration;

    public ExpiringMap(long expiration){
        items = new TreeMap<>((k1, k2) -> k1.first.compareTo(k2.first));
        this.expiration = expiration;
    }

    public void put(K k, V v){
        invalidate();
        long time = System.currentTimeMillis();
        Pair<Long, K> key = new Pair<>(time, k);
        items.put(key, v);
    }

    /**
     * Finds matching value for the provided key.
     * @param k key
     * @return matching value or null if not found
     */
    @SuppressWarnings("SuspiciousMethodCalls")
    public V get(K k){
        invalidate();

        // This call might seem suspicious as the sorted map actually
        // uses Pair as the key, but since the implementation has a
        // specific hash function which only considers the second value,
        // there should not be any problems with the key type.
        return items.get(k);
    }

    public V latest(){
        invalidate();
        if(items.isEmpty()){
            return null;
        }
        return items.get(items.firstKey());
    }

    public List<K> keySet(){
        List<K> result = new ArrayList<K>();
        for(Pair<Long, K> key : items.keySet()){
            result.add(key.second);
        }
        return result;
    }

    public void invalidate(){
        long time = System.currentTimeMillis();
        for(Pair<Long, K> key : items.keySet()){
            long timestamp = key.first;
            if(time - timestamp <= expiration){
                items.remove(key);
            }
        }
    }

    @Override
    public Iterator<V> iterator() {
        invalidate();
        return items.values().iterator();
    }
}
