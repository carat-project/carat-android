package edu.berkeley.cs.amplab.carat.android.fragments;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;

import java.util.Map;

import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.Keys;
import edu.berkeley.cs.amplab.carat.android.MainActivity;
import edu.berkeley.cs.amplab.carat.android.R;
import edu.berkeley.cs.amplab.carat.android.UsageManager;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;
import edu.berkeley.cs.amplab.carat.android.utils.PreferenceUtil;

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
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        // Manually disable settings button from menu
        MenuItem item= menu.findItem(R.id.action_settings);
        item.setVisible(false);
    }
}
