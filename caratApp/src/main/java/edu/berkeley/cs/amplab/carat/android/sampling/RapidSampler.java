package edu.berkeley.cs.amplab.carat.android.sampling;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import java.util.Timer;
import java.util.TimerTask;

import edu.berkeley.cs.amplab.carat.android.CaratActions;
import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.Keys;
import edu.berkeley.cs.amplab.carat.android.MainActivity;
import edu.berkeley.cs.amplab.carat.android.R;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;

import static android.app.PendingIntent.*;

/**
 * Created by Jonatan Hamberg on 6/26/17.
 */
public class RapidSampler extends Service {
    private static final String TAG = RapidSampler.class.getSimpleName();
    private static final int ID = 48908227;
    private static final long SHUTDOWN_GRACE_PERIOD = 5000; // 5 seconds

    private static boolean convicted = false;
    private Timer scheduler;
    private Timer deathRow;
    private SharedPreferences preferences;
    private ChargingSessionManager chargingManager;

    private BroadcastReceiver batteryChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action != null && action.equals(Intent.ACTION_BATTERY_CHANGED)){
                Sampler.sample(getApplicationContext(), action);
            }
            if(!SamplingLibrary.isDeviceCharging(context)){
                stopService();
            } else {
                chargingManager.handleBatteryIntent(intent);
            }
        }
    };

    private BroadcastReceiver chargingAnomalyReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO: Change notification at very least
            Logger.d(TAG, "Notify user, charging is anomalous!");
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Context context = getApplicationContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = getActivity(this, Intent.FLAG_ACTIVITY_SINGLE_TOP,
                notificationIntent, 0);
        int pluggedState = SamplingLibrary.getDevicePluggedState(context);
        chargingManager = ChargingSessionManager.getInstance();
        chargingManager.handleStartCharging();

        // This service is innocent, let them live
        convicted = false;
        if(deathRow != null){
            deathRow.cancel();
        }

        StringBuilder chargingString = new StringBuilder("Device is charging");
        switch(pluggedState){
            case BatteryManager.BATTERY_PLUGGED_AC:
                chargingString.append(" via power outlet");
                chargingManager.updatePlugState("ac");
                break;
            case BatteryManager.BATTERY_PLUGGED_USB:
                chargingString.append(" via USB port");
                chargingManager.updatePlugState("usb");
                break;
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                chargingString.append(" wirelessly");
                chargingManager.updatePlugState("wireless");
                break;
        }

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.carat_notif_icon)
                .setContentTitle(chargingString.toString())
                .setContentText("Tap to open Carat.")
                .setContentIntent(pendingIntent).build();

        scheduler = new Timer();
        scheduler.cancel(); // Just in case
        scheduler.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long last = preferences.getLong(Keys.lastSampleTimestamp, 0);
                long now = System.currentTimeMillis();
                if(now-last >= Constants.RAPID_SAMPLING_INTERVAL - 100 /* Margin of error */){
                    Sampler.sample(context, CaratActions.RAPID_SAMPLING);
                }
                if(!SamplingLibrary.isDeviceCharging(context)){
                    stopService();
                }
            }
        }, Constants.RAPID_SAMPLING_INTERVAL, Constants.RAPID_SAMPLING_INTERVAL);

        startForeground(ID, notification);
        this.registerReceiver(batteryChangeReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        this.registerReceiver(chargingAnomalyReceiver, new IntentFilter(CaratActions.CHARGING_ANOMALY));
        return START_STICKY;
    }

    public void stopService(){
        if(chargingManager != null){
            chargingManager.handlePauseCharging();
        }

        convicted = true; // This allows onCreate to be called again
        deathRow = new Timer();
        deathRow.schedule(new TimerTask() {
            @Override
            public void run() {
                if(convicted){
                    if(scheduler != null) scheduler.cancel();
                    if(chargingManager != null) chargingManager.handleStopCharging();
                    stopSelf();
                } else {
                    Logger.d(TAG, "Shutdown cancelled");
                }
            }
        }, SHUTDOWN_GRACE_PERIOD);

        Logger.d(TAG, "Scheduled for shutdown in " + SHUTDOWN_GRACE_PERIOD/1000.0 + "s");
    }

    @Override
    public void onDestroy() {
        if(chargingManager != null){
            chargingManager.handleStopCharging(); // Make sure this gets called
        }
        this.unregisterReceiver(batteryChangeReceiver);
        super.onDestroy();
    }

    public static boolean isAwaitingShutdown(){
        return convicted;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
