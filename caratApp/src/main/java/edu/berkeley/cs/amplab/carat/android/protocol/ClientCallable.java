package edu.berkeley.cs.amplab.carat.android.protocol;

import org.apache.thrift.TException;

import edu.berkeley.cs.amplab.carat.thrift.CaratService;

/**
 * Created by Jonatan Hamberg on 31.1.2018.
 */

public abstract class ClientCallable<T> {
    abstract T task(CaratService.Client client) throws TException;
}
