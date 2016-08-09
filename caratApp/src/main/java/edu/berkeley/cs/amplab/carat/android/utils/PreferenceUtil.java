package edu.berkeley.cs.amplab.carat.android.utils;

import android.content.SharedPreferences;
import java.util.Map;

//
//  Created by Jonatan C Hamberg on 9.8.2016.
//  Copyright © 2016 University of Helsinki. All rights reserved.
//
public class PreferenceUtil {

    /**
     * Copies values between shared preferences
     * @param origin Preferences to copy from
     * @param target Preferences to be copied to
     */
    public static void copy(SharedPreferences origin, SharedPreferences target) {
        SharedPreferences.Editor targetEditor = target.edit();
        for (Map.Entry<String, ?> entry : origin.getAll().entrySet()) {
            Object v = entry.getValue();
            String key = entry.getKey();
            if (v instanceof Boolean) {
                targetEditor.putBoolean(key, (Boolean) v);
            } else if (v instanceof Float) {
                targetEditor.putFloat(key, (Float) v);
            } else if (v instanceof Integer) {
                targetEditor.putInt(key, (Integer) v);
            } else if (v instanceof Long) {
                targetEditor.putLong(key, (Long) v);
            } else if (v instanceof String) {
                targetEditor.putString(key, (String) v);
            }
            targetEditor.commit();
        }
    }
}
