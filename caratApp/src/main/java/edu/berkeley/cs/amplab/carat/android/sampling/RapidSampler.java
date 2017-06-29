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
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import java.util.Timer;
import java.util.TimerTask;

import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.Keys;
import edu.berkeley.cs.amplab.carat.android.MainActivity;
import edu.berkeley.cs.amplab.carat.android.R;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;

import static android.app.PendingIntent.*;

/**
 * Created by Jonatan Hamberg on 6/26/17.
 */
public class RapidSampler extends Service implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final int ID = 48908227;
    private static final String TAG = RapidSampler.class.getSimpleName();
    private SharedPreferences preferences;

    private BroadcastReceiver batteryChangeReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if(action != null && action.equals(Intent.ACTION_BATTERY_CHANGED)){
                Sampler.sample(getApplicationContext(), action);
            }
            if(!SamplingLibrary.isDeviceCharging(context)){
                stopSelf();
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Context context = getApplicationContext();
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = getActivity(this, 0,
                notificationIntent, 0);
        int pluggedState = SamplingLibrary.getDevicePluggedState(context);
        StringBuilder chargingString = new StringBuilder("Device is charging");
        switch(pluggedState){
            case BatteryManager.BATTERY_PLUGGED_AC:
                chargingString.append(" via power outlet");
                break;
            case BatteryManager.BATTERY_PLUGGED_USB:
                chargingString.append(" via USB port");
                break;
            case BatteryManager.BATTERY_PLUGGED_WIRELESS:
                chargingString.append(" wirelessly");
                break;
        }

        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.carat_notif_icon)
                .setContentTitle(chargingString.toString())
                .setContentText("Tap to open Carat.")
                .setContentIntent(pendingIntent).build();

        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                long last = preferences.getLong(Keys.lastSampleTimestamp, 0);
                long now = System.currentTimeMillis();
                if(now-last >= Constants.RAPID_SAMPLING_INTERVAL - 100 /* Margin of error */){
                    Sampler.sample(context, Constants.RAPID_SAMPLING);
                }
                if(!SamplingLibrary.isDeviceCharging(context)){
                    stopSelf();
                }
            }
        }, Constants.RAPID_SAMPLING_INTERVAL, Constants.RAPID_SAMPLING_INTERVAL);

        startForeground(ID, notification);
        this.registerReceiver(batteryChangeReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        preferences.registerOnSharedPreferenceChangeListener(this);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        preferences.unregisterOnSharedPreferenceChangeListener(this);
        this.unregisterReceiver(batteryChangeReceiver);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(Keys.rapidSamplingDisabled)){
            if(sharedPreferences.getBoolean(Keys.rapidSamplingDisabled, false)){
                stopSelf();
            }
        }
    }
}
