package edu.berkeley.cs.amplab.carat.android.sampling;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import java.util.concurrent.TimeUnit;

import edu.berkeley.cs.amplab.carat.android.CaratApplication;
import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;
import edu.berkeley.cs.amplab.carat.android.utils.Util;

/**
 * Created by Jonatan Hamberg on 1.2.2017.
 */
public class IntentRouter extends IntentService {
    private final static String TAG = IntentRouter.class.getSimpleName();
    private final static long SAMPLING_INTERVAL = TimeUnit.MINUTES.toMillis(15);
    private final static int requestCode = 67294580;
    private final static int NO_FLAG = 0;

    private Context context;
    private AlarmManager alarmManager;

    public IntentRouter(){
        super(TAG);
        context = getApplicationContext();
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(intent != null){
            String action = intent.getAction();
            if(action != null){
                switch(action){
                    case Constants.SCHEDULED_SAMPLE: scheduledSample(); break;
                    // TODO: Implement rest of the actions: normal sample, rapid charging...
                    default: Logger.d(TAG, "Implement me: " + action + "!");
                }
            }
        }
    }

    private void scheduledSample(){
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        String uuId = p.getString(CaratApplication.getRegisteredUuid(), null);
        Sampler2.sample(uuId, Constants.SCHEDULED_SAMPLE, "FIXME", SamplingLibrary.from(context));

        scheduleNext();
    }

    private void scheduleNext(){
        // TODO: Check if already have something scheduled and exit if we do.
        Intent scheduleIntent = new Intent(context, IntentRouter.class);
        scheduleIntent.setAction(Constants.SCHEDULED_SAMPLE);
        PendingIntent pendingIntent =
                PendingIntent.getBroadcast(context, requestCode, scheduleIntent, NO_FLAG);
        long t =  Util.timeAfterTime(SAMPLING_INTERVAL);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pendingIntent);
        } else if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, t, pendingIntent);
        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, t, pendingIntent);
        }
    }
}
