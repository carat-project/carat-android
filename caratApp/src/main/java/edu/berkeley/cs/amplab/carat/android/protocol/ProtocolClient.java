package edu.berkeley.cs.amplab.carat.android.protocol;

import java.io.IOException;
import java.io.InputStream;
import java.security.Security;
import java.util.Properties;

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import android.content.Context;
import android.util.Log;

import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.utils.AssetUtils;
import edu.berkeley.cs.amplab.carat.thrift.CaratService;

/**
 * Client for the Carat Protocol.
 * 
 * @author Eemil Lagerspetz
 * 
 */
public class ProtocolClient {
    public static final String TAG = "ProtocolClient";
    public static final String SERVER_PROPERTIES = "caratserver.properties";
    public static int SERVER_PORT_GLOBAL = 0;
    public static String SERVER_ADDRESS_GLOBAL = null;
    public static int SERVER_PORT_EU = 0;
    public static String SERVER_ADDRESS_EU = null;

    public static final String TRUSTSTORE_PROPERTIES = "truststore.properties";
    public static String TRUSTSTORE_NAME = null;
    public static String TRUSTSTORE_PASS = null;

    public enum ServerLocation {GLOBAL, EU, USA}

    /**
     * FIXME: this needs to come from a factory, so that connections are not
     * kept open unnecessarily, and that they do not become stale, and that we
     * handle disconnections gracefully.
     * 
     * @param c
     * @return
     * @throws NumberFormatException 
     * @throws TTransportException 
     */
    public static CaratService.Client getInstance(Context c, ServerLocation location) throws NumberFormatException, TTransportException {
        if(SERVER_ADDRESS_GLOBAL == null || SERVER_ADDRESS_EU == null){
            if(!loadServerProperties(c)){
                return null; // Failed to load server properties
            }
        }

        TTransport transport = null;
        if(location == ServerLocation.GLOBAL){
            if(SERVER_ADDRESS_GLOBAL == null || SERVER_PORT_GLOBAL == 0) return null;
            transport = new TSocket(SERVER_ADDRESS_GLOBAL, SERVER_PORT_GLOBAL, Constants.THRIFT_CONNECTION_TIMEOUT);
        }
        else if(location == ServerLocation.EU){
            if(TRUSTSTORE_NAME == null || TRUSTSTORE_PASS == null){
                if(!loadSSLProperties(c)){
                    return null; // Failed to load SSL properties
                }
            }
            if(SERVER_ADDRESS_EU == null || SERVER_PORT_EU == 0) return null;
            TSSLTransportFactory.TSSLTransportParameters params = new TSSLTransportFactory.TSSLTransportParameters();
            String truststorePath = AssetUtils.getAssetPath(c, TRUSTSTORE_NAME);
            params.setTrustStore(truststorePath, TRUSTSTORE_PASS, null, "BKS"); // Important: Use BKS!
            transport = TSSLTransportFactory.getClientSocket(SERVER_ADDRESS_EU, SERVER_PORT_EU, Constants.THRIFT_CONNECTION_TIMEOUT, params);
        }

        TProtocol p = new TBinaryProtocol(transport, true, true);
        CaratService.Client instance = new CaratService.Client(p);
        if (transport != null && !transport.isOpen()){
            transport.open();
        }

        return instance;
    }

    private static boolean loadSSLProperties(Context c){
        Properties properties = new Properties();
        try {
            InputStream raw = c.getAssets().open(TRUSTSTORE_PROPERTIES);
            properties.load(raw);
            TRUSTSTORE_NAME = properties.getProperty("storeName");
            TRUSTSTORE_PASS = properties.getProperty("storePass");
            return TRUSTSTORE_NAME != null && TRUSTSTORE_PASS != null;
        } catch(Throwable th){
            Log.e(TAG, "Could not open truststore property file!");
            th.printStackTrace();
        }
        return false;
    }

    private static boolean loadServerProperties(Context c){
        Properties properties = new Properties();
        try {
            InputStream raw = c.getAssets().open(SERVER_PROPERTIES);
            if(raw != null){
                properties.load(raw);

                SERVER_PORT_GLOBAL = Integer.parseInt(properties.getProperty("PORT_GLOBAL", "8080"));
                SERVER_ADDRESS_GLOBAL = properties.getProperty("ADDRESS_GLOBAL", "server.caratproject.com");
                SERVER_PORT_EU = Integer.parseInt(properties.getProperty("PORT_EU", "8080"));
                SERVER_ADDRESS_EU = properties.getProperty("ADDRESS_EU", "caratserver-eu.cs.helsinki.fi");

                if(Constants.DEBUG){
                    Log.d(TAG, "Set global address=" + SERVER_ADDRESS_GLOBAL + " port=" + SERVER_PORT_GLOBAL);
                    Log.d(TAG, "Set eu address=" + SERVER_ADDRESS_EU + " port=" + SERVER_PORT_EU);
                }
                return true;
            } else {
                Log.e(TAG, "Could not open server property file!");
            }
        } catch (IOException e) {
            Log.e(TAG, "Could not open server property file: " + e.toString());
        }
        return false;
    }
    
    public static CaratService.Client open(Context c, ServerLocation location) throws NumberFormatException, TTransportException {
        if (Constants.DEBUG)
            Log.d("ProtocolClient", "trying to get an instance of CaratProtocol.");
        return getInstance(c, location);
    }
}
