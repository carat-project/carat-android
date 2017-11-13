package edu.berkeley.cs.amplab.carat.android;

/**
 * Created by Jonatan Hamberg on 13.11.2017.
 */
public class CaratActions {
    private static final String ACTION_BASE = BuildConfig.APPLICATION_ID;

    public static final String SCHEDULED_SAMPLE = ACTION_BASE + ".SCHEDULED_SAMPLE";
    public static final String CHECK_SCHEDULE = ACTION_BASE + ".CHECK_SCHEDULE";
    public static final String RAPID_SAMPLING = ACTION_BASE + ".RAPID_SAMPLING";
    public static final String CHARGING_ANOMALY = ACTION_BASE + ".CHARGING_ANOMALY";
}
