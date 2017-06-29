package edu.berkeley.cs.amplab.carat.android.models;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

import java.io.Serializable;

/**
 * Created by Jonatan Hamberg on 27.4.2016.
 */
public class SimpleProcessInfo  implements Serializable, Comparable<SimpleProcessInfo> {
    private String packageName;
    private String localizedName;
    private String versionName;
    private String importance;
    private Drawable icon;
    private int activityCount;
    private int serviceCount;

    public SimpleProcessInfo(){}

    public String getPackageName() {
        return packageName;
    }

    public SimpleProcessInfo setPackageName(String packageName) {
        this.packageName = packageName;
        return this;
    }

    public String getLocalizedName() {
        return localizedName;
    }

    public SimpleProcessInfo setLocalizedName(String localizedName) {
        this.localizedName = localizedName;
        return this;
    }

    public String getVersionName() {
        return versionName;
    }

    public SimpleProcessInfo setVersionName(String versionName) {
        this.versionName = versionName;
        return this;
    }

    public String getImportance() {
        return importance;
    }

    public SimpleProcessInfo setImportance(String importance) {
        this.importance = importance;
        return this;
    }

    public Drawable getIcon() {
        return icon;
    }

    public SimpleProcessInfo setIcon(Drawable icon) {
        this.icon = icon;
        return this;
    }

    public int getActivityCount() {
        return activityCount;
    }

    public SimpleProcessInfo setActivityCount(int activityCount) {
        this.activityCount = activityCount;
        return this;
    }

    public int getServiceCount() {
        return serviceCount;
    }

    public SimpleProcessInfo setServiceCount(int serviceCount) {
        this.serviceCount = serviceCount;
        return this;
    }

    @Override
    public int compareTo(@NonNull SimpleProcessInfo another) {
        Integer a1 = this.activityCount;
        Integer a2 = another.activityCount;
        if(a1.equals(a2)){
            return this.localizedName.compareToIgnoreCase(another.localizedName);
        }
        return a2.compareTo(a1);
    }
}
