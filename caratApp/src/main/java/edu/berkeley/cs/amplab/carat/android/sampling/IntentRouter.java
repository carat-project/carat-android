package edu.berkeley.cs.amplab.carat.android.sampling;

import android.app.AlarmManager;
import android.app.IntentService;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import java.util.concurrent.TimeUnit;

import edu.berkeley.cs.amplab.carat.android.Constants;
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
        alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(intent != null){
            String action = intent.getAction();
            if(action != null){
                switch(action){
                    case Constants.SCHEDULED_SAMPLE: scheduledSample();
                }
            }
        }
    }


    private void scheduledSample(){
        //Sampler2.sample();

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
