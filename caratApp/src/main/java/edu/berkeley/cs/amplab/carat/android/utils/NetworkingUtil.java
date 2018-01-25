package edu.berkeley.cs.amplab.carat.android.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;

import java.util.Locale;

import edu.berkeley.cs.amplab.carat.android.Keys;
import edu.berkeley.cs.amplab.carat.android.sampling.SamplingLibrary;

/**
 * Created by Jonatan on 14.8.2017.
 */
public class NetworkingUtil {
    private static final String TAG = NetworkingUtil.class.getSimpleName();

    /**
     * Check is wifi is enabled on the device
     * @param context caller context
     * @return true if enabled, false otherwise
     */
    public static boolean isWifiEnabled(Context context){
        Context applicationContext = context.getApplicationContext(); // Prevent memory leak
        WifiManager wifiManager = (WifiManager) applicationContext.getSystemService(Context.WIFI_SERVICE);
        if(wifiManager == null){
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if(connectivityManager != null){
                NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
                if(networkInfo != null){
                    return networkInfo.getTypeName().equalsIgnoreCase("wifi");
                }
            }
            return false;
        }
        return wifiManager.isWifiEnabled();
    }

    /**
     * Check network connectivity and make sure wifi-only mode is respected.
     * @param context caller context
     * @return true if allowed
     */
    public static boolean canConnect(Context context){
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        if(prefs != null){
            String status = SamplingLibrary.getNetworkStatus(context);
            String networkType = SamplingLibrary.getNetworkType(context);

            boolean isConnected = status.equalsIgnoreCase(SamplingLibrary.NETWORKSTATUS_CONNECTED);
            boolean wifiOnly = prefs.getBoolean(Keys.useWifiOnly, false);
            boolean isWifi =  networkType.equalsIgnoreCase("WIFI");
            boolean isAllowed = !wifiOnly || isWifi;

            String format = "isConnected: %b, wifiOnly: %b, isWifi: %b, isAllowed: %b";
            Logger.d(TAG, String.format(Locale.getDefault(), format, isConnected, wifiOnly, isWifi, isAllowed));

            return isConnected && isAllowed;
        }
        return true;
    }
}
