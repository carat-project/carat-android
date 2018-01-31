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

import android.content.Context;

import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.utils.AssetUtils;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;
import edu.berkeley.cs.amplab.carat.thrift.CaratService;

/**
 * Client for the Carat Protocol.
 * 
 * @author Eemil Lagerspetz
 * @author Jonatan Hamberg
 */
public class ProtocolClient {
    public static final String TAG = ProtocolClient.class.getSimpleName();
    public static boolean legacy = false; // HOTFIX for protocol issues. Remove when new protocol works again.

    public enum ServerLocation {GLOBAL, EU, USA}
    
    public static CaratService.Client open(Context c, ServerLocation location) throws TTransportException {
        TProtocol protocol = getProtocol(location, c);
        CaratService.Client instance = new CaratService.Client(protocol);

        TTransport transport = protocol.getTransport();
        if (transport != null && !transport.isOpen()){
            transport.open();
        }

        return instance;
    }

    public static <T> T run(Context context, ServerLocation location, ClientCallable<T> callable){
        CaratService.Client client = null;
        T result = null;
        try {
            client = open(context, location);
            result = callable.task(client);
        } catch (TTransportException e) {
            Logger.e(TAG, "Thrift connection failed " + e);
        }
        close(client);
        return result;
    }

    private static TProtocol getProtocol(ServerLocation location, Context c) throws TTransportException {
        TTransport transport = null;
        TProtocolFactory factory;

        String SERVER_GLOBAL = PropertyLoader.getGlobalServer(c);
        String SERVER_EU = PropertyLoader.getEuServer(c);
        int PORT_GLOBAL = PropertyLoader.getGlobalPort(c);
        int PORT_EU = PropertyLoader.getEuPort(c);

        if(legacy && location == ServerLocation.GLOBAL){
            factory = new TBinaryProtocol.Factory(true, true);
            transport = new TSocket(SERVER_GLOBAL, 8080);
        } else {
            factory = new TCompactProtocol.Factory();
            TSSLTransportFactory.TSSLTransportParameters params = getParams(c);
            int timeout = Constants.THRIFT_CONNECTION_TIMEOUT;

            switch(location){
                case GLOBAL:
                    transport = TSSLTransportFactory.getClientSocket(SERVER_GLOBAL, PORT_GLOBAL, timeout, params);
                    break;
                case EU:
                    transport = TSSLTransportFactory.getClientSocket(SERVER_EU, PORT_EU, timeout, params);
                    break;
            }
        }

        return factory.getProtocol(transport);
    }

    private static TSSLTransportFactory.TSSLTransportParameters getParams(Context c){
        TSSLTransportFactory.TSSLTransportParameters params = new TSSLTransportFactory.TSSLTransportParameters();
        String trustStorePath = AssetUtils.getAssetPath(c, PropertyLoader.getTrustStoreName(c));
        params.setTrustStore(trustStorePath, PropertyLoader.getTrustStorePass(c), null, "BKS");
        return params;
    }

    private static void close(CaratService.Client client) {
        if(client != null){
            TProtocol input = client.getInputProtocol();
            TProtocol output = client.getOutputProtocol();
            close(input);
            close(output);
        }
    }

    private static void close(TProtocol protocol){
        if(protocol != null){
            TTransport transport = protocol.getTransport();
            if(transport != null){
                transport.close();
            }
        }
    }
}
