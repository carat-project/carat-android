package edu.berkeley.cs.amplab.carat.android.receivers;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.google.gson.Gson;

import java.util.List;

import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.Keys;
import edu.berkeley.cs.amplab.carat.android.sampling.SamplingLibrary;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;
import edu.berkeley.cs.amplab.carat.android.utils.Util;

/**
 * Created by Jonatan Hamberg on 26.4.2017.
 */
public class LocationReceiver extends Service implements LocationListener{
    private final static String TAG = LocationReceiver.class.getSimpleName();
    private Context context;
    private PowerManager powerManager;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        context = getApplicationContext();
        powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        requestLocationUpdates();
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void requestLocationUpdates() {
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
        PowerManager.WakeLock wl = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        Util.safeReleaseWakelock(wl);
        wl.acquire(10*60*1000L /*10 minutes*/);
        Logger.d(TAG, "Received a location update");

        Context context = getApplicationContext();
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        long distance = prefs.getLong(Keys.distanceTraveled, 0);
        String locationJSON = prefs.getString(Keys.lastKnownLocation, "");

        Location lastKnownLocation = new Gson().fromJson(locationJSON, Location.class);
        if (location != null && lastKnownLocation != null) {
            distance += lastKnownLocation.distanceTo(location);
        }

        locationJSON = new Gson().toJson(location);
        prefs.edit().putLong(Keys.distanceTraveled, distance).apply();
        prefs.edit().putString(Keys.lastKnownLocation, locationJSON).apply();

        Logger.d(TAG, "Distance traveled: " + distance);
        Logger.d(TAG, "Last known location: " + locationJSON);
        Util.safeReleaseWakelock(wl);

        // Enable for maximum energy efficiency
        stopSelf();
        Logger.d(TAG, "Location service shutting down");
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
