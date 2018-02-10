package edu.berkeley.cs.amplab.carat.android.receivers;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.support.annotation.Nullable;

import com.google.gson.Gson;

import java.util.List;
import java.util.concurrent.TimeUnit;

import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.Keys;
import edu.berkeley.cs.amplab.carat.android.sampling.SamplingLibrary;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;

/**
 * Created by Jonatan Hamberg on 26.4.2017.
 */
public class LocationReceiver extends Service implements LocationListener{
    private static final String TAG = LocationReceiver.class.getSimpleName();
    private static final long ACCURACY_GRACE_PERIOD = TimeUnit.MINUTES.toMillis(1);
    private static final long WAIT_BETWEEN_REQUESTS = TimeUnit.SECONDS.toMillis(5);
    private boolean terminating = false;
    private long lastRequest = 0;
    private Location bestLocation = null;
    private Context context;
    private LocationManager locationManager;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        context = getApplicationContext();
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        requestLocationUpdates();
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onLocationChanged(Location location) {
        Logger.d(TAG, "Received a location update with accuracy " + location.getAccuracy());
        if(bestLocation != null){
            float lastAccuracy = bestLocation.getAccuracy();
            float accuracy = location.getAccuracy();
            Logger.d(TAG, "Last accuracy: " + lastAccuracy + ", accuracy: " + accuracy);
            if(accuracy < lastAccuracy){
                Logger.d(TAG, "New best location!");
                bestLocation = location;
            }
        } else {
            bestLocation = location;
        }
    }

    public void requestLocationUpdates() {
        long now = System.currentTimeMillis();
        Logger.d(TAG, "Requesting location updates");
        if(locationManager == null){
            stop();
        } else if(now - lastRequest >= WAIT_BETWEEN_REQUESTS){ // Make sure we dont loop requests
            lastRequest = now;
            Criteria criteria = new Criteria();
            criteria.setAccuracy(Criteria.ACCURACY_COARSE);
            locationManager.requestSingleUpdate(criteria, this, Looper.myLooper());
            locationManager.requestLocationUpdates(0, 0, criteria, this, Looper.myLooper());

            // Also request on each provider, if any
            List<String> providers = SamplingLibrary.getEnabledLocationProviders(context);
            if (providers != null) {
                Logger.d(TAG, providers + " " + providers.size());
                for (String provider : providers) {
                    Logger.d(TAG, "Provider: " + provider);
                    locationManager.requestSingleUpdate(provider, this, Looper.myLooper());
                    locationManager.requestLocationUpdates(provider, Constants.FRESHNESS_TIMEOUT, 0, this);
                }
            }
        } else {
            Logger.d(TAG, "Requesting location updates too quickly");
        }
        scheduleSaveAndStop();
    }

    private void scheduleSaveAndStop(){
        if(!terminating){
            terminating = true;
            new Handler().postDelayed(() -> {
                if(bestLocation != null){
                    saveInSharedPreferences();
                }
                stop();
            }, ACCURACY_GRACE_PERIOD);
            Logger.d(TAG, "Service stopping in " + ACCURACY_GRACE_PERIOD/1000 + " seconds");
        }
    }

    private void saveInSharedPreferences(){
        final SharedPreferences prefs = getSharedPreferences(Constants.PREFERENCE_LOCATION_NAME, MODE_PRIVATE);
        long distance = prefs.getLong(Keys.distanceTraveled, 0);
        String locationJSON = prefs.getString(Keys.lastKnownLocation, "");

        Location lastKnownLocation = new Gson().fromJson(locationJSON, Location.class);
        if (lastKnownLocation != null) {
            distance += lastKnownLocation.distanceTo(bestLocation);
        }

        locationJSON = new Gson().toJson(bestLocation);
        prefs.edit().putLong(Keys.distanceTraveled, distance).apply();
        prefs.edit().putString(Keys.lastKnownLocation, locationJSON).apply();

        Logger.d(TAG, "Distance traveled: " + distance);
        Logger.d(TAG, "Last known location: " + locationJSON);
    }

    public void stop(){
        if(locationManager != null){
            locationManager.removeUpdates(this);
            locationManager = null;
        }
        Logger.d(TAG, "Stopped location receiver");
        stopSelf();
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
