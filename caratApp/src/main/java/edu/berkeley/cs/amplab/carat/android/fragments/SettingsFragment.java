package edu.berkeley.cs.amplab.carat.android.fragments;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Switch;

import java.util.Map;

import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.Keys;
import edu.berkeley.cs.amplab.carat.android.MainActivity;
import edu.berkeley.cs.amplab.carat.android.R;
import edu.berkeley.cs.amplab.carat.android.sampling.RapidSampler;
import edu.berkeley.cs.amplab.carat.android.utils.PreferenceUtil;
import edu.berkeley.cs.amplab.carat.android.utils.ProcessUtil;
import edu.berkeley.cs.amplab.carat.android.utils.Util;

//
//  Created by Jonatan C Hamberg on 8.8.2016.
//  Copyright © 2016 University of Helsinki. All rights reserved.
//
public class SettingsFragment extends PreferenceFragmentCompat implements SharedPreferences.OnSharedPreferenceChangeListener{

    private MainActivity mainActivity;
    private SharedPreferences globalPrefs;
    private SharedPreferences localPrefs;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mainActivity = (MainActivity) activity;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity){
            mainActivity =(MainActivity) context;
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        // Ensure that local preferences are in sync
        addPreferencesFromResource(R.xml.settings);

        globalPrefs = mainActivity.getSharedPreferences(Constants.PREFERENCE_FILE_NAME, Context.MODE_PRIVATE);
        localPrefs = getPreferenceScreen().getSharedPreferences();
        PreferenceUtil.copy(globalPrefs, localPrefs);

        String impactKey = getString(R.string.hog_hide_threshold);
        String impactValue = globalPrefs.getString(impactKey, "");
        if(!impactValue.isEmpty()){
            ListPreference impact = (ListPreference) findPreference(impactKey);
            impact.setValue(impactValue);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        mainActivity.setUpActionBar(R.string.settings, true);
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key){
        Map<String, ?> localPreferences = sharedPreferences.getAll();
        SharedPreferences globalPreferences = mainActivity
                .getSharedPreferences(Constants.PREFERENCE_FILE_NAME, Context.MODE_PRIVATE);
        boolean exists = localPreferences.containsKey(key);
        if(exists){
            Object value = localPreferences.get(key);
            if(value instanceof String){
                globalPreferences.edit().putString(key, (String)value).apply();
            }
            else if(value instanceof Boolean){
                boolean enabled = (Boolean) value;

                // Delete questionnaires immediately after toggling the option
                if(enabled && key.equals(getString(R.string.disable_questionnaires))){
                    mainActivity.deleteQuestionnaires();
                }
                globalPreferences.edit().putBoolean(key, (Boolean) value).apply();
            }
        }
        if(key.equals(Keys.rapidSamplingDisabled)){
            boolean enabled = sharedPreferences.getBoolean(Keys.rapidSamplingDisabled, false);
            if(enabled){
                Context context = getContext();
                if(ProcessUtil.isServiceRunning(context, RapidSampler.class)){
                    context.stopService(new Intent(context, RapidSampler.class));
                }
            }
        }
        if(key.equals(Keys.noNotifications)){
            boolean noNotifications = sharedPreferences.getBoolean(Keys.noNotifications, false);
            boolean rapidSamplingDisabled = sharedPreferences.getBoolean(Keys.rapidSamplingDisabled, false);
            if(noNotifications && !rapidSamplingDisabled && Constants.RAPID_SAMPLING_ENABLED){
                String text = "Turning off notifications will also disable charging measurements as the background service requires an ongoing notification";
                Util.showConfirmationDialog(getContext(), getString(R.string.confirmDisableNotifications), () -> {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                    preferences.edit().putBoolean(Keys.rapidSamplingDisabled, true).apply();
                    SwitchPreferenceCompat preference = (SwitchPreferenceCompat) findPreference(Keys.rapidSamplingDisabled);
                    preference.setChecked(true);
                }, () -> {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
                    preferences.edit().putBoolean(key, false).apply();
                    SwitchPreferenceCompat preference = (SwitchPreferenceCompat) findPreference(Keys.noNotifications);
                    preference.setChecked(false);
                });
            }
        }
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // Manually disable settings button from menu
        MenuItem item = menu.findItem(R.id.action_settings);
        item.setVisible(false);
    }
}
