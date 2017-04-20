package edu.berkeley.cs.amplab.carat.android.sampling;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManager.RunningServiceInfo;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.Signature;
import android.database.Cursor;
import android.location.Criteria;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.os.StatFs;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.provider.Settings.Secure;
import android.provider.Settings.SettingNotFoundException;
import android.telephony.CellLocation;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.telephony.cdma.CdmaCellLocation;
import android.telephony.gsm.GsmCellLocation;
import android.util.Log;
import android.widget.Toast;

import com.flurry.android.FlurryAgent;

import edu.berkeley.cs.amplab.carat.android.CaratApplication;
import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.R;
import edu.berkeley.cs.amplab.carat.android.models.SystemLoadPoint;
import edu.berkeley.cs.amplab.carat.android.utils.BatteryUtils;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;
import edu.berkeley.cs.amplab.carat.android.utils.Util;
import edu.berkeley.cs.amplab.carat.thrift.BatteryDetails;
import edu.berkeley.cs.amplab.carat.thrift.CallMonth;
import edu.berkeley.cs.amplab.carat.thrift.CellInfo;
import edu.berkeley.cs.amplab.carat.thrift.CpuStatus;
import edu.berkeley.cs.amplab.carat.thrift.Feature;
import edu.berkeley.cs.amplab.carat.thrift.NetworkDetails;
import edu.berkeley.cs.amplab.carat.thrift.ProcessInfo;
import edu.berkeley.cs.amplab.carat.thrift.Sample;
import edu.berkeley.cs.amplab.carat.thrift.StorageDetails;

/**
 * Library class for methods that obtain information about the phone that is
 * running Carat.
 * 
 * @author Eemil Lagerspetz
 * 
 */
public final class SamplingLibrary {
	private static final boolean collectSignatures = true;
	public static final String SIG_SENT = "sig-sent:";
	public static final String SIG_SENT_256 = "sigs-sent:";
	public static final String INSTALLED = "installed:";
	public static final String REPLACED = "replaced:";
	public static final String UNINSTALLED = "uninstalled:";
	// Disabled or turned off applications will be scheduled for reporting using this prefix
	public static final String DISABLED = "disabled:";

	private static final int READ_BUFFER_SIZE = 2 * 1024;
	// Network status constants
	public static String NETWORKSTATUS_DISCONNECTED = "disconnected";
	public static String NETWORKSTATUS_DISCONNECTING = "disconnecting";
	public static String NETWORKSTATUS_CONNECTED = "connected";
	public static String NETWORKSTATUS_CONNECTING = "connecting";
	// Network type constants
	public static String TYPE_UNKNOWN = "unknown";
	// Data State constants
	public static String DATA_DISCONNECTED = NETWORKSTATUS_DISCONNECTED;
	public static String DATA_CONNECTING = NETWORKSTATUS_CONNECTING;
	public static String DATA_CONNECTED = NETWORKSTATUS_CONNECTED;
	public static String DATA_SUSPENDED = "suspended";
	// Data Activity constants
	public static String DATA_ACTIVITY_NONE = "none";
	public static String DATA_ACTIVITY_IN = "in";
	public static String DATA_ACTIVITY_OUT = "out";
	public static String DATA_ACTIVITY_INOUT = "inout";
	public static String DATA_ACTIVITY_DORMANT = "dormant";
	// Wifi State constants
	public static String WIFI_STATE_DISABLING = "disabling";
	public static String WIFI_STATE_DISABLED = "disabled";
	public static String WIFI_STATE_ENABLING = "enabling";
	public static String WIFI_STATE_ENABLED = "enabled";
	public static String WIFI_STATE_UNKNOWN = "unknown";
	// Wifi AP State constants
	public static final int WIFI_AP_STATE_DISABLING = 10;
	public static final int WIFI_AP_STATE_DISABLED = 11;
	public static final int WIFI_AP_STATE_ENABLING = 12;
	public static final int WIFI_AP_STATE_ENABLED = 13;
	public static final int WIFI_AP_STATE_FAILED = 14;

	// Call state constants
	public static String CALL_STATE_IDLE = "idle";
	public static String CALL_STATE_OFFHOOK = "offhook";
	public static String CALL_STATE_RINGING = "ringing";

	// Mobile network constants
	/*
	 * we cannot find network types:EVDO_B,LTE,EHRPD,HSPAP from TelephonyManager
	 * now
	 */
	public static String NETWORK_TYPE_UNKNOWN = "unknown";
	public static String NETWORK_TYPE_GPRS = "gprs";
	public static String NETWORK_TYPE_EDGE = "edge";
	public static String NETWORK_TYPE_UMTS = "utms";
	public static String NETWORK_TYPE_CDMA = "cdma";
	public static String NETWORK_TYPE_EVDO_0 = "evdo_0";
	public static String NETWORK_TYPE_EVDO_A = "evdo_a";
	public static String NETWORK_TYPE_EVDO_B = "evdo_b";
	public static String NETWORK_TYPE_1xRTT = "1xrtt";
	public static String NETWORK_TYPE_HSDPA = "hsdpa";
	public static String NETWORK_TYPE_HSUPA = "hsupa";
	public static String NETWORK_TYPE_HSPA = "hspa";
	public static String NETWORK_TYPE_IDEN = "iden";
	public static String NETWORK_TYPE_LTE = "lte";
	public static String NETWORK_TYPE_EHRPD = "ehrpd";
	public static String NETWORK_TYPE_HSPAP = "hspap";

	private static final int EVDO_B = 12;
	private static final int LTE = 13;
	private static final int EHRPD = 14;
	private static final int HSPAP = 15;

	// Phone type constants
	public static String PHONE_TYPE_CDMA = "cdma";
	public static String PHONE_TYPE_GSM = "gsm";
	// public static String PHONE_TYPE_SIP="sip";
	public static String PHONE_TYPE_NONE = "none";

	public static double startLatitude = 0;
	public static double startLongitude = 0;
	public static double distance = 0;

	private static final String STAG = "sample";
	// private static final String TAG="FeaturesPowerConsumption";

	public static final int UUID_LENGTH = 16;

	private static double lastSampledBatteryLevel;
	private static double currentBatteryLevel;

	// we might not be able to read the current battery level at the first run
	// of Carat.
	// so it might be zero until we get the non-zero value from the intent
	// (BatteryManager.EXTRA_LEVEL & BatteryManager.EXTRA_SCALE)

	private static Location lastKnownLocation = null;


	/**
	 * Returns a randomly generated unique identifier that stays constant for
	 * the lifetime of the device. (May change if wiped). This is probably our
	 * best choice for a UUID across the Android landscape, since it is present
	 * on both phones and non-phones.
	 * 
	 * @return a String that uniquely identifies this device.
	 */
	public static String getAndroidId(Context c) {
		return Secure.getString(c.getContentResolver(), Secure.ANDROID_ID);
	}

	public static String getUuid(Context c) {
		return getTimeBasedUuid(c, false);
	}

	public static String getTimeBasedUuid(Context c) {
		return getTimeBasedUuid(c, true);
	}

	public static double readLastBatteryLevel() {
		return lastSampledBatteryLevel;
	}

	public static void setLastSampledBatteryLevel(double level) {
		SamplingLibrary.lastSampledBatteryLevel = level;
	}

	public static double getLastSampledBatteryLevel(Context context) {
		return lastSampledBatteryLevel;
	}

	public static double getCurrentBatteryLevel() {
		return currentBatteryLevel;
	}

	public static void setCurrentBatteryLevel(double level) {
		SamplingLibrary.currentBatteryLevel = level;
	}
	
	/**
	 * 
	 * Take in currentLevel and scale as doubles to avoid loss of precision issues.
	 * Note that Carat stores battery level as a value between 0 and 1, e.g. 0.45 for 45%.
	 * @param currentLevel Current battery level, usually in percent.
	 * @param scale Battery scale, usually 100.0.
	 */
	public static void setCurrentBatteryLevel(double currentLevel, double scale) {
		/* we should multiply the result of the division below by 100.0 to get the battery level 
		 * in the scale of 0-100, but since the previous samples in our server's dataset are in the scale of 0.00-1.00, 
		 * we omit the multiplication. */
		double level = currentLevel / scale;
		/*
		 * whenever we get these two arguments (extras from the intent:
		 * EXTRA_LEVEL & EXTRA_SCALE), it doens't necessarily mean that a battery
		 * percentage change has happened. Check the comments in the
		 * broadcast receiver (sampler).
		 */
		if (level != getCurrentBatteryLevel()) {
			setCurrentBatteryLevel(level);
			// Logger.d("SamplingLibrary.setCurrentBatteryLevel()", "currentBatteryLevel="
			//		+ getCurrentBatteryLevel());
		}
	}

	/**
	 * Generate a time-based, random identifier.
	 * 
	 * @param c
	 *            the app's Context
	 * @return a time-based, random identifier.
	 */
	public static String getTimeBasedUuid(Context c, boolean includeTimestamp) {
		String aID = getAndroidId(c);
		String wifiMac = getWifiMacAddress(c);
		String devid = getDeviceId(c);
		String concat = "";
		if (aID != null)
			concat = aID;
		else
			concat = "0000000000000000";
		if (wifiMac != null)
			concat += wifiMac;
		else
			concat += "00:00:00:00:00:00";

		// IMEI is 15 characters, decimal, while MEID is 14 characters, hex. Add
		// a space if length is less than 15:
		if (devid != null) {
			concat += devid;
			if (devid.length() < 15)
				concat += " ";
		} else
			concat += "000000000000000";
		if (includeTimestamp) {
			long timestamp = System.currentTimeMillis();
			concat += timestamp;
		}

		// Logger.d(STAG,
		// "AID="+aID+" wifiMac="+wifiMac+" devid="+devid+" rawUUID=" +concat );
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-512");
			md.update(concat.getBytes());
			byte[] mdbytes = md.digest();
			StringBuilder hexString = new StringBuilder();
			for (int i = 0; i < mdbytes.length; i++) {
				String hx = Integer.toHexString(0xFF & mdbytes[i]);
				if (hx.equals("0"))
					hexString.append("00");
				else
					hexString.append(hx);
			}
			String uuid = hexString.toString().substring(0, UUID_LENGTH);
			// FlurryAgent.logEvent("ANDROID_ID=" + aID +" UUID=" + uuid);
			return uuid;
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return aID;
		}
	}

	/**
	 * Returns the model of the device running Carat, for example "sdk" for the
	 * emulator, Galaxy Nexus for Samsung Galaxy Nexus.
	 * 
	 * @return the model of the device running Carat, for example "sdk" for the
	 *         emulator, Galaxy Nexus for Samsung Galaxy Nexus.
	 */
	public static String getModel() {
		return android.os.Build.MODEL;
	}

	/**
	 * Returns the manufacturer of the device running Carat, for example
	 * "google" or "samsung".
	 * 
	 * @return the manufacturer of the device running Carat, for example
	 *         "google" or "samsung".
	 */
	public static String getManufacturer() {
		return android.os.Build.MANUFACTURER;
	}

	/**
	 * Returns the OS version of the device running Carat, for example 2.3.3 or
	 * 4.0.2.
	 * 
	 * @return the OS version of the device running Carat, for example 2.3.3 or
	 *         4.0.2.
	 */
	public static String getOsVersion() {
		return android.os.Build.VERSION.RELEASE;
	}

	/**
	 * Returns the product name.
	 * 
	 * @return the product name.
	 */
	public static String getProductName() {
		return android.os.Build.PRODUCT;
	}

	/**
	 * Returns the kernel version, e.g. 3.4-1101.
	 * 
	 * @return the kernel version, e.g. 3.4-1101.
	 */
	public static String getKernelVersion() {
		return System.getProperty("os.version", TYPE_UNKNOWN);
	}

	/**
	 * Returns the build serial number. May only work for 2.3 and up.
	 * 
	 * @return the build serial number.
	 */
	public static String getBuildSerial() {
		// return android.os.Build.Serial;
		return System.getProperty("ro.serial", TYPE_UNKNOWN);
	}

	/**
	 * Print all system properties for debugging.
	 * 
	 */
	public static void printAllProperties() {
		Properties list = System.getProperties();
		Enumeration<Object> keys = list.keys();
		while (keys.hasMoreElements()) {
			String k = (String) keys.nextElement();
			String v = list.getProperty(k);
			if (Constants.DEBUG)
			    Logger.d("PROPS", k + "=" + v);
		}
	}

	/**
	 * Returns the brand for which the device is customized, e.g. Verizon.
	 * 
	 * @return the brand for which the device is customized, e.g. Verizon.
	 */
	public static String getBrand() {
		return android.os.Build.BRAND;
	}

	/**
	 * Return misc system details that we might want to use later. Currently
	 * does nothing.
	 * 
	 * @return
	 */
	public static Map<String, String> getSystemDetails() {
		Map<String, String> results = new HashMap<String, String>();
		// TODO: Some of this should be added to registration to identify the
		// device and OS.
		// Cyanogenmod and others may have different kernels etc that affect
		// performance.

		/*
		 * Logger.d("SetModel", "board:" + android.os.Build.BOARD);
		 * Logger.d("SetModel", "bootloader:" + android.os.Build.BOOTLOADER);
		 * Logger.d("SetModel", "brand:" + android.os.Build.BRAND);
		 * Logger.d("SetModel", "CPU_ABI 1 and 2:" + android.os.Build.CPU_ABI +
		 * ", " + android.os.Build.CPU_ABI2); Logger.d("SetModel", "dev:" +
		 * android.os.Build.DEVICE); Logger.d("SetModel", "disp:" +
		 * android.os.Build.DISPLAY); Logger.d("SetModel", "FP:" +
		 * android.os.Build.FINGERPRINT); Logger.d("SetModel", "HW:" +
		 * android.os.Build.HARDWARE); Logger.d("SetModel", "host:" +
		 * android.os.Build.HOST); Logger.d("SetModel", "ID:" +
		 * android.os.Build.ID); Logger.d("SetModel", "manufacturer:" +
		 * android.os.Build.MANUFACTURER); Logger.d("SetModel", "prod:" +
		 * android.os.Build.PRODUCT); Logger.d("SetModel", "radio:" +
		 * android.os.Build.RADIO); // FIXME: SERIAL not available on 2.2 //
		 * Logger.d("SetModel", "ser:" + android.os.Build.SERIAL);
		 * Logger.d("SetModel", "tags:" + android.os.Build.TAGS); Logger.d("SetModel",
		 * "time:" + android.os.Build.TIME); Logger.d("SetModel", "type:" +
		 * android.os.Build.TYPE); Logger.d("SetModel", "unknown:" +
		 * android.os.Build.UNKNOWN); Logger.d("SetModel", "user:" +
		 * android.os.Build.USER); Logger.d("SetModel", "model:" +
		 * android.os.Build.MODEL); Logger.d("SetModel", "codename:" +
		 * android.os.Build.VERSION.CODENAME); Logger.d("SetModel", "release:" +
		 * android.os.Build.VERSION.RELEASE);
		 */

		return results;
	}

	/**
	 * Read memory information from /proc/meminfo. Return used, free, inactive,
	 * and active memory.
	 * 
	 * @return an int[] with used, free, inactive, and active memory, in kB, in
	 *         that order.
	 */
	public static int[] readMeminfo() {
		try {
			RandomAccessFile reader = new RandomAccessFile("/proc/meminfo", "r");
			String load = reader.readLine();

			String[] toks = load.split("\\s+");
			// Log.v("meminfo", "Load: " + load + " 1:" + toks[1]);
			int total = Integer.parseInt(toks[1]);
			load = reader.readLine();
			toks = load.split("\\s+");
			// Log.v("meminfo", "Load: " + load + " 1:" + toks[1]);
			int free = Integer.parseInt(toks[1]);
			load = reader.readLine();
			load = reader.readLine();
			load = reader.readLine();
			load = reader.readLine();
			toks = load.split("\\s+");
			// Log.v("meminfo", "Load: " + load + " 1:" + toks[1]);
			int act = Integer.parseInt(toks[1]);
			load = reader.readLine();
			toks = load.split("\\s+");
			// Log.v("meminfo", "Load: " + load + " 1:" + toks[1]);
			int inact = Integer.parseInt(toks[1]);
			reader.close();
			return new int[] { free, total, act, inact };
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		return new int[] { 0, 0, 0, 0 };
	}

	/**
	 * Read memory usage using the public Android API methods in
	 * ActivityManager, such as MemoryInfo and getProcessMemoryInfo.
	 * 
	 * @param c
	 *            the Context from the running Activity.
	 * @return int[] with total and used memory, in kB, in that order.
	 */
	public static int[] readMemory(Context c) {
		ActivityManager man = (ActivityManager) c.getSystemService(Activity.ACTIVITY_SERVICE);
		/* Get available (free) memory */
		ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
		man.getMemoryInfo(info);
		int totalMem = (int) info.availMem;

		/* Get memory used by all running processes. */

		/* Step 1: gather pids */
		List<ActivityManager.RunningAppProcessInfo> procs = man.getRunningAppProcesses();
		List<ActivityManager.RunningServiceInfo> servs = man.getRunningServices(Integer.MAX_VALUE);
		int[] pids = new int[procs.size() + servs.size()];
		int i = 0;
		for (ActivityManager.RunningAppProcessInfo pinfo : procs) {
			pids[i] = pinfo.pid;
			i++;
		}
		for (ActivityManager.RunningServiceInfo pinfo : servs) {
			pids[i] = pinfo.pid;
			i++;
		}

		/*
		 * Step 2: Sum up Pss values (weighted memory usage, taking into account
		 * shared page usage)
		 */
		android.os.Debug.MemoryInfo[] mems = man.getProcessMemoryInfo(pids);
		int memUsed = 0;
		for (android.os.Debug.MemoryInfo mem : mems) {
			memUsed += mem.getTotalPss();
		}
		Log.v("Mem", "Total mem:" + totalMem);
		Log.v("Mem", "Mem Used:" + memUsed);
		return new int[] { totalMem, memUsed };
	}

	// FIXME: Describe this. Why are there so many fields? Why is it divided by
	// 100?
	/*
	 * The value of HZ varies across kernel versions and hardware platforms. On
	 * i386 the situation is as follows: on kernels up to and including 2.4.x,
	 * HZ was 100, giving a jiffy value of 0.01 seconds; starting with 2.6.0, HZ
	 * was raised to 1000, giving a jiffy of 0.001 seconds. Since kernel 2.6.13,
	 * the HZ value is a kernel configuration parameter and can be 100, 250 (the
	 * default) or 1000, yielding a jiffies value of, respectively, 0.01, 0.004,
	 * or 0.001 seconds. Since kernel 2.6.20, a further frequency is available:
	 * 300, a number that divides evenly for the common video frame rates (PAL,
	 * 25 HZ; NTSC, 30 HZ).
	 * 
	 * I will leave the unit of cpu time as the jiffy and we can discuss later.
	 * 
	 * 0 name of cpu 1 space 2 user time 3 nice time 4 sys time 5 idle time(it
	 * is not include in the cpu total time) 6 iowait time 7 irg time 8 softirg
	 * time
	 * 
	 * the idleTotal[5] is the idle time which always changes. There are two
	 * spaces between cpu and user time.That is a tricky thing and messed up
	 * splitting.:)
	 */

	/**
	 * Read CPU usage from /proc/stat, return a fraction of
	 * usage/(usage+idletime)
	 * 
	 * @return a fraction of usage/(usage+idletime)
	 */
	public static long[] readUsagePoint() {
		try {
			RandomAccessFile reader = new RandomAccessFile("/proc/stat", "r");
			String load = reader.readLine();

			String[] toks = load.split(" ");

			long idle1 = Long.parseLong(toks[5]);
			long cpu1 = Long.parseLong(toks[2]) + Long.parseLong(toks[3]) + Long.parseLong(toks[4])
					+ Long.parseLong(toks[6]) + Long.parseLong(toks[7]) + Long.parseLong(toks[8]);

			reader.close();
			return new long[] { idle1, cpu1 };
		} catch (IOException ex) {
			ex.printStackTrace();
		}

		return null;
	}

	/**
	 * Calculate CPU usage between the cpu and idle time given at two time
	 * points.
	 * 
	 * @param then
	 * @param now
	 * @return
	 */
	public static double getUsage(long[] then, long[] now) {
		if (then == null || now == null || then.length < 2 || now.length < 2)
			return 0.0;
		double idleAndCpuDiff = (now[0] + now[1]) - (then[0] + then[1]);
		return (now[1] - then[1]) / idleAndCpuDiff;
	}

	public static double getCpuUsage(SystemLoadPoint cpu1, SystemLoadPoint cpu2){
		float totalDiff = cpu2.total - cpu1.total;
		if(totalDiff == 0) return 100; // Avoid diving by zero
		float idleDiff = cpu2.idleAll - cpu1.idleAll;
		float cpuP = 100*(totalDiff - idleDiff)/totalDiff;

		// Disregard negative values caused by a bug in linux kernel
		return (cpuP > 0)? cpuP : 0;
	}


	public static SystemLoadPoint getSystemLoad() {
		Integer[][] data = Util.readRAF("/proc/stat", 1, 10, "\\s+");
		if(data == null || data.length == 0){
			return null;
		}
		else return new SystemLoadPoint(data[0]);
	}

	private static WeakReference<List<RunningAppProcessInfo>> runningAppInfo = null;

	public static List<ProcessInfo> getRunningAppInfo(Context context) {
		List<RunningAppProcessInfo> runningProcs = getRunningProcessInfo(context);
		List<RunningServiceInfo> runningServices = getRunningServiceInfo(context);

		Set<String> packages = new HashSet<String>();
		List<ProcessInfo> l = new ArrayList<ProcessInfo>();

		if (runningProcs != null) {
			for (RunningAppProcessInfo pi : runningProcs) {
				if (pi == null)
					continue;
				if (packages.contains(pi.processName))
				    continue;
                packages.add(pi.processName);
				ProcessInfo item = new ProcessInfo();
				item.setImportance(CaratApplication.importanceString(pi.importance));
				item.setPId(pi.pid);
				item.setPName(pi.processName);
				l.add(item);
			}
		}

		if (runningServices != null) {
			for (RunningServiceInfo pi : runningServices) {
				if (pi == null)
					continue;
				if (packages.contains(pi.process))
                    continue;
                packages.add(pi.process);
				ProcessInfo item = new ProcessInfo();
				item.setImportance(pi.foreground ? "Foreground app" : "Service");
				item.setPId(pi.pid);
				//item.setApplicationLabel(pi.service.flattenToString());
				item.setPName(pi.process);
				
				l.add(item);
			}
		}

		return l;
	}

	private static WeakReference<List<RunningServiceInfo>> runningServiceInfo = null;


	/**
	 * Populate running process info into the runningAppInfo WeakReference list, and return its value.
	 * @param context the Context
	 * @return the value of the runningAppInfo WeakReference list after setting it.
	 */
	private static List<RunningAppProcessInfo> getRunningProcessInfo(Context context) {
		if (runningAppInfo == null || runningAppInfo.get() == null) {
			ActivityManager pActivityManager = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);
			List<RunningAppProcessInfo> runningProcs = pActivityManager.getRunningAppProcesses();
			/*
			 * TODO: Is this the right thing to do? Remove part after ":" in
			 * process names
			 */
			for (RunningAppProcessInfo i : runningProcs) {
				if (i != null && i.processName != null) {
					int idx = i.processName.lastIndexOf(':');
					if (idx <= 0)
						idx = i.processName.length();
					i.processName = i.processName.substring(0, idx);
				}
			}

			runningAppInfo = new WeakReference<List<RunningAppProcessInfo>>(runningProcs);
			return runningProcs;
		}else
		    return runningAppInfo.get();
	}

	/**
	 * Returns a list of currently running Services.
	 * @param c the Context.
	 * @return Returns a list of currently running Services.
	 */
	public static List<RunningServiceInfo> getRunningServiceInfo(Context c) {
		if(runningServiceInfo == null || runningServiceInfo.get() == null) {
			ActivityManager pActivityManager = (ActivityManager) c.getSystemService(Activity.ACTIVITY_SERVICE);
			List<RunningServiceInfo> runningServices = pActivityManager.getRunningServices(255);
			runningServiceInfo = new WeakReference<List<RunningServiceInfo>>(runningServices);
			return runningServices;
		} else return runningServiceInfo.get();
	}

	/**
	 * Helper to query whether an application is currently running and its code has not been evicted from memory. 
	 * @param context the Context
	 * @param appName the package name or process name of the application.
	 * @return true if the application is running, false otherwise.
	 */
	public static boolean isRunning(Context context, String appName) {
		List<RunningAppProcessInfo> runningProcs = getRunningProcessInfo(context);
		for (RunningAppProcessInfo i : runningProcs) {
		   // Logger.d(TAG, "Matching process: "+i.processName +" with app: "+ appName);
			if (i.processName.equals(appName) && i.importance != RunningAppProcessInfo.IMPORTANCE_EMPTY)
				return true;
		}
		
		List<RunningServiceInfo> services = getRunningServiceInfo(context);
		for (RunningServiceInfo service: services){
		  //  Logger.d(TAG, "Matching service: "+service.process +" with app: "+ appName + " service pkg: " + service.clientPackage + " label: " + service.clientLabel);
		    String pname = service.process;
		    int idx = pname.indexOf(":");
		    if (idx > 0)
		        pname = pname.substring(0, idx);
		    if (pname.equals(appName))
		        return true;
		}
		
		return false;
	}

	public static boolean isSettingsSuggestion(Context context, String appName) {
		// TODO: fill in (if everything is a suggestion, and no need for checking at the client side, then remove this method)
		return true;
	}
	
	/**
	 * Used to clear the runningAppInfo WeakReference list when the process list view needs to be refreshed.
	 */
	public static void resetRunningProcessInfo() {
		runningAppInfo = null;
		runningServiceInfo = null;
	}

	/**
	 * package name to packageInfo map for quick querying.
	 */
	static WeakReference<Map<String, PackageInfo>> packages = null;

	/**
	 * Returns true if an application should be hidden in the UI. Uses the blacklist downloaded from Carat servers.
	 * @param c the Context
	 * @param processName the process name
	 * @return true if the process should be hidden from the user, usually because it is an unkillable application that belongs to the system.
	 */
	public static boolean isHidden(Context c, String processName) {
		boolean isSystem = isSystem(c, processName);
		boolean blocked = isDisabled(c, processName) || (isSystem && !isWhiteListed(c, processName));
		return blocked || isBlacklisted(c, processName);
	}

	/**
	 * We currently do not employ a whitelist, so this returns true iff isBlacklisted(c, processName) returns false and vice versa.
	 * 
	 * @param c the Context.
	 * @param processName the process name.
	 * @return true iff isBlacklisted(c, processName) returns false and vice versa.
	 */
	private static boolean isWhiteListed(Context c, String processName) {
		return !isBlacklisted(c, processName);
	}

	/**
	 * Returns true if the processName matches an intem on the blacklist downloaded from Carat servers. 
	 * 
	 * @param c the Context.
	 * @param processName the process name.
	 * @return true if the processName matches an intem on the blacklist downloaded from Carat servers.
	 */
	private static boolean isBlacklisted(Context c, String processName) {
		/*
		 * Whitelist: Messaging, Voice Search, Bluetooth Share
		 * 
		 * Blacklist: Key chain, google partner set up, package installer,
		 * package access helper
		 */
		if (CaratApplication.getStorage() != null) {
			List<String> blacklist = CaratApplication.getStorage().getBlacklist();
			if (blacklist != null && blacklist.size() > 0 && processName != null && blacklist.contains(processName)) {
				return true;
			}

			blacklist = CaratApplication.getStorage().getGloblist();
			if (blacklist != null && blacklist.size() > 0 && processName != null) {
				for (String glob : blacklist) {
					if (glob == null)
						continue;
					// something*
					if (glob.endsWith("*") && processName.startsWith(glob.substring(0, glob.length() - 1)))
						return true;
					// *something
					if (glob.startsWith("*") && processName.endsWith(glob.substring(1)))
						return true;
				}
			}
		}
		String label = CaratApplication.labelForApp(c, processName);

		if (processName != null && label != null && label.equals(processName)) {
			// Log.v("Hiding uninstalled", processName);
			return true;
		}

		// FlurryAgent.logEvent("Whitelisted "+processName + " \""+ label+"\"");
		return false;
	}

	/**
	 * Returns true if the application is preinstalled on the device.
	 * This usually means it is a system application, e.g. Key chain, google partner set up, package installer, package access helper.
	 * We currently do not filter these out, because some of them are killable by the user, and not part of the core system, even if they are preinstalled on the device. 
	 * @param context the Context.
	 * @param processName the process name.
	 * @return true if the application is preinstalled on the device.
	 */
	private static boolean isSystem(Context context, String processName) {
		PackageInfo pak = getPackageInfo(context, processName);
		if (pak != null) {
			ApplicationInfo i = pak.applicationInfo;
			int flags = i.flags;
			boolean isSystemApp = (flags & ApplicationInfo.FLAG_SYSTEM) > 0;
			isSystemApp = isSystemApp || (flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) > 0;
			// Log.v(STAG, processName + " is System app? " + isSystemApp);
			return isSystemApp;
		}
		return false;
	}
	
	public static boolean isDisabled(Context c, String processName) {
	    PackageManager pm = c.getPackageManager();
        if (pm == null)
            return false;
        try {
            ApplicationInfo info = pm.getApplicationInfo(processName, 0);
            boolean disabled = !info.enabled;
            /* If an app is disabled, schedule it for sending with the next sample.
             * This is triggered in the UI, so the amount of times that an app being
             * disabled is sent is limited to the number of times the user refreshes Carat
             * between two analysis runs. Disabled applications will then be recorded by
             * the analysis, and not sent to the client when they ask for hogs/bugs after that.
             * Over time, Carat then follows users' Hogs and Bugs better, knowing which apps are
             * disabled.
             */
            if (disabled) {
                if (Constants.DEBUG)
                    Logger.i(STAG, "DISABLED: " + processName);
                Editor e = PreferenceManager.getDefaultSharedPreferences(c.getApplicationContext()).edit();
                e.putBoolean(SamplingLibrary.DISABLED + processName, true).commit();
            }
            return disabled;
        } catch (NameNotFoundException e) {
            if (Constants.DEBUG)
                Logger.d(STAG, "Could not find app info for: "+processName);
        }
	    return false;
	}

	/**
	 * Helper to ensure the WeakReferenced `packages` is populated.
	 * 
	 * @param context
	 * @return The content of `packages` or null in case of failure.
	 */
	private static Map<String, PackageInfo> getPackages(Context context) {
		List<android.content.pm.PackageInfo> packagelist = null;

		if (packages == null || packages.get() == null || packages.get().size() == 0) {
			Map<String, PackageInfo> mp = new HashMap<String, PackageInfo>();
			PackageManager pm = context.getPackageManager();
			if (pm == null)
				return null;

			try {
				if (collectSignatures)
					packagelist = pm.getInstalledPackages(PackageManager.GET_SIGNATURES
							| PackageManager.GET_PERMISSIONS);
				else
					packagelist = pm.getInstalledPackages(0);
			} catch (Throwable th) {
				// Forget about it...
			}
			if (packagelist == null)
				return null;
			for (PackageInfo pak : packagelist) {
				if (pak == null || pak.applicationInfo == null || pak.applicationInfo.processName == null)
					continue;
				mp.put(pak.applicationInfo.processName, pak);
			}

			packages = new WeakReference<Map<String, PackageInfo>>(mp);

			if (mp == null || mp.size() == 0)
				return null;
			return mp;
		} else {
			if (packages == null)
				return null;
			Map<String, PackageInfo> p = packages.get();
			if (p == null || p.size() == 0)
				return null;
			return p;
		}
	}

	/**
	 * Get info for a single package from the WeakReferenced packagelist.
	 * 
	 * @param context
	 * @param processName
	 *            The package to get info for.
	 * @return info for a single package from the WeakReferenced packagelist.
	 */
	public static PackageInfo getPackageInfo(Context context, String processName) {
		Map<String, PackageInfo> mp = getPackages(context);
		if (mp == null || !mp.containsKey(processName))
			return null;
		PackageInfo pak = mp.get(processName);
		return pak;
	}

	/**
	 * Returns a list of installed packages on the device. Will be called for
	 * the first Carat sample on a phone, to get signatures for the malware
	 * detection project. Later on, single package information is got by
	 * receiving the package installed intent.
	 * 
	 * @param context
	 * @param filterSystem
	 *            if true, exclude system packages.
	 * @return a list of installed packages on the device.
	 */
	public static Map<String, ProcessInfo> getInstalledPackages(Context context, boolean filterSystem) {
		Map<String, PackageInfo> packageMap = getPackages(context);
		PackageManager pm = context.getPackageManager();
		if (pm == null)
			return null;

		Map<String, ProcessInfo> result = new HashMap<String, ProcessInfo>();

		for (Entry<String, PackageInfo> pentry : packageMap.entrySet()) {
			try {
				String pkg = pentry.getKey();
				PackageInfo pak = pentry.getValue();
				if (pak != null) {
					int vc = pak.versionCode;
					ApplicationInfo appInfo = pak.applicationInfo;
					String label = pm.getApplicationLabel(appInfo).toString();
					// we need application UID to be able to use Android's
					// TrafficStat API
					// in order to get the traffic info of a particular app:
					int appUid = appInfo.uid;
					// get the amount of transmitted and received bytes by an
					// app
					// TODO: disabled for debugging
//					TrafficRecord trafficRecord = getAppTraffic(appUid);

					int flags = pak.applicationInfo.flags;
					// Check if it is a system app
					boolean isSystemApp = (flags & ApplicationInfo.FLAG_SYSTEM) > 0;
					isSystemApp = isSystemApp || (flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) > 0;
					if (filterSystem & isSystemApp)
						continue;
					if (pak.signatures.length > 0) {
						List<String> sigList = getSignatures(pak);
						ProcessInfo pi = new ProcessInfo();
						pi.setPName(pkg);
						pi.setApplicationLabel(label);
						pi.setVersionCode(vc);
						pi.setPId(-1);
						pi.setIsSystemApp(isSystemApp);
						pi.setAppSignatures(sigList);
						pi.setImportance(Constants.IMPORTANCE_NOT_RUNNING);
						pi.setInstallationPkg(pm.getInstallerPackageName(pkg));
						pi.setVersionName(pak.versionName);
						//TODO: disbaled for debugging
//						pi.setTrafficRecord(trafficRecord);
						result.put(pkg, pi);
					}
				}
			} catch (Throwable th) {
				// Forget about it...
			}
		}
		return result;
	}

	/**
	 * Returns info about an installed package. Will be called when receiving
	 * the PACKAGE_ADDED or PACKAGE_REPLACED intent.
	 * 
	 * @param context
	 * @param filterSystem
	 *            if true, exclude system packages.
	 * @return a list of installed packages on the device.
	 */
	public static ProcessInfo getInstalledPackage(Context context, String pkg) {
		PackageManager pm = context.getPackageManager();
		if (pm == null)
			return null;
		PackageInfo pak;
		try {
			pak = pm.getPackageInfo(pkg, PackageManager.GET_SIGNATURES | PackageManager.GET_PERMISSIONS);
		} catch (NameNotFoundException e) {
			return null;
		}
		if (pak == null)
			return null;

		ProcessInfo pi = new ProcessInfo();
		int vc = pak.versionCode;
		ApplicationInfo info = pak.applicationInfo;
		String label = pm.getApplicationLabel(info).toString();
		int flags = pak.applicationInfo.flags;
		// Check if it is a system app
		boolean isSystemApp = (flags & ApplicationInfo.FLAG_SYSTEM) > 0;
		isSystemApp = isSystemApp || (flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) > 0;

		if (pak.signatures.length > 0) {
			List<String> sigList = getSignatures(pak);
			pi.setPName(pkg);
			pi.setApplicationLabel(label);
			pi.setVersionCode(vc);
			pi.setPId(-1);
			pi.setIsSystemApp(isSystemApp);
			pi.setAppSignatures(sigList);
			pi.setImportance(Constants.IMPORTANCE_NOT_RUNNING);
			pi.setInstallationPkg(pm.getInstallerPackageName(pkg));
			pi.setVersionName(pak.versionName);
		}
		return pi;
	}

	/**
	 * Returns a List of ProcessInfo objects, helper for sample.
	 *
	 * @return a List of ProcessInfo objects, helper for sample.
	 */
	public static List<ProcessInfo> getRunningProcessInfoForSample(Context context) {
		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);

		// Reset list for each sample
		runningAppInfo = null;
		List<ProcessInfo> list = getRunningAppInfo(context);
		List<ProcessInfo> result = new ArrayList<ProcessInfo>();

		PackageManager pm = context.getPackageManager();

		Set<String> procs = new HashSet<String>();

		boolean inst = p.getBoolean(Constants.PREFERENCE_SEND_INSTALLED_PACKAGES, true);

		Map<String, ProcessInfo> ipkg = null;
		if (inst)
			ipkg = getInstalledPackages(context, false);

		for (ProcessInfo pi : list) {
			String pname = pi.getPName();
			if (ipkg != null && ipkg.containsKey(pname))
				ipkg.remove(pname);
			procs.add(pname);
			ProcessInfo item = new ProcessInfo();
			PackageInfo pak = getPackageInfo(context, pname);
			if (pak != null) {
				String ver = pak.versionName;
				int vc = pak.versionCode;
				item.setVersionName(ver);
				item.setVersionCode(vc);
				ApplicationInfo info = pak.applicationInfo;

				// Human readable label (if any)
				String label = pm.getApplicationLabel(info).toString();
				if (label.length() > 0){
					item.setApplicationLabel(label);
				}
				int flags = pak.applicationInfo.flags;

				boolean isSystemApp = (flags & ApplicationInfo.FLAG_SYSTEM) > 0;
				isSystemApp = isSystemApp || (flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) > 0;
				item.setIsSystemApp(isSystemApp);
			}
			item.setImportance(pi.getImportance());
			item.setPId(pi.getPId());
			item.setPName(pname);

			String installationSource = null;
			if (!pi.isSystemApp) {
				try {
					installationSource = pm.getInstallerPackageName(pname);
				} catch (IllegalArgumentException iae) {
					Logger.e(STAG, "Could not get installer for " + pname);
				}
			}
			if (installationSource == null)
				installationSource = "null";
			item.setInstallationPkg(installationSource);
			result.add(item);
		}

		// Send installed packages if we were to do so.
		if (ipkg != null && ipkg.size() > 0) {
			result.addAll(ipkg.values());
			p.edit().putBoolean(Constants.PREFERENCE_SEND_INSTALLED_PACKAGES, false).commit();
		}

		// Go through the preferences and look for UNINSTALL, INSTALL and
		// REPLACE keys set by InstallReceiver.
		Set<String> ap = p.getAll().keySet();
		SharedPreferences.Editor e = p.edit();
		boolean edited = false;
		for (String pref : ap) {
			if (pref.startsWith(INSTALLED)) {
				String pname = pref.substring(INSTALLED.length());
				boolean installed = p.getBoolean(pref, false);
				if (installed) {
					Logger.i(STAG, "Installed:" + pname);
					ProcessInfo i = getInstalledPackage(context, pname);
					if (i != null) {
						i.setImportance(Constants.IMPORTANCE_INSTALLED);
						result.add(i);
						e.remove(pref);
						edited = true;
					}
				}
			} else if (pref.startsWith(REPLACED)) {
				String pname = pref.substring(REPLACED.length());
				boolean replaced = p.getBoolean(pref, false);
				if (replaced) {
					Logger.i(STAG, "Replaced:" + pname);
					ProcessInfo i = getInstalledPackage(context, pname);
					if (i != null) {
						i.setImportance(Constants.IMPORTANCE_REPLACED);
						result.add(i);
						e.remove(pref);
						edited = true;
					}
				}
			} else if (pref.startsWith(UNINSTALLED)) {
				String pname = pref.substring(UNINSTALLED.length());
				boolean uninstalled = p.getBoolean(pref, false);
				if (uninstalled) {
					Logger.i(STAG, "Uninstalled:" + pname);
					result.add(uninstalledItem(pname, pref, e));
					edited = true;
				}
			} else if (pref.startsWith(DISABLED)) {
                String pname = pref.substring(DISABLED.length());
                boolean disabled = p.getBoolean(pref, false);
                if (disabled) {
                    Logger.i(STAG, "Disabled app:" + pname);
                    result.add(disabledItem(pname, pref, e));
                    edited = true;
                }
            }
		}
		if (edited)
			e.commit();

		return result;
	}

	/**
	 * Helper to set application to the uninstalled state in the Carat sample.
	 * @param pname the package that was uninstalled.
	 * @param pref The preference that stored the uninstallation directive. This preference will be deleted to ensure uninstallations are not sent multiple times.
	 * @param e the Editor (passed and not created here for efficiency)
	 * @return a new ProcessInfo entry describing the uninstalled item.
	 */
	private static ProcessInfo uninstalledItem(String pname, String pref, SharedPreferences.Editor e) {
		ProcessInfo item = new ProcessInfo();
		item.setPName(pname);
		List<String> sigs = new LinkedList<String>();
		sigs.add("uninstalled");
		item.setAppSignatures(sigs);
		item.setPId(-1);
		item.setImportance(Constants.IMPORTANCE_UNINSTALLED);
		// Remember to remove it so we do not send
		// multiple uninstall events
		e.remove(pref);
		return item;
	}
	
	/**
     * Helper to set application to the disabled state in the Carat sample.
     * @param pname the package that was disabled.
     * @param pref The preference that stored the disabled directive. This preference will be deleted to ensure disabled apps are not sent multiple times.
     * @param e the Editor (passed and not created here for efficiency)
     * @return a new ProcessInfo entry describing the uninstalled item.
     */
    private static ProcessInfo disabledItem(String pname, String pref, SharedPreferences.Editor e) {
        ProcessInfo item = new ProcessInfo();
        item.setPName(pname);
        item.setPId(-1);
        item.setImportance(Constants.IMPORTANCE_DISABLED);
        // Remember to remove it so we do not send
        // multiple uninstall events
        e.remove(pref);
        return item;
    }

	/**
	 * Depratecated, use int[] meminfo = readMemInfo(); int totalMemory =
	 * meminfo[0] + meminfo[1];
	 */
	@Deprecated
	public static String getMemoryInfo() {
		String tmp = null;
		BufferedReader br = null;

		try {
			File file = new File("/proc/meminfo");
			FileInputStream in = new FileInputStream(file);
			br = new BufferedReader(new InputStreamReader(in), READ_BUFFER_SIZE);

		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			tmp = br.readLine();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		StringBuilder sMemory = new StringBuilder();
		sMemory.append(tmp);

		try {
			tmp = br.readLine();
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		sMemory.append("\n").append(tmp).append("\n");
		String result = "Memery Status:\n" + sMemory;
		return result;

	}

	/*
	 * Deprecated, use readMemInfo()[1]
	 */
	@Deprecated
	public static String getMemoryFree() {
		String tmp = null;
		BufferedReader br = null;

		try {
			File file = new File("/proc/meminfo");
			FileInputStream in = new FileInputStream(file);
			br = new BufferedReader(new InputStreamReader(in), READ_BUFFER_SIZE);

		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			tmp = br.readLine();
			tmp = br.readLine();
			if (tmp != null) {
				// split by whitespace and take 2nd element, so that in:
				// MemoryFree: x kb
				// the x remains.
				String[] arr = tmp.split("\\s+");
				if (arr.length > 1)
					tmp = arr[1];
			}
			br.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return tmp;
	}

	/**
	 * @return Real time in seconds since last boot
	 */
	public static double getUptime() {
		long uptime = SystemClock.elapsedRealtime();
		return TimeUnit.MILLISECONDS.toSeconds(uptime);
	}

	/**
	 * @return CPU sleep time in seconds
     */
	public static double getSleepTime(){
		long active = SystemClock.uptimeMillis();
		long real = SystemClock.elapsedRealtime();
		long sleep = real-active;
		if(sleep < 0) return 0; // Just in case
		return TimeUnit.MILLISECONDS.toSeconds(sleep);
	}

	public static String getNetworkStatusForSample(Context context){
		String network = getNetworkStatus(context);
		String networkType = getNetworkType(context);
		String mobileNetworkType = getMobileNetworkType(context);

		// Required in new Carat protocol
		if (network.equals(NETWORKSTATUS_CONNECTED)) {
			if (networkType.equals("WIFI"))
				return networkType;
			else
				return mobileNetworkType;
		} else
			return network;
	}

	/**
	 * Get the network status, one of connected, disconnected, connecting, or disconnecting.
	 * @param context the Context.
	 * @return the network status, one of connected, disconnected, connecting, or disconnecting.
	 */
	public static String getNetworkStatus(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm == null)
			return NETWORKSTATUS_DISCONNECTED;
		NetworkInfo i = cm.getActiveNetworkInfo();
		if (i == null)
			return NETWORKSTATUS_DISCONNECTED;
		NetworkInfo.State s = i.getState();
		if (s == NetworkInfo.State.CONNECTED)
			return NETWORKSTATUS_CONNECTED;
		if (s == NetworkInfo.State.DISCONNECTED)
			return NETWORKSTATUS_DISCONNECTED;
		if (s == NetworkInfo.State.CONNECTING)
			return NETWORKSTATUS_CONNECTING;
		if (s == NetworkInfo.State.DISCONNECTING)
			return NETWORKSTATUS_DISCONNECTING;
		else
			return NETWORKSTATUS_DISCONNECTED;
	}

	
	/**
	 * Get the network type, for example Wifi, mobile, wimax, or none.
	 * 
	 * @param context
	 * @return
	 */
	public static String getNetworkType(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if (cm == null)
			return TYPE_UNKNOWN;
		NetworkInfo i = cm.getActiveNetworkInfo();
		if (i == null)
			return TYPE_UNKNOWN;
		return i.getTypeName();
	}

	/**
	 * Returns true if the Internet is reachable.
	 * @param c the Context
	 * @return true if the Internet is reachable.
	 */
	public static boolean networkAvailable(Context context) {
		String network = getNetworkStatus(context);
		return network.equals(NETWORKSTATUS_CONNECTED);
	}

	/* Get current WiFi signal Strength */
	public static int getWifiSignalStrength(Context context) {
		WifiManager myWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo myWifiInfo = myWifiManager.getConnectionInfo();
		int wifiRssi = myWifiInfo.getRssi();
		// Log.v("WifiRssi", "Rssi:" + wifiRssi);
		return wifiRssi;

	}

	/**
	 * Get Wifi MAC ADDR. Hashed and used in UUID calculation.
	 */
	private static String getWifiMacAddress(Context context) {
		WifiManager myWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		if (myWifiManager == null)
			return null;
		WifiInfo myWifiInfo = myWifiManager.getConnectionInfo();
		if (myWifiInfo == null)
			return null;
		return myWifiInfo.getMacAddress();
	}

	/* Get current WiFi link speed */
	public static int getWifiLinkSpeed(Context context) {
		WifiManager myWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo myWifiInfo = myWifiManager.getConnectionInfo();
		int linkSpeed = myWifiInfo.getLinkSpeed();

		// Log.v("linkSpeed", "Link speed:" + linkSpeed);
		return linkSpeed;
	}

	/* Check whether WiFi is enabled */
	public static boolean getWifiEnabled(Context context) {
		boolean wifiEnabled = false;

		WifiManager myWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		wifiEnabled = myWifiManager.isWifiEnabled();
		// Log.v("WifiEnabled", "Wifi is enabled:" + wifiEnabled);
		return wifiEnabled;
	}

	/* Get Wifi state: */
	public static String getWifiState(Context context) {
		WifiManager myWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		int wifiState = myWifiManager.getWifiState();
		switch (wifiState) {
		case WifiManager.WIFI_STATE_DISABLED:
			return WIFI_STATE_DISABLED;
		case WifiManager.WIFI_STATE_DISABLING:
			return WIFI_STATE_DISABLING;
		case WifiManager.WIFI_STATE_ENABLED:
			return WIFI_STATE_ENABLED;
		case WifiManager.WIFI_STATE_ENABLING:
			return WIFI_STATE_ENABLING;
		default:
			return WIFI_STATE_UNKNOWN;
		}
	}

	public static WifiInfo getWifiInfo(Context context) {
		WifiManager myWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		WifiInfo connectionInfo = myWifiManager.getConnectionInfo();
		// Log.v("WifiInfo", "Wifi information:" + connectionInfo);
		return connectionInfo;

	}

	/*
	 * This method is deprecated. As of ICE_CREAM_SANDWICH, availability of
	 * background data depends on several combined factors, and this method will
	 * always return true. Instead, when background data is unavailable,
	 * getActiveNetworkInfo() will now appear disconnected.
	 */
	/* Check whether background data are enabled */
	@Deprecated
	public static boolean getBackgroundDataEnabled(Context context) {
		boolean bacDataEnabled = false;
		try {
			if (Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.BACKGROUND_DATA) == 1) {
				bacDataEnabled = true;
			}
		} catch (SettingNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Log.v("BackgroundDataEnabled", "Background data enabled? " +
		// bacDataEnabled);
		// return bacDataEnabled;
		return true;
	}

	/* Get Current Screen Brightness Value */
	public static int getScreenBrightness(Context context) {
		if(isAutoBrightness(context)){
			return -1;
		}
		int screenBrightnessValue = 0;
		try {
			screenBrightnessValue = android.provider.Settings.System.getInt(context.getContentResolver(),
					android.provider.Settings.System.SCREEN_BRIGHTNESS);
		} catch (SettingNotFoundException e) {
			e.printStackTrace();
		}
		return screenBrightnessValue;
	}

	public static boolean isAutoBrightness(Context context) {
		boolean autoBrightness = false;
		try {
			autoBrightness = Settings.System.getInt(context.getContentResolver(),
					Settings.System.SCREEN_BRIGHTNESS_MODE) == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
		} catch (SettingNotFoundException e) {
			e.printStackTrace();
		}
		return autoBrightness;
	}

	/* Check whether GPS are enabled */
	public static boolean getGpsEnabled(Context context) {
		boolean gpsEnabled = false;
		LocationManager myLocationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

		gpsEnabled = myLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
		// Log.v("GPS", "GPS is :" + gpsEnabled);
		return gpsEnabled;
	}

	// This value gets updated in IntentRouter, look there for implementation
	public static long getDistanceTraveled(Context context){
		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
		long distanceTraveled = p.getLong("distanceTraveled", 0);
		p.edit().putLong("distanceTraveled", 0).apply(); // Reset the counter
		return distanceTraveled;
	}

	public static Location getLastKnownLocation(Context c) {
		String provider = getBestProvider(c);
		if (provider != null && !provider.equals("gps")) {
			return getLastKnownLocation(c, provider);
		}
		return null;
	}

	private static Location getLastKnownLocation(Context context, String provider) {
		LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		return lm.getLastKnownLocation(provider);
	}

	/* Get the distance users between two locations */
	public static double getDistance(double startLatitude, double startLongitude, double endLatitude,
			double endLongitude) {
		float[] results = new float[1];
		Location.distanceBetween(startLatitude, startLongitude, endLatitude, endLongitude, results);
		return results[0];
	}

	/**
	 * Get location as coarse latitude and longitude.
	 * IMPORTANT: Use only with user's consent!
	 * @param context Application context
	 * @return Comma-separated latitude and longitude
	 */
	public static String getCoarseLocation(Context context) {
		String provider = getBestProvider(context);
		double latitude, longitude;
		LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		if (provider != null && !provider.equals("gps")) {
			Location location = lm.getLastKnownLocation(provider);
			try {
				latitude = location.getLatitude();
				longitude = location.getLongitude();
				return String.valueOf(latitude)+","+String.valueOf(longitude);
			} catch (Throwable th){
				if(Constants.DEBUG){
					Logger.d("SamplingLibrary", "Failed getting coarse location!");
					th.printStackTrace();
				}
			}
		}
		return "Unknown";
	}

	/**
	 * Return a list of enabled LocationProviders, such as GPS, Network, etc.
	 * 
	 * @param context
	 *            from onReceive or app.
	 * @return
	 */
	public static List<String> getEnabledLocationProviders(Context context) {
		LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		return lm.getProviders(true);
	}

	public static String getBestProvider(Context context) {
		LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
		Criteria c = new Criteria();
		c.setAccuracy(Criteria.ACCURACY_COARSE);
		c.setPowerRequirement(Criteria.POWER_LOW);
		String provider = lm.getBestProvider(c, true);
		return provider;
	}

	private static String getDeviceId(Context context) {
		TelephonyManager telManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		if (telManager == null)
			return null;
		return telManager.getDeviceId();
	}

	/* Get network type */
	public static String getMobileNetworkType(Context context) {
		TelephonyManager telManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

		int netType = telManager.getNetworkType();
		switch (netType) {
		case TelephonyManager.NETWORK_TYPE_1xRTT:
			return NETWORK_TYPE_1xRTT;
		case TelephonyManager.NETWORK_TYPE_CDMA:
			return NETWORK_TYPE_CDMA;
		case TelephonyManager.NETWORK_TYPE_EDGE:
			return NETWORK_TYPE_EDGE;
		case EHRPD:
			return NETWORK_TYPE_EHRPD;
		case TelephonyManager.NETWORK_TYPE_EVDO_0:
			return NETWORK_TYPE_EVDO_0;
		case TelephonyManager.NETWORK_TYPE_EVDO_A:
			return NETWORK_TYPE_EVDO_A;
		case EVDO_B:
			return NETWORK_TYPE_EVDO_B;
		case TelephonyManager.NETWORK_TYPE_GPRS:
			return NETWORK_TYPE_GPRS;
		case TelephonyManager.NETWORK_TYPE_HSDPA:
			return NETWORK_TYPE_HSDPA;
		case TelephonyManager.NETWORK_TYPE_HSPA:
			return NETWORK_TYPE_HSPA;
		case HSPAP:
			return NETWORK_TYPE_HSPAP;
		case TelephonyManager.NETWORK_TYPE_HSUPA:
			return NETWORK_TYPE_HSUPA;
		case TelephonyManager.NETWORK_TYPE_IDEN:
			return NETWORK_TYPE_IDEN;
		case LTE:
			return NETWORK_TYPE_LTE;
		case TelephonyManager.NETWORK_TYPE_UMTS:
			return NETWORK_TYPE_UMTS;
		default:
			// If we don't know the type, just return the number and let the
			// backend take care of it
			return netType + "";
		}
	}

	/* Get Phone Type */
	public static String getPhoneType(Context context) {
		TelephonyManager telManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

		int phoneType = telManager.getPhoneType();
		switch (phoneType) {
		case TelephonyManager.PHONE_TYPE_CDMA:
			return PHONE_TYPE_CDMA;
		case TelephonyManager.PHONE_TYPE_GSM:
			return PHONE_TYPE_GSM;
		default:
			return PHONE_TYPE_NONE;
		}
	}

	/* Check is it network roaming */
	public static boolean getRoamingStatus(Context context) {
		boolean roamStatus = false;

		TelephonyManager telManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

		roamStatus = telManager.isNetworkRoaming();
		// Log.v("RoamingStatus", "Roaming status:" + roamStatus);
		return roamStatus;
	}

	/* Get data state */
	public static String getDataState(Context context) {
		TelephonyManager telManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

		int dataState = telManager.getDataState();
		switch (dataState) {
		case TelephonyManager.DATA_CONNECTED:
			return DATA_CONNECTED;
		case TelephonyManager.DATA_CONNECTING:
			return DATA_CONNECTING;
		case TelephonyManager.DATA_DISCONNECTED:
			return DATA_DISCONNECTED;
		default:
			return DATA_SUSPENDED;
		}
	}

	/* Get data activity */
	public static String getDataActivity(Context context) {
		TelephonyManager telManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

		int dataActivity = telManager.getDataActivity();
		switch (dataActivity) {
		case TelephonyManager.DATA_ACTIVITY_IN:
			return DATA_ACTIVITY_IN;
		case TelephonyManager.DATA_ACTIVITY_OUT:
			return DATA_ACTIVITY_OUT;
		case TelephonyManager.DATA_ACTIVITY_INOUT:
			return DATA_ACTIVITY_INOUT;
		default:
			return DATA_ACTIVITY_NONE;
		}
	}
	/**
	 * Get whether the screen is on or off.
	 * 
	 * @return true if the screen is on.
	 */
	public static int isScreenOn(Context context) {
		PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		if (powerManager != null) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT_WATCH) {
				return powerManager.isInteractive() ? 1 : 0;
			} else {
				return powerManager.isScreenOn() ? 1 : 0;
			}
		}
		return 0;
	}

	/**
	 * Get the current timezone of the device.
	 */

	public static String getTimeZone(Context context) {
		Calendar cal = Calendar.getInstance();
		TimeZone tz = cal.getTimeZone();
		return tz.getID();
	}

	/**
	 * 
	 * @param context
	 * @return true when app installation from unknown sources is enabled.
	 */
	public static int allowUnknownSources(Context context) {
		ContentResolver res = context.getContentResolver();
		int unknownSources = Settings.Secure.getInt(res, Settings.Secure.INSTALL_NON_MARKET_APPS, 0);
		return unknownSources;
	}

	/**
	 * 
	 * @param context
	 * @return true when developer mode is enabled.
	 */
	public static int isDeveloperModeOn(Context context) {
		ContentResolver res = context.getContentResolver();
		int adb = Settings.Secure.getInt(res, Settings.Secure.ADB_ENABLED, 0);
		// In API level 17, this is Settings.Global.ADB_ENABLED.
		return adb;
	}

	/*
	 * TODO: Make the app running when the system reboots, and provide a stop
	 * button. CPU and Memory info per application CPU core/ frequency, CPU
	 * governors usage How to motivate user to upload more samples.
	 */

	/**
	 * Safely terminate (kill) the given app.
	 * 
	 * @param context
	 * @param packageName
	 * @param label
	 * @return
	 */
	public static boolean killApp(Context context, String packageName, String label) {
		ActivityManager am = (ActivityManager) context.getSystemService(Activity.ACTIVITY_SERVICE);
		if (am != null) {
			try {
				PackageInfo p = getPackageInfo(context, packageName);
				// Log.v(STAG, "Trying to kill proc=" + packageName + " pak=" +
				// p.packageName);
				FlurryAgent.logEvent("Killing app=" + (label == null ? "null" : label) + " proc=" + packageName
						+ " pak=" + (p == null ? "null" : p.packageName));
				am.killBackgroundProcesses(packageName);
				/*Toast.makeText(context, context.getResources().getString(R.string.stopping) + ((label == null) ? "" : " "+label),
						Toast.LENGTH_SHORT).show();*/
				resetRunningProcessInfo();
				return true;
			} catch (Throwable th) {
				Toast.makeText(context,  context.getResources().getString(R.string.stopping_failed),
						Toast.LENGTH_SHORT).show();
				Logger.e(STAG, "Could not kill process: " + packageName, th);
			}
		}
		return false;
	}

	/**
	 * Open application manager
	 *
	 * @param context
	 * @param packageName
     * @return
     */
	public static boolean openAppManager(Context context, String packageName){
		Intent intent = new Intent();
		// Use this in case of frequent AndroidRuntimeExceptions
		// intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		final int sdkLevel = Build.VERSION.SDK_INT;

		// API levels greater or equal than 9
		if (sdkLevel >= Build.VERSION_CODES.GINGERBREAD) {
			intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
			Uri uri = Uri.fromParts("package", packageName, null);
			intent.setData(uri);
		} else {
			//API levels 8 and below
			final String extension = (sdkLevel == Build.VERSION_CODES.FROYO
					? "pkg"
					: "com.android.settings.ApplicationPkgName");
			intent.setAction(Intent.ACTION_VIEW);
			intent.setClassName(
					"com.android.settings",
					"com.android.settings.InstalledAppDetails"
			);
			//Send information to activity
			intent.putExtra(extension, packageName);
		}
		try{
			context.startActivity(intent);
			/*Toast.makeText(context,  context.getResources().getString(R.string.opening_manager),
					Toast.LENGTH_SHORT).show();*/
		} catch(Exception e){
			if(e.getLocalizedMessage() != null){
				Toast.makeText(context,  context.getResources().getString(R.string.opening_manager_failed),
						Toast.LENGTH_SHORT).show();
				Log.v("error", "Failed to open application manager", e);
			}
			return false;
		}
		return true;
	}

	/**
	 * Get highest priority process for the package
	 * @param c
	 * @param packageName
     * @return
     */
	public static String getAppPriority(Context c, String packageName){
		List<RunningAppProcessInfo> processInfos = getRunningProcessInfo(c);
		List<RunningServiceInfo> serviceInfos = getRunningServiceInfo(c);
		int highestPriority = Integer.MAX_VALUE;

		// Check if there are running services for the package
		for(RunningServiceInfo si : serviceInfos) {
			if(si.service.getPackageName().equals(packageName)){
				highestPriority = RunningAppProcessInfo.IMPORTANCE_SERVICE;
			}

		}
		// Check if there are running processes for the package
		for(RunningAppProcessInfo pi : processInfos){
			if(Arrays.asList(pi.pkgList).contains(packageName)) {
				if(pi.importance < highestPriority){
					highestPriority = pi.importance;
				}
			}
		}
		String importance = CaratApplication.importanceString(highestPriority);
		return CaratApplication.translatedPriority(importance);
	}

	private static String convertToHex(byte[] data) {
		StringBuilder buf = new StringBuilder();
		for (byte b : data) {
			int halfbyte = (b >>> 4) & 0x0F;
			int two_halfs = 0;
			do {
				buf.append((0 <= halfbyte) && (halfbyte <= 9) ? (char) ('0' + halfbyte)
						: (char) ('a' + (halfbyte - 10)));
				halfbyte = b & 0x0F;
			} while (two_halfs++ < 1);
		}
		return buf.toString();
	}

	// TODO: This should be removed in favor of Sampler2.sample()
	public static Sample sample(Context context, String action, String lastBatteryState) {
		final String TAG = "SamplingLibrary.sample";
		if (Constants.DEBUG)
		    Logger.d(TAG, "sample() was invoked.");

		if (Constants.DEBUG)
		    Logger.d(TAG, "action = " + action);

		// Construct sample and return it in the end
		Sample mySample = new Sample();
		SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
		String uuId = p.getString(CaratApplication.getRegisteredUuid(), null);
		mySample.setUuId(uuId);
		mySample.setTriggeredBy(action);
		// required always
		long now = System.currentTimeMillis();
		mySample.setTimestamp(now / 1000.0);

		// Record first data point for CPU usage
		long[] idleAndCpu1 = readUsagePoint();
		List<ProcessInfo> processes = getRunningProcessInfoForSample(context);
		mySample.setPiList(processes);
		// }

		int screenBrightness = SamplingLibrary.getScreenBrightness(context);
		mySample.setScreenBrightness(screenBrightness);
		boolean autoScreenBrightness = SamplingLibrary.isAutoBrightness(context);
		if (autoScreenBrightness)
			mySample.setScreenBrightness(-1); // Auto
		// boolean gpsEnabled = SamplingLibrary.getGpsEnabled(context);
		// Location providers
		List<String> enabledLocationProviders = SamplingLibrary.getEnabledLocationProviders(context);
		mySample.setLocationProviders(enabledLocationProviders);

		// TODO: not in Sample yet
		// int maxNumSatellite = SamplingLibrary.getMaxNumSatellite(context);

		String network = SamplingLibrary.getNetworkStatus(context);
		String networkType = SamplingLibrary.getNetworkType(context);
		String mobileNetworkType = SamplingLibrary.getMobileNetworkType(context);

		// Required in new Carat protocol
		if (network.equals(NETWORKSTATUS_CONNECTED)) {
			if (networkType.equals("WIFI"))
				mySample.setNetworkStatus(networkType);
			else
				mySample.setNetworkStatus(mobileNetworkType);
		} else
			mySample.setNetworkStatus(network);

		// String ns = mySample.getNetworkStatus();
		// Logger.d(STAG, "Set networkStatus="+ns);

		// Network details
		NetworkDetails nd = new NetworkDetails();

		// Network type
		nd.setNetworkType(networkType);
		nd.setMobileNetworkType(mobileNetworkType);
		boolean roamStatus = SamplingLibrary.getRoamingStatus(context);
		nd.setRoamingEnabled(roamStatus);
		String dataState = SamplingLibrary.getDataState(context);
		nd.setMobileDataStatus(dataState);
		String dataActivity = SamplingLibrary.getDataActivity(context);
		nd.setMobileDataActivity(dataActivity);
		String simOperator = SamplingLibrary.getSIMOperator(context);
		nd.setSimOperator(simOperator);
		String networkOperator = SamplingLibrary.getNetworkOperator(context);
		nd.setNetworkOperator(networkOperator);
		String mcc = SamplingLibrary.getMcc(context);
		nd.setMcc(mcc);
		String mnc = SamplingLibrary.getMnc(context);
		nd.setMnc(mnc);

		// Wifi stuff
		String wifiState = SamplingLibrary.getWifiState(context);
		nd.setWifiStatus(wifiState);
		int wifiSignalStrength = SamplingLibrary.getWifiSignalStrength(context);
		nd.setWifiSignalStrength(wifiSignalStrength);
		int wifiLinkSpeed = SamplingLibrary.getWifiLinkSpeed(context);
		nd.setWifiLinkSpeed(wifiLinkSpeed);
		String wifiApStatus = SamplingLibrary.getWifiHotspotState(context);
		nd.setWifiApStatus(wifiApStatus);

		mySample.setNetworkDetails(nd);

		// Battery details
		Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		if(intent == null){
			return null;
		}

		int health = intent.getIntExtra(BatteryManager.EXTRA_HEALTH, 0);
		int status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, 0);
		int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
		int temperature = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10;
		double batteryLevel = BatteryUtils.getBatteryLevel(intent) / 100.0;
		double voltage = intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000;
		String batteryTechnology = intent.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY);

		// FIXED: Not used yet, Sample needs more fields
		String batteryHealth = "Unknown";
		String batteryStatus;

		switch (health) {
			case BatteryManager.BATTERY_HEALTH_DEAD:
				batteryHealth = "Dead";
				break;
			case BatteryManager.BATTERY_HEALTH_GOOD:
				batteryHealth = "Good";
				break;
			case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
				batteryHealth = "Over voltage";
				break;
			case BatteryManager.BATTERY_HEALTH_OVERHEAT:
				batteryHealth = "Overheat";
				break;
			case BatteryManager.BATTERY_HEALTH_UNKNOWN:
				batteryHealth = "Unknown";
				break;
			case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
				batteryHealth = "Unspecified failure";
				break;
		}

		switch (status) {
			case BatteryManager.BATTERY_STATUS_CHARGING:
				batteryStatus = "Charging";
				break;
			case BatteryManager.BATTERY_STATUS_DISCHARGING:
				batteryStatus = "Discharging";
				break;
			case BatteryManager.BATTERY_STATUS_FULL:
				batteryStatus = "Full";
				break;
			case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
				batteryStatus = "Not charging";
				break;
			case BatteryManager.BATTERY_STATUS_UNKNOWN:
				batteryStatus = "Unknown";
				break;
			default:
				batteryStatus = lastBatteryState != null ? lastBatteryState : "Unknown";
		}

		String batteryCharger = "unplugged";
		switch (plugged) {
			case BatteryManager.BATTERY_PLUGGED_AC:
				batteryCharger = "ac";
				break;
			case BatteryManager.BATTERY_PLUGGED_USB:
				batteryCharger = "usb";
				break;
		}

		BatteryDetails battery = new BatteryDetails();
		// otherInfo.setCPUIdleTime(totalIdleTime);

		// IMPORTANT: All of the battery details fields were never set (=always
		// zero), like the last battery level.
		// Now all must have been fixed.

		// current battery temperature in degrees Centigrade (the unit of the
		// temperature value
		// (returned by BatteryManager) is not Centigrade, it should be divided
		// by 10)
		battery.setBatteryTemperature(temperature);
		// otherInfo.setBatteryTemperature(temperature);

		// current battery voltage in VOLTS (the unit of the returned value by
		// BatteryManager is millivolts)
		battery.setBatteryVoltage(voltage);
		// otherInfo.setBatteryVoltage(voltage);
		battery.setBatteryTechnology(batteryTechnology);
		battery.setBatteryCharger(batteryCharger);
		battery.setBatteryHealth(batteryHealth);
		mySample.setBatteryDetails(battery);
		mySample.setBatteryLevel(batteryLevel);
		mySample.setBatteryState(batteryStatus);

		// Memory statistics
		int[] usedFreeActiveInactive = SamplingLibrary.readMeminfo();
		if (usedFreeActiveInactive != null && usedFreeActiveInactive.length == 4) {
			mySample.setMemoryUser(usedFreeActiveInactive[0]);
			mySample.setMemoryFree(usedFreeActiveInactive[1]);
			mySample.setMemoryActive(usedFreeActiveInactive[2]);
			mySample.setMemoryInactive(usedFreeActiveInactive[3]);
		}

		// Record second data point for cpu/idle time
		long[] idleAndCpu2 = readUsagePoint();

		// CPU status
		CpuStatus cs = new CpuStatus();
		double uptime = getUptime();
		double sleep = getSleepTime();
		cs.setCpuUsage(getUsage(idleAndCpu1, idleAndCpu2));
		cs.setUptime(uptime);
		cs.setSleeptime(sleep);
		mySample.setCpuStatus(cs);

		// Storage details
		mySample.setStorageDetails(getStorageDetails());

		// System settings
		edu.berkeley.cs.amplab.carat.thrift.Settings settings = new edu.berkeley.cs.amplab.carat.thrift.Settings();
		settings.setBluetoothEnabled(getBluetoothEnabled());
		mySample.setSettings(settings);

		// Other fields
		mySample.setDeveloperMode(isDeveloperModeOn(context));
		mySample.setUnknownSources(allowUnknownSources(context));
		mySample.setScreenOn(isScreenOn(context));
		mySample.setTimeZone(getTimeZone(context));
		mySample.setCountryCode(getCountryCode(context));

		// If there are extra fields, include them into the sample.
		List<Feature> extras = getExtras(context);
		if (extras != null && extras.size() > 0)
			mySample.setExtra(extras);

		if(Constants.DEBUG){
			// Need to split since output is over 1000 characters
			Logger.d("debug", "Created the following sample:");
			String sampleString = mySample.toString();
			int limit = 1000;
			for(int i = 0; i <= sampleString.length() / limit; i++) {
				int start = i * limit;
				int end = (i+1) * limit;
				end = (end > sampleString.length()) ? sampleString.length() : end;
				Logger.d("debug", sampleString.substring(start, end));
			}
		}

		lastSampledBatteryLevel = batteryLevel;
		return mySample;
	}

	public static Intent getLastBatteryIntent(Context context){
		return context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
	}

	/**
	 * Returns numeric mobile country code.
	 * @param context Application context
	 * @return 3-digit country code
     */
	public static String getMcc(Context context){
		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		String networkOperator = telephonyManager.getNetworkOperator();
		if(networkOperator != null && networkOperator.length() >= 5) {
			return networkOperator.substring(0, 3);
		}
		String operatorProperty = "gsm.operator.numeric";
		if(telephonyManager.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) {
			operatorProperty = "ro.cdma.home.operator.numeric"; // CDMA
		}
		networkOperator = getStringFromSystemProperty(context, operatorProperty);
		if(networkOperator != null && networkOperator.length() >= 5){
			return networkOperator.substring(0, 3);
		}
		return "Unknown";
	}

	public static int getBatteryChargeCounter(Context context ){
		if(Build.VERSION.SDK_INT >= 21){
			BatteryManager mBatteryManager = (BatteryManager) context.getSystemService(Context.BATTERY_SERVICE);
			return mBatteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
		}
		return -1;
	}

	public static double getBatteryCapacity(Context context) {
		try {
			// Please note: Uses reflection, API not available on all devices
			Class<?> powerProfile = Class.forName("com.android.internal.os.PowerProfile");
			Object mPowerProfile = powerProfile.getConstructor(Context.class).newInstance(context);
			Method getAveragePower = powerProfile.getMethod("getAveragePower", String.class);
			getAveragePower.setAccessible(true);
			return ((double) getAveragePower.invoke(mPowerProfile, "battery.capacity"));
		} catch(Throwable th){
			th.printStackTrace();
			return -1;
		}
	}

	public static double getBatteryVoltage(Context context) {
		Intent receiver = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
		if(receiver == null){
			return -1;
		}
		double voltage = receiver.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
		if(voltage == -1){
			return voltage;
		}
		return voltage / 1000; // Convert mv to V
	}

	/**
	 * Returns numeric mobile network code.
	 * @param context Application context
	 * @return 2-3 digit network code
     */
	public static String getMnc(Context context){
		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		String networkOperator = telephonyManager.getNetworkOperator();
		if(networkOperator != null && networkOperator.length() >= 5) {
			return networkOperator.substring(3);
		}
		String operatorProperty = "gsm.operator.numeric";
		if(telephonyManager.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) {
			operatorProperty = "ro.cdma.home.operator.numeric"; // CDMA
		}
		networkOperator = getStringFromSystemProperty(context, operatorProperty);
		if(networkOperator != null && networkOperator.length() >= 5){
			return networkOperator.substring(3);
		}
		return "Unknown";
	}

	/**
	 * Returns a two letter ISO3166-1 alpha-2 standard country code
	 * https://en.wikipedia.org/wiki/ISO_3166-1
     * GetCellLocation returns longitude and latitude
 	 * @param context Application context
	 * @return Two letter country code
     */
	public static String getCountryCode(Context context) {
		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        String cc;
        if(telephonyManager.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA){
            /// Network location is most accurate
            cc = telephonyManager.getNetworkCountryIso();
            if(cc != null && cc.length() == 2) return cc;
            cc = getCountryCodeFromProperty(context, "gsm.operator.numeric");
            if(cc != null && cc.length() == 2) return cc;
            cc = telephonyManager.getSimCountryIso();
            if(cc != null && cc.length() == 2) return cc;
        } else {
            // Telephony manager is unreliable with CDMA
            cc = getCountryCodeFromProperty(context, "ro.cdma.home.operator.numeric");
            if(cc != null && cc.length() == 2) return cc;
        }
        return "Unknown";
	}

	/**
	 * @param context Application context
	 * @return Wifi access point state
     */
	public static String getWifiHotspotState(Context context){
		try {
			int state = getWifiApState(context);
			switch(state){
				case WIFI_AP_STATE_DISABLED: return "disabled";
				case WIFI_AP_STATE_DISABLING: return "disabling";
				case WIFI_AP_STATE_ENABLED: return "enabled";
				case WIFI_AP_STATE_ENABLING: return "enabling";
				case WIFI_AP_STATE_FAILED: return "failed";
				default: return "unknown";
			}
		} catch(Exception e){
			return "unknown";
		}
	}

	/**
	 * SIM Operator is responsible for the product that is subscription.
	 * It is directly associated with the SIM card and remains the same
	 * even when changing between physical networks.
	 *
	 * SIM Operator might or might not own the infrastructure in use.
	 * NOTE: Getting multiple operators is highly experimental.
	 *
	 * @param context Application context
	 * @return SIM Operator name
     */
	public static String getSIMOperator(Context context){
		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		String operator;

		operator = getSIMOperators(context); // Supports multiple sim cards
		if(operator != null && operator.length() > 0) return operator;
		operator = telephonyManager.getSimOperatorName();
		if(operator != null && operator.length() > 0) return operator;

		return "unknown";
	}

	/**
	 * Network operator is responsible for the network infrastructure which
	 * might be used by many virtual network operators. Network operator
	 * is not necessarily bound to the device and might change at any time.
	 *
	 * @param context Application context
	 * @return Network operator name, aka. carrier
	 */
	public static String getNetworkOperator(Context context){
		TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		String operator;

		operator = getNetworkOperators(context);
		if(operator != null && operator.length() != 0) return operator;
		operator = telephonyManager.getNetworkOperatorName();
		if(operator != null && operator.length() != 0) return operator;
		operator = getStringFromSystemProperty(context, "ro.cdma.home.operator.alpha"); // CDMA support
		if(operator != null && operator.length() != 0) return operator;

		return "unknown";
	}


	/**
	 * Experimental call to retrieve sim operator names by subscription ids.
	 * @param context Application context
	 * @return SIM operator name/names with ";" as a delimiter for many.
     */
	private static String getSIMOperators(Context context){
		String operators = "";
		if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1){
			List<SubscriptionInfo> subscriptions = SubscriptionManager.from(context).getActiveSubscriptionInfoList();
			if(subscriptions != null && subscriptions.size() > 0){
				for(SubscriptionInfo info : subscriptions){
					int subId = info.getSubscriptionId();
					String operator = getSimOperatorNameForSubscription(context, subId);
					if(operator != null && operator.length() > 0){
						operators += operator + ";";
					}
				}
				// Remove last delimiter
				if(operators.length() > 1){
					operators = operators.substring(0, operators.length()-1);
				}
			}
		}
		return operators;
	}

	/**
	 * Retrieves network operator names from subscription manager.
	 * NOTE: Requires SDK level 22 or above
	 * @param context
	 * @return
     */
	private static String getNetworkOperators(Context context){
		String operator = "";
		if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1){
			SubscriptionManager subscriptionManager = SubscriptionManager.from(context);
			if(subscriptionManager != null){
				List<SubscriptionInfo> subscriptions = subscriptionManager.getActiveSubscriptionInfoList();
				if(subscriptions != null){
					for(SubscriptionInfo info : subscriptions){
						CharSequence carrierName = info.getCarrierName();
						if(carrierName != null && carrierName.length() > 0){
							operator += carrierName + ";";
						}
					}
					// Remove last delimiter
					if(operator.length() >= 1){
						operator = operator.substring(0, operator.length()-1);
					}
				}
			}
		}
		return operator;
	}

	/**
	 * Storage details for internal, external, secondary and system partitions.
	 * External and secondary storage details are not exactly reliable.
	 * @return Thrift-compatible StorageDetails object
     */
	public static StorageDetails getStorageDetails(){
		StorageDetails sd = new StorageDetails();

		// Internal
		File path = Environment.getDataDirectory();
		long[] internal = getStorageDetailsForPath(path);
		if(internal.length == 2){
			sd.setFree((int)internal[0]);
			sd.setTotal((int)internal[1]);
		}

		// External
		long[] external = getExternalStorageDetails();
		if(external.length == 2){
			sd.setFreeExternal((int)external[0]);
			sd.setTotalExternal((int)external[1]);
		}

		// Secondary
		long[] secondary = getSecondaryStorageDetails();
		if(secondary.length == 2){
			sd.setFreeSecondary((int)secondary[0]);
			sd.setTotalSecondary((int)secondary[1]);
		}

		// System
		path = Environment.getRootDirectory();
		long[] system = getStorageDetailsForPath(path);
		if(system.length == 2){
			sd.setFreeSystem((int)system[0]);
			sd.setTotalSystem((int)system[1]);
		}

		return sd;
	}

	/**
	 * Returns free and total external storage space
	 * @return Two values as a pair or none
     */
	private static long[] getExternalStorageDetails(){
		File path = getStoragePathFromEnv("EXTERNAL_STORAGE");
		if(path != null && path.exists()){
			long[] storage = getStorageDetailsForPath(path);
			if(storage.length == 2) return storage;
		}

		// Make sure external storage isn't a secondary device
		if(!isExternalStorageRemovable() || isExternalStorageEmulated()){
			path = Environment.getExternalStorageDirectory();
			if(path != null && path.exists()){
				long[] storage = getStorageDetailsForPath(path);
				return storage;
			}
		}
		return new long[]{};
	}

	/**
	 * Returns free and total secondary storage space
	 * @return Two values as a pair or none
     */
	private static long[] getSecondaryStorageDetails(){
		File path = getStoragePathFromEnv("SECONDARY_STORAGE");
		if(path != null && path.exists()){
			long[] storage = getStorageDetailsForPath(path);
			return storage;
		}
		// Make sure external storage is a secondary device
		if(isExternalStorageRemovable() && !isExternalStorageEmulated()){
			path = Environment.getExternalStorageDirectory();
			if(path != null && path.exists()){
				long[] storage = getStorageDetailsForPath(path);
				return storage;
			}
		}
		return new long[]{};
	}

	/**
	 * Returns a storage path from an environment variable, if supported.
	 * @param variable Variable name
	 * @return Storage path or null if not found
     */
	private static File getStoragePathFromEnv(String variable){
		String path;
		try{
			path = System.getenv(variable);
			return new File(path);
		} catch (Exception e){
			return null;
		}
	}

	private static boolean isExternalStorageRemovable(){
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD){
			return Environment.isExternalStorageRemovable();
		}
		return false;
	}

	/**
	 * Checks if external storage is emulated, works on API level 11+.
	 * @return True if method is supported and storage is emulated
     */
	private static boolean isExternalStorageEmulated(){
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
			return Environment.isExternalStorageEmulated();
		}
		return false;
	}

	/**
	 * Returns free and total storage space in bytes
	 * @param path Path to the storage medium
	 * @return Free and total space in long[]
     */
	private static long[] getStorageDetailsForPath(File path){
		if(path == null) return new long[]{};
		final int KB = 1024;
		final int MB = KB*1024;
		long free;
		long total;
		long blockSize;
		try {
			StatFs stats = new StatFs(path.getAbsolutePath());
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
				free = stats.getAvailableBytes()/MB;
				total = stats.getTotalBytes()/MB;
				return new long[]{free, total};
			} else {
				blockSize = (long)stats.getBlockSize();
				free = ((long)stats.getAvailableBlocks()*blockSize)/MB;
				total = ((long)stats.getBlockCount()*blockSize)/MB;
				if(free < 0 || total < 0) return new long[]{};
				return new long[]{free, total};
			}
		} catch(Exception e){
			return new long[]{};
		}
	}

	/**
	 * Retrieves sim operator name using an undocumented telephony manager call.
	 * WARNING: Uses reflection, data might not always be available.
	 * @param context
	 * @param subId
     * @return
     */
	private static String getSimOperatorNameForSubscription(Context context, int subId){
		TelephonyManager stub = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
		try {
			Class<?> telephonyManager = Class.forName(stub.getClass().getName());
			Method getName = telephonyManager.getMethod("getSimOperatorNameForSubscription", int.class);
			getName.setAccessible(true);
			return ((String) getName.invoke(context, subId));
		} catch (Exception e) {
			if(Constants.DEBUG && e != null && e.getLocalizedMessage() != null){
				Logger.d(STAG, "Failed getting sim operator with subid: " + e.getLocalizedMessage());
			}
		}
		return null;
	}

    /**
     * Retrieves a two-letter country code using an undocumented system properties call.
     * WARNING: Uses reflection, data might not always be available.
     * @param context Application context
     * @param property Property name
     * @return Two-letter country code
     */
    private static String getCountryCodeFromProperty(Context context, String property) {
        try {
            String operator = getSystemProperty(context, property);
            if(operator != null && operator.length() >= 5){
                int mcc = Integer.parseInt(operator.substring(0,3));
                return getCountryCodeForMcc(context, mcc);
            }
        } catch(Exception e){
            if(Constants.DEBUG && e != null && e.getLocalizedMessage() != null){
                Logger.d(STAG, "Failed getting network location: " + e.getLocalizedMessage());
            }
        }
        return null;
    }

	/**
	 * Retrieves service provider using an undocumented system properties call.
	 * WARNING: Uses reflection, data might not always be available.
	 * @param context
	 * @param property
     * @return
     */
	private static String getStringFromSystemProperty(Context context, String property){
		try {
			String operator = getSystemProperty(context, property);
			if(operator != null && operator.length() > 0){
				return operator;
			}
		} catch (Exception e){
			if(Constants.DEBUG && e != null && e.getLocalizedMessage() != null){
				Logger.d(STAG, "Failed getting service provider: " + e.getLocalizedMessage());
			}
		}
		return null;
	}

    /**
     * Undocumented call to read a string value from system properties.
     * WARNING: Uses reflection, data might not always be available.
     * @param context Application context
     * @param property Property name
     * @return Property value
     * @throws Exception
     */
    private static String getSystemProperty(Context context, String property) throws Exception {
		Class<?> systemProperties = Class.forName("android.os.SystemProperties");
		Method get = systemProperties.getMethod("get", String.class);
		get.setAccessible(true);
		return ((String) get.invoke(context, property));
	}

    /**
     * Undocumented call to look up a two-letter country code with an mcc.
     * WARNING: Uses reflection, data might not always be available.
     * @param context Application context
     * @param mcc Numeric country code
     * @return Country code
     * @throws Exception
     */
	private static String getCountryCodeForMcc(Context context, int mcc) throws Exception{
		Class<?> mccTable = Class.forName("com.android.internal.telephony.MccTable");
		Method countryCodeForMcc = mccTable.getMethod("countryCodeForMcc", int.class);
		countryCodeForMcc.setAccessible(true);
		return ((String) countryCodeForMcc.invoke(context, mcc));
	}

	/**
	 * Undocumented call to check if wifi access point is enabled.
	 * WARNING: Uses reflection, data might not always be available.
	 * @param context Application context
	 * @return State integer, see WIFI_AP statics
	 * @throws Exception
     */
	private static int getWifiApState(Context context) throws Exception {
		WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		Method getWifiApState = wifiManager.getClass().getDeclaredMethod("getWifiApState");
		getWifiApState.setAccessible(true);
		return (int)getWifiApState.invoke(wifiManager);
	}

	/**
	 * Checks if bluetooth is enabled on the device
	 * @return True if bluetooth is enabled
     */
	public static boolean getBluetoothEnabled(){
		BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
		if(adapter == null) return false;
		return (adapter.isEnabled());
	}

	/**
	 * Helper method to collect all the extra information we wish to add to the sample into the Extra Feature list.
	 * @param context the Context
	 * @return a List<Feature> populated with extra items to collect outside of the protocol spec.
	 */
	public static List<Feature> getExtras(Context context) {
		LinkedList<Feature> res = new LinkedList<Feature>();
		res.add(getVmVersion(context));
		return res;
	}

	/**
	 * Get the java.vm.version system property as a Feature("vm", version).
	 * @param context the Context.
	 * @return a Feature instance with the key "vm" and value of the "java.vm.version" system property. 
	 */
	private static Feature getVmVersion(Context context) {
		String vm = System.getProperty("java.vm.version");
		if (vm == null)
			vm = "";
		Feature vmVersion = new Feature();
		vmVersion.setKey("vm");
		vmVersion.setValue(vm);
		return vmVersion;
	}

	public static List<String> getSignatures(PackageInfo pak) {
		List<String> sigList = new LinkedList<String>();
		String[] pmInfos = pak.requestedPermissions;
		if (pmInfos != null) {
			byte[] bytes = getPermissionBytes(pmInfos);
			String hexB = convertToHex(bytes);
			sigList.add(hexB);
		}
		Signature[] signatures = pak.signatures;

		for (Signature s : signatures) {
			MessageDigest md = null;
			try {
				md = MessageDigest.getInstance("SHA-1");
				md.update(s.toByteArray());
				byte[] dig = md.digest();
				// Add SHA-1
				sigList.add(convertToHex(dig));

				CertificateFactory fac = CertificateFactory.getInstance("X.509");
				if (fac == null)
					continue;
				X509Certificate cert = (X509Certificate) fac.generateCertificate(new ByteArrayInputStream(s
						.toByteArray()));
				if (cert == null)
					continue;
				PublicKey pkPublic = cert.getPublicKey();
				if (pkPublic == null)
					continue;
				String al = pkPublic.getAlgorithm();
				switch (al) {
					case "RSA": {
						md = MessageDigest.getInstance("SHA-256");
						RSAPublicKey rsa = (RSAPublicKey) pkPublic;
						byte[] data = rsa.getModulus().toByteArray();
						if (data[0] == 0) {
							byte[] copy = new byte[data.length - 1];
							System.arraycopy(data, 1, copy, 0, data.length - 1);
							md.update(copy);
						} else
							md.update(data);
						dig = md.digest();
						// Add SHA-256 of modulus
						sigList.add(convertToHex(dig));
						break;
					}
					case "DSA": {
						DSAPublicKey dsa = (DSAPublicKey) pkPublic;
						md = MessageDigest.getInstance("SHA-256");
						byte[] data = dsa.getY().toByteArray();
						if (data[0] == 0) {
							byte[] copy = new byte[data.length - 1];
							System.arraycopy(data, 1, copy, 0, data.length - 1);
							md.update(copy);
						} else
							md.update(data);
						dig = md.digest();
						// Add SHA-256 of public key (DSA)
						sigList.add(convertToHex(dig));
						break;
					}
					default:
						Logger.e("SamplingLibrary", "Weird algorithm: " + al + " for " + pak.packageName);
						break;
				}
			} catch (NoSuchAlgorithmException | CertificateException e) {
				// Do nothing
			}

		}
		return sigList;
	}

	public static byte[] getPermissionBytes(String[] perms) {
		if (perms == null)
			return null;
		if (permList.size() == 0)
			populatePermList();
		// Logger.i(STAG, "PermList Size: " + permList.size());
		byte[] bytes = new byte[permList.size() / 8 + 1];
		for (String p : perms) {
			int idx = permList.indexOf(p);
			if (idx > 0) {
				int i = idx / 8;
				idx = (int) Math.pow(2, idx - i * 8);
				bytes[i] = (byte) (bytes[i] | idx);
			}
		}
		return bytes;
	}

	private static final ArrayList<String> permList = new ArrayList<String>();

	private static void populatePermList() {
		final String[] permArray = { "android.permission.ACCESS_CHECKIN_PROPERTIES",
				"android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION",
				"android.permission.ACCESS_LOCATION_EXTRA_COMMANDS", "android.permission.ACCESS_MOCK_LOCATION",
				"android.permission.ACCESS_NETWORK_STATE", "android.permission.ACCESS_SURFACE_FLINGER",
				"android.permission.ACCESS_WIFI_STATE", "android.permission.ACCOUNT_MANAGER",
				"android.permission.AUTHENTICATE_ACCOUNTS", "android.permission.BATTERY_STATS",
				"android.permission.BIND_APPWIDGET", "android.permission.BIND_DEVICE_ADMIN",
				"android.permission.BIND_INPUT_METHOD", "android.permission.BIND_WALLPAPER",
				"android.permission.BLUETOOTH", "android.permission.BLUETOOTH_ADMIN", "android.permission.BRICK",
				"android.permission.BROADCAST_PACKAGE_REMOVED", "android.permission.BROADCAST_SMS",
				"android.permission.BROADCAST_STICKY", "android.permission.BROADCAST_WAP_PUSH",
				"android.permission.CALL_PHONE", "android.permission.CALL_PRIVILEGED", "android.permission.CAMERA",
				"android.permission.CHANGE_COMPONENT_ENABLED_STATE", "android.permission.CHANGE_CONFIGURATION",
				"android.permission.CHANGE_NETWORK_STATE", "android.permission.CHANGE_WIFI_MULTICAST_STATE",
				"android.permission.CHANGE_WIFI_STATE", "android.permission.CLEAR_APP_CACHE",
				"android.permission.CLEAR_APP_USER_DATA", "android.permission.CONTROL_LOCATION_UPDATES",
				"android.permission.DELETE_CACHE_FILES", "android.permission.DELETE_PACKAGES",
				"android.permission.DEVICE_POWER", "android.permission.DIAGNOSTIC",
				"android.permission.DISABLE_KEYGUARD", "android.permission.DUMP",
				"android.permission.EXPAND_STATUS_BAR", "android.permission.FACTORY_TEST",
				"android.permission.FLASHLIGHT", "android.permission.FORCE_BACK", "android.permission.GET_ACCOUNTS",
				"android.permission.GET_PACKAGE_SIZE", "android.permission.GET_TASKS",
				"android.permission.GLOBAL_SEARCH", "android.permission.HARDWARE_TEST",
				"android.permission.INJECT_EVENTS", "android.permission.INSTALL_LOCATION_PROVIDER",
				"android.permission.INSTALL_PACKAGES", "android.permission.INTERNAL_SYSTEM_WINDOW",
				"android.permission.INTERNET", "android.permission.KILL_BACKGROUND_PROCESSES",
				"android.permission.MANAGE_ACCOUNTS", "android.permission.MANAGE_APP_TOKENS",
				"android.permission.MASTER_CLEAR", "android.permission.MODIFY_AUDIO_SETTINGS",
				"android.permission.MODIFY_PHONE_STATE", "android.permission.MOUNT_FORMAT_FILESYSTEMS",
				"android.permission.MOUNT_UNMOUNT_FILESYSTEMS", "android.permission.PERSISTENT_ACTIVITY",
				"android.permission.PROCESS_OUTGOING_CALLS", "android.permission.READ_CALENDAR",
				"android.permission.READ_CONTACTS", "android.permission.READ_FRAME_BUFFER",
				"com.android.browser.permission.READ_HISTORY_BOOKMARKS", "android.permission.READ_INPUT_STATE",
				"android.permission.READ_LOGS", "android.permission.READ_OWNER_DATA",
				"android.permission.READ_PHONE_STATE", "android.permission.READ_SMS",
				"android.permission.READ_SYNC_SETTINGS", "android.permission.READ_SYNC_STATS",
				"android.permission.REBOOT", "android.permission.RECEIVE_BOOT_COMPLETED",
				"android.permission.RECEIVE_MMS", "android.permission.RECEIVE_SMS",
				"android.permission.RECEIVE_WAP_PUSH", "android.permission.RECORD_AUDIO",
				"android.permission.REORDER_TASKS", "android.permission.RESTART_PACKAGES",
				"android.permission.SEND_SMS", "android.permission.SET_ACTIVITY_WATCHER",
				"android.permission.SET_ALWAYS_FINISH", "android.permission.SET_ANIMATION_SCALE",
				"android.permission.SET_DEBUG_APP", "android.permission.SET_ORIENTATION",
				"android.permission.SET_PREFERRED_APPLICATIONS", "android.permission.SET_PROCESS_LIMIT",
				"android.permission.SET_TIME", "android.permission.SET_TIME_ZONE", "android.permission.SET_WALLPAPER",
				"android.permission.SET_WALLPAPER_HINTS", "android.permission.SIGNAL_PERSISTENT_PROCESSES",
				"android.permission.STATUS_BAR", "android.permission.SUBSCRIBED_FEEDS_READ",
				"android.permission.SUBSCRIBED_FEEDS_WRITE", "android.permission.SYSTEM_ALERT_WINDOW",
				"android.permission.UPDATE_DEVICE_STATS", "android.permission.USE_CREDENTIALS",
				"android.permission.VIBRATE", "android.permission.WAKE_LOCK", "android.permission.WRITE_APN_SETTINGS",
				"android.permission.WRITE_CALENDAR", "android.permission.WRITE_CONTACTS",
				"android.permission.WRITE_EXTERNAL_STORAGE", "android.permission.WRITE_GSERVICES",
				"com.android.browser.permission.WRITE_HISTORY_BOOKMARKS", "android.permission.WRITE_OWNER_DATA",
				"android.permission.WRITE_SECURE_SETTINGS", "android.permission.WRITE_SETTINGS",
				"android.permission.WRITE_SMS", "android.permission.WRITE_SYNC_SETTINGS" };

		for (String s : permArray)
			permList.add(s);
	}
}
