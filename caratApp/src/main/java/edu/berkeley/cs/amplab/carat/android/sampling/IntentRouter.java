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

import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.Keys;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;
import edu.berkeley.cs.amplab.carat.android.utils.Util;

/**
 * Created by Jonatan Hamberg on 1.2.2017.
 */
public class IntentRouter extends IntentService implements LocationListener {
    private final static String TAG = IntentRouter.class.getSimpleName();
    private final static long SAMPLING_INTERVAL = TimeUnit.MINUTES.toMillis(15);
    private final static int REQUEST_CODE = 67294580;

    private Context context;
    private AlarmManager alarmManager;

    public IntentRouter(){
        super(TAG);
        context = getApplicationContext();
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        requestLocationUpdates();
        if(intent != null){
            String action = intent.getAction();
            if(action != null){
                switch(action){
                    case Constants.RAPID_SAMPLING:
                        // TODO: Foreground notification and sampling
                        break;
                    case Constants.SCHEDULED_SAMPLE:
                        scheduleNextSample(SAMPLING_INTERVAL);
                        break;
                    case Intent.ACTION_BATTERY_CHANGED:
                        // TODO: Do something here?
                        break;
                    default: Logger.d(TAG, "Implement me: " + action + "!");
                }
                checkSchedule();
                Sampler2.sample(context, action);
            }
        }
    }

    private void checkSchedule(){
        // TODO: See if the schedule still holds and reschedule if not.
    }

    private boolean isAlreadyScheduled(Intent intent){
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_NO_CREATE) != null;
    }

    private void scheduleNextSample(long interval){
        Intent scheduleIntent = new Intent(context, IntentRouter.class);
        scheduleIntent.setAction(Constants.SCHEDULED_SAMPLE);
        if(!isAlreadyScheduled(scheduleIntent)){
            PendingIntent pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, scheduleIntent, 0);
            long then = Util.timeAfterTime(interval);

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, then, pendingIntent);
            } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, then, pendingIntent);
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, then, pendingIntent);
            }
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
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long distance = prefs.getLong(Keys.distanceTraveled, 0);
        String locationJSON = prefs.getString(Keys.lastKnownLocation, "");
        Location lastKnownLocation = new Gson().fromJson(locationJSON, Location.class);
        if (location != null && lastKnownLocation != null) {
            distance += lastKnownLocation.distanceTo(location);
        }
        prefs.edit().putLong(Keys.distanceTraveled, distance).apply();
        prefs.edit().putString(Keys.lastKnownLocation, new Gson().toJson(location)).apply();
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
