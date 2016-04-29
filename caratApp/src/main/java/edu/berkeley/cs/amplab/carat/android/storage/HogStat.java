package edu.berkeley.cs.amplab.carat.android.storage;

import java.io.Serializable;

/**
 * Created by Jonatan Hamberg on 28.4.2016.
 */
public class HogStat implements Serializable, Comparable<HogStat> {

    /**
     * Auto-generated UID for serialization
     */
    private static final long serialVersionUID = -7501985396874679871L;

    private String appName;
    private long killBenefit;
    private long users;
    private long samples;
    private String packageName;

    public HogStat(String appName, long killBenefit, long users,
                   long samples, String packageName){
        this.appName = appName;
        this.killBenefit = killBenefit;
        this.users = users;
        this.samples = samples;
        this.packageName = packageName;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getAppName() {
        return appName;
    }

    public long getKillBenefit() {
        return killBenefit;
    }

    public long getUsers() {
        return users;
    }

    public long getSamples() {
        return samples;
    }

    @Override
    public int compareTo(HogStat another) {
        Long a = killBenefit;
        Long b = another.killBenefit;
        return a.compareTo(b);
    }
}
