package edu.berkeley.cs.amplab.carat.android.utils;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Map.Entry;

import edu.berkeley.cs.amplab.carat.android.CaratApplication;

/**
 * Created by Jonatan Hamberg on 8.11.2017.
 */
public class PrefsManager extends ContentProvider{
    private static final Class contextClass = CaratApplication.class;
    private static final String AUTHORITY = "edu.berkeley.cs.amplab.carat.PREFERENCE_AUTHORITY";
    private static final int MATCH_CODE = 1;
    private static Uri BASE_URI;
    private static UriMatcher matcher;

    @Override
    public boolean onCreate() {
        if(matcher == null){
            initialize();
        }
        return false;
    }

    private SharedPreferences getSharedPreferences(){
        Context context = getContext();
        if(context != null && !contextClass.isInstance(context)){
            context = context.getApplicationContext(); // Make sure we use correct context
        }
        return PreferenceManager.getDefaultSharedPreferences(context);
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] strings, @Nullable String s, @Nullable String[] strings1, @Nullable String s1) {
        MatrixCursor cursor;
        if(matcher.match(uri) == MATCH_CODE){ // Matcher is always initialized by getContentUri
            String key = uri.getPathSegments().get(0);
            String type = uri.getPathSegments().get(1);
            cursor = new MatrixCursor(new String[]{key});
            SharedPreferences preferences = getSharedPreferences();
            if(!preferences.contains(key)){
                return cursor;
            }
            MatrixCursor.RowBuilder rowBuilder = cursor.newRow();
            Object object = null;
            if("string".equals(type)){
                object = preferences.getString(key, null);
            } else if("boolean".equals(type)){
                object = preferences.getBoolean(key, false) ? 1 : 0; // Convert to int
            } else if("long".equals(type)){
                object = preferences.getLong(key, 0L);
            } else if("integer".equals(type)){
                object = preferences.getInt(key, 0);
            } else if("float".equals(type)){
                object = preferences.getFloat(key, 0f);
            }
            rowBuilder.add(object);
        } else {
            throw new IllegalArgumentException("Unsupported type");
        }
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd." + AUTHORITY + ".item";
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        // Matcher is initialized by getContentUri
        if(matcher.match(uri) == MATCH_CODE && contentValues != null){
            SharedPreferences.Editor editor = getSharedPreferences().edit();
            for(Entry<String, Object> entry : contentValues.valueSet()){
                String key = entry.getKey();
                Object value = entry.getValue();
                if(value == null){
                    editor.remove(key).apply();
                } else if(value instanceof Boolean){
                    editor.putBoolean(key, (Boolean) value);
                } else if(value instanceof Integer){
                    editor.putInt(key, (Integer) value);
                } else if(value instanceof Float){
                    editor.putFloat(key, (Float) value);
                } else if(value instanceof Long){
                    editor.putLong(key, (Long) value);
                } else if(value instanceof String){
                    editor.putString(key, (String) value);
                } else {
                    throw new IllegalArgumentException("Unsupported type");
                }
            }
            editor.apply();
        } else {
            throw new IllegalArgumentException("Unsupported uri or value");
        }
        return null; // This value can be null
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        throw new UnsupportedOperationException();
    }

    private static void initialize(){
        matcher = new UriMatcher(UriMatcher.NO_MATCH);
        matcher.addURI(AUTHORITY, "*/*", MATCH_CODE);
        BASE_URI = Uri.parse("content://" + AUTHORITY);
    }

    private static Uri getContentUri(String key, String type){
        if(BASE_URI == null){
            initialize();
        }
        return BASE_URI.buildUpon().appendPath(key).appendPath(type).build();
    }

    private static boolean getBoolean(Cursor cursor, boolean defaultValue){
        boolean result = defaultValue;
        if(cursor != null && cursor.moveToFirst()){
            result = cursor.getInt(0) == 1;
            cursor.close();
        }
        return result;
    }

    private static int getInt(Cursor cursor, int defaultValue){
        int result = defaultValue;
        if(cursor != null && cursor.moveToFirst()){
            result = cursor.getInt(0);
            cursor.close();
        }
        return result;
    }

    private static float getFloat(Cursor cursor, float defaultValue){
        float result = defaultValue;
        if(cursor != null && cursor.moveToFirst()){
            result = cursor.getFloat(0);
            cursor.close();
        }
        return result;
    }

    private static long getLong(Cursor cursor, long defaultValue){
        long result = defaultValue;
        if(cursor != null && cursor.moveToFirst()){
            result = cursor.getLong(0);
            cursor.close();
        }
        return result;
    }

    private static String getString(Cursor cursor, String defaultValue){
        String result = defaultValue;
        if(cursor != null && cursor.moveToFirst()){
            result = cursor.getString(0);
            cursor.close();
        }
        return result;
    }

    public static MultiPrefs getPreferences(Context context){
        return new MultiPrefs(context);
    }

    public static class Editor {
        private Context context;
        private ContentValues values;

        private Editor(Context context){
            this.context = context;
            this.values = new ContentValues();
        }

        public void apply(){
            context.getContentResolver().insert(getContentUri("key", "type"), values);
        }

        public void commit(){
            apply();
        }

        public Editor putBoolean(String key, Boolean value){
            values.put(key, value);
            return this;
        }

        public Editor putInteger(String key, Integer value){
            values.put(key, value);
            return this;
        }

        public Editor putFloat(String key, Float value){
            values.put(key, value);
            return this;
        }

        public Editor putLong(String key, Long value){
            values.put(key, value);
            return this;
        }

        public Editor putString(String key, String value){
            values.put(key, value);
            return this;
        }
    }

    public static class MultiPrefs {
        private Context context;
        private MultiPrefs(Context context){
            this.context = context;
        }

        public Editor edit(){
            return new Editor(context);
        }

        private Cursor query(String key, String type){
            ContentResolver resolver = context.getContentResolver();
            return resolver.query(getContentUri(key, type), null, null, null, null);
        }

        public boolean getBoolean(String key, boolean defaultValue){
            Cursor cursor = query(key, "boolean");
            return PrefsManager.getBoolean(cursor, defaultValue);
        }

        public int getInt(String key, int defaultValue){
            Cursor cursor = query(key, "integer");
            return PrefsManager.getInt(cursor, defaultValue);
        }

        public float getFloat(String key, float defaultValue){
            Cursor cursor = query(key, "float");
            return PrefsManager.getFloat(cursor, defaultValue);
        }

        public long getLong(String key, long defaultValue){
            Cursor cursor = query(key, "long");
            return PrefsManager.getLong(cursor, defaultValue);
        }

        public String getString(String key, String defaultValue){
            Cursor cursor = query(key, "string");
            return PrefsManager.getString(cursor, defaultValue);
        }
    }
}
