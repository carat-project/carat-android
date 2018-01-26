package edu.berkeley.cs.amplab.carat.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

import edu.berkeley.cs.amplab.carat.android.Keys;
import edu.berkeley.cs.amplab.carat.android.models.NetworkState;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;
import edu.berkeley.cs.amplab.carat.android.utils.NetworkingUtil;

/**
 * Created by Jonatan Hamberg on 25.1.2018.
 */
public abstract class NetworkChangeListener{
    private static final String TAG = NetworkChangeListener.class.getSimpleName();
    private NetworkState state;
    private Context context;
    protected NetworkChangeListener(){
        state = NetworkState.RESUME;
    }

    private BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            check();
        }
    };

    private void check(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        boolean useWifiOnly = preferences.getBoolean(Keys.useWifiOnly, false);
        boolean canConnect = NetworkingUtil.canConnect(context);

        // Check conditions for STOP
        if(useWifiOnly && !NetworkingUtil.isWifiEnabled(context)){
            if(state != NetworkState.STOP){
                updateNetworkState(NetworkState.STOP);
            }
            return; // This overrides other states
        }

        // Check conditions for PAUSE
        if(!canConnect && (state == NetworkState.STOP || state == NetworkState.RESUME)){
            updateNetworkState(NetworkState.PAUSE);
        }

        // Check conditions for RESUME
        if(canConnect && (state == NetworkState.PAUSE || state == NetworkState.STOP)){
            updateNetworkState(NetworkState.RESUME);
        }
    }

    private void updateNetworkState(NetworkState state){
        this.state = state;
        Logger.d(TAG, "Network state changed to " + state);
        onNetworkChange(state);
    }

    private OnSharedPreferenceChangeListener preferenceListener = (sharedPreferences, key) -> {
        if(key.equalsIgnoreCase(Keys.useWifiOnly)){
            check();
        }
    };

    public void register(Context context){
        this.context = context;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        context.registerReceiver(networkReceiver, filter);
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener);
        check(); // Let's check right away
    }

    public void unregister(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener);
    }

    public abstract void onNetworkChange(NetworkState state);
}
