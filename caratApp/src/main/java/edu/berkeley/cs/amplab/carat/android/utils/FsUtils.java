package edu.berkeley.cs.amplab.carat.android.utils;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
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

    private static class Filter implements FileFilter {
        private String prefix;

        public Filter(String prefix){
            this.prefix = prefix;
        }

        @Override
        public boolean accept(File pathname) {
            String pattern = String.format(Locale.getDefault(), "%s[0-9]+", prefix);
            return Pattern.matches(pattern, pathname.getName());
        }
    }

    public static class CPU {
        private WeakReference<File[]> systemFiles; // No need to keep in memory
        private WeakReference<File[]> thermalZones;

        public long getCount(){
            File[] listing = getSystemListing();
            if(!Util.isNullOrEmpty(listing)){
                return listing.length;
            }
            return INVALID_VALUE;
        }

        public ArrayList<Long> getCurrentFrequency(){
            return readSubValues("/cpufreq/scaling_cur_freq");
        }

        public ArrayList<Long> getUtilization(){
            return readSubValues("/cpufreq/cpu_utilization");
        }

        public ArrayList<Long> getThermalZones(){
            return readSubValues("/temp");
        }

        private ArrayList<Long> readSubValues(String subPath){
            ArrayList<Long> frequencies = new ArrayList<>();

            File[] listing = getSystemListing();
            for(File file : listing){
                long frequency = readLong(file.getPath() + subPath);
                frequencies.add(frequency);
            }
            return frequencies;
        }

        private File[] getSystemListing(){
            File[] listing = Util.getWeakOrFallback(systemFiles, () -> {
                File directory = new File("/sys/devices/system/cpu/");
                if(directory.canRead()){
                    return directory.listFiles(new Filter("cpu"));
                }
                return null;
            });
            systemFiles = new WeakReference<>(listing);
            return listing;
        }

        private File[] getThermalListing(){
            File[] listing = Util.getWeakOrFallback(thermalZones, () -> {
                File directory = new File("/sys/devices/virtual/thermal/");
                if(directory.canRead()){
                    return directory.listFiles(new Filter("thermal_zone"));
                }
                return null;
            });
            thermalZones = new WeakReference<>(listing);
            return listing;
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
