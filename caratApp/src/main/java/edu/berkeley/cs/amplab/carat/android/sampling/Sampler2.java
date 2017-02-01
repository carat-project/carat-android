package edu.berkeley.cs.amplab.carat.android.sampling;

import edu.berkeley.cs.amplab.carat.android.models.SystemLoadPoint;
import edu.berkeley.cs.amplab.carat.android.utils.Util;
import edu.berkeley.cs.amplab.carat.thrift.Sample;

/**
 * Created by Jonatan on 2.2.2017.
 */

public class Sampler2 {

    public void sample(String uuId, String trigger, SamplingLibrary samplingLibrary){
        SystemLoadPoint load1 = SamplingLibrary.getSystemLoad();

        Sample sample = new Sample();
        sample.setUuId(uuId);
        sample.setTriggeredBy(trigger);
        sample.setTimestamp(System.currentTimeMillis()/1000.0);
        sample.setPiList(samplingLibrary.getRunningProcessInfoForSample());
    }

}
