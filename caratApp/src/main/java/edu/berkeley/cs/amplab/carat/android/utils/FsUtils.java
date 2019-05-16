package edu.berkeley.cs.amplab.carat.android.utils;

import android.os.Handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Created by Jonatan Hamberg on 24.1.2018.
 */
public class FsUtils {
    private static final String TAG = FsUtils.class.getSimpleName();
    private static long INVALID_VALUE = -1;

    public static class THERMAL {
        private static WeakReference<File[]> thermalFiles = null;

        public static ArrayList<Long> getThermalZones(){
            File[] files = getFiles();
            Map<Integer, Long> result = readValues(files, "/temp", Long.class);
            return Util.sanitizeLongList(new ArrayList<>(result.values()));
        }

        public static ArrayList<String> getThermalZoneNames(){
            File[] files = getFiles();
            Map<Integer, String> result = readValues(files, "/type", String.class);
            return Util.sanitizeList(new ArrayList<>(result.values()));
        }

        public static long getCount() {
            File[] files = getFiles();
            if(!Util.isNullOrEmpty(files)){
                return files.length;
            }
            return INVALID_VALUE;
        }

        private static File[] getFiles() {
            return Util.getWeakOrFallback(thermalFiles, () -> {
                File[] result = listFiles("/sys/devices/virtual/thermal/", "thermal_zone[0-9]+");
                if (!Util.isNullOrEmpty(result)) {
                    thermalFiles = new WeakReference<>(result);
                }
                return result;
            });
        }
    }

    public static class CPU {
        private static WeakReference<File[]> cpuFiles = null;

        public static long getCount(){
            File[] files = getFiles();
            if(!Util.isNullOrEmpty(files)){
                return files.length;
            }
            return INVALID_VALUE;
        }

        public static ArrayList<Long> getCurrentFrequencies(){
            String subPath = "/cpufreq/scaling_cur_freq";
            Map<Integer, Long> frequencies = readValues(getFiles(), subPath, Long.class);
            return Util.sanitizeLongList(new ArrayList<>(frequencies.values()));
        }

        public static ArrayList<Long> getMinimumFrequencies(){
            String subPath = "/cpufreq/cpuinfo_min_freq";
            Map<Integer, Long> frequencies = readValues(getFiles(), subPath, Long.class);
            return Util.sanitizeLongList(new ArrayList<>(frequencies.values()));
        }

        public static ArrayList<Long> getMaximumFrequencies(){
            String subPath = "/cpufreq/cpuinfo_max_freq";
            Map<Integer, Long> frequencies = readValues(getFiles(), subPath, Long.class);
            return Util.sanitizeLongList(new ArrayList<>(frequencies.values()));
        }

        public static ArrayList<Long> getUtilization(){
            String subPath = "/cpufreq/cpu_utilization";
            Map<Integer, Long> utilization = readValues(getFiles(), subPath, Long.class);
            return Util.sanitizeLongList(new ArrayList<>(utilization.values()));
        }

        private static File[] getFiles(){
            return Util.getWeakOrFallback(cpuFiles, () -> {
                File[] result = listFiles("/sys/devices/system/cpu/", "cpu[0-9]+");
                if(!Util.isNullOrEmpty(result)){
                    cpuFiles = new WeakReference<>(result);
                }
                return result;
            });
        }
    }

    public static class MEMORY {
        private static String memInfoFile = "/proc/meminfo";
        private static WeakReference<Map<String, Long>> cache = null;

        public static long getTotalMemory(){
            return readMemInfo("MemTotal");
        }

        public static long getAvailableMemory(){
            return readMemInfo("MemAvailable");
        }

        public static long getFreeMemory(){
            return readMemInfo("MemFree");
        }

        public static long getBufferMemory(){
            return readMemInfo("Buffers");
        }

        public static long getCachedMemory(){
            return readMemInfo("Cached");
        }

        public static long getSwapCachedMemory(){
            return readMemInfo("SwapCached");
        }

        public static long getActiveMemory(){
            return readMemInfo("Active");
        }

        public static long getInactiveMemory(){
            return readMemInfo("Inactive");
        }

        public static long getLowWatermark(){
            return readZoneInfo("low");
        }

        public static long getActiveFileMemory(){
            return readMemInfo("Active(file)");
        }

        public static long getInactiveFileMemory(){
            return readMemInfo("Inactive(file)");
        }

        public static long getSlabReclaimableMemory(){
            return readMemInfo("SReclaimable");
        }

        private static long readMemInfo(String field){
            Long result = 0L;
            Map<String, Long> memInfo = Util.getWeakOrFallback(cache, () -> readMap(memInfoFile));
            if(!Util.isNullOrEmpty(memInfo)){
                cache = new WeakReference<>(memInfo);
                result = memInfo.get(field);
                scheduleCacheReset();
            }
            return result != null ? result : 0;
        }

        private static long readZoneInfo(String field){
            long pageSum = 0;
            try {
                RandomAccessFile file = new RandomAccessFile("/proc/zoneinfo" , "r");
                String line;
                while((line = file.readLine()) != null){
                    // Colon-separated values should be skipped
                    if(line.contains(":")){
                        continue;
                    }
                    String[] tokens = line.trim().split("\\s++");
                    if(tokens.length > 1){
                        if(tokens[0].equalsIgnoreCase(field)){
                            try {
                                pageSum += Long.parseLong(tokens[1]);
                            } catch (NumberFormatException e){
                                // This happens when lines aren't in proper key-value format
                                Logger.d(TAG, "Skipping line in /proc/zoneinfo");
                            }
                        }
                    }
                }
            } catch (IOException e) {
                Logger.e(TAG, "Failed reading zoneinfo: " + e);
            }
            return pageSum * 4; // Assume 4KB page size for ARM
        }

        private static void scheduleCacheReset(){
            new Handler().postDelayed(() -> {
                if(cache != null){
                    cache.clear();
                    cache = null;
                }
            }, 500); // Should be enough to query all needed values
        }
    }

    @SuppressWarnings("unchecked") // The casts are actually checked
    private static <V> Map<Integer, V> readValues(File[] files, String subPath, Class<V> valueClass){
        TreeMap<Integer, V> values = new TreeMap<>();
        if(!Util.isNullOrEmpty(files)){
            for(File file : files){
                try {
                    Integer id = Util.getDigits(file.getName());
                    if(id != null){
                        String path = file.getPath() + subPath;
                        V value = null;
                        if(valueClass == Long.class){
                            value = (V) readLong(path);
                        } else if(valueClass == String.class){
                            value = (V) readString(path);
                        } else {
                            Logger.e(TAG, "Unsupported type: " + valueClass.getSimpleName());
                        }
                        values.put(id, value);
                    }
                } catch (Exception e){
                    Logger.d(TAG, "Failed reading " + file.getPath());
                }
            }
        }
        return values;
    }

    private static File[] listFiles(String path, String pattern){
        File directory = new File(path);
        if(directory.canRead()){
            return directory.listFiles(file -> Pattern.matches(pattern, file.getName()));
        }
        return null;
    }

    private static Map<String, Long> readMap(String path){
        Map<String, Long> result = new HashMap<>();
        try {
            RandomAccessFile reader = new RandomAccessFile(path, "r");
            String line;
            while((line = reader.readLine()) != null){
                if(!Util.isNullOrEmpty(line)){
                    String[] tokens = line.split("\\s+");
                    if(tokens.length > 1){
                        try {
                            String key = tokens[0].replace(":", "");
                            Long value = Long.parseLong(tokens[1]);
                            result.put(key, value);
                        } catch (NumberFormatException e){
                            Logger.d(TAG, "Unexpected format when reading /proc/meminfo");
                        }
                    }
                }
            }
        } catch (IOException e)  {
            Logger.e(TAG, "Error when reading /proc/meminfo: " + e);
        }
        return result;
    }

    private static Long readLong(String path){
        String content = readString(path);
        if(!Util.isNullOrEmpty(content)){
            try {
                return Long.parseLong(content);
            } catch (NumberFormatException e){
                Logger.d(TAG,  "Unable to convert to long " + e);
            }
        }
        return INVALID_VALUE;
    }

    private static String readString(String path){
        byte[] buffer = new byte[4096];
        FileInputStream inputStream = null;
        try {
            inputStream = new FileInputStream(path);
            int length = inputStream.read(buffer);
            inputStream.close();

            if(length > 0){
                length = find(buffer, length);
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

    private static int find(byte[] array, int length){
        for(int i=0; i < length; i++){
            if(array[i] == '\n'){
                return i;
            }
        }
        return length;
    }

}
