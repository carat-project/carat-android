package edu.berkeley.cs.amplab.carat.android.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TSimpleJSONProtocol;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import edu.berkeley.cs.amplab.carat.android.Keys;
import edu.berkeley.cs.amplab.carat.android.R;
import edu.berkeley.cs.amplab.carat.android.sampling.Sampler;
import edu.berkeley.cs.amplab.carat.android.sampling.SamplingLibrary;
import edu.berkeley.cs.amplab.carat.android.storage.SampleDB;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;
import edu.berkeley.cs.amplab.carat.thrift.Sample;

/**
 * Created by Jonatan Hamberg on 27.2.2018
 */
public class SamplePreviewFragment extends Fragment {
    private final String TAG = SamplePreviewFragment.class.getSimpleName();
    private TextView jsonTextView;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        ScrollView view = (ScrollView) inflater.inflate(R.layout.fragment_sample_preview, container, false);
        jsonTextView = (TextView) view.findViewById(R.id.sample_json);
        return view;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        new Thread(() -> {
            try {
                Context context = getContext();
                SampleDB db = SampleDB.getInstance(context);
                Sample sample = db.getLastSample(context);
                if(sample == null){
                    // Generate a dummy sample and show it
                    sample = constructTempSample(context);
                }
                TSerializer serializer = new TSerializer(new TSimpleJSONProtocol.Factory());
                JSONObject json = new JSONObject(serializer.toString(sample));
                getActivity().runOnUiThread(setJsonText(json));
            } catch (TException e) {
                Logger.d(TAG, "Error when deserializing sample " + e);
            } catch(JSONException e){
                printJSONException(e);
            }
        }).start();
    }

    private Runnable setJsonText(JSONObject json){
        return () -> {
            try {
                jsonTextView.setText(json.toString(2));
            } catch (JSONException e) {
                printJSONException(e);
            }
        };
    }

    private void printJSONException(JSONException e){
        Logger.d(TAG, "Error when parsing sample JSON: " + e);
    }

    private Sample constructTempSample(Context context){
        long monthAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        long lastSampleTime =  preferences.getLong(Keys.lastSampleTimestamp, monthAgo);
        Intent batteryIntent = SamplingLibrary.getLastBatteryIntent(context);
        return Sampler.constructSample(context, batteryIntent, "PREVIEW_SAMPLE", lastSampleTime, false);
    }
}
