package edu.berkeley.cs.amplab.carat.android.utils;

import android.util.Log;

import edu.berkeley.cs.amplab.carat.android.Constants;

/**
 * Created by Jonatan Hamberg on 25.01.2017.
 */
@SuppressWarnings("PointlessBooleanExpression")
public class Logger {
    // Disable all logging by setting this to false
    private static final boolean ENABLED = true;

    private static final boolean DEBUG = ENABLED && Constants.DEBUG;
    private static final boolean INFO = ENABLED && true;
    private static final boolean ERROR = ENABLED && true;

    public static void d(String TAG, String message){
        if(DEBUG){
            Log.d(TAG, message);
        }
    }

    public static void d(String TAG, String message, Throwable th){
        if(DEBUG){
            Log.d(TAG, message, th);
        }
    }

    public static void i(String TAG, String message){
        if(INFO){
            Log.i(TAG, message);
        }
    }

    public static void i(String TAG, String message, Throwable th){
        if(INFO){
            Log.i(TAG, message, th);
        }
    }

    public static void e(String TAG, String message){
        if(ERROR){
            Log.e(TAG, message);
        }
    }

    public static void e(String TAG, String message, Throwable th){
        if(ERROR){
            Log.e(TAG, message, th);
        }
    }
}
