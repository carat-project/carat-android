package edu.berkeley.cs.amplab.carat.android.utils;

import android.os.Build;
import android.os.StrictMode;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * Created by Jonatan Hamberg on 24.1.2018.
 */
public class FsUtils {
    private static final String TAG = FsUtils.class.getSimpleName();
    private static long INVALID_VALUE = -1;
    private static final String systemCpu = "/sys/devices/system/cpu/";
    private static final String scalingCurFreq = "/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq";
    private static final String cpuUtilization = "/sys/devices/system/cpu/cpu0/cpufreq/cpu_utilization";
    private static final String thermalZoneTemp = "/sys/devices/virtual/thermal/thermal_zone0/temp";

    private static class CpuFilter implements FileFilter {
        @Override
        public boolean accept(File pathname) {
            return Pattern.matches("cpu[0-9]+", pathname.getName());
        }
    }

    public static class CPU {
        public long getCount(){
            File directory = new File("/sys/devices/system/cpu/");
            if(directory.canRead()){
                File[] files = directory.listFiles(new CpuFilter());
                return files.length;
            }
            return INVALID_VALUE;
        }
    }


    private static long readLong(String path){
        String content = read(path);
        if(!Util.isNullOrEmpty(content)){
            try {
                return Long.parseLong(content);
            } catch (NumberFormatException e){
                Logger.d(TAG,  "Unable to convert to long " + e);
            }
        }
        return INVALID_VALUE;
    }

    private static String read(String path){
        byte[] buffer = new byte[4096];
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(path);
            int length = inputStream.read(buffer);
            inputStream.close();

            if(length > 0){
                length = find(buffer, length, '\n');
                return new String(buffer, 0, length);
            }
        } catch (IOException e) {
            Logger.d(TAG, "Unable to file " + path + ", " + e);
        } finally {
            if(inputStream != null){
                try {
                    inputStream.close();
                } catch (IOException ignored){
                    // This is normal
                }
            }
        }
        return null;
    }

    private static int find(byte[] array, int length, char until){
        for(int i=0; i < length; i++){
            if(array[i] == until){
                return i;
            }
        }
        return length;
    }

}
