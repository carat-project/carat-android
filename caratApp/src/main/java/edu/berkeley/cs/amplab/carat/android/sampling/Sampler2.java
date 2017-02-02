package edu.berkeley.cs.amplab.carat.android.sampling;

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
    public static void sample(String uuId, String trigger, String state, SamplingLibrary library){
        SystemLoadPoint load1 = SamplingLibrary.getSystemLoad();
        Intent batteryIntent = library.getLastBatteryIntent();

        Sample sample = new Sample();
        sample.setUuId(uuId);
        sample.setTriggeredBy(trigger);
        sample.setBatteryLevel(BatteryUtils.getBatteryLevel(batteryIntent)/100.0);
        sample.setBatteryDetails(getBatteryDetails(library, batteryIntent));
        sample.setBatteryState(getBatteryStatusString(batteryIntent, state));

        sample.setTimestamp(System.currentTimeMillis()/1000.0);
        sample.setPiList(library.getRunningProcessInfoForSample());
        sample.setScreenBrightness(library.getScreenBrightness());
        sample.setLocationProviders(library.getEnabledLocationProviders());
        sample.setNetworkStatus(library.getNetworkStatusForSample());
        sample.setNetworkDetails(constructNetworkDetails(library));

        sample.setStorageDetails(SamplingLibrary.getStorageDetails());
        sample.setSettings(constructSettings());
        sample.setDeveloperMode(library.isDeveloperModeOn());
        sample.setUnknownSources(library.allowUnknownSources());
        sample.setScreenOn(library.isScreenOn());
        sample.setTimeZone(library.getTimeZone());
        sample.setCountryCode(library.getCountryCode());
        sample.setExtra(library.getExtras());


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

    private static BatteryDetails getBatteryDetails(SamplingLibrary samplingLibrary, Intent intent){
        if(intent == null) return null;

        BatteryDetails details = new BatteryDetails();
        details.setBatteryHealth(getHealthString(intent));
        details.setBatteryTechnology(intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY));
        details.setBatteryTemperature(intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 100);
        details.setBatteryVoltage(intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000.0);
        details.setBatteryCharger(getChargerString(intent));
        details.setBatteryCapacity(samplingLibrary.getBatteryCapacity());
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

    private static NetworkDetails constructNetworkDetails(SamplingLibrary library){
        NetworkDetails details = new NetworkDetails();
        details.setNetworkType(library.getNetworkType());
        details.setMobileNetworkType(library.getMobileNetworkType());
        details.setRoamingEnabled(library.getRoamingStatus());
        details.setMobileDataStatus(library.getDataState());
        details.setMobileDataActivity(library.getDataActivity());
        details.setSimOperator(library.getSIMOperator());
        details.setNetworkOperator(library.getNetworkOperator());
        details.setMcc(library.getMcc());
        details.setMnc(library.getMnc());
        details.setWifiStatus(library.getWifiState());
        details.setWifiSignalStrength(library.getWifiSignalStrength());
        details.setWifiLinkSpeed(library.getWifiLinkSpeed());
        details.setWifiApStatus(library.getWifiHotspotState());
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
