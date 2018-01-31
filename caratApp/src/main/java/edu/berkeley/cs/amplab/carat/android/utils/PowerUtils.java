package edu.berkeley.cs.amplab.carat.android.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

import java.lang.reflect.Method;

/**
 * Created by Jonatan Hamberg on 31.1.2018.
 */
public class PowerUtils {
    private static final String TAG = PowerUtils.class.getSimpleName();

    public static boolean isPowerSaving(Context context){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                return pm.isPowerSaveMode();
            }
        }
        return false;
    }

    public static boolean isIgnoringBatteryOptimizations(Context context, String packageName) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                return pm.isIgnoringBatteryOptimizations(packageName);
            }
        }
        return false;
    }

    public static boolean isDeepDoze (Context context) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if(pm != null){
                return pm.isDeviceIdleMode();
            }
        }
        return false;
    }

    @SuppressWarnings("JavaReflectionMemberAccess")
    @SuppressLint("PrivateApi")
    public static boolean isLightDoze(Context context){
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            try {
                Method method = pm.getClass().getDeclaredMethod("isLightDeviceIdleMode");
                method.setAccessible(true);
                return (boolean)method.invoke(pm);
            } catch (Exception e) {
               Logger.d(TAG, "Failed to check light doze with reflection " + e);
            }
        }
        return false;
    }
}
