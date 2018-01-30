package edu.berkeley.cs.amplab.carat.android.protocol;

import android.content.Context;
import android.content.res.AssetManager;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import edu.berkeley.cs.amplab.carat.android.Keys;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;
import edu.berkeley.cs.amplab.carat.android.utils.Util;

/**
 * Created by Jonatan Hamberg on 30.1.2018.
 */
public class PropertyLoader {
    private static final String TAG = PropertyLoader.class.getSimpleName();

    public final static String SERVER_FILE = "caratserver.properties";
    public final static String TRUSTSTORE_FILE = "truststore.properties";

    private static String SERVER_GLOBAL = null;
    private static String SERVER_EU = null;
    private static Integer PORT_GLOBAL = null;
    private static Integer PORT_EU = null;
    private static String TRUSTSTORE_NAME = null;
    private static String TRUSTSTORE_PASS = null;

    public static class Defaults {
        private final static String SERVER_GLOBAL = "caratserver.cs.helsinki.fi";
        private final static String SERVER_EU = "caratserver-eu.cs.helsinki.fi";
        private final static Integer PORT_GLOBAL = 8080;
        private final static Integer PORT_EU = 8080;
        private final static String TRUSTSTORE_NAME = "truststore.bks";
        private final static String TRUSTSTORE_PASS = ""; // TODO: Build flag?
    }

    public static String getGlobalServer(Context context){
        if(SERVER_GLOBAL == null){
            loadServerProperties(context);
        }
        return SERVER_GLOBAL;
    }

    public static String getEuServer(Context context){
        if(SERVER_EU == null){
            loadServerProperties(context);
        }
        return SERVER_EU;
    }

    public static int getGlobalPort(Context context){
        if(PORT_GLOBAL == null){
            loadServerProperties(context);
        }
        return PORT_GLOBAL;
    }

    public static int getEuPort(Context context){
        if(PORT_EU == null){
            loadServerProperties(context);
        }
        return PORT_EU;
    }

    public static String getTrustStoreName(Context context){
        if(TRUSTSTORE_NAME == null){
            loadTrustStoreProperties(context);
        }
        return TRUSTSTORE_NAME;
    }

    public static String getTrustStorePass(Context context){
        if(TRUSTSTORE_PASS == null){
            loadTrustStoreProperties(context);
        }
        return TRUSTSTORE_PASS;
    }

    private static void loadServerProperties(Context context){
        Properties properties = loadProperties(context, SERVER_FILE);
        if(!Util.isNullOrEmpty(properties)){
            SERVER_GLOBAL = properties.getProperty(Keys.serverAddressGlobal, Defaults.SERVER_GLOBAL);
            SERVER_EU = properties.getProperty(Keys.serverAddressEu, Defaults.SERVER_EU);
            PORT_GLOBAL = getIntProperty(properties, Keys.serverPortGlobal, Defaults.PORT_GLOBAL);
            PORT_EU = getIntProperty(properties, Keys.serverPortEu, Defaults.PORT_EU);
        } else {
            SERVER_GLOBAL = Defaults.SERVER_GLOBAL;
            SERVER_EU = Defaults.SERVER_EU;
            PORT_GLOBAL = Defaults.PORT_GLOBAL;
            PORT_EU = Defaults.PORT_EU;
        }
    }

    private static void loadTrustStoreProperties(Context context){
        Properties properties = loadProperties(context, TRUSTSTORE_FILE);
        if(!Util.isNullOrEmpty(properties)){
            TRUSTSTORE_NAME = properties.getProperty(Keys.trustStoreName, Defaults.TRUSTSTORE_NAME);
            TRUSTSTORE_PASS = properties.getProperty(Keys.trustStorePass, Defaults.TRUSTSTORE_PASS);
        } else {
            TRUSTSTORE_NAME = Defaults.TRUSTSTORE_NAME;
            TRUSTSTORE_PASS = Defaults.TRUSTSTORE_PASS;
        }
    }

    private static Integer getIntProperty(Properties properties, String key, int defaultValue){
        if(Util.isNullOrEmpty(properties)){
            return defaultValue;
        }
        Integer result = defaultValue;
        try {
            String value = properties.getProperty(key);
            if(!Util.isNullOrEmpty(value)){
                return Integer.parseInt(value);
            }
        } catch (NumberFormatException e){
            Logger.d(TAG, "Failed getting property " + key + " falling back to default");
        }
        return result;
    }

    private static Properties loadProperties(Context context, String file){
        try {
            Properties properties = new Properties();
            AssetManager assetManager = context.getAssets();
            if(assetManager != null){
                InputStream inputStream = assetManager.open(file);
                properties.load(inputStream);
                return properties;
            }
        } catch (IOException e) {
            Logger.d(TAG, "Failed to read properties from " + file + " " + e);
        }
        return null;
    }
}
