package edu.berkeley.cs.amplab.carat.android.sampling;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

import java.util.concurrent.TimeUnit;

import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.Keys;
import edu.berkeley.cs.amplab.carat.android.receivers.ActionReceiver;
import edu.berkeley.cs.amplab.carat.android.receivers.LocationReceiver;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;
import edu.berkeley.cs.amplab.carat.android.utils.Util;

/**
 * Created by Jonatan Hamberg on 1.2.2017.
 */
public class IntentRouter extends IntentService {
    private final static String TAG = IntentRouter.class.getSimpleName();
    private final static long SAMPLING_INTERVAL = TimeUnit.MINUTES.toMillis(15);
    private final static int REQUEST_CODE = 67294580;

    private Context context;
    private AlarmManager alarmManager;
    private PowerManager powerManager;

    public IntentRouter(){
        super(TAG);
    }

    public void initInstanceValues(){
        if(context == null || alarmManager == null || powerManager == null){
            context = getApplicationContext();
            alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        initInstanceValues();

        // This is a bit hacky, intent service should handle the wakelock by itself but
        // we are enforcing our own lock here just in case.
        PowerManager.WakeLock wl = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wl.acquire();

        // Start up a location receiver in case it has died, it should stay up long enough
        // to get at least one update, which is enough for the coarse location sampling
        // we do for distance traveled.
        if(!Util.isServiceRunning(context, LocationReceiver.class)){
            startService(new Intent(this, LocationReceiver.class));
        }

        String action = intent.getStringExtra(Keys.intentReceiverAction);
        if(action != null){
            Logger.d(TAG, "Routing intent for " + action);
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
            Sampler2.sample(context, action, wl::release);
            ActionReceiver.completeWakefulIntent(intent);
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
}
