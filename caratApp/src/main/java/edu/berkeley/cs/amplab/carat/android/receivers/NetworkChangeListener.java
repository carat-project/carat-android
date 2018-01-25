package edu.berkeley.cs.amplab.carat.android.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

import edu.berkeley.cs.amplab.carat.android.Keys;
import edu.berkeley.cs.amplab.carat.android.sampling.SamplingLibrary;
import edu.berkeley.cs.amplab.carat.android.utils.NetworkingUtil;

/**
 * Created by Jonatan Hamberg on 25.1.2018.
 */
public abstract class NetworkChangeListener{
    private Context context;
    private boolean wasConnected;
    private BroadcastReceiver networkReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean canConnect = NetworkingUtil.canConnect(context);
            if(wasConnected && !canConnect){
                onNetworkingPause();
            } else if(!wasConnected && canConnect){
                onNetworkingResume();
            }
            wasConnected = canConnect;
        }
    };

    private OnSharedPreferenceChangeListener preferenceListener = (sharedPreferences, key) -> {
        boolean canConnect = NetworkingUtil.canConnect(context);
        if(key.equalsIgnoreCase(Keys.useWifiOnly)){
            boolean useWifiOnly = sharedPreferences.getBoolean(Keys.useWifiOnly, false);
            if(useWifiOnly && !NetworkingUtil.isWifiEnabled(context)){
                onNetworkingStop(); // It's time to stop
            } else if(!wasConnected && canConnect){
                onNetworkingResume();
            } else if(wasConnected && !canConnect){
                onNetworkingPause();
            }
        }
        wasConnected = canConnect;
    };

    public void register(Context context){
        this.context = context;
        wasConnected = NetworkingUtil.canConnect(context);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
        filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        context.registerReceiver(networkReceiver, filter);
        preferences.registerOnSharedPreferenceChangeListener(preferenceListener);
    }

    public void unregister(){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.unregisterOnSharedPreferenceChangeListener(preferenceListener);
    }

    public abstract void onNetworkingResume();
    public abstract void onNetworkingStop();
    public abstract void onNetworkingPause();
}
