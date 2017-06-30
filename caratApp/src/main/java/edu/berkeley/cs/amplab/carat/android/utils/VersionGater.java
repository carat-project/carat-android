package edu.berkeley.cs.amplab.carat.android.utils;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.widget.Button;

import org.json.JSONException;
import org.json.JSONObject;

import edu.berkeley.cs.amplab.carat.android.BuildConfig;
import edu.berkeley.cs.amplab.carat.android.R;

/**
 * Created by Jonatan Hamberg on 6/30/17.
 */
public class VersionGater {
    private static final String TAG = VersionGater.class.getSimpleName();
    private Activity activity;
    private Context context;
    private int versionCode;
    private boolean mustUpdate;
    private String packageName;
    private String title;
    private String message;

    public VersionGater(Activity activity){
        this.activity = activity;
        this.context = activity.getApplicationContext();
    }

    private static void checkVersion(Activity activity){
        VersionGater versionGater = new VersionGater(activity);
        versionGater.check();
    }

    private void check(){
        String json = JsonParser.getJSONFromUrl("carat.cs.helsinki.fi/questionnaires.json");
        if(!Util.isNullOrEmpty(json)){
            try {
                JSONObject jsonObject = new JSONObject(json);
                versionCode = jsonObject.getInt("versionCode");
                mustUpdate = jsonObject.getBoolean("mustUpdate");
                packageName = jsonObject.getString("packageName");
                message = jsonObject.getString("message");
                title = jsonObject.getString("title");
                if(isVersionTooOld(versionCode)){
                    displayDialog();
                }
            } catch (JSONException e) {
                Logger.e(TAG, "Error parsing version gating JSON");
                e.printStackTrace();
            }
        }
    }

    private boolean isVersionTooOld(int versionCode){
        return BuildConfig.VERSION_CODE < versionCode;
    }

    private void displayDialog(){
        String exit = mustUpdate ? "Exit" : "Not now";
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton("OK", null);
        builder.setNegativeButton(exit, null);
        builder.setIcon(R.drawable.carat_material_icon);

        AlertDialog alertDialog = builder.create();
        alertDialog.setOnShowListener(dialog -> {
            Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            positiveButton.setOnClickListener(v -> {
                Util.openStorePage(context, packageName);
            });
            negativeButton.setOnClickListener(v -> {
                if(mustUpdate){
                    activity.finish();
                    System.exit(0);
                } else {
                    alertDialog.dismiss();
                }
            });
        });
        alertDialog.setCanceledOnTouchOutside(mustUpdate);
        alertDialog.show();
    }
}
