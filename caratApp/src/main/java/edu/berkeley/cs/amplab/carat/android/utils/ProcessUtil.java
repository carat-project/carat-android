package edu.berkeley.cs.amplab.carat.android.utils;

import android.app.ActivityManager;
import android.content.Context;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.berkeley.cs.amplab.carat.android.CaratApplication;
import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.sampling.SamplingLibrary;
import edu.berkeley.cs.amplab.carat.android.storage.SimpleHogBug;
import edu.berkeley.cs.amplab.carat.thrift.ProcessInfo;

/**
 * Created by Jonatan Hamberg on 6/28/17.
 */

public class ProcessUtil {
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
                        running.add(hb);
                    }
                }
            }
        }

        return running;
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
}
