package edu.berkeley.cs.amplab.carat.android.sampling;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.BatteryManager;
import android.os.Bundle;
import android.support.v4.content.WakefulBroadcastReceiver;

import edu.berkeley.cs.amplab.carat.android.CaratApplication;
import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.models.ChargingSession;
import edu.berkeley.cs.amplab.carat.android.utils.BatteryUtils;
import edu.berkeley.cs.amplab.carat.android.utils.ExpiringList;
import edu.berkeley.cs.amplab.carat.android.utils.ExpiringMap;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;
import edu.berkeley.cs.amplab.carat.android.utils.Util;
import edu.berkeley.cs.amplab.carat.thrift.Sample;

public class Sampler extends WakefulBroadcastReceiver implements LocationListener {
    private static final String TAG = Sampler.class.getSimpleName();
    public static final int MAX_SAMPLES = 250;
    public static final int CHARGING_SESSION_LIMIT = 10;
    public static final int CHARGING_MINIMUM_LEVELS = 5;

    private static Sampler instance = null;
    private ChargingSession session = null;
    private long lastLevelTimestamp;
    private Context context = null;
    private Location lastKnownLocation = null;
    private double distance = 0.0;

    private Sample lastSample = null;
    private long lastNotify;

    public static Sampler getInstance() {
    	if (instance == null){
            Sampler.instance = new Sampler();
        }
    	return instance;
    }
    
    private void requestLocationUpdates() {
        LocationManager locationManager =
                (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(this);
        List<String> providers = SamplingLibrary.from(context).getEnabledLocationProviders();
        if (providers != null) {
            for (String provider : providers) {
                locationManager.requestLocationUpdates(provider,
                        Constants.FRESHNESS_TIMEOUT, 0, this);
            }
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        double level = BatteryUtils.getBatteryLevel(intent);
        if(level != 1){
            // For some reason we use dec
            // imal levels
            SamplingLibrary.setCurrentBatteryLevel(level/100);
            if(this.context == null){
                this.context = context;
            }

            // Only request location updates on level broadcasts
            requestLocationUpdates();
            if(lastKnownLocation == null){
                lastKnownLocation = SamplingLibrary.getLastKnownLocation(context);
            }
        }

        startSamplerService(context, intent);
    }

    public void startSamplerService(Context context, Intent intent){
        Intent service = new Intent(context, SamplerService.class);
        // Pass the action and extras to the service

        if(intent != null){
            Bundle extras = intent.getExtras();
            if(extras != null){
                service.putExtras(intent.getExtras());
            }
            service.setAction(intent.getAction());
        }
        service.putExtra("distance", distance);

        startWakefulService(context, service);
    }

    public double getDistanceSinceLastSample(){
        double result = distance;

        // Reset the distance here
        distance = 0;
        return result;
    }

    @Override
    public void onLocationChanged(Location location) {
        if (lastKnownLocation != null && location != null) {
            distance += lastKnownLocation.distanceTo(location);
        }
        lastKnownLocation = location;
    }

    @Override
    public void onProviderDisabled(String provider) {
        requestLocationUpdates();
    }

    @Override
    public void onProviderEnabled(String provider) {
        requestLocationUpdates();
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        requestLocationUpdates();
    }

    public void setLastSample(Sample sample){
        lastSample = sample;
    }

    public Sample getLastSample(){
        return lastSample;
    }

    public ChargingSession newChargingSession(){
        if(session != null){
            saveCurrentChargingSession();
        }
        session = ChargingSession.create();
        Logger.i(TAG, "Started a charging session!");
        return session;
    }

    public void stopChargingSession(){
        if(session != null){
            saveCurrentChargingSession();
        }
        session = null;
    }

    public ChargingSession getCurrentChargingSession(){
        return session;
    }

    private void saveCurrentChargingSession(){
        // TODO: Is this too slow to do here, move to separate service?
        if(session.getPoints().size() < CHARGING_MINIMUM_LEVELS){
            Logger.d(TAG, "Charging session did not have enough points, discarding.");
            return;
        }
        long timestamp = session.getTimestamp();
        SortedMap<Long, ChargingSession> sessions
                = CaratApplication.getStorage().getChargingSessions();
        int diff = sessions.size() - CHARGING_SESSION_LIMIT;
        if(diff >= 0){
            sessions = Util.firstEntries(CHARGING_SESSION_LIMIT-1, sessions);
        }
        sessions.put(timestamp, session);
        Logger.d(TAG, "Saved session " + session);
        CaratApplication.getStorage().writeChargingSessions(sessions);
        Logger.i(TAG, "Stopped charging session!");
    }

    public void setLastLevelTimestamp(long timestamp){
        lastLevelTimestamp = timestamp;
    }

    public long getLastLevelTimestamp(){
        return lastLevelTimestamp;
    }

    public long getLastNotify() {
        return lastNotify;
    }

    public void setLastNotify(long now) {
        this.lastNotify = now;
    }
}
