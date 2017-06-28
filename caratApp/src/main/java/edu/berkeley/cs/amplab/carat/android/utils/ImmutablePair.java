package edu.berkeley.cs.amplab.carat.android.utils;

import java.io.Serializable;

/**
 * Created by Jonatan on 30.1.2017.
 */
public class ImmutablePair<F extends Serializable, S extends Serializable> implements Serializable {
    private static final long serialVersionUID = -6388997905154127431L;

    private F first;
    private S second;

    public ImmutablePair(F first, S second){
        this.first = first;
        this.second = second;
    }

    public F getFirst() {
        return first;
    }

    public S getSecond() {
        return second;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof ImmutablePair)){
            return false;
        }
        ImmutablePair<?, ?> pair = (ImmutablePair<?, ?>) obj;
        boolean eq1 = first == pair.first || first.equals(pair.first);
        boolean eq2 = second == pair.second || second.equals(pair.second);
        return eq1 && eq2;
    }

    @Override
    public int hashCode() {
        int h1 = first == null ? 0 : first.hashCode();
        int h2 = second == null ? 0 : second.hashCode();
        return h1 ^ h2;
    }
}
