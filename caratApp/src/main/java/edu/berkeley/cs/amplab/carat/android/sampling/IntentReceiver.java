package edu.berkeley.cs.amplab.carat.android.sampling;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.WakefulBroadcastReceiver;

import edu.berkeley.cs.amplab.carat.android.utils.Logger;

/**
 * Created by Jonatan Hamberg on 1.2.2017.
 */
public class IntentReceiver extends WakefulBroadcastReceiver {
    private static final String TAG = IntentReceiver.class.getSimpleName();

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent service = new Intent(context, IntentRouter.class);
        service.putExtras(intent);
        Logger.d(TAG, "Starting wakeful service for " + service.getAction());
        startWakefulService(context, service);
    }
}
