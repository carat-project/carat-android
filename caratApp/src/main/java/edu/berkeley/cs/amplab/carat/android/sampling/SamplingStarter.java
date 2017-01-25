package edu.berkeley.cs.amplab.carat.android.sampling;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;

/**
 * Created by Jonatan Hamberg on 1/25/17.
 */
public class SamplingStarter extends Thread {
    private static final String TAG = SamplingStarter.class.getSimpleName();
    private Context context;
    private String[] actions = {Intent.ACTION_BATTERY_CHANGED, Intent.ACTION_POWER_CONNECTED,
            Intent.ACTION_POWER_DISCONNECTED, Intent.ACTION_TIMEZONE_CHANGED,
            Constants.ACTION_SCHEDULED_SAMPLE};

    private SamplingStarter(){
        // Not implemented
    }

    public static SamplingStarter from(Context context){
        SamplingStarter starter = new SamplingStarter();
        starter.context = context;
        return starter;
    }

    @Override
    public void run() {
        Logger.i(TAG, "Registering sampler with following actions: ");

        Sampler sampler = Sampler.getInstance();
        IntentFilter filter = new IntentFilter();

        for(String action : actions){
            Logger.i(TAG, action);
            filter.addAction(action);
        }

        // Unregister, since Carat may have been started multiple times since reboot
        try {
            context.unregisterReceiver(sampler);
        } catch (IllegalArgumentException e) {
            // Ignore
        }
        context.registerReceiver(sampler, filter);
    }
}
