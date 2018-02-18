package edu.berkeley.cs.amplab.carat.android.utils;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

/**
 * General utility class to benchmark how much time a code block takes to execute.
 * Please note that this class is very slow and should not be used in production.
 *
 * Created by Jonatan Hamberg on 18.2.2018
 */
public class Profiler {
    private static final String TAG = Profiler.class.getSimpleName();
    private static final String START_FORMAT = "[%s] Started profiling task \"%s\"";
    private static final String END_FORMAT = "[%s] Task \"%s\" took %.4f %s";

    private static final Locale locale = new Locale("en");

    private static final long SECOND = TimeUnit.SECONDS.toMillis(1);
    private static final long MINUTE = TimeUnit.MINUTES.toMillis(1);
    private static final long HOUR = TimeUnit.HOURS.toMillis(1);
    private static final long DAY = TimeUnit.DAYS.toMillis(1);

    public interface Callback {
        void run();
    }

    @SuppressWarnings("ThrowableNotThrown")
    public static void profile(String what, Callback with){
        String callerClass = new Exception().getStackTrace()[1].getClassName();
        String caller = callerClass.substring(callerClass.lastIndexOf(".")+1).trim();

        Logger.d(TAG, String.format(locale, START_FORMAT, caller, what));
        long start = System.currentTimeMillis();
        with.run();
        long end = System.currentTimeMillis();
        printResult(what, start, end, caller);
    }

    private static void printResult(String what, long start, long end, String caller){
        double elapsed = end - start;

        String unit;
        if(elapsed < SECOND){
            unit = "milliseconds";
        } else if(elapsed < MINUTE){
            elapsed = elapsed / SECOND;
            unit = "seconds";
        } else if(elapsed < HOUR){
            elapsed = MINUTE;
            unit = "minutes";
        } else if(elapsed < DAY){
            elapsed = elapsed / HOUR;
            unit = "hours";
        } else {
            elapsed = elapsed / DAY;
            unit = "days";
        }

        Logger.d(TAG, String.format(locale, END_FORMAT, caller, what, elapsed, unit));
    }
}