package edu.berkeley.cs.amplab.carat.android.utils;

import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.os.PowerManager;
import android.net.Uri;
import android.support.v7.app.AlertDialog;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;

import edu.berkeley.cs.amplab.carat.android.R;
import edu.berkeley.cs.amplab.carat.thrift.PackageProcess;

/**
 * General utilities for handling different data types.
 * Created by Jonatan Hamberg on 1.2.2017.
 */
public class Util {
    private final static String TAG = Util.class.getSimpleName();
    public interface Fallback<T> {
        T call();
    }

    public static double[] toArray(List<Double> list){
        double[] result = new double[list.size()];
        for(int i=0; i<result.length; i++){
            result[i] = list.get(i);
        }
        return result;
    }

    public static <K,V> Map<K, V> firstEntries(int limit, Map<K,V> source){
        TreeMap<K, V> result = new TreeMap<>();
        for(Map.Entry<K,V> entry : source.entrySet()){
            if(result.size() >= limit){
                break;
            }
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }

    public static int[][] readLines(RandomAccessFile reader, int maxRows, int maxColumns, String delim) throws IOException {
        int[][] result = new int[maxRows + 1][maxColumns + 1];
        for (int row = 0; row < maxRows; row++) {
            String line = reader.readLine();
            if (line == null) break; // EOF
            String[] tokens = line.split(delim);
            for (int column = 0; column < maxColumns; column++) {
                if (maxColumns < tokens.length && isInteger(tokens[column])) {
                    result[row][column] = Integer.parseInt(tokens[column]);
                } else result[row][column] = 0; // Default to zero
            }
        }
        reader.seek(0); // Rewind
        return result;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public static boolean isInteger(String value){
        try {
            Integer.parseInt(value);
        } catch(Throwable th){
            return false;
        }
        return true;
    }

    public static <T> T getWeakOrFallback(WeakReference<T> weakReference, Fallback<T> fallback){
        if(weakReference != null && weakReference.get() != null){
            T value = weakReference.get();
            if(value != null){ // Might become null between instructions
                return value;
            }
        }
        return fallback.call();
    }

    public static long timeAfterTime(long milliseconds){
        return System.currentTimeMillis() + milliseconds;
    }

    public static boolean isNullOrEmpty(File[] array){
        return array == null || array.length == 0;
    }

    public static boolean isNullOrEmpty(List<?> list){
        return list == null || list.isEmpty();
    }

    public static boolean isNullOrEmpty(String string){
        return string == null || string.trim().isEmpty();
    }

    public static <K, V> boolean isNullOrEmpty(Map<K, V> map){
        return map == null || map.isEmpty();
    }

    public static <K> long sumMapValues(Map<K, Integer> map){
        long result = 0;
        if(isNullOrEmpty(map)){
            return result;
        }
        for(int value : map.values()){
            result += value;
        }
        return result;
    }

    public static boolean isSystemApp(int flags){
        return (flags & ApplicationInfo.FLAG_SYSTEM) > 0
                || (flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) > 0;
    }

    public static synchronized void safeReleaseWakelock(PowerManager.WakeLock wl){
        try{
            if(wl.isHeld()){
                wl.release();
            }
        } catch(Throwable th){
            Logger.d(TAG, "Releasing wakelock failed: " + th);
        }
    }

    public static PackageProcess getDefaultPackageProcess(){
        int ERR_VAL = -1;
        return new PackageProcess()
                .setProcessName("Unknown")
                .setProcessCount(ERR_VAL)
                .setUId(ERR_VAL)
                .setSleeping(false)
                .setForeground(false)
                .setForegroundTime(ERR_VAL)
                .setLaunchCount(ERR_VAL)
                .setImportance(ERR_VAL)
                .setCrashCount(ERR_VAL)
                .setLastStartSinceBoot(ERR_VAL)
                .setLastStartTimestamp(ERR_VAL);
    }

    public static void openStorePage(Context context, String packageName){
        try {
            Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
            context.startActivity(marketIntent);
        } catch (android.content.ActivityNotFoundException anfe) {
            Intent playIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
            context.startActivity(playIntent);
        }
    }

    public static void showConfirmationDialog(Context context, String text, Runnable ok, Runnable cancel){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setMessage(text);
        builder.setCancelable(false);
        builder.setPositiveButton(context.getString(R.string.dialog_ok), (dialog, id) -> ok.run());
        builder.setNegativeButton(context.getString(R.string.dialog_cancel2), (dialog, id) -> cancel.run());
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            // TODO: Rather than doing this programmatically, change the theme.
            dialog.getButton(Dialog.BUTTON_NEGATIVE).setTextColor(Color.rgb(248, 176, 58));
            dialog.getButton(Dialog.BUTTON_POSITIVE).setTextColor(Color.rgb(248, 176, 58));
        });
        dialog.show();
    }

    public static Integer getDigits(String string){
        Integer result = null;
        if(!Util.isNullOrEmpty(string)){
            try {
                String digits = string.replaceAll("\\D+", "");
                result = Integer.parseInt(digits);
            } catch (Exception e){
                Logger.d(TAG, "Failed getting digits out of string \"" + string + "\"");
            }
        }
        return result;
    }

    public static void printStackTrace(String tag, Throwable th){
        Logger.d(tag, getStackTrace(th));
    }

    public static String getStackTrace(Throwable th){
        StringBuilder buf = new StringBuilder();
        StackTraceElement[] trace = th.getStackTrace();
        boolean first = true;
        for (StackTraceElement elem: trace){
            if (!first) {
                buf.append("\t");

            }else
                first = false;
             buf.append("in " + elem.getMethodName() + " in " + elem.getClassName() + "("+elem.getLineNumber()+")\n");
        }
        return buf.toString();
    }
}
