package edu.berkeley.cs.amplab.carat.android.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import edu.berkeley.cs.amplab.carat.android.CaratApplication;
import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.UsageManager;
import edu.berkeley.cs.amplab.carat.android.sampling.SamplingLibrary;
import edu.berkeley.cs.amplab.carat.android.storage.SimpleHogBug;
import edu.berkeley.cs.amplab.carat.thrift.PackageProcess;
import edu.berkeley.cs.amplab.carat.thrift.ProcessInfo;

/**
 * Created by Jonatan Hamberg on 6/28/17.
 */

public class ProcessUtil {
    private static final String TAG = ProcessUtil.class.getSimpleName();
    private static List<ProcessInfo> inMemoryProcesses;

    public static void invalidateInMemoryProcesses(){
        inMemoryProcesses = null;
    }

    public static ArrayList<SimpleHogBug> filterByRunning(SimpleHogBug[] packages, Context context) {
        if (packages == null || packages.length == 0){
            return new ArrayList<>();
        }
        ArrayList<SimpleHogBug> running = new ArrayList<>();
        List<SimpleHogBug> visible = filterByVisibility(packages);

        List<ProcessInfo> processes = getCachedOrFetchProcesses(context);
        for(ProcessInfo pi : processes){
            String processName = trimProcessName(pi.pName)[0];
            if(processName != null){
                for(SimpleHogBug hb : visible){
                    if(processName.equals(hb.getAppName())){
                        Long lastSeen = getLastSeenTimestamp(pi);
                        hb.setLastSeen(lastSeen);
                        running.add(hb);
                    }
                }
            }
        }

        return running;
    }

    private static Long getLastSeenTimestamp(ProcessInfo info){
        Long lastSeen = -1L;
        List<PackageProcess> processes = info.getProcesses();
        if(!Util.isNullOrEmpty(processes)){
            for(PackageProcess process : processes){
                Logger.d(TAG, process.processName);
                if(process.isSetProcessName()){
                    String processName = process.getProcessName();
                    if(processName.contains("@service")){
                        return Long.MAX_VALUE; // Now
                    }
                }
                Logger.d(TAG, process.getLastStartTimestamp()+"");
                if(process.isSetLastStartTimestamp()){
                    long lastStartTimestamp = (long) process.lastStartTimestamp;
                    lastSeen = Math.max(lastSeen, lastStartTimestamp);
                }
            }
        } else {
            Logger.d(TAG, "Processes empty for " + info.getApplicationLabel());
        }
        return lastSeen;
    }

    public static ArrayList<SimpleHogBug> filterByVisibility(SimpleHogBug[] reports){
        ArrayList<SimpleHogBug> result = new ArrayList<>();
        if(reports == null) return result;
        ArrayList<SimpleHogBug> temp = new ArrayList<>(Arrays.asList(reports));

        for (SimpleHogBug app : temp) {
            String packageName = app.getAppName();
            if(packageName == null) continue;
            if(!CaratApplication.isPackageInstalled(packageName)) continue;
            // Enable this when we can reliably detect killable apps
            //if(CaratApplication.isPackageSystemApp(packageName)) continue;
            if(packageName.equalsIgnoreCase(Constants.CARAT_PACKAGE_NAME)) continue;
            if(packageName.equalsIgnoreCase(Constants.CARAT_OLD)) continue;
            result.add(app);
        }

        return result;
    }

    public static boolean isServiceRunning(Context context, Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public static String[] trimProcessName(String processName){
        if (processName != null) {
            int idx = processName.lastIndexOf(':');
            if(idx <= 0){
                return new String[]{processName};
            }
            String packageName = processName.substring(0, idx);
            if(processName.length() == idx + 1){
                // Rare case where process name simply ends in :
                return new String[]{packageName};
            } else {
                String serviceName = processName.substring(idx+1, processName.length());
                return new String[]{packageName, serviceName};
            }
        }
        return new String[]{null};
    }

    private static List<ProcessInfo> getCachedOrFetchProcesses(Context context){
        List<ProcessInfo> processes = null;
        if(inMemoryProcesses != null){
            processes = inMemoryProcesses;
        }
        if(processes == null){
            long recent = System.currentTimeMillis() - Constants.FRESHNESS_RUNNING_PROCESS;
            processes = SamplingLibrary.getRunningProcesses(context, recent, false);
            if(processes != null){
                inMemoryProcesses = processes;
            }
        }
        return processes;
    }

    public static String getCurrentProcessName(Context context){
        int pid = android.os.Process.myPid();
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningAppProcessInfo processInfo : manager.getRunningAppProcesses())
        {
            if (processInfo.pid == pid)
            {
                return processInfo.processName;
            }
        }
        return "Unknown";
    }

    public static String mostRecentPriority(Context context, String packageName){
        Map<String, List<PackageProcess>> runningApps = SamplingLibrary.getRunningNow(context);
        Map<String, List<PackageProcess>> runningServices = SamplingLibrary.getRunningServices(context);

        // Find both services and applications (activities) running for this package
        int appImportance = getLowestImportance(runningApps.get(packageName));
        int serviceImportance = getLowestImportance(runningServices.get(packageName));
        int importance = Math.min(appImportance, serviceImportance); // Whichever is lower

        // If still at default value, fall back to event log to find the most recent importance
        if(importance == Integer.MAX_VALUE && Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP){
            long freshness = System.currentTimeMillis() - Constants.FRESHNESS_RUNNING_PROCESS;
            importance = UsageManager.getLastImportance(context, packageName, freshness);
        }

        // Convert to string
        String importanceString = CaratApplication.importanceString(importance);
        return CaratApplication.translatedPriority(importanceString);
    }

    private static Integer getLowestImportance(List<PackageProcess> processes){
        int result = Integer.MAX_VALUE;
        if(!Util.isNullOrEmpty(processes)){
            for(PackageProcess app : processes){
                if(app.isSetImportance()){
                    int importance = app.getImportance();
                    if(importance >= 0 && importance < result ){
                        result = importance;
                    }
                }
            }
        }
        return result;
    }
}
