package edu.berkeley.cs.amplab.carat.android.sampling;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.google.gson.Gson;

import java.util.List;
import java.util.concurrent.TimeUnit;

import edu.berkeley.cs.amplab.carat.android.CaratApplication;
import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;
import edu.berkeley.cs.amplab.carat.android.utils.Util;

/**
 * Created by Jonatan Hamberg on 1.2.2017.
 */
public class IntentRouter extends IntentService implements LocationListener {
    private final static String TAG = IntentRouter.class.getSimpleName();
    private final static long SAMPLING_INTERVAL = TimeUnit.MINUTES.toMillis(15);
    private final static int requestCode = 67294580;
    private final static int NO_FLAG = 0;

    private Context context;
    private AlarmManager alarmManager;

    public IntentRouter(){
        super(TAG);
        context = getApplicationContext();
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(intent != null){
            String action = intent.getAction();
            if(action != null){
                switch(action){
                    case Constants.SCHEDULED_SAMPLE: scheduledSample(); break;
                    // TODO: Implement rest of the actions: normal sample, rapid charging...
                    // TODO: Call requestLocationUpdates here before collecting Sample
                    default: Logger.d(TAG, "Implement me: " + action + "!");
                }
            }
        }
    }

    private void scheduledSample(){
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        String uuId = p.getString(CaratApplication.getRegisteredUuid(), null);
        Sampler2.sample(context, uuId, Constants.SCHEDULED_SAMPLE, "FIXME");

        scheduleNext();
    }

    private void scheduleNext(){
        // TODO: Check if already have something scheduled and exit if we do.
        Intent scheduleIntent = new Intent(context, IntentRouter.class);
        scheduleIntent.setAction(Constants.SCHEDULED_SAMPLE);
        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(context, requestCode, scheduleIntent, NO_FLAG);
        long t =  Util.timeAfterTime(SAMPLING_INTERVAL);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pendingIntent);
        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, t, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, t, pendingIntent);
        }
    }

    private void requestLocationUpdates() {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        List<String> providers = SamplingLibrary.getEnabledLocationProviders(context);
        if (providers != null) {
            for (String provider : providers) {
                locationManager.requestLocationUpdates(provider, Constants.FRESHNESS_TIMEOUT, 0, this);
            }
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Context context = getApplicationContext();
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        long distance = p.getLong("distanceMoved", 0);
        String locationJSON = p.getString("lastKnownLocation", "");
        Location lastKnownLocation = new Gson().fromJson(locationJSON, Location.class);
        if (location != null && lastKnownLocation != null) {
            distance += lastKnownLocation.distanceTo(location);
        }
        p.edit().putLong("distanceMoved", distance).apply();
        p.edit().putString("lastKnownLocation", new Gson().toJson(location)).apply();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        requestLocationUpdates();
    }

    @Override
    public void onProviderEnabled(String provider) {
        requestLocationUpdates();
    }

    @Override
    public void onProviderDisabled(String provider) {
        requestLocationUpdates();
    }
}
