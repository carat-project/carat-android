package edu.berkeley.cs.amplab.carat;

import android.app.Instrumentation;
import android.content.Context;
import android.content.res.Resources;
import android.test.ActivityInstrumentationTestCase2;
import android.util.DisplayMetrics;

import java.util.List;
import java.util.Locale;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;

import edu.berkeley.cs.amplab.carat.android.MainActivity;
import edu.berkeley.cs.amplab.carat.android.R;
import edu.berkeley.cs.amplab.carat.android.models.ChargingPoint;
import edu.berkeley.cs.amplab.carat.android.utils.PeakUtils;

//
//  Created by Jonatan C Hamberg on 8.5.2016.
//  Copyright © 2016 University of Helsinki. All rights reserved.
//
public class MainActivityTest extends ActivityInstrumentationTestCase2<MainActivity> {

    private MainActivity activity;
    private Instrumentation instrumentation;
    private Context context;

    private String UPDATED_NEVER;
    private String UPDATED_NOW;

    public MainActivityTest(){
        super(MainActivity.class);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        instrumentation = getInstrumentation();
        activity = getActivity();
        context = instrumentation.getContext();
        Resources resources = getInstrumentation().getTargetContext().getResources();

        UPDATED_NEVER = resources.getString(R.string.neverupdated);
        UPDATED_NOW = resources.getString(R.string.updatedjustnow);

        // Use default locale when testing
        DisplayMetrics metrics = resources.getDisplayMetrics();
        android.content.res.Configuration configuration = resources.getConfiguration();
        configuration.locale = new Locale("en");
        resources.updateConfiguration(configuration, metrics);
    }

    public void testTimeStrings() {
        assertEquals(activity.getTimeString(-1), UPDATED_NEVER);
        assertEquals(activity.getTimeString(-1000), UPDATED_NEVER);
        assertEquals(activity.getTimeString(-TimeUnit.DAYS.toMillis(10000)), UPDATED_NEVER);
        assertEquals(activity.getTimeString(0), UPDATED_NOW);
        assertEquals(activity.getTimeString(TimeUnit.SECONDS.toMillis(1)), UPDATED_NOW);
        assertEquals(activity.getTimeString(TimeUnit.SECONDS.toMillis(59)), UPDATED_NOW);
        assertEquals(activity.getTimeString(TimeUnit.SECONDS.toMillis(60)), "Updated a minute ago");
        assertEquals(activity.getTimeString(TimeUnit.SECONDS.toMillis(119)), "Updated a minute ago");
        assertEquals(activity.getTimeString(TimeUnit.SECONDS.toMillis(120)), "Updated 2 minutes ago");
        assertEquals(activity.getTimeString(TimeUnit.SECONDS.toMillis(179)), "Updated 2 minutes ago");
        assertEquals(activity.getTimeString(TimeUnit.MINUTES.toMillis(59)), "Updated 59 minutes ago");
        assertEquals(activity.getTimeString(TimeUnit.MINUTES.toMillis(60)), "Updated an hour ago");
        assertEquals(activity.getTimeString(TimeUnit.MINUTES.toMillis(119)), "Updated an hour ago");
        assertEquals(activity.getTimeString(TimeUnit.MINUTES.toMillis(120)), "Updated 2 hours ago");
        assertEquals(activity.getTimeString(TimeUnit.HOURS.toMillis(23)), "Updated 23 hours ago");
        assertEquals(activity.getTimeString(TimeUnit.HOURS.toMillis(24)), "Updated a day ago");
        assertEquals(activity.getTimeString(TimeUnit.HOURS.toMillis(47)), "Updated a day ago");
        assertEquals(activity.getTimeString(TimeUnit.HOURS.toMillis(48)), "Updated 2 days ago");
        assertEquals(activity.getTimeString(TimeUnit.DAYS.toMillis(6)), "Updated 6 days ago");
        assertEquals(activity.getTimeString(TimeUnit.DAYS.toMillis(7)), "Updated a week ago");
        assertEquals(activity.getTimeString(TimeUnit.DAYS.toMillis(13)), "Updated a week ago");
        assertEquals(activity.getTimeString(TimeUnit.DAYS.toMillis(14)), "Updated 2 weeks ago");
        assertEquals(activity.getTimeString(TimeUnit.DAYS.toMillis(100*7)), "Updated 100 weeks ago");
        assertEquals(activity.getTimeString(TimeUnit.DAYS.toMillis(10000*7)), "Updated 10000 weeks ago");
    }
}