package edu.berkeley.cs.amplab.carat.android;

import java.net.InetAddress;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.Application;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.util.SparseArray;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

import edu.berkeley.cs.amplab.carat.android.models.MyDeviceData;
import edu.berkeley.cs.amplab.carat.android.models.CustomAction;
import edu.berkeley.cs.amplab.carat.android.protocol.CommunicationManager;
import edu.berkeley.cs.amplab.carat.android.protocol.SampleSender;
import edu.berkeley.cs.amplab.carat.android.sampling.Sampler;
import edu.berkeley.cs.amplab.carat.android.sampling.SamplingLibrary;
import edu.berkeley.cs.amplab.carat.android.sampling.SamplingStarter;
import edu.berkeley.cs.amplab.carat.android.storage.CaratDataStorage;
import edu.berkeley.cs.amplab.carat.android.storage.SimpleHogBug;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;
import edu.berkeley.cs.amplab.carat.thrift.Questionnaire;
import edu.berkeley.cs.amplab.carat.thrift.Reports;

import static edu.berkeley.cs.amplab.carat.android.models.CustomAction.ActionType;

/**
 * Application class for Carat Android App. Place App-global static constants
 * and methods here.
 *
 * @author Eemil Lagerspetz
 */
public class CaratApplication extends Application {

    private static CaratApplication mInstance;
    // Used for logging
    private static final String TAG = "CaratApp";
    private static long installDate = 0;

    public static Context mAppContext = null;
    public static SharedPreferences mPrefs = null;

    // Used to map importances to human readable strings for sending samples to
    // the server, and showing them in the process list.
    private static final SparseArray<String> importanceToString = new SparseArray<String>();

    {
        importanceToString.put(RunningAppProcessInfo.IMPORTANCE_EMPTY, "Not running");
        importanceToString.put(RunningAppProcessInfo.IMPORTANCE_BACKGROUND, "Background process");
        importanceToString.put(RunningAppProcessInfo.IMPORTANCE_SERVICE, "Service");
        importanceToString.put(RunningAppProcessInfo.IMPORTANCE_VISIBLE, "Visible task");
        importanceToString.put(RunningAppProcessInfo.IMPORTANCE_FOREGROUND, "Foreground app");

        importanceToString.put(Constants.IMPORTANCE_PERCEPTIBLE, "Perceptible task");
        importanceToString.put(Constants.IMPORTANCE_SUGGESTION, "Suggestion");

        mInstance = this;
    }

    public static Context getContext() {
        return mInstance;
    }

    // NOTE: This needs to be initialized before CommunicationManager.
    private static CaratDataStorage storage = null;
    // NOTE: The CommunicationManager requires a working instance of
    // CaratDataStorage.
    public CommunicationManager commManager = null;

    // Activity pointers so that all activity UIs can be updated with a callback
    // to CaratApplication
    static MainActivity main = null;
    // The Sampler samples the battery level when it changes.
    private static Sampler sampler = null;

    public static MyDeviceData myDeviceData = new MyDeviceData();

    // used to check if Internet is available
    private static ConnectivityManager mConnectivityManager = null;

    // Application overrides:

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

    /**
     * 1. Create CaratDataStorage and read reports from disk. Does not seem too
     * slow.
     * <p/>
     * 2. Take a sample in a new thread so that the GUI has fresh data.
     * <p/>
     * 3. Create CommunicationManager for communicating with the Carat server.
     * <p/>
     * 4. Communicate with the server to fetch new reports if current ones are
     * outdated, and to send old stored and the new just-recorded sample. See
     * MainActivity for this task.
     */
    @Override
    public void onCreate() {
        // Ensure SSL compatibility with older Android versions
        Security.addProvider(new BouncyCastleProvider());

        // Create global configuration and initialize ImageLoader
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(this).build();
        ImageLoader.getInstance().init(config);

        mConnectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        // use a private preference file (created and used only by CaratApp).
        // don't use PreferenceManager.getDefaultSharedPreferences (it might cause problem in different OS versions).
        new Thread() {
            public void run() {
                mPrefs = CaratApplication.this.getSharedPreferences(Constants.PREFERENCE_FILE_NAME, Context.MODE_PRIVATE);
            }
        }.start();

        getInstallationDate();
        setStorage(new CaratDataStorage(this));
        setReportData(); // Show initial data asap

        SamplingStarter.from(getApplicationContext()).run();

        new Thread() {
            public void run() {
                commManager = new CommunicationManager(CaratApplication.this);
            }
        }.start();

        super.onCreate();
    }

    // Utility methods

    /**
     * Converts <code>importance</code> to a human readable string.
     *
     * @param importance the importance from Android process info.
     * @return a human readable String describing the importance.
     */
    public static String importanceString(int importance) {
        String s = importanceToString.get(importance);
        if (s == null || s.length() == 0) {
            Logger.e("Importance not found:", "" + importance);
            s = "Unknown";
        }
        return s;
    }

    /**
     * Here is where you define static actions
     * @return List of static actions
     */
    public static ArrayList<CustomAction> getStaticActions(){
            ArrayList<CustomAction> actions = new ArrayList<>();

            // Google forms survey
            String surveyUrl = CaratApplication.getStorage().getQuestionnaireUrl();
            if(surveyUrl != null && surveyUrl.contains("http")){
                actions.add(new CustomAction(ActionType.GOOGLE_SURVEY,
                        getContext().getString(R.string.survey_action_title),
                        getContext().getString(R.string.survey_action_subtitle)));
            }

            // Local survey
            HashMap<Integer, Questionnaire> questionnaires = CaratApplication.getStorage().getQuestionnaires();
            if(questionnaires != null && !questionnaires.isEmpty()){
                for(Questionnaire q : questionnaires.values()){
                    // This is checked server-side as well, but better safe than sorry
                    if(q.isSetExpirationDate()){
                        long expirationDate = q.getExpirationDate();
                        long now = System.currentTimeMillis();
                        if(now >= expirationDate) continue;
                    }
                    actions.add(new CustomAction(ActionType.QUESTIONNAIRE,
                            q.getActionTitle(), q.getActionText(), q.getId()));
                }
            }

            // Help Carat collect data
            if(getActionsAmount() == 0){
                actions.add(new CustomAction(ActionType.COLLECT,
                        getContext().getString(R.string.helpcarat), getContext().getString(R.string.helpcarat_subtitle))
                        .makeExpandable(R.string.helpcarat_expanded_title,
                                R.string.no_actions_message));
            }
            return actions;
    }

    public static void postNotification(String title, String text){
        postNotification(title, text, null);
    }

    public static void postNotification(String title, String text, Integer fragment){
        Context context = getContext();
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        final boolean disableNotifications =
                p.getBoolean(context.getString(R.string.disable_notifications), false);
        if(disableNotifications) return;
        Intent intent = new Intent(context, MainActivity.class);
        if(fragment != null){
            intent.putExtra("fragment", fragment);
        }
        PendingIntent carat = PendingIntent.getActivity(context, 0, intent, 0);
        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context)
                .setSmallIcon(R.drawable.carat_notif_icon)
                .setContentTitle(title)
                .setContentText(text);
        mBuilder.setContentIntent(carat);
        mBuilder.setAutoCancel(true);
        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.notify(1, mBuilder.build());
    }

    public static void dismissNotifications(){
        Context context = getContext();
        NotificationManager mNotificationManager = (NotificationManager) context
                .getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();
    }

    public void acceptEula(){
        if(Constants.DEBUG){
            Logger.d(TAG, "** Accepted EULA **");
        }
        main.resumeTasksAndUpdate();
    }

    public boolean isOnBackground(){
        return main.isOnBackground();
    }

    /**
     * This method is used to get the application installation date. It is initially
     * called on application launch and the more complicated part should ideally need
     * to run only once. This  part checks if the installation date is already
     * stored in settings, and if not, it tries checking it from package manager and
     * if that fails, the installation date is set to current time.
     *
     * @return Installation time in milliseconds from epoch
     */
    public static long getInstallationDate(){
        if(installDate != 0){
            return installDate;
        }
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(getContext());
        Context c = getContext();
        String key = c.getString(R.string.installation_key);
        PackageManager pm = c.getPackageManager();
        String packageName = c.getPackageName();
        long date = 0;
        if(p.contains(key)){
            return p.getLong(key, 0);
        } else {
            try{
                date = pm.getPackageInfo(packageName, 0).firstInstallTime;
            } catch(Throwable th){
                if(Constants.DEBUG){
                    Logger.d(TAG, "Failed getting application installation date from package");
                    th.printStackTrace();
                }
            }
        }

        if(date == 0){
            date = System.currentTimeMillis();
            // This can happen synchronously, it almost never runs
            p.edit().putLong("installDate", date).commit();
        }
        installDate = date;
        return date;
    }

    public static void refreshStaticActionCount(){
        main.setStaticActionsAmount(getStaticActions().size());
    }

    public static int getActionsAmount(){
        int actionsAmount=0;
        if(CaratApplication.getStorage() != null){
            SimpleHogBug[] report = CaratApplication.getStorage().getBugReport();
            actionsAmount += filterByRunning(report).size();
            report = CaratApplication.getStorage().getHogReport();
            actionsAmount += filterByRunning(report).size();
        }
        return actionsAmount;
    }

    public static ArrayList<SimpleHogBug> filterByRunning(SimpleHogBug[] packages){
        if(packages == null || packages.length == 0) return new ArrayList<>();

        ArrayList<SimpleHogBug> report = filterByVisibility(packages);
        HashMap<String, SimpleHogBug> running = new HashMap<>();
        if(report == null) return new ArrayList<>();
        for(SimpleHogBug s : report){
            if(SamplingLibrary.isRunning(getContext(), s.getAppName())){
                SimpleHogBug duplicate = running.get(s.getAppName());
                if(duplicate != null
                        && s.getAppPriority() == duplicate.getAppPriority()
                        && s.getBenefit() == duplicate.getBenefit()){
                    continue;
                }
                running.put(s.getAppName(), s);
            }
        }
        return new ArrayList<>(running.values());
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

    public static String translatedPriority(String importanceString) {
        if (main != null) {
            if (importanceString == null)
                return main.getString(R.string.priorityDefault);
            if (importanceString.equals("Not running")) {
                return main.getString(R.string.prioritynotrunning);
            } else if (importanceString.equals("Background process")) {
                return main.getString(R.string.prioritybackground);
            } else if (importanceString.equals("Service")) {
                return main.getString(R.string.priorityservice);
            } else if (importanceString.equals("Visible task")) {
                return main.getString(R.string.priorityvisible);
            } else if (importanceString.equals("Foreground app")) {
                return main.getString(R.string.priorityforeground);
            } else if (importanceString.equals("Perceptible task")) {
                return main.getString(R.string.priorityperceptible);
            } else if (importanceString.equals("Suggestion")) {
                return main.getString(R.string.prioritysuggestion);
            } else
                return main.getString(R.string.priorityDefault);
        } else if(importanceString == null) {
            return "Not running";
        }

        return importanceString;
    }

    /**
     * Return a Drawable that contains an app icon for the named app. If not
     * found, return the Drawable for the Carat icon.
     *
     * @param appName the application name
     * @return the Drawable for the application's icon
     */
    public static Drawable iconForApp(Context context, String appName) {
        try {
            return context.getPackageManager().getApplicationIcon(appName);
        } catch (NameNotFoundException e) {
            return context.getResources().getDrawable(R.drawable.process_icon);
        }
    }

    /**
     * Return a human readable application label for the named app. If not
     * found, return appName.
     *
     * @param appName the application name
     * @return the human readable application label
     */
    public static String labelForApp(Context context, String appName) {
        if (appName == null)
            return "Unknown";
        try {
            ApplicationInfo i = context.getPackageManager().getApplicationInfo(appName, 0);
            if (i != null)
                return context.getPackageManager().getApplicationLabel(i).toString();
            else
                return appName;
        } catch (NameNotFoundException e) {
            return appName;
        }
    }

    /**
     * Searches system for a matching package and tells if it's installed.
     * This considers apps that are disabled uninstalled.
     *
     * @param packageName Package name
     * @return True if package is found, otherwise false
     */
    public static boolean isPackageInstalled(String packageName){
        PackageManager packageManager = getContext().getPackageManager();
        ApplicationInfo applicationInfo = null;
        try {
            // Throws an exception when there is no such package installed
            applicationInfo = packageManager.getApplicationInfo(packageName, 0);
            return applicationInfo.enabled;
        } catch (NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Compares package flags and signature with those of a system app.
     *
     * @param packageName Package name
     * @return True if package is a system app
     */
    public static boolean isPackageSystemApp(String packageName){
        if(packageName == null) return false;

        PackageManager packageManager = getContext().getPackageManager();
        try{
            ApplicationInfo info = packageManager.getApplicationInfo(packageName, 0);
            if(info != null){
                return (isSystemPackage(info) && isSystemSigned(packageName));
            }
            return false;
        } catch(Exception e){
            return false;
        }
    }

    private static boolean isSystemPackage(ApplicationInfo info){
        return (info.flags & ApplicationInfo.FLAG_SYSTEM) != 0
                || (info.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0;
    }

    private static boolean isSystemSigned(String packageName){
        try {

            PackageManager pm = getContext().getPackageManager();
            PackageInfo pi = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            PackageInfo sys = pm.getPackageInfo("android", PackageManager.GET_SIGNATURES);
            if(pi==null || pi.signatures == null) return false;
            if(sys==null || sys.signatures == null) return false;
            return (sys.signatures[0].equals(pi.signatures[0]));
        } catch (Exception e) {
            // Package not found
            return false;
        }
    }

    public static int getJscore() {
        final Reports reports = getStorage().getReports();
        int jscore = 0;
        if (reports != null) {
            jscore = ((int) (reports.getJScore() * 100));
        }
        return jscore;
    }

    /**
     * Return titles from the drawer items array.
     *
     * @return titles from the drawer items array.
     */
    public static String[] getTitles() {
        Resources res = getContext().getResources();
        return res.getStringArray(R.array.drawer_items);
    }

    public static void setActionInProgress() {
        if (main != null) {
            main.runOnUiThread(new Runnable() {
                public void run() {
                    // Updating done
                    //main.setTitleUpdating(getTitles()[2]);
                    main.setProgress(0);
                    main.setProgressBarVisibility(true);
                    main.setProgressBarIndeterminateVisibility(true);
                }
            });
        }
    }

    public static void setStatus(final String what) {
        if (main != null) {
            main.runOnUiThread(new Runnable() {
                public void run() {
                    main.setStatusText(what);
                }
            });
        }
    }

    protected static void setMain(MainActivity mainActivity) {
        main = mainActivity;
    }

    public static String getRegisteredUuid() {
        return Constants.REGISTERED_UUID;
    }

    public boolean isNetworkReady() {
        Context context = getContext();
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(context);
        final boolean useWifiOnly = p.getBoolean(getString(R.string.wifi_only_key), false);
        String networkStatus = SamplingLibrary.getNetworkStatus(getApplicationContext());
        String networkType = SamplingLibrary.getNetworkType(context);
        return (!useWifiOnly && networkStatus.equalsIgnoreCase(SamplingLibrary.NETWORKSTATUS_CONNECTED))
                || networkType.equals("WIFI");
    }

    public void checkAndRefreshReports(){
        long freshness = getStorage().getFreshness();
        long elapsed = System.currentTimeMillis() - freshness;
        if(elapsed < Constants.FRESHNESS_TIMEOUT) return;

        // Wait for 5 seconds to see if network comes up
        String status = SamplingLibrary.getNetworkStatus(getApplicationContext());
        if(status.equals(SamplingLibrary.NETWORKSTATUS_CONNECTING)){
            try {Thread.sleep(Constants.COMMS_WIFI_WAIT);}
            catch (InterruptedException e1) {
                // No operation
            }
        }

        // Check network status and data freshness, then update
        if(isNetworkReady()){
            boolean success = false;
            main.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    main.setProgressCircle(true);
                }
            });
            CaratApplication.setActionInProgress();
            try {
                success = commManager.refreshAllReports();
            } catch (Throwable th){
                // Any sort of malformed response
                Log.w(TAG, "Failed to refresh reports: " + th + Constants.MSG_TRY_AGAIN);
                th.printStackTrace();
            }
            main.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    main.setStatusText(getString(R.string.finishing));
                    main.setProgressCircle(false);
                }
            });
            if(success){
                setReportData();
                CaratApplication.refreshStaticActionCount();
            }
        }
    }

    public void checkAndSendSamples(){
        long lastUploaded = getStorage().getLastUploadTimestamp();
        long elapsed = System.currentTimeMillis() - lastUploaded;
        if(elapsed > Constants.FRESHNESS_TIMEOUT){
            if(SampleSender.sendSamples(CaratApplication.this)){
                getStorage().writeLastUploadTimestamp();
            }
        }
    }

    public boolean isSendingSamples(){
        return SampleSender.isSendingSamples();
    }

    // Checks if communication manager is busy
    public boolean isUpdatingReports(){
        if(commManager != null){
            return commManager.isRefreshingReports();
        }
        return false;
    }


    public static boolean isInternetAvailable2() {
        try {
            InetAddress ipAddr = InetAddress.getByName("google.com"); //You can replace it with your name
            return !ipAddr.equals("");

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Checks whether WiFi or mobile data is enabled
     *
     * @return true of false
     */
    public static boolean isInternetAvailable() {
        boolean haveConnectedWifi = false;
        boolean haveConnectedMobile = false;


        NetworkInfo[] netInfo = mConnectivityManager.getAllNetworkInfo();
        for (NetworkInfo ni : netInfo) {
            if (ni.getTypeName().equalsIgnoreCase("WIFI"))
                if (ni.isConnected())
                    haveConnectedWifi = true;
            if (ni.getTypeName().equalsIgnoreCase("MOBILE"))
                if (ni.isConnected())
                    haveConnectedMobile = true;
        }
        return haveConnectedWifi || haveConnectedMobile;
    }

    public static void setReportData() {
        final Reports r = getStorage().getReports();
        if (Constants.DEBUG)
            Logger.d(TAG, "Got reports.");
        long freshness = CaratApplication.getStorage().getFreshness();
        long l = System.currentTimeMillis() - freshness;
        final long h = l / 3600000;
        final long min = (l - h * 3600000) / 60000;
        double bl = 0;
        double error = 0;

        if (r != null) {
            if (Constants.DEBUG)
                Logger.d(TAG, "r (reports) not null.");
            // Try exact battery life
            if (r.jScoreWith != null) {
                // Logger.d(TAG, "jscoreWith not null.");
                double exp = r.jScoreWith.expectedValue;
                if (exp > 0.0) {
                    bl = 100 / exp;
                    error = 100 / (exp + r.jScoreWith.error);
                } else if (r.getModel() != null) {
                    exp = r.getModel().expectedValue;
                    if (Constants.DEBUG)
                        Logger.d(TAG, "Model expected value: " + exp);
                    if (exp > 0.0) {
                        bl = 100 / exp;
                        error = 100 / (exp + r.getModel().error);
                    }
                }
                // If not possible, try model battery life
            }
        }


        // Only take the error part
        error = bl - error;

        int blh = (int) (bl / 3600);
        bl -= blh * 3600;
        int blmin = (int) (bl / 60);

        int errorH = 0;
        int errorMin = 0;
        if (error > 7200) {
            errorH = (int) (error / 3600);
            error -= errorH * 3600;
        }

        errorMin = (int) (error / 60);


        String blS;
        if(blh == 0 && blmin == 0){
            blS = getContext().getString(R.string.calibrating);
        } else if(errorH > 0){
            blS =  blh + "h " + blmin + "min \u00B1 " + errorH + "h";
        } else if(errorMin > 0){
            blS =  blh + "h " + blmin + "min \u00B1 " + errorMin + "min";
        } else {
            blS =  blh + "h " + blmin + "min";
        }

		/*
		 * we removed direct manipulation of MyDevice fragment,
		 * and moved the data pertaining to this fragment to a class field, called myDeviceData.
		 * In the onResume() method of MyDeviceFragment, we fetch this data and show (see setViewData())
		 * The reason for this movement is that we migrated from tabs to fragments.
		 * We cannot change a fragment's view while it's not in the foreground
		 * (fragments get replaced by a fragment transaction:
		 * the parent activity which hosts a frame-layout
		 * (a placeholder for fragment's layout), replaces the frame-layout with
		 * the new fragment's layout)
		 */

        SharedPreferences p = PreferenceManager
                .getDefaultSharedPreferences(getContext());
        String caratId = p.getString(Constants.REGISTERED_UUID, "0");

        myDeviceData.setAllFields(freshness, h, min, caratId, blS);
    }

    /**
     * @return the storage
     */
    public static CaratDataStorage getStorage() {
        if (storage == null)
            storage = new CaratDataStorage(CaratApplication.getContext());
        return storage;
    }

    /**
     * @param storage the storage to set
     */
    public static void setStorage(CaratDataStorage storage) {
        CaratApplication.storage = storage;
    }
}
