package edu.berkeley.cs.amplab.carat.android.sampling;

import android.content.Intent;

import java.lang.ref.WeakReference;
import java.util.SortedMap;

import edu.berkeley.cs.amplab.carat.android.CaratApplication;
import edu.berkeley.cs.amplab.carat.android.models.ChargingSession;
import edu.berkeley.cs.amplab.carat.android.storage.CaratDataStorage;
import edu.berkeley.cs.amplab.carat.android.utils.BatteryUtils;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;
import edu.berkeley.cs.amplab.carat.android.utils.Util;

/**
 * Created by Jonatan on 13.11.2017.
 */
public class ChargingSessionManager {
    private static final String TAG = ChargingSessionManager.class.getSimpleName();
    private static ChargingSessionManager instance;
    private static final long MAX_PAUSE_BETWEEN_REPLUG = 10000; // 10 seconds
    private WeakReference<SortedMap<Long, ChargingSession>> sessions;

    private CaratDataStorage storage;
    private ChargingSession session;
    private boolean paused = false;
    private long pauseTime = -1L;
    private long lastTime = -1L;

    // Create a singleton instance
    public static ChargingSessionManager getInstance(){
        synchronized (TAG){
            if(instance == null){
                instance = new ChargingSessionManager();
            }
            return instance;
        }
    }

    // Depend on storage rather than context which is leaky
    private ChargingSessionManager(){
        this.storage = CaratApplication.getStorage();
        this.paused = false;
        this.pauseTime = -1L;
    }

    public void updatePlugState(String state){
        if(session != null){
            if(session.getPlugState().equals("unknown")){
                // Keep first valid charger type
                session.setPlugState(state);
            } else if(!session.getPlugState().equals(state)){
                // Charger type changed, invalidate session
                saveSession();
                session = null;
            }
        }
    }

    private boolean validateSession(){
        // TODO: Check length etc.
        return session != null;
    }

    private boolean saveSession(){
        if(validateSession()){
            SortedMap<Long, ChargingSession> result = Util.getWeakOrFallback(sessions, () -> storage.getChargingSessions());
            if(result != null){
                result.put(session.getTimestamp(), session);
                storage.writeChargingSessions(result);
                sessions = new WeakReference<>(result);
                return true;
            }
        }
        return false;
    }

    public void createIfNull(){
        if(session == null){
            session = ChargingSession.create();
        }
    }

    public void handleBatteryIntent(Intent intent){
        deleteIfExpired();
        createIfNull();
        long time = System.currentTimeMillis();
        int level = (int)BatteryUtils.getBatteryLevel(intent);
        long now = System.currentTimeMillis();

        if(session.isNew()){
            session.addPoint(level, 0.0);
            lastTime = now;
        } else {
            // TODO: Check if we're discharging / taking too long / etc.
            session.addPoint(level, (double) now - lastTime);
        }
    }

    public boolean deleteIfExpired(){
        long now = System.currentTimeMillis();
        if(now - pauseTime >= MAX_PAUSE_BETWEEN_REPLUG){
            Logger.d(TAG, "Session has been paused for too long, stopping it");
            saveSession();
            session = null; // This might've been true anyways
            paused = false;
            return true;
        }
        return false;
    }

    public void handleStartCharging(){
        deleteIfExpired();
        if(session != null){
            // Previous session exists and didn't expire, resume it
            paused = false;
        } else {
            createIfNull();
        }
    }

    public void handleStopCharging(){
        if(!paused){
            paused = true;
            pauseTime = System.currentTimeMillis();
        }
    }
}
