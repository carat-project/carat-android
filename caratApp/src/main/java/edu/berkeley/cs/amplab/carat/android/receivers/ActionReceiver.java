package edu.berkeley.cs.amplab.carat.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import edu.berkeley.cs.amplab.carat.android.Keys;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;

/**
 * Created by Jonatan Hamberg on 1.2.2017.
 */
public class ActionReceiver extends WakefulBroadcastReceiver {
    private static final String TAG = ActionReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, IntentRouter.class);
        service.putExtra(Keys.intentReceiverAction, intent.getAction());
        Logger.d(TAG, "Starting wakeful service for " + intent.getAction());
        startWakefulService(context, service);
    }
}
