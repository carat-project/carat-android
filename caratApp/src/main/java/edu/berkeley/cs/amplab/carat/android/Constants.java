package edu.berkeley.cs.amplab.carat.android;

import android.app.AlarmManager;
import android.graphics.Color;

import java.util.concurrent.TimeUnit;

public class Constants {
    // Whether to output debug messages.
    public static final boolean DEBUG = true;

    public static final long DUPLICATE_INTERVAL = 60;

    public static final long FRESHNESS_TIMEOUT = AlarmManager.INTERVAL_FIFTEEN_MINUTES;
    public static final long FRESHNESS_TIMEOUT_BLACKLIST = 24 * 3600 * 1000;
    public static final long FRESHNESS_TIMEOUT_QUICKHOGS = TimeUnit.DAYS.toMillis(2);
    public static final long FRESHNESS_TIMEOUT_SAMPLE_REMINDER = TimeUnit.DAYS.toMillis(2);
    public static final long FRESHNESS_TIMEOUT_HOGSTATS = TimeUnit.DAYS.toMillis(1);
    public static final long FRESHNESS_TIMEOUT_QUESTIONNAIRE = TimeUnit.DAYS.toMillis(1);
    public static final long DASHBOARD_REFRESH_INTERVAL = TimeUnit.MINUTES.toMillis(1);
    public static final long RAPID_SAMPLING_INTERVAL = TimeUnit.MINUTES.toMillis(5);
    public static final int THRIFT_CONNECTION_TIMEOUT = (int)TimeUnit.MINUTES.toMillis(1);

    public static final String PREFERENCE_FIRST_RUN = "carat.first.run";
    public static final String REGISTERED_UUID = "carat.registered.uuid";
    public static final String REGISTERED_OS = "carat.registered.os";
    public static final String REGISTERED_MODEL = "carat.registered.model";

    // for caching summary statistics fetched from server
    public static final String PREFERENCE_FILE_NAME = "caratPrefs";
    public static final String STATS_WELLBEHAVED_COUNT_PREFERENCE_KEY = "wellbehavedPrefKey";
    public static final String STATS_HOGS_COUNT_PREFERENCE_KEY = "hogsPrefKey";
    public static final String STATS_BUGS_COUNT_PREFERENCE_KEY = "bugsPrefKey";

    public static final String STATS_APP_WELLBEHAVED_COUNT_PREFERENCE_KEY = "appWellbehavedPrefKey";
    public static final String STATS_APP_HOGS_COUNT_PREFERENCE_KEY = "appHogsPrefKey";
    public static final String STATS_APP_BUGS_COUNT_PREFERENCE_KEY = "appBugsPrefKey";

    public static final String STATS_IOS_WELLBEHAVED_COUNT_PREFERENCE_KEY = "iosWellbehavedPrefKey";
    public static final String STATS_IOS_HOGS_COUNT_PREFERENCE_KEY = "iosHogsPrefKey";
    public static final String STATS_IOS_BUGS_COUNT_PREFERENCE_KEY = "iosBugsPrefKey";

    public static final String STATS_USER_BUGS_COUNT_PREFERENCE_KEY = "userBugsPrefKey";
    public static final String STATS_USER_NO_BUGS_COUNT_PREFERENCE_KEY = "userNoBugsPrefKey";

    public static final String PREFERENCE_NEW_UUID = "carat.new.uuid";
    public static final String PREFERENCE_TIME_BASED_UUID = "carat.uuid.timebased";

    // When waking up from screen off, wait 5 seconds for wifi etc to come up
    public static final long COMMS_WIFI_WAIT = 5 * 1000;
    // Send up to 10 samples at a time
    public static final int COMMS_MAX_UPLOAD_BATCH = 10;

    public static final String SCHEDULED_SAMPLE = "edu.berkeley.cs.amplab.carat.android.SCHEDULED_SAMPLE";
    public static final String CHECK_SCHEDULE = "edu.berkeley.cs.amplab.carat.android.CHECK_SCHEDULE";
    public static final String RAPID_SAMPLING = "edu.berkeley.cs.amplab.carat.android.RAPID_SAMPLING";

    public static final int SAMPLES_MAX_BACKLOG = 250;
    // If true, install Sampling events to occur at boot. Currently not used.


    // default icon and Carat package name:
    public static final String CARAT_PACKAGE_NAME = "edu.berkeley.cs.amplab.carat.android";
    // Used to blacklist old Carat
    public static final String CARAT_OLD = "edu.berkeley.cs.amplab.carat";

    // Not in Android 2.2, but needed for app importances
    public static final int IMPORTANCE_PERCEPTIBLE = 130;
    // Used for non-app suggestions
    public static final int IMPORTANCE_SUGGESTION = 123456789;
    public static final int IMPORTANCE_FOREGROUND_SERVICE = 125; // Not in 2.2 as well

    public static final String IMPORTANCE_NOT_RUNNING = "Not Running";
    public static final String IMPORTANCE_UNINSTALLED = "uninstalled";
    public static final String IMPORTANCE_DISABLED = "disabled";
    public static final String IMPORTANCE_INSTALLED = "installed";
    public static final String IMPORTANCE_REPLACED = "replaced";

    public static final int COMMS_MAX_BATCHES = 50;

    // Used for bugs and hogs, and EnergyDetails sub-screen (previously known as drawing)
    public enum Type {
        OS, MODEL, HOG, BUG, SIMILAR, JSCORE, OTHER, BRIGHTNESS, WIFI, MOBILEDATA
    }

    // Used for messages in comms threads
    static final String MSG_TRY_AGAIN = " will try again in " + (FRESHNESS_TIMEOUT / 1000) + "s.";

    public static int VALUE_NOT_AVAILABLE = -1;

    public static final int[] CARAT_COLORS = {
            Color.rgb(90, 198, 108), /* green 3 - Normal green*/
            Color.rgb(240, 71, 31) /*Beautiful Orange*/,
            Color.rgb(250, 150, 38) /*Yellow*/,
            Color.rgb(193, 216, 216) /*Gray*/,
            Color.rgb(207, 218, 227) /*Mild Gray*/
    };

    public static final int REQUESTCODE_ACCEPT_EULA = 16401;

    public static final long MIN_FOREGROUND_SESSION = 1000;

    // FRAGMENT TAGS
    public static final String FRAGMENT_BUGS_TAG = "fragment_bugs";
    public static final String FRAGMENT_HOGS_TAG = "fragment_hogs";
    public static final String FRAGMENT_GLOBAL_TAG = "fragment_global";
    public static final String FRAGMENT_ACTIONS_TAG = "fragment_actions";
    public static final String FRAGMENT_MY_DEVICE_TAG = "fragment_my_device";
    public static final String FRAGMENT_ABOUT_TAG = "fragment_about";
    public static final String FRAGMENT_SETTINGS_TAG = "fragment_settings";
    public static final String FRAGMENT_CB_WEBVIEW_TAG = "fragment_callback_webview";
    public static final String FRAGMENG_HOG_STATS_TAG = "fragment_hog_stats";
    public static final String FRAGMENT_PROCESS_LIST = "fragment_process_list";

    public static final String FRAGMENT_QUESTIONNAIRE_CHOICE = "questionnaire_choice";
    public static final String FRAGMENT_QUESTIONNAIRE_MULTICHOICE = "questionnaire_multichoice";
    public static final String FRAGMENT_QUESTIONNAIRE_INFORMATION = "questionnaire_information";
    public static final String FRAGMENT_QUESTIONNAIRE_INPUT = "questionnaire_input";
}
