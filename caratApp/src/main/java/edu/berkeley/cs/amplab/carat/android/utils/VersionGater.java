package edu.berkeley.cs.amplab.carat.android.utils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;

import org.json.JSONException;
import org.json.JSONObject;

import edu.berkeley.cs.amplab.carat.android.BuildConfig;
import edu.berkeley.cs.amplab.carat.android.CaratApplication;
import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.R;
import edu.berkeley.cs.amplab.carat.android.storage.CaratDataStorage;

/**
 * Created by Jonatan Hamberg on 6/30/17.
 */
public class VersionGater {
    private static final String TAG = VersionGater.class.getSimpleName();
    private Activity activity;
    private Context context;
    private SharedPreferences preferences;
    private int versionCode;
    private boolean mustUpdate;
    private String packageName;
    private String title;
    private String message;

    public VersionGater(Activity activity){
        this.activity = activity;
        this.context = activity.getApplicationContext();
        this.preferences = PreferenceManager.getDefaultSharedPreferences(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    public static void checkVersion(Activity activity){
        VersionGater versionGater = new VersionGater(activity);
        versionGater.check();
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    private void check(){
        AsyncTask.execute(() -> {
            try {
                boolean online = NetworkingUtil.isOnline(context);
                String json = getVersionGateJSON(online);

                if(!Util.isNullOrEmpty(json)) {
                    JSONObject jsonObject = new JSONObject(json);
                    versionCode = jsonObject.getInt("versionCode");
                    mustUpdate = jsonObject.getBoolean("mustUpdate");
                    packageName = jsonObject.getString("packageName");
                    message = jsonObject.getString("message");
                    title = jsonObject.getString("title");
                    if (isVersionTooOld(versionCode)) {
                        displayDialog();
                    }
                }
            } catch (JSONException e) {
                Logger.e(TAG, "Error parsing version gating JSON", e);
            }
        });
    }

    private String getVersionGateJSON(boolean online){
        CaratDataStorage storage = CaratApplication.getStorage();
        String json = null;
        if(online){
            json = JsonParser.getJSONFromUrl(Constants.WEBSITE+"version.json");
            if(storage != null && !Util.isNullOrEmpty(json)){
                storage.writeVersionGateJSON(json);
            }
        } else {
            if(storage != null){
                json = storage.getVersionGateJSON();
            }
        }
        return json;
    }

    private boolean isVersionTooOld(int versionCode){
        return BuildConfig.VERSION_CODE <= versionCode;
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    private void displayDialog(){
        activity.runOnUiThread(() -> {
            activity.setFinishOnTouchOutside(false);
            String exit = mustUpdate ? "Exit" : "Not now";
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            builder.setTitle(title);
            builder.setMessage(message);
            builder.setPositiveButton("OK", null);
            builder.setNegativeButton(exit, null);
            builder.setIcon(R.drawable.carat_material_icon);
            builder.setOnKeyListener((dialog, keyCode, event) -> true);

            AlertDialog alertDialog = builder.create();
            alertDialog.setOnShowListener(dialog -> {
                Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                Button negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                positiveButton.setOnClickListener(v -> {
                    Util.openStorePage(context, packageName);
                });
                negativeButton.setOnClickListener(v -> {
                    if(mustUpdate){
                        if(activity != null){
                            activity.finish();
                        }
                        System.exit(0);
                    } else {
                        alertDialog.dismiss();
                        activity.setFinishOnTouchOutside(true);
                        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
                    }
                });
            });
            alertDialog.setCancelable(false);
            alertDialog.setCanceledOnTouchOutside(mustUpdate);
            activity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
            Window window = alertDialog.getWindow();
            if(window != null){
                alertDialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL);
            }
            alertDialog.show();
            alertDialog.getButton(Dialog.BUTTON_NEGATIVE).setTextColor(Color.rgb(248, 176, 58));
            alertDialog.getButton(Dialog.BUTTON_POSITIVE).setTextColor(Color.rgb(248, 176, 58));
        });
    }
}
