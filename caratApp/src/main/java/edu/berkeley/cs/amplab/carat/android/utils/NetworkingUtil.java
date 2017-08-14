package edu.berkeley.cs.amplab.carat.android.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import edu.berkeley.cs.amplab.carat.android.Keys;
import edu.berkeley.cs.amplab.carat.android.sampling.SamplingLibrary;

/**
 * Created by Jonatan on 14.8.2017.
 */
public class NetworkingUtil {
    /**
     * Check network connectivity and make sure wifi-only mode is respected.
     * @param context caller context
     * @return true if allowed
     */
    public static boolean isOnline(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if(prefs != null){
            String status = SamplingLibrary.getNetworkStatus(context);
            String networkType = SamplingLibrary.getNetworkType(context);

            boolean isConnected = status.equalsIgnoreCase(SamplingLibrary.NETWORKSTATUS_CONNECTED);
            boolean wifiOnly = prefs.getBoolean(Keys.useWifiOnly, false);
            boolean isWifi =  networkType.equalsIgnoreCase("WIFI");
            boolean isAllowed = !wifiOnly || isWifi;

            return isConnected && isAllowed;
        }
        return true;
    }
}
