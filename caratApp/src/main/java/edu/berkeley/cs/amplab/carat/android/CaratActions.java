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
    public static final String CHARGING_NEW = ACTION_BASE + ".CHARGING_NEW";
    public static final String CHARGING_UPDATE = ACTION_BASE + ".CHARGING_UPDATE";
    public static final String STOP_RAPID_SAMPLING = ACTION_BASE + ".STOP_RAPID_SAMPLING";
    public static final String RAPID_SAMPLER_DYING = ACTION_BASE + ".RAPID_SAMPLER_DYING";
}
