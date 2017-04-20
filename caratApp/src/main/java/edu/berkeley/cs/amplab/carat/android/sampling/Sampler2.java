package edu.berkeley.cs.amplab.carat.android.sampling;

import android.content.Context;
import android.content.Intent;
import android.os.BatteryManager;

import edu.berkeley.cs.amplab.carat.android.models.SystemLoadPoint;
import edu.berkeley.cs.amplab.carat.android.utils.BatteryUtils;
import edu.berkeley.cs.amplab.carat.thrift.BatteryDetails;
import edu.berkeley.cs.amplab.carat.thrift.CpuStatus;
import edu.berkeley.cs.amplab.carat.thrift.NetworkDetails;
import edu.berkeley.cs.amplab.carat.thrift.Sample;
import edu.berkeley.cs.amplab.carat.thrift.Settings;

/**
 * Created by Jonatan Hamberg on 2.2.2017.
 */
public class Sampler2 {
    // This class should either save the sample to SampleDB or have the methods calling it do so.

    public static void sample(Context context, String uuId, String trigger, String state){
        SystemLoadPoint load1 = SamplingLibrary.getSystemLoad();
        Intent batteryIntent = SamplingLibrary.getLastBatteryIntent(context);

        Sample sample = new Sample();
        sample.setUuId(uuId);
        sample.setTriggeredBy(trigger);
        sample.setBatteryLevel(BatteryUtils.getBatteryLevel(batteryIntent)/100.0);
        sample.setBatteryDetails(getBatteryDetails(context, batteryIntent));
        sample.setBatteryState(getBatteryStatusString(batteryIntent, state));

        sample.setTimestamp(System.currentTimeMillis()/1000.0);
        sample.setPiList(SamplingLibrary.getRunningProcessInfoForSample(context));
        sample.setScreenBrightness(SamplingLibrary.getScreenBrightness(context));
        sample.setLocationProviders(SamplingLibrary.getEnabledLocationProviders(context));
        sample.setNetworkStatus(SamplingLibrary.getNetworkStatusForSample(context));
        sample.setNetworkDetails(constructNetworkDetails(context));

        sample.setStorageDetails(SamplingLibrary.getStorageDetails());
        sample.setSettings(constructSettings());
        sample.setDeveloperMode(SamplingLibrary.isDeveloperModeOn(context));
        sample.setUnknownSources(SamplingLibrary.allowUnknownSources(context));
        sample.setScreenOn(SamplingLibrary.isScreenOn(context));
        sample.setTimeZone(SamplingLibrary.getTimeZone(context));
        sample.setCountryCode(SamplingLibrary.getCountryCode(context));
        sample.setExtra(SamplingLibrary.getExtras(context));


        int[] memoryInfo = SamplingLibrary.readMeminfo();
        if(memoryInfo != null && memoryInfo.length == 4){
            sample.setMemoryUser(memoryInfo[0]);
            sample.setMemoryFree(memoryInfo[1]);
            sample.setMemoryActive(memoryInfo[2]);
            sample.setMemoryInactive(memoryInfo[3]);
        }

        // Take as much time between cpu measurements as possible
        SystemLoadPoint load2 = SamplingLibrary.getSystemLoad();
        sample.setCpuStatus(constructCpuStatus(load1, load2));
    }

    private static BatteryDetails getBatteryDetails(Context context, Intent intent){
        if(intent == null) return null;

        BatteryDetails details = new BatteryDetails();
        details.setBatteryHealth(getHealthString(intent));
        details.setBatteryTechnology(intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY));
        details.setBatteryTemperature(intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 100);
        details.setBatteryVoltage(intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000.0);
        details.setBatteryCharger(getChargerString(intent));
        details.setBatteryCapacity(SamplingLibrary.getBatteryCapacity(context));
        return details;
    }

    private static String getBatteryStatusString(Intent intent, String lastState){
        int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, 0);
        switch(status){
            case BatteryManager.BATTERY_STATUS_CHARGING: return "Charging";
            case BatteryManager.BATTERY_STATUS_DISCHARGING: return "Discharging";
            case BatteryManager.BATTERY_STATUS_FULL: return "Full";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING: return "Not charging";
            case BatteryManager.BATTERY_STATUS_UNKNOWN: return "Unknown";
            default: return lastState != null ? lastState : "Unknown";
        }
    }

    private static NetworkDetails constructNetworkDetails(Context context){
        NetworkDetails details = new NetworkDetails();
        details.setNetworkType(SamplingLibrary.getNetworkType(context));
        details.setMobileNetworkType(SamplingLibrary.getMobileNetworkType(context));
        details.setRoamingEnabled(SamplingLibrary.getRoamingStatus(context));
        details.setMobileDataStatus(SamplingLibrary.getDataState(context));
        details.setMobileDataActivity(SamplingLibrary.getDataActivity(context));
        details.setSimOperator(SamplingLibrary.getSIMOperator(context));
        details.setNetworkOperator(SamplingLibrary.getNetworkOperator(context));
        details.setMcc(SamplingLibrary.getMcc(context));
        details.setMnc(SamplingLibrary.getMnc(context));
        details.setWifiStatus(SamplingLibrary.getWifiState(context));
        details.setWifiSignalStrength(SamplingLibrary.getWifiSignalStrength(context));
        details.setWifiLinkSpeed(SamplingLibrary.getWifiLinkSpeed(context));
        details.setWifiApStatus(SamplingLibrary.getWifiHotspotState(context));
        return details;
    }

    private static CpuStatus constructCpuStatus(SystemLoadPoint load1, SystemLoadPoint load2){
        CpuStatus cpuStatus = new CpuStatus();
        cpuStatus.setCpuUsage(SamplingLibrary.getCpuUsage(load1, load2));
        cpuStatus.setUptime(SamplingLibrary.getUptime());
        cpuStatus.setSleeptime(SamplingLibrary.getSleepTime());
        return cpuStatus;
    }

    private static Settings constructSettings(){
        Settings settings = new Settings();
        settings.setBluetoothEnabled(SamplingLibrary.getBluetoothEnabled());
        return settings;
    }

    private static String getChargerString(Intent intent){
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
        switch(plugged){
            case BatteryManager.BATTERY_PLUGGED_AC: return "ac";
            case BatteryManager.BATTERY_PLUGGED_USB: return "usb";
            case BatteryManager.BATTERY_PLUGGED_WIRELESS: return "wireless";
            default: return "unplugged";
        }
    }

    private static String getHealthString(Intent intent){
        int health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0);
        switch(health){
            case BatteryManager.BATTERY_HEALTH_DEAD: return "Dead";
            case BatteryManager.BATTERY_HEALTH_GOOD: return "Good";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: return "Over voltage";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: return "Overheat";
            case BatteryManager.BATTERY_HEALTH_UNKNOWN: return "Unknown";
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE: return "Unspecified failure";
            default: return "Unknown";
        }
    }
}
