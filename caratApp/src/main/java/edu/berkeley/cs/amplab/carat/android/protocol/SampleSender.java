package edu.berkeley.cs.amplab.carat.android.protocol;

import java.util.Set;
import java.util.SortedMap;

import android.content.Context;
import android.util.Log;
import edu.berkeley.cs.amplab.carat.android.CaratApplication;
import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.R;
import edu.berkeley.cs.amplab.carat.android.storage.SampleDB;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;
import edu.berkeley.cs.amplab.carat.android.utils.NetworkingUtil;
import edu.berkeley.cs.amplab.carat.android.utils.Util;
import edu.berkeley.cs.amplab.carat.thrift.Sample;

/**
 * Communicates with the Carat Server. Sends samples stored in CaratDB every
 * COMMS_INTERVAL ms.
 * 
 * @author Eemil Lagerspetz
 * 
 */
public class SampleSender {
    
    private static final String TAG = "sendSamples";
    private static final Object sendLock = new Object();

    public static boolean sendingSamples = false;

    // Prevent instantiation
    private SampleSender(){}
    
    public static boolean sendSamples(CaratApplication app) {
        Logger.d(Constants.SF, "Awaiting for sendLock...");
        synchronized(sendLock){
            Context c = app.getApplicationContext();
            boolean online = NetworkingUtil.canConnect(c);
            Logger.d(Constants.SF, "SampleSender online: " + online);
            if (online) {
                Logger.d(TAG, "Awaiting SampleDB");
                SampleDB db = SampleDB.getInstance(c);
                Logger.d(TAG, "Got SampleDB");
                CommunicationManager commManager = app.commManager;
                if(commManager == null){
                    Logger.d(TAG, "Communication manager is not ready yet");
                    return false;
                }
                int sampleCount = db.countSamples();
                String progressString = "0% of "+sampleCount +" "+ app.getString(R.string.samplesreported);
                CaratApplication.setStatus(progressString);
                int successSum = 0;
                int failures = 0;
                Logger.d(TAG, "Entering query phase for stored samples");
                SortedMap<Long, Sample> batch = db.queryOldestSamples(Constants.COMMS_MAX_UPLOAD_BATCH);
                Logger.d(TAG, "Queried a batch of samples of size: " + batch.size());
                sendingSamples = true;
                while(batch.size() > 0 && failures <= 3){
                    Logger.d(TAG, "Attempt " + failures + " at uploading samples");
                    int sent = commManager.uploadSamples(batch.values(), successSum, sampleCount);
                    if(sent > 0){
                        failures = 0; // Reset tries
                        successSum += sent;
                        /*
                        Report this within uploadSamples for better granularity
                        int progress = (int)(successSum*100.0 / sampleCount);
                        progressString = progress + "% " + app.getString(R.string.samplesreported);
                        CaratApplication.setStatus(progressString);
                        */
                        // Delete samples that were sent successfully
                        Logger.d(TAG, "Attempting to delete " + sent + " samples..");
                        Set<Long> sentRowIds = Util.firstEntries(sent, batch).keySet();
                        db.deleteSamples(sentRowIds);
                        Logger.d(TAG, "Deleted sent and discarded samples");
                    } else {
                        Log.d(TAG, "Failed sending batch, increasing failures to " + failures);
                        failures++;
                    }
                    batch = db.queryOldestSamples(Constants.COMMS_MAX_UPLOAD_BATCH);
                }
                commManager.disposeRpcService(); //
                sendingSamples = false;
                return db.countSamples() == 0 || successSum == sampleCount;
            } else {
                Logger.d(TAG, "Not online, cannot send samples");
            }
            return false;
        }
    }

    public static boolean isSendingSamples(){
        return sendingSamples;
    }
}
