package edu.berkeley.cs.amplab.carat.android.sampling;

import android.content.Intent;

import java.lang.ref.WeakReference;
import java.util.SortedMap;

import edu.berkeley.cs.amplab.carat.android.models.ChargingSession;
import edu.berkeley.cs.amplab.carat.android.storage.CaratDataStorage;
import edu.berkeley.cs.amplab.carat.android.utils.BatteryUtils;
import edu.berkeley.cs.amplab.carat.android.utils.Util;

/**
 * Created by Jonatan on 13.11.2017.
 */

public class ChargingSessionManager {
    private static final String TAG = ChargingSessionManager.class.getSimpleName();
    private static ChargingSessionManager instance;
    private static final long LONGEST_ALLOWED_BREAK = 300000; // 5 minutes
    private WeakReference<SortedMap<Long, ChargingSession>> sessions;

    private CaratDataStorage storage;
    private ChargingSession session;
    private long lastEventTime;

    // Create a singleton instance
    private ChargingSessionManager getInstance(CaratDataStorage storage){
        synchronized (this){
            if(instance == null){
                instance = new ChargingSessionManager(storage);
            }
            return instance;
        }
    }

    // Depend on storage rather than context which is leaky
    private ChargingSessionManager(CaratDataStorage storage){
        this.storage = storage;
    }

    private boolean shouldContinueSession(int level){
        long now = System.currentTimeMillis();
        return now - lastEventTime >= LONGEST_ALLOWED_BREAK;
    }


    private boolean validateSession(){
        // TODO: Implement me
        return true;
    }

    private ChargingSession newSession(){
        return ChargingSession.create();
    }

    private boolean saveSession(){
        // TODO: Validate session before saving, or it might be too short!
        SortedMap<Long, ChargingSession> result = Util.getWeakOrFallback(sessions, () -> storage.getChargingSessions());
        if(result != null){
            result.put(session.getTimestamp(), session);
            storage.writeChargingSessions(result);
            sessions = new WeakReference<>(result);
            return true;
        }
        return false;
    }

    public void handleBatteryIntent(Intent intent){
        long time = System.currentTimeMillis();
        int level = (int) BatteryUtils.getBatteryLevel(intent);
        if(time - lastEventTime > LONGEST_ALLOWED_BREAK){

        }

        if(level < session.getLastLevel()){
            session = ChargingSession.create();
        }
    }
}
