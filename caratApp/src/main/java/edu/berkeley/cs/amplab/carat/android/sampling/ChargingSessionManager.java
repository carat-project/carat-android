package edu.berkeley.cs.amplab.carat.android.sampling;

import android.content.Intent;

import java.lang.ref.WeakReference;
import java.util.SortedMap;

import edu.berkeley.cs.amplab.carat.android.CaratActions;
import edu.berkeley.cs.amplab.carat.android.CaratApplication;
import edu.berkeley.cs.amplab.carat.android.models.ChargingSession;
import edu.berkeley.cs.amplab.carat.android.storage.CaratDataStorage;
import edu.berkeley.cs.amplab.carat.android.utils.BatteryUtils;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;
import edu.berkeley.cs.amplab.carat.android.utils.Util;

/**
 * Created by Jonatan Hamberg on 13.11.2017.
 */
public class ChargingSessionManager {
    private static ChargingSessionManager instance;
    private static final String TAG = ChargingSessionManager.class.getSimpleName();
    private static final long MAX_PAUSE_BETWEEN_REPLUG = 10000; // 10 seconds
    private static final long MAX_PAUSE_BETWEEN_CHANGE = 600000; // 10 minutes
    private static final long MINIMUM_SESSION_DURATION = 300000; // 5 minutes
    private static final long MINIMUM_SESSION_POINTS = 5;

    private WeakReference<SortedMap<Long, ChargingSession>> sessions;
    private CaratDataStorage storage;
    private ChargingSession session;
    private boolean paused = false;
    private long pauseTime = -1L;
    private long lastTime = -1L;
    private int lastLevel = -1;

    // Create a singleton instance
    public static synchronized ChargingSessionManager getInstance(){
        if(instance == null){
            instance = new ChargingSessionManager();
        }
        return instance;
    }

    // Depend on storage rather than context which is leaky
    private ChargingSessionManager(){
        // Synchronized just in case, probably not needed
        synchronized (this){
            this.storage = CaratApplication.getStorage(); // This seems dangerous
        }
    }

    public synchronized SortedMap<Long, ChargingSession> getChargingSessions(){
        return storage.getChargingSessions();
    }

    public synchronized void updatePlugState(String state){
        deleteIfExpired();
        if(session != null){
            if(session.getPlugState().equals("unknown")){
                // Keep first valid charger type
                Logger.d(TAG, "Session " + session.getTimestamp() + " plugState to " + state);
                session.setPlugState(state);
            } else if(!session.getPlugState().equals(state)){
                // Charger type changed, invalidate session
                Logger.d(TAG, "Charger type changed, stopping session");
                stopSaveSession();
            }
        }
    }

    private synchronized boolean validateSession() {
        boolean valid = session != null
                && session.getPointCount() >= MINIMUM_SESSION_POINTS
                && session.getDurationInSeconds() >= MINIMUM_SESSION_DURATION;
        Logger.d(TAG, "Session is valid: " + valid);
        return valid;
    }

    private synchronized  boolean saveSession(){
        if(validateSession()){
            SortedMap<Long, ChargingSession> result = Util.getWeakOrFallback(sessions, () -> storage.getChargingSessions());
            if(result != null){
                result.put(session.getTimestamp(), session);
                storage.writeChargingSessions(result);
                sessions = new WeakReference<>(result);

                Logger.d(TAG, "Saved session to storage: " + session);
                return true;
            }
        }
        return false;
    }

    public synchronized void handleStartCharging(){
        deleteIfExpired();
        if(session != null){
            // Previous session exists and didn't expire, resume it
            paused = false;
        } else {
            createIfNull();
        }
    }

    public synchronized void handleStopCharging(){
        if(!paused){
            paused = true;
            pauseTime = System.currentTimeMillis();
        }
    }

    public synchronized void handleBatteryIntent(Intent intent){
        deleteIfExpired();
        createIfNull();
        int level = (int)BatteryUtils.getBatteryLevel(intent);
        long now = System.currentTimeMillis();

        if(!isValidChange(now, level)){
            stopSaveSession();
            return; // Exit early
        }

        if(level != lastLevel){
            double elapsed = session.isNew() ? 0.0 : (now - lastTime);
            session.addPoint(level, elapsed);
            Logger.d(TAG, "New level " + level + " after " + elapsed + "s");

            lastTime = now;
            paused = false;
            lastLevel = level;
            checkAnomalies();
        } else {
            Logger.d(TAG, "Same level as last one, skipping");
        }
    }

    private void checkAnomalies(){
        if(session != null && session.hasPeaks()){
            // Inform application (mostly RapidSampler) about an ongoing anomaly
            // TODO: Rework this? Static context and child processes seem bad together
            Intent intent = new Intent(CaratActions.CHARGING_ANOMALY);
            CaratApplication.getAppContext().sendBroadcast(intent);
        }
    }

    private synchronized  void createIfNull(){
        if(session == null){
            session = ChargingSession.create();
            Logger.d(TAG, "New charging session " + session.getTimestamp());
        }
    }

    private synchronized boolean isValidChange(long now, int level){
        if(now - lastTime >= MAX_PAUSE_BETWEEN_CHANGE){
            Logger.d(TAG, "Too much time passed since last level change");
            return false;
        }
        if(level <= session.getLastLevel()){ // Should be a safe call, session can't be new
            Logger.d(TAG, "New battery level was less than last one, discharging");
            return false;
        }
        return true;
    }

    private synchronized void deleteIfExpired(){
        if(paused){
            long now = System.currentTimeMillis();
            if(now - pauseTime >= MAX_PAUSE_BETWEEN_REPLUG){
                Logger.d(TAG, "Session has been paused for too long, stopping it");
                stopSaveSession();
            }
        }
    }

    private synchronized void stopSaveSession(){
        saveSession();
        session = null;
        paused = false;

        // Reset these just in case
        lastLevel = -1;
        lastTime = -1L;
        pauseTime = -1L;

        Logger.d(TAG, "Stopped session");
    }
}
