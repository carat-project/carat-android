package edu.berkeley.cs.amplab.carat.android;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.TimeUnit;

import edu.berkeley.cs.amplab.carat.android.fragments.ActionsFragment;
import edu.berkeley.cs.amplab.carat.android.fragments.GlobalFragment;
import edu.berkeley.cs.amplab.carat.android.fragments.HogStatsFragment;
import edu.berkeley.cs.amplab.carat.android.fragments.SettingsFragment;
import edu.berkeley.cs.amplab.carat.android.protocol.AsyncStats;
import edu.berkeley.cs.amplab.carat.android.receivers.ActionReceiver;
import edu.berkeley.cs.amplab.carat.android.storage.SimpleHogBug;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;
import edu.berkeley.cs.amplab.carat.android.utils.PrefetchData;
import edu.berkeley.cs.amplab.carat.android.fragments.AboutFragment;
import edu.berkeley.cs.amplab.carat.android.fragments.DashboardFragment;
import edu.berkeley.cs.amplab.carat.android.fragments.EnableInternetDialogFragment;
import edu.berkeley.cs.amplab.carat.android.sampling.SamplingLibrary;
import edu.berkeley.cs.amplab.carat.android.utils.ProcessUtil;
import edu.berkeley.cs.amplab.carat.android.utils.Tracker;
import edu.berkeley.cs.amplab.carat.android.utils.VersionGater;
import edu.berkeley.cs.amplab.carat.thrift.Questionnaire;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final String TAG = "CaratMainActivity";

    private SharedPreferences p;

    private boolean acceptedEula = false;
    private String batteryLife;
    private String bugAmount, hogAmount, actionsAmount;
    private int staticActionsAmount;
    private float cpu;
    private int jScore;
    private long[] lastPoint = null;
    private boolean onBackground = false;
    private boolean schedulerRunning = false;

    private boolean shouldAddTabs = true;

    private TextView actionBarTitle;
    private RelativeLayout backArrow;
    private ProgressBar progressCircle;
    private DashboardFragment dashboardFragment;

    private HashMap<String, Integer> androidDevices, iosDevices;

    private Tracker tracker;
    private String statusText;

    public int appWellbehaved = Constants.VALUE_NOT_AVAILABLE,
            appHogs = Constants.VALUE_NOT_AVAILABLE,
            appBugs = Constants.VALUE_NOT_AVAILABLE;

    public int mWellbehaved = Constants.VALUE_NOT_AVAILABLE,
            mHogs = Constants.VALUE_NOT_AVAILABLE,
            mBugs = Constants.VALUE_NOT_AVAILABLE,
            mActions = Constants.VALUE_NOT_AVAILABLE;

    public int iosWellbehaved = Constants.VALUE_NOT_AVAILABLE,
            iosHogs = Constants.VALUE_NOT_AVAILABLE,
            iosBugs = Constants.VALUE_NOT_AVAILABLE;

    public int userHasBug = Constants.VALUE_NOT_AVAILABLE,
            userHasNoBugs = Constants.VALUE_NOT_AVAILABLE;

    public static abstract class DialogCallback<T>{
        public abstract void run(T value);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        onBackground = false;
        schedulerRunning = false;
        getWindow().requestFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        getWindow().requestFeature(Window.FEATURE_PROGRESS);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // TODO: What about devices < HONEYCOMB?
            VersionGater.checkVersion(this);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(getResources().getColor(R.color.statusbar_color));
            if(!UsageManager.isPermissionGranted(this)) {
                UsageManager.promptPermission(this);
            }
        }

        p = PreferenceManager.getDefaultSharedPreferences(this);
        //PreferenceManager.setDefaultValues(this, R.xml.settings, false);
        acceptedEula = p.getBoolean(getResources().getString(R.string.save_accept_eula), false);
        if (!acceptedEula) {
            Intent i = new Intent(this, TutorialActivity.class);
            this.startActivityForResult(i, Constants.REQUESTCODE_ACCEPT_EULA);
        }
        getStatsFromServer();
        super.onCreate(savedInstanceState);

        CaratApplication.setMain(this);
        tracker = Tracker.getInstance(this);
        tracker.trackUser("caratstarted", getTitle());

        // TODO SHOW DIALOG, NOT FRAGMENT
        if (!CaratApplication.isInternetAvailable()) {
            EnableInternetDialogFragment dialog = new EnableInternetDialogFragment();
            dialog.show(getSupportFragmentManager(), "dialog");
        }

        setContentView(R.layout.activity_dashboard);
        dashboardFragment = new DashboardFragment();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_holder, dashboardFragment).commit();
        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                onFragmentPop();
            }
        });

        // Load fragment when coming from notification intent
        int fragment = getIntent().getIntExtra("fragment", -1);
        if(fragment != -1){
            if(fragment == R.id.actions_layout){
                ActionsFragment actionsFragment = new ActionsFragment();
                replaceFragment(actionsFragment, Constants.FRAGMENT_ACTIONS_TAG);
            }
        }

        // TODO: Add this as an accessible flag
        staticActionsAmount = CaratApplication.getStaticActions().size();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    // This needs to be in a separate method so it can be called
    // from Eula Activity without invoking super.onResume()
    public void resumeTasksAndUpdate(){
        this.onBackground = false;
        if ((!isStatsDataAvailable()) && CaratApplication.isInternetAvailable()) {
            getStatsFromServer();
        }
        // Refresh reports and upload samples every 15 minutes, but only
        // if user has accepted EULA.
        acceptedEula = p.getBoolean(getResources().getString(R.string.save_accept_eula), false);
        if(acceptedEula){
            synchronized (this){
                scheduleRefresh(Constants.FRESHNESS_TIMEOUT);
            }
        }
        setValues();
    }

    @Override
    protected void onResume() {
        Intent intent = new Intent(getApplicationContext(), ActionReceiver.class);
        intent.setAction(Constants.CHECK_SCHEDULE);
        sendBroadcast(intent);
        resumeTasksAndUpdate();

        super.onResume();
    }

    public void scheduleRefresh(final long interval){
        // This method is primarily called by onResume, meaning that a
        // timer might already be running. In that case we still want
        // to force a refresh to make the UI feel responsive.
        refresh();
        if(schedulerRunning){
            return;
        }

        // Use a handler here since it allows a background thread to
        // communicate with the UI, which is needed to refresh the
        // current fragment after downloading new data.
        final Handler timer = new Handler();
        timer.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Stop the scheduler when application is no longer in the
                // foreground unless user has enabled sending samples and
                // downloading reports while in the background.
                boolean allowBackground = p.getBoolean(getString(R.string.enable_background), false);
                schedulerRunning = !isOnBackground() || allowBackground;
                if(schedulerRunning){
                    refresh();
                    timer.postDelayed(this, interval);
                } else if(Constants.DEBUG){
                    Logger.d(TAG, "** Data refresh timer stopped ** ");
                }
            }
        }, interval);
        schedulerRunning = true;
        if(Constants.DEBUG){
            Logger.d(TAG, "** Data refresh timer started **");
        }
    }

    public void refresh(){
        if(Constants.DEBUG){
            Logger.d(TAG, "** Started refreshing data **");
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                CaratApplication application = (CaratApplication) getApplication();
                if(application != null){
                    application.checkAndRefreshReports();
                    application.checkAndSendSamples();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            refreshCurrentFragment();
                        }
                    });
                }
                if(Constants.DEBUG){
                    Logger.d(TAG, "** Stopped refreshing data **");
                }
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_carat, menu);
        final SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);

        //setProgressCircle(false);
        return super.onCreateOptionsMenu(menu);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        SharedPreferences p = PreferenceManager.getDefaultSharedPreferences(this);
        switch (id) {
            case R.id.action_feedback:
                final String[] choices = new String[]{
                        "Rate us on Play Store",
                        "Problem with app, please specify",
                        "No J-Score after 7 days of use",
                        "Other, please specify"
                };
                showOptionDialog("Give feedback", new DialogCallback<Integer>() {
                    @Override
                    public void run(Integer choice) {
                        if(choice == 0) showStorePage();
                        else giveFeedback(choices, choice);
                    }
                }, choices);
                break;
            case R.id.action_share:
                share();
                break;
            case R.id.action_settings:
                PreferenceFragmentCompat settings = new SettingsFragment();
                replaceFragment(settings, Constants.FRAGMENT_SETTINGS_TAG);
                break;
            case R.id.action_about:
                AboutFragment aboutFragment = new AboutFragment();
                replaceFragment(aboutFragment, Constants.FRAGMENT_ABOUT_TAG);
                break;
            case android.R.id.home:
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                }
                return true;
            default:
                break;

        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Logger.d(TAG, "Application exited to the background");
        onBackground = true;
        ProcessUtil.invalidateInMemoryProcesses();
        SamplingLibrary.resetRunningProcessInfo();
    }

    public boolean isOnBackground(){
        return onBackground;
    }

    private void showStorePage() {
        final String appPackageName = getPackageName();
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
         }  catch (Exception e) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
        }
    }

    public void setValues() {
        setJScore(CaratApplication.getJscore());
        setBatteryLife(CaratApplication.myDeviceData.getBatteryLife());
        SimpleHogBug[] b = CaratApplication.getStorage().getBugReport();
        SimpleHogBug[] h = CaratApplication.getStorage().getHogReport();
        int actionsAmount = 0;
        if (b != null) {
            int bugsAmount = ProcessUtil.filterByVisibility(b).size();
            setBugAmount(String.valueOf(bugsAmount));
            actionsAmount += ProcessUtil.filterByRunning(b, getApplicationContext()).size();
        } else {
            setBugAmount("0");
        }
        if (h != null) {
            int hogsAmount = ProcessUtil.filterByVisibility(h).size();
            setHogAmount(String.valueOf(hogsAmount));
            actionsAmount += ProcessUtil.filterByRunning(h, getApplicationContext()).size();
        } else {
            setHogAmount("0");
        }

        setActionsAmount(actionsAmount);

        setCpuValue();
        Logger.d("debug", "*** Values set");
    }

    public void setUpActionBar(int resId, boolean canGoBack) {
        this.setUpActionBar(getString(resId), canGoBack);
    }

    public void setUpActionBar(String title, boolean canGoBack, boolean showSettings){
    }

    public void setUpActionBar(String title, boolean canGoBack){
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.actionbar);
        actionBarTitle = (TextView) getSupportActionBar().getCustomView().findViewById(R.id.action_bar_title);
        backArrow = (RelativeLayout) getSupportActionBar().getCustomView().findViewById(R.id.back_arrow);
        backArrow.setOnClickListener(this);
        actionBarTitle.setText(title);
        if (canGoBack) {
            backArrow.setVisibility(View.VISIBLE);
        } else {
            backArrow.setVisibility(View.GONE);
        }
    }

    public void setProgressCircle(boolean visibility) {
        if(getSupportActionBar() == null || getSupportActionBar().getCustomView() == null){
            return;
        }
        backArrow = (RelativeLayout) getSupportActionBar().getCustomView().findViewById(R.id.back_arrow);
        progressCircle = (ProgressBar) getSupportActionBar().getCustomView().findViewById(R.id.action_bar_progress_circle);
        progressCircle.getIndeterminateDrawable().setColorFilter(0xF2FFFFFF,
                android.graphics.PorterDuff.Mode.SRC_ATOP);
        if((backArrow.getVisibility() != View.VISIBLE) && visibility){
            progressCircle.setVisibility(View.VISIBLE);
        } else {
            progressCircle.setVisibility(View.GONE);
        }
    }

    public void deleteQuestionnaires(){
        List<Questionnaire> empty = new ArrayList<>();
        CaratApplication.getStorage().writeQuestionnaires(empty);
        refreshCurrentFragment();
    }

    public void refreshCurrentFragment() {
        if (getSupportFragmentManager() != null) {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
            if(fragment instanceof GlobalFragment){
                ((GlobalFragment) fragment).refresh();
            }
            else if (fragment instanceof  DashboardFragment
                    || getSupportFragmentManager().getBackStackEntryCount() == 0) {
                dashboardFragment.refresh();
            }
        }
    }

    public void refreshHogStatsFragment() {
        if (getSupportFragmentManager() != null) {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
            if(fragment instanceof HogStatsFragment){
                ((HogStatsFragment) fragment).refresh();
            }
        }
    }

    public void onFragmentPop(){
        Fragment top = getSupportFragmentManager().findFragmentById(R.id.fragment_holder);
        if(top instanceof DashboardFragment){
            ((DashboardFragment) top).refreshStatusText();
        }
    }

    public void setCpuValue() {
        long[] currentPoint = SamplingLibrary.readUsagePoint();
        float cpu = 0;
        if (lastPoint == null) {
            lastPoint = currentPoint;
        } else {
            cpu = (float) SamplingLibrary.getUsage(lastPoint, currentPoint);
        }
        this.cpu = cpu;

    }

    public float getCpuValue() {
        return cpu;
    }

    public void setJScore(int jScore) {
        this.jScore = jScore;
    }

    public void setBatteryLife(String batteryLife) {
        this.batteryLife = batteryLife;
    }

    public String getBatteryLife() {
        return batteryLife;
    }

    public int getJScore() {
        return jScore;
    }

    public String getBugAmount() {
        return bugAmount;
    }

    public void setBugAmount(String bugAmount) {
        this.bugAmount = bugAmount;

    }

    public String getHogAmount() {
        return hogAmount;
    }

    public void setHogAmount(String hogAmount) {
        this.hogAmount = hogAmount;
    }

    public String getActionsAmount() {
        return actionsAmount;
    }

    public void setStaticActionsAmount(int amount){
        staticActionsAmount = amount;
    }

    public int getStaticActionsAmount(){
        return staticActionsAmount;
    }

    public void setActionsAmount(int actionsAmount) {
        this.actionsAmount = String.valueOf(actionsAmount);
    }

    public void replaceFragment(Fragment fragment, String tag){
        final String FRAGMENT_TAG = tag;
        setProgressCircle(false);
        boolean fragmentPopped = getSupportFragmentManager().popBackStackImmediate(FRAGMENT_TAG, 0);

        if (!fragmentPopped) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right);

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2 || !isDestroyed()) {
                transaction.replace(R.id.fragment_holder, fragment, FRAGMENT_TAG)
                        .addToBackStack(FRAGMENT_TAG).commitAllowingStateLoss();
            }
        }
    }

    public void restoreStatusText(){
        if(statusText != null){
            setStatusText(statusText);
        }
    }

    public void setStatusText(String what){
        Logger.d(TAG, "Setting status to " + what);
        statusText = what;
        if(dashboardFragment != null){
            dashboardFragment.setStatusText(what);
        }
    }

    @SuppressLint("NewApi")
    private void getStatsFromServer() {
        PrefetchData prefetchData = new PrefetchData(this);
        AsyncStats hogStats = new AsyncStats(this);
        // run this asyncTask in a new thread [from the thread pool] (run in parallel to other asyncTasks)
        // (do not wait for them to finish, it takes a long time)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
            prefetchData.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            hogStats.executeOnExecutor(AsyncStats.THREAD_POOL_EXECUTOR);
        } else {
            hogStats.execute();
            prefetchData.execute();
        }
    }

    public String getShareText(){
        int jScore = getJScore();
        String caratText = getString(R.string.sharetext1);
        if(jScore <= 0){
            caratText += "! \n\n" + getString(R.string.findoutmore);
        } else {
            caratText += ". " + getString(R.string.myjscoreis, jScore) + " " + getString(R.string.sharetext2);
        }
        return caratText;
    }

    public void share() {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_TEXT, getShareText());
        startActivity(Intent.createChooser(shareIntent, getString(R.string.share)));
    }

    public void shareOnFacebook() {
        // Facebook doesn't allow prefilled text, but since custom URIs are allowed,
        // we could instead have dynamic content.
        long appId = 258193747569113L;
        String website = "http://carat.cs.helsinki.fi/";
        Uri uri = Uri.parse("https://www.facebook.com/dialog/share?app_id="+appId+ "&display=popup&href="+website);
        startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }

    public void shareOnTwitter() {
        String tweetUrl = "https://twitter.com/intent/tweet?text="
                + getShareText();
        Uri uri = Uri.parse(tweetUrl);
        startActivity(new Intent(Intent.ACTION_VIEW, uri));
    }

    public void shareViaEmail() {
        String subject = Uri.encode(getString(R.string.sharetitle));
        String body = Uri.encode(getShareText());
        Uri uri = Uri.parse("mailto:" + "?subject=" + subject + "&body=" + body);
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(uri);
        startActivity(Intent.createChooser(intent, "Send email"));
    }

    // General purpose multi-choice dialog with a callback
    public void showOptionDialog(String title, final DialogCallback<Integer> callback, final String... options){
        if(title == null || title.length() == 0){
            throw new IllegalArgumentException("Dialog title cannot be null!");
        }
        if(options == null || options.length == 0){
            throw new IllegalArgumentException("You need to specify at least one option!");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(title);
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(callback != null){
                    callback.run(which);
                }
            }
        });
        builder.show();
    }

    public void giveFeedback(String[] options, int which) {
        float memoryUsedConverted;
        float memoryActiveConverted = 0;

        int[] totalAndUsed = SamplingLibrary.readMeminfo();
        memoryUsedConverted = 1 - ((float) totalAndUsed[0] / totalAndUsed[1]);
        if (totalAndUsed.length > 2) {
            memoryActiveConverted = (float) totalAndUsed[2] / (totalAndUsed[3] + totalAndUsed[2]);
        }

        String versionName = BuildConfig.VERSION_NAME;
        String title = "[Carat][Android] Feedback from " + Build.MODEL + ", v"+versionName;

        String caratVersion = "Carat " + versionName;
        String feedback = "Feedback: " + options[which];
        String caratId = "Carat ID: " + CaratApplication.myDeviceData.getCaratId();
        String jScore = "JScore: " + getJScore();
        String osVersion = "OS Version: " + SamplingLibrary.getOsVersion();
        String deviceModel = "Device Model: " + Build.MODEL;
        String memoryUsed = "Memory Used: " + (memoryUsedConverted * 100) + "%";
        String memoryActive = "Memory Active: " + (memoryActiveConverted * 100) + "%";
        String chargeCounter = "Battery charge counter: " + SamplingLibrary.getBatteryChargeCounter(this);
        String batteryCapacity = "Battery capacity: " + SamplingLibrary.getBatteryCapacity(this) + " mAh";
        String batteryVoltage = "Battery voltage: " + SamplingLibrary.getBatteryVoltage(this) + "V";
        String pleaseSpecify = "";
        if(which == 1 || which == 3) {
            pleaseSpecify = "\n\nPlease write your feedback here";
        }

        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", "carat@cs.helsinki.fi", null));
        intent.putExtra(Intent.EXTRA_SUBJECT, title);
        intent.putExtra(Intent.EXTRA_TEXT, caratVersion + "\n" + feedback + "\n" + caratId + "\n" + jScore +
                "\n" + osVersion + "\n" + deviceModel + "\n" + memoryUsed + "\n" + memoryActive +
                "\n" + chargeCounter + "\n" + batteryCapacity + "\n" + batteryVoltage + pleaseSpecify);

        startActivity(Intent.createChooser(intent, "Send email"));
    }

    public String getLastUpdated() {
        long freshness = CaratApplication.getStorage().getFreshness();
        if(freshness <= 0){
            return getString(R.string.neverupdated);
        }
        long elapsed = System.currentTimeMillis() - freshness;
        return getTimeString(elapsed);
    }

    public String getTimeString(long elapsedTime){
        Resources res = getResources();
        long longDays = TimeUnit.MILLISECONDS.toDays(elapsedTime);
        if(elapsedTime < 0 || longDays >= Integer.MAX_VALUE){
            return getString(R.string.neverupdated);
        }

        // Casts to int should be safe from here on because we check that days
        // don't exceed Integer.MAX_VALUE. In other words, days cannot overflow
        // because of the condition, hours cannot go beyond 24 because then we'd
        // have days and same for the minutes.
        int days = (int)longDays;
        int weeks = days / 7;
        if(weeks > 0){
            return getString(R.string.updated) +
                    " " + res.getQuantityString(R.plurals.weeks, weeks, weeks) +
                    " " + getString(R.string.ago);
        }
        if(days > 0){
            return getString(R.string.updated) +
                    " " + res.getQuantityString(R.plurals.days, days, days) +
                    " " + getString(R.string.ago);
        }
        int hours = (int)TimeUnit.MILLISECONDS.toHours(elapsedTime);
        if(hours > 0){
            return getString(R.string.updated) +
                    " " + res.getQuantityString(R.plurals.hours, hours, hours) +
                    " " + getString(R.string.ago);
        }
        int minutes = (int)TimeUnit.MILLISECONDS.toMinutes(elapsedTime);
        if(minutes > 0){
            return getString(R.string.updated) +
                    " " + res.getQuantityString(R.plurals.minutes, minutes, minutes) +
                    " " + getString(R.string.ago);
        }
        else {
            return getString(R.string.updatedjustnow);
        }
    }

    public boolean isStatsDataAvailable() {
        if (isStatsDataLoaded()) {
            return true;
        } else {
            return isStatsDataStoredInPref();
        }
    }

    private boolean isStatsDataLoaded() {
        return mHogs != Constants.VALUE_NOT_AVAILABLE && mBugs != Constants.VALUE_NOT_AVAILABLE
                && appBugs != Constants.VALUE_NOT_AVAILABLE && iosHogs != Constants.VALUE_NOT_AVAILABLE
                && userHasBug != Constants.VALUE_NOT_AVAILABLE;
    }

    private boolean isStatsDataStoredInPref() {
        int appWellbehaved = CaratApplication.mPrefs.getInt(Constants.STATS_APP_WELLBEHAVED_COUNT_PREFERENCE_KEY, Constants.VALUE_NOT_AVAILABLE);
        int appHogs = CaratApplication.mPrefs.getInt(Constants.STATS_APP_HOGS_COUNT_PREFERENCE_KEY, Constants.VALUE_NOT_AVAILABLE);
        int appBugs = CaratApplication.mPrefs.getInt(Constants.STATS_APP_BUGS_COUNT_PREFERENCE_KEY, Constants.VALUE_NOT_AVAILABLE);

        int wellbehaved = CaratApplication.mPrefs.getInt(Constants.STATS_WELLBEHAVED_COUNT_PREFERENCE_KEY, Constants.VALUE_NOT_AVAILABLE);
        int hogs = CaratApplication.mPrefs.getInt(Constants.STATS_HOGS_COUNT_PREFERENCE_KEY, Constants.VALUE_NOT_AVAILABLE);
        int bugs = CaratApplication.mPrefs.getInt(Constants.STATS_BUGS_COUNT_PREFERENCE_KEY, Constants.VALUE_NOT_AVAILABLE);

        int iosWellbehaved = CaratApplication.mPrefs.getInt(Constants.STATS_IOS_WELLBEHAVED_COUNT_PREFERENCE_KEY, Constants.VALUE_NOT_AVAILABLE);
        int iosHogs = CaratApplication.mPrefs.getInt(Constants.STATS_IOS_HOGS_COUNT_PREFERENCE_KEY, Constants.VALUE_NOT_AVAILABLE);
        int iosBugs = CaratApplication.mPrefs.getInt(Constants.STATS_IOS_BUGS_COUNT_PREFERENCE_KEY, Constants.VALUE_NOT_AVAILABLE);

        int userBugs = CaratApplication.mPrefs.getInt(Constants.STATS_USER_BUGS_COUNT_PREFERENCE_KEY, Constants.VALUE_NOT_AVAILABLE);
        int userNoBugs = CaratApplication.mPrefs.getInt(Constants.STATS_USER_NO_BUGS_COUNT_PREFERENCE_KEY, Constants.VALUE_NOT_AVAILABLE);

        if (wellbehaved != Constants.VALUE_NOT_AVAILABLE && hogs != Constants.VALUE_NOT_AVAILABLE &&
                bugs != Constants.VALUE_NOT_AVAILABLE && appWellbehaved != Constants.VALUE_NOT_AVAILABLE
                && iosWellbehaved != Constants.VALUE_NOT_AVAILABLE && userBugs != Constants.VALUE_NOT_AVAILABLE) {
            this.appWellbehaved = appWellbehaved;
            this.appBugs = appBugs;
            this.appHogs = appHogs;
            mWellbehaved = wellbehaved;
            mHogs = hogs;
            mBugs = bugs;
            mActions = hogs + bugs;
            this.iosWellbehaved = iosWellbehaved;
            this.iosBugs = iosBugs;
            this.iosHogs = iosHogs;
            this.userHasBug = userBugs;
            this.userHasNoBugs = userNoBugs;
            return true;
        } else {
            return false;
        }
    }

    /* public HashMap<String, Integer> getIosDevices() {
        return iosDevices;
    }

    public void setIosDevices(HashMap<String, Integer> iosDevices) {
        this.iosDevices = iosDevices;
    } */

    public HashMap<String, Integer> getAndroidDevices() {
        return androidDevices;
    }

    public void setAndroidDevices(HashMap<String, Integer> androidDevices) {
        this.androidDevices = androidDevices;
    }

    public void GoToWifiScreen() {
        safeStart(android.provider.Settings.ACTION_WIFI_SETTINGS, getString(R.string.wifisettings));
    }

    public void safeStart(String intentString, String thing) {
        Intent intent = null;
        try {
            intent = new Intent(intentString);
            startActivity(intent);
        } catch (Throwable th) {
            if (thing != null) {
                Toast t = Toast.makeText(this, getString(R.string.opening) + thing + getString(R.string.notsupported),
                        Toast.LENGTH_SHORT);
                t.show();
            }
        }
    }

    public void showKeyboard(View v){
        InputMethodManager imm = (InputMethodManager) this.getSystemService(INPUT_METHOD_SERVICE);
        if (v == null) v = this.getCurrentFocus();
        if (v == null) v = new View(this);
        imm.showSoftInput(v, InputMethodManager.SHOW_FORCED);
    }

    public void hideKeyboard(View v){
        InputMethodManager imm = (InputMethodManager) this.getSystemService(INPUT_METHOD_SERVICE);
        if (v == null) v = this.getCurrentFocus();
        if (v == null) v = new View(this);
        imm.hideSoftInputFromWindow(v.getApplicationWindowToken(), 0);
    }

    public void loadHomeScreen(){
        hideKeyboard(null);
        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    @Override
    public void onBackPressed() {
        hideKeyboard(null);
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
            finish();
        } else {
            getSupportFragmentManager().popBackStack();
        }
    }


    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.back_arrow) {
            hideKeyboard(null);
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_HOME);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            } else {
                getSupportFragmentManager().popBackStack();
            }
        }
    }
}


