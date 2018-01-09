package edu.berkeley.cs.amplab.carat.android.utils;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import edu.berkeley.cs.amplab.carat.android.Constants;

/**
 * General utilities for easier handling of Android assets.
 *
 * Created by Jonatan Hamberg on 4.6.2016.
 * Copyright © 2016 University of Helsinki. All rights reserved.
 */
public class AssetUtils {
    private static final String TAG = "AssetUtils";
    private static boolean invalidate = true;

    public static boolean isAssetCached(Context context, String fileName){
        File file = new File(context.getCacheDir(), fileName);
        return file.exists();
    }

    public static String getAssetPath(Context context, String fileName){
        File cached = getAssetFile(context, fileName);
        if(cached != null){
            return cached.getAbsolutePath();
        }
        return null;
    }

    public static File getAssetFile(Context context, String fileName) {
        File cached = new File(context.getCacheDir(), fileName);
        if(!invalidate && cached.exists()){
            return cached;
        } else {
            return writeAssetToCache(context, fileName);
        }
    }

    private static File writeAssetToCache(Context context, String fileName){
        File cached = new File(context.getCacheDir(), fileName);
        try {
            InputStream in = context.getAssets().open(fileName);
            try {
                FileOutputStream out = new FileOutputStream(cached);
                try {
                    byte[] buf = new byte[1024];
                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }
                } finally {
                    out.close();
                }
            } finally {
                in.close();
            }
        } catch (Throwable th) {
            if(Constants.DEBUG){
                Logger.e(TAG, "Could not open asset file " + fileName + " for caching!", th);
            }
        }
        invalidate = false;
        return cached;
    }
}
