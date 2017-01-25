package edu.berkeley.cs.amplab.carat.android.sampling;

import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.util.Date;

public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = BroadcastReceiver.class.getSimpleName();

    /**
     * @param context the context
     * @param intent the intent (should be ACTION_BOOT_COMPLETED)
     */
    @SuppressLint("CommitPrefEdits")
    @Override
    public void onReceive(Context context, Intent intent) {
        Logger.d(TAG, "Received boot event!");

        // Save boot time in shared preferences
        SharedPreferences p = context.getSharedPreferences("SystemBootTime", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = p.edit();
        editor.putLong("bootTime", new Date().getTime());
        editor.commit();

        // Register sampler
        SamplingStarter.from(context).run();
    }
}
