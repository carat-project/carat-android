package edu.berkeley.cs.amplab.carat.android.utils;

import android.util.Log;

import edu.berkeley.cs.amplab.carat.android.Constants;

/**
 * Created by Jonatan on 1/25/17.
 */
public class Logger {
    public static void d(String TAG, String message){
        if(Constants.DEBUG){
            Log.d(TAG, message);
        }
    }
}
