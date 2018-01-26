package edu.berkeley.cs.amplab.carat.android.protocol;

import java.util.concurrent.Callable;

/**
 * Created by Jonatan Hamberg on 26.1.2018.
 */

public abstract class RefreshCallable<Boolean>{
    public abstract boolean call(String v1, String v2, String v3);
}
