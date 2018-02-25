package edu.berkeley.cs.amplab.carat.android;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.Dialog;
import android.app.usage.UsageEvents;
import android.app.usage.UsageEvents.Event;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.CheckBox;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import edu.berkeley.cs.amplab.carat.android.receivers.AsyncSuccess;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;
import edu.berkeley.cs.amplab.carat.android.utils.Util;

/**
 * Created by Jonatan Hamberg on 5/30/17.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class UsageManager {
    private final static String TAG = UsageManager.class.getSimpleName();
    private static WeakReference<HashMap<String, TreeMap<Long, Integer>>> events;
    private static Intent menuIntent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);

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

    public static LinkedList<String> getRunningProcesses(Context context, long beginTime){
        List<Event> events = getEvents(context, beginTime);
        HashSet<String> eventPkgs = new HashSet<>();
        for (Event e : events) {
            eventPkgs.add(e.getPackageName());
        }
        Log.d(TAG, "These apps were running during this period:");
        for(String pkgName : eventPkgs){
            Log.d(TAG, "\t"+pkgName);

        }
        return new LinkedList<>(eventPkgs);
    }

    public static HashMap<String, TreeMap<Long, Integer>> getEventLogs(Context context, long beginTime){
        HashMap<String, TreeMap<Long, Integer>> eventLog = new HashMap<>();
        try {
            List<Event> events = getEvents(context, beginTime);
            for(Event event : events){
                String pkg = event.getPackageName();
                TreeMap<Long, Integer> log =  eventLog.containsKey(pkg) ? eventLog.get(pkg) : new TreeMap<>();
                log.put(event.getTimeStamp(), event.getEventType());
                eventLog.put(pkg, log);
            }
        } catch (Throwable th){
            Logger.e(TAG, "Unexpected exception when getting event logs: " + th);
        }
        return eventLog;
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
        boolean granted = false;
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        if(appOps != null){
            int mode = appOps.checkOpNoThrow("android:get_usage_stats", android.os.Process.myUid(), context.getPackageName());

            // This extra check is needed on Android 6.0+
            if (mode == AppOpsManager.MODE_DEFAULT && Build.VERSION.SDK_INT > Build.VERSION_CODES.M) {
                granted = (context.checkCallingOrSelfPermission(Manifest.permission.PACKAGE_USAGE_STATS) == PackageManager.PERMISSION_GRANTED);
            } else {
                granted = (mode == AppOpsManager.MODE_ALLOWED);
            }
        }
        return granted;
    }

    private static boolean isMenuAvailable(Context context) {
        PackageManager pm = context.getPackageManager();
        return pm != null && menuIntent.resolveActivity(pm) != null;
    }

    public static void promptPermission(Context context){
        promptPermission(context, null);
    }

    public static void promptPermission(Context context, AsyncSuccess callback){
        // Some devices have disabled this menu
        // TODO: Should we notify the user?
        if(!isMenuAvailable(context)){
            if(callback != null){
                callback.complete(false);
            }
            return;
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        if(preferences.getBoolean(Keys.promptUsageStats, true)){
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle(R.string.permission_request);
            builder.setView(R.layout.permission_prompt);
            builder.setMessage(R.string.allow_usagestats);
            builder.setPositiveButton("OK", (dialog, which) -> {
                Toast.makeText(context, R.string.return_with_back, Toast.LENGTH_LONG).show();

                context.startActivity(menuIntent);
                if(callback != null){
                    callback.complete(true);
                }
            });
            builder.setIcon(R.drawable.carat_material_icon);
            builder.setNegativeButton(R.string.dialog_cancel, (dialog, which) -> {
                Toast.makeText(context, R.string.enable_later, Toast.LENGTH_SHORT).show();
                Window window = ((AlertDialog) dialog).getWindow();
                if(window != null){
                    CheckBox remindCheckbox = (CheckBox)window.findViewById(R.id.remember_checkbox);
                    preferences.edit().putBoolean(Keys.promptUsageStats, !remindCheckbox.isChecked()).apply();
                }
                if(callback != null){
                    callback.complete(false);
                }
            });
            AlertDialog dialog = builder.create();
            dialog.show();
            dialog.getButton(Dialog.BUTTON_NEGATIVE).setTextColor(Color.rgb(248, 176, 58));
            dialog.getButton(Dialog.BUTTON_POSITIVE).setTextColor(Color.rgb(248, 176, 58));
            Window window = dialog.getWindow();
            if(window != null){
                View custom = window.findViewById(R.id.custom);
                if(custom != null){
                    View parent = (View)custom.getParent();
                    parent.setMinimumHeight(0);
                }
            }
        }
    }

    @SuppressLint("WrongConstant") // Usage
    public static UsageStatsManager getUsageStatsManager(Context context){
        return (UsageStatsManager) context.getSystemService("usagestats");
    }

    public static String getLastImportance(Context context, UsageStats stats, long beginTime){
        String ERR_VAL = "Unknown";
        String importance = getLastEvent(context, stats.getPackageName(), beginTime);
        if(importance.equals(ERR_VAL)){
            Logger.i(TAG, "Missing log entry, falling back to usage stats..");
            importance = getLastEvent(stats);
        }
        return importance;
    }

    private static String getLastEvent(UsageStats stats){
        Field mLastEvent;
        try {
            mLastEvent = UsageStats.class.getDeclaredField("mLastEvent");
            Integer launchCount = (Integer)mLastEvent.get(stats);
            return priorityFromEvent(launchCount);
        } catch (Exception e) {
            Log.e(TAG, "Failed to get last event via reflection", e);
            return "Unknown";
        }
    }

    private static HashMap<String, TreeMap<Long, Integer>> getOrFetchEvents(Context context, long beginTime){
        return Util.getWeakOrFallback(events, () -> {
            HashMap<String, TreeMap<Long, Integer>> logs = getEventLogs(context, beginTime);
            if(!Util.isNullOrEmpty(logs)){
                events = new WeakReference<>(logs);
            }
            return logs;
        });
    }

    public static int getAppLaunchCount(Context context, UsageStats stats, long beginTime){
        // int count = launchCountFromStats(stats); // TODO: Can we trust this?
        int count = -1;
        if(count == -1){
            Logger.i(TAG, "Missing mLaunchCount field, falling back to event log..");
            count = launchCountFromEvents(context, stats, beginTime);
        }
        return count;
    }

    // Uses reflection, experimental
    private static int launchCountFromStats(UsageStats stats){
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

    private static int launchCountFromEvents(Context context, UsageStats stats, long beginTime){
        HashMap<String, TreeMap<Long, Integer>> e = getOrFetchEvents(context, beginTime);
        String pkgName = stats.getPackageName();
        int count = 0;
        if(e.containsKey(pkgName)){
            TreeMap<Long, Integer> pkgEvents = e.get(pkgName);
            for(int eventCode : pkgEvents.values()){
                if(eventCode == Event.MOVE_TO_FOREGROUND){
                    count++;
                }
            }
        }
        return count;
    }

    private static String getLastEvent(Context context, String packageName, long beginTime){
        HashMap<String, TreeMap<Long, Integer>> e = getOrFetchEvents(context, beginTime);
        if(e.containsKey(packageName)){
            TreeMap<Long, Integer> pkgEvents = e.get(packageName);
            Map.Entry<Long, Integer> lastEvent = pkgEvents.lastEntry();
            if(lastEvent != null){
                Integer eventCode = lastEvent.getValue();
                if(eventCode != null){
                    Logger.i(TAG, "Found the importance from event log!");
                    return priorityFromEvent(eventCode);
                }
            }
        }
        return "Unknown";
    }

    public static int getLastImportance(Context context, String packageName, long since){
        long lastTimestamp = -1;
        int mostRecentImportance = -1;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Map<String, TreeMap<Long, Integer>> log = UsageManager.getEventLogs(context, since);
            TreeMap<Long, Integer> events = log.get(packageName);
            if(!Util.isNullOrEmpty(events)){
                for(Long timestamp : events.keySet()){
                    int event = events.get(timestamp);
                    if(timestamp > lastTimestamp){ // Check just in case
                        switch(event){
                            case UsageEvents.Event.MOVE_TO_BACKGROUND:
                                mostRecentImportance = ActivityManager.RunningAppProcessInfo.IMPORTANCE_BACKGROUND;
                                break;
                            case UsageEvents.Event.USER_INTERACTION:
                            case UsageEvents.Event.SHORTCUT_INVOCATION:
                            case UsageEvents.Event.MOVE_TO_FOREGROUND:
                                mostRecentImportance = ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND;
                                break;
                        }
                    }
                    lastTimestamp = timestamp;
                }
            }
        }
        return mostRecentImportance;
    }

    public static void disposeInMemoryEvents(){
        if(events != null && events.get() != null){
            events.clear(); // No idea if this is a stub method
            events = null;
            System.gc();
        }
    }

    private static String priorityFromEvent(Integer eventCode){
        // Only these fields can be directly converted
        if(eventCode == 1) return "Foreground app";
        if(eventCode == 2) return "Background process";
        return "Unknown";
    }

    public static String getEventName(int eventCode){
        switch(eventCode){
            case 0: return "NONE";
            case 1: return "MOVE_TO_FOREGROUND";
            case 2: return "MOVE_TO_BACKGROUND";
            case 3: return "END_OF_DAY";
            case 4: return "CONTINUE_PREVIOUS_DAY";
            case 5: return "CONFIGURATION CHANGE";
            case 6: return "SYSTEM_INTERACTION";
            case 7: return "USER_INTERACTION";
            case 8: return "SHORTCUT_INVOCATION";
            default: return "UNKNOWN";
        }
    }
}
