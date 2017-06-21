package edu.berkeley.cs.amplab.carat.android.receivers;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.PowerManager;
import android.preference.PreferenceManager;

import java.util.concurrent.TimeUnit;

import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.Keys;
import edu.berkeley.cs.amplab.carat.android.sampling.Sampler;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;
import edu.berkeley.cs.amplab.carat.android.utils.Util;

/**
 * Created by Jonatan Hamberg on 1.2.2017.
 */
public class IntentRouter extends IntentService {
    private final static String TAG = IntentRouter.class.getSimpleName();
    private final static long SAMPLING_INTERVAL = TimeUnit.MINUTES.toMillis(2);
    private final static int REQUEST_CODE = 67294580;

    private Context context;
    private AlarmManager alarmManager;
    private PowerManager powerManager;
    private SharedPreferences preferences;

    public IntentRouter(){
        super(TAG);
    }

    public void initInstanceValues(){
        if(context == null || alarmManager == null || powerManager == null){
            context = getApplicationContext();
            alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            preferences = PreferenceManager.getDefaultSharedPreferences(context);
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        initInstanceValues();

        // This is a bit hacky, intent service should handle the wakelock by itself but
        // we are enforcing our own lock here just in case.
        PowerManager.WakeLock wl = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wl.acquire(10*60*1000L /*10 minutes*/);

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
                    // TODO: Implement me, old code in version history
                    break;
                case Constants.SCHEDULED_SAMPLE:
                    Sampler.sample(context, Constants.SCHEDULED_SAMPLE, wl::release);
                    scheduleNextSample(SAMPLING_INTERVAL);
                    break;
                case Intent.ACTION_BATTERY_CHANGED:
                case Intent.ACTION_POWER_CONNECTED:
                case Intent.ACTION_POWER_DISCONNECTED:
                    cancelScheduledSample();
                    Sampler.sample(context, action, wl::release);
                    scheduleNextSample(SAMPLING_INTERVAL);
                    break;
                default:
                    Logger.i(TAG, "Waken up by " + action + " to check schedule");
                    long now = System.currentTimeMillis();
                    long future = preferences.getLong(Keys.nextSamplingTime, 0);
                    long lastSample = preferences.getLong(Keys.lastSampleTimestamp, 0);

                    // First condition takes care of the scenario where we have woken up to check
                    // schedule but found out that the time is either really soon or already passed.
                    // If the time is really soon, we might not get an alarm for that since we woke
                    // up now, and if it's already due, might as well do it now.
                    if(isAlreadyScheduled(getScheduleIntent()) && (future - now < SAMPLING_INTERVAL/4.0 || now > future)){
                        Logger.d(TAG, "Next scheduled sampling time either soon or already passed, sampling now");
                        cancelScheduledSample();
                        Sampler.sample(context, Constants.SCHEDULED_SAMPLE, wl::release);
                        scheduleNextSample(SAMPLING_INTERVAL);
                    }

                    // Second condition means that the scheduler is dead for some reason. In this
                    // case we always want to reschedule, but if over 15 minutes have elapsed since
                    // the last sample, we've been dead for a good while and want to sample right
                    // away before rescheduling.
                    else if(!isAlreadyScheduled(getScheduleIntent())){
                        if(now - lastSample >= SAMPLING_INTERVAL){
                            Logger.d(TAG, "Sampler has been dead for a long while, sampling now");
                            Sampler.sample(context, Constants.SCHEDULED_SAMPLE, wl::release);
                        }
                        Logger.d(TAG, "Revived sampler");
                        scheduleNextSample(SAMPLING_INTERVAL);
                    }

                    // Unless the first two conditions are met, we can just happily wait for the next
                    // scheduled sampling to happen.
                    else {
                        Logger.d(TAG, "Everything was fine with scheduling");
                    }
            }
            ActionReceiver.completeWakefulIntent(intent);
        }
    }

    private boolean isAlreadyScheduled(Intent intent){
        return PendingIntent.getBroadcast(context, REQUEST_CODE, intent, PendingIntent.FLAG_NO_CREATE) != null;
    }

    private Intent getScheduleIntent(){
        Intent scheduleIntent = new Intent(context, ActionReceiver.class);
        scheduleIntent.setAction(Constants.SCHEDULED_SAMPLE);
        return scheduleIntent;
    }

    private boolean shouldSampleNow(){
        if(!isAlreadyScheduled(getScheduleIntent())){
            return true;
        }
        long now = System.currentTimeMillis();
        long last = preferences.getLong(Keys.nextSamplingTime, 0);
        return now - last < SAMPLING_INTERVAL/2.0;
    }

    private void cancelScheduledSample(){
        Intent scheduleIntent = getScheduleIntent();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, scheduleIntent, 0);
        try {
            alarmManager.cancel(pendingIntent); // Cancel previously scheduled sample
        } catch(Exception e){
            Logger.i(TAG, "No alarm to cancel when rescheduling sample");
        }
    }

    private void scheduleNextSample(long interval){
        Intent scheduleIntent = getScheduleIntent();
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, scheduleIntent, 0);
        long then = Util.timeAfterTime(interval);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, then, pendingIntent);
        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, then, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, then, pendingIntent);
        }
        Logger.d(TAG, "Next sampling scheduled in " +
                ((then-System.currentTimeMillis())/1000) +  " seconds");
        preferences.edit().putLong(Keys.nextSamplingTime, then).apply();
    }
}
