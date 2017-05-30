package edu.berkeley.cs.amplab.carat.android;

import android.Manifest;
import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageEvents.Event;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Created by Jonatan Hamberg on 5/30/17.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class UsageManager {
    private final static String TAG = UsageManager.class.getSimpleName();

    public static List<Event> getEvents(Context context, long beginTime){
        UsageStatsManager usm = getUsageStatsManager(context);
        long now = System.currentTimeMillis();
        UsageEvents uEvents = usm.queryEvents(beginTime, now);
        LinkedList<Event> events = new LinkedList<>();
        while(uEvents.hasNextEvent()){
            Event event = new Event();
            if(uEvents.getNextEvent(event)){
                events.add(event);
            }
        }
        return events;
    }

    public static Map<String, UsageStats> getUsageAggregate(Context context, long beginTime){
        UsageStatsManager usm = getUsageStatsManager(context);
        long now = System.currentTimeMillis();
        return usm.queryAndAggregateUsageStats(beginTime, now);
    }

    public static List<UsageStats> getUsage(Context context, long beginTime){
        UsageStatsManager usm = getUsageStatsManager(context);
        long now = System.currentTimeMillis();
        return usm.queryUsageStats(UsageStatsManager.INTERVAL_BEST, beginTime, now);
    }

    public static boolean isPermissionGranted(Context context){
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), context.getPackageName());
        boolean granted;

        // This extra check is needed on Android 6.0+
        if (mode == AppOpsManager.MODE_DEFAULT && Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
            granted = (context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED);
        } else {
            granted = (mode == AppOpsManager.MODE_ALLOWED);
        }
        return granted;
    }

    public static void promptPermission(Context context){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Permission request");
        builder.setMessage("Carat needs Usage Stats permission to view the list of running applications.");
        builder.setPositiveButton("OK", (dialog, which) -> {
            Toast.makeText(context, "Enable the permission and return with the back button", Toast.LENGTH_LONG);
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            context.startActivity(intent);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            Toast.makeText(context, "You can enable this option in the settings.", Toast.LENGTH_SHORT).show();
        });
        builder.show();
    }

    public static UsageStatsManager getUsageStatsManager(Context context){
        //noinspection ResourceType, for some reason Android studio does not recognize this
        return (UsageStatsManager) context.getSystemService("usagestats");
    }

    // Uses reflection, experimental
    public static int getAppLaunchCount(UsageStats stats){
        int ERR_VAL = -1;
        Field mLaunchCount;
        try {
            mLaunchCount = UsageStats.class.getDeclaredField("mLaunchCount");
            int launchCount = (Integer)mLaunchCount.get(stats);
            return launchCount > 0 ? launchCount : ERR_VAL;
        } catch (Exception e) {
            Log.e(TAG, "Failed to get launch count via reflection", e);
            return ERR_VAL;
        }
    }
}
