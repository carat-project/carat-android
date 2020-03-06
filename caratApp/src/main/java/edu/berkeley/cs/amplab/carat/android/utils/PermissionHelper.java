package edu.berkeley.cs.amplab.carat.android.utils;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;

import java.util.LinkedList;
import java.util.List;

public class PermissionHelper {
    private static final String TAG = PermissionHelper.class.getSimpleName();
    private static final int REQUEST_CODE = 8762;

    public static void promptMissingPermissions(Activity activity) {
        Context context = activity.getApplicationContext();
        String[] permissions = getPackagePermissions(context);
        if (permissions == null || permissions.length <= 0) {
            Logger.e(TAG, context.getPackageName() + " had empty or null permissions!");
            return;
        }
        String[] missing = filterMissingPermissions(activity, permissions);
        if (missing != null && missing.length > 0) {
            ActivityCompat.requestPermissions(activity, missing, REQUEST_CODE);
        }
    }

    private static String[] filterMissingPermissions(Context context, String[] permissions) {
        List<String> missing = new LinkedList<>();
        for (String permission : permissions) {
            try {
                int code = ActivityCompat.checkSelfPermission(context, permission);
                if (code != PackageManager.PERMISSION_GRANTED) {
                    missing.add(permission);
                }
            } catch (IllegalArgumentException e) {
                Logger.e(TAG, "Permission " + permission + " does not exist!", e);
            }
        }
        return missing.toArray(new String[0]);
    }

    private static String[] getPackagePermissions(Context context) {
        PackageManager pm = context.getPackageManager();
        if (pm == null) {
            return null;
        }

        String pkg = context.getPackageName();
        try {
            PackageInfo info = pm.getPackageInfo(pkg, PackageManager.GET_PERMISSIONS);
            if (info == null) {
                return null;
            }
            return info.requestedPermissions;
        } catch (PackageManager.NameNotFoundException e) {
            Logger.e(TAG, "Failed finding permissions for package " + pkg, e);
            return null;
        }
    }
}
