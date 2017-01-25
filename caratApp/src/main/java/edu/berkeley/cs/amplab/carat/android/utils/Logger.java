package edu.berkeley.cs.amplab.carat.android.utils;

import android.util.Log;

import edu.berkeley.cs.amplab.carat.android.Constants;

/**
 * Created by Jonatan on 1/25/17.
 */
public class Logger {
    public static final boolean DEBUG = Constants.DEBUG;
    public static final boolean INFO = true;

    public static void d(String TAG, String message){
        if(DEBUG){
            Log.d(TAG, message);
        }
    }

    public static void i(String TAG, String message){
        if(INFO){
            Log.i(TAG, message);
        }
    }
}
