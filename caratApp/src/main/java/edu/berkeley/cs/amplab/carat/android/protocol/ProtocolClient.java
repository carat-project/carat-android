package edu.berkeley.cs.amplab.carat.android.protocol;

import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TCompactProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.TZlibTransport;

import android.content.Context;

import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.utils.AssetUtils;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;
import edu.berkeley.cs.amplab.carat.android.utils.Util;
import edu.berkeley.cs.amplab.carat.thrift.CaratService;

/**
 * Client for the Carat Protocol.
 * 
 * @author Eemil Lagerspetz
 * @author Jonatan Hamberg
 */
public class ProtocolClient {
    public static final String TAG = ProtocolClient.class.getSimpleName();
    private static final int timeout = Constants.THRIFT_CONNECTION_TIMEOUT;

    public enum ServerLocation {GLOBAL, EU}

    /**
     * Generic wrapper for any task that needs temporary access to CaratClient.
     * @param context application context
     * @param location server location
     * @param callable task to be executed with CaratClient in its scope
     * @param <T> return value type
     * @return value from callable task
     */
    public static <T> T run(Context context, ServerLocation location, ClientCallable<T> callable){
        CaratService.Client client = null;
        T result = null;
        try {
            client = open(context, location);
            result = callable.task(client);
        } catch (TException e) {
            Logger.e(TAG, "Thrift connection failed: " + e);
            Util.printStackTrace(TAG, e);
        }
        close(client);
        return result;
    }

    /**
     * Opens an instance of CaratClient, responsible for communicating with remote servers
     * @param context application context
     * @param location server location
     * @return instance of CaratClient
     * @throws TTransportException when transport needed by the client fails
     */
    private static CaratService.Client open(Context context, ServerLocation location) throws TTransportException {
        TProtocol protocol = getProtocol(context, location);
        CaratService.Client instance = new CaratService.Client(protocol);

        TTransport transport = protocol.getTransport();
        if (transport != null && !transport.isOpen()){
            transport.open(); // Open whichever transport we got
        }
        return instance;
    }

    /**
     * Creates a protocol based on server location
     * @param context application context
     * @param location server location
     * @return Thrift protocol
     * @throws TTransportException when transport cannot be created
     */
    private static TProtocol getProtocol(Context context, ServerLocation location) throws TTransportException {
        TProtocolFactory factory = new TCompactProtocol.Factory();
        switch(location){
            case GLOBAL: return factory.getProtocol(getGlobalTransport(context));
            case EU: return factory.getProtocol(getEuTransport(context));
            default: throw new TTransportException("Unknown transport location: " + location);
        }
    }

    /**
     * Creates a zlib and ssl-enabled transport used with the global servers
     * @param context application context
     * @return Thrift transport with zlib on top of an ssl socket
     * @throws TTransportException when transport cannot be created
     */
    private static TTransport getGlobalTransport(Context context) throws TTransportException {
        TSSLTransportFactory.TSSLTransportParameters params = getParams(context);
        String server = PropertyLoader.getGlobalServer(context);
        int port = PropertyLoader.getGlobalPort(context);

        TTransport sslSocket = TSSLTransportFactory.getClientSocket(server, port, timeout, params);
        return new TZlibTransport(sslSocket, 9);
    }

    /**
     * Creates an ssl-enabled transport used with survey servers
     * @param context application context
     * @return Thrift transport with ssl on top of a socket
     * @throws TTransportException when transport cannot be created
     */
    private static TTransport getEuTransport(Context context) throws TTransportException {
        TSSLTransportFactory.TSSLTransportParameters params = getParams(context);
        String server = PropertyLoader.getEuServer(context);
        int port = PropertyLoader.getEuPort(context);

        return TSSLTransportFactory.getClientSocket(server, port, timeout, params);
    }

    /**
     * Loads trust store information needed for ssl transports
     * @param c application context
     * @return Thrift ssl parameters
     */
    private static TSSLTransportFactory.TSSLTransportParameters getParams(Context c){
        TSSLTransportFactory.TSSLTransportParameters params = new TSSLTransportFactory.TSSLTransportParameters();
        String trustStorePath = AssetUtils.getAssetPath(c, PropertyLoader.getTrustStoreName(c));
        params.setTrustStore(trustStorePath, PropertyLoader.getTrustStorePass(c), null, "BKS");
        return params;
    }

    // Close client safely
    private static void close(CaratService.Client client) {
        if(client != null){
            TProtocol input = client.getInputProtocol();
            TProtocol output = client.getOutputProtocol();
            close(input);
            close(output);
        }
    }

    // Close protocol safely
    private static void close(TProtocol protocol){
        if(protocol != null){
            TTransport transport = protocol.getTransport();
            if(transport != null){
                transport.close();
            }
        }
    }
}
