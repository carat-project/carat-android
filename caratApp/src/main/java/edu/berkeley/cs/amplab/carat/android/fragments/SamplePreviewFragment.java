package edu.berkeley.cs.amplab.carat.android.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import com.google.gson.Gson;

import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TSimpleJSONProtocol;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;

import edu.berkeley.cs.amplab.carat.android.MainActivity;
import edu.berkeley.cs.amplab.carat.android.R;
import edu.berkeley.cs.amplab.carat.android.storage.SampleDB;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;
import edu.berkeley.cs.amplab.carat.android.utils.Util;
import edu.berkeley.cs.amplab.carat.thrift.Sample;

/**
 * Created by Jonatan Hamberg on 27.2.2018
 */
public class SamplePreviewFragment extends Fragment {
    private final String TAG = SamplePreviewFragment.class.getSimpleName();
    private ScrollView mainView;
    private TextView sampleJson;
    private StringBuilder builder;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainView = (ScrollView) inflater.inflate(R.layout.fragment_sample_preview, container, false);
        sampleJson = (TextView) mainView.findViewById(R.id.sample_json);
        return mainView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        new Thread(() -> {
            try {
                Context context = getContext();
                SampleDB db = SampleDB.getInstance(context);
                Sample sample = db.getLastSample(context);
                TSerializer serializer = new TSerializer(new TSimpleJSONProtocol.Factory());
                String json = serializer.toString(sample);
                processSampleJSON(json);
                getActivity().runOnUiThread(() -> {
                    //sampleJson.setText(json);
                });
            } catch (TException e) {
                e.printStackTrace();
            }
        }).start();

    }

    @SuppressWarnings("unchecked")
    private void printJSONObject(JSONObject jsonObject, int indentation){
        try {
            for (Iterator<String> it = jsonObject.keys(); it.hasNext(); ) {
                String key = it.next();
                Object object = jsonObject.get(key);
                if(object instanceof JSONObject){
                    addLine(key + ": ", indentation);
                    printJSONObject((JSONObject) object, indentation+1);
                } else if(isPrintable(object)){
                    addLine(key + ": " + String.valueOf(object), indentation);
                } else if(object instanceof JSONArray){
                    JSONArray array = (JSONArray) object;
                    Logger.d(TAG, Util.repeat("\t", indentation)+ key + ": ");
                    for(int i=0; i<array.length(); i++){
                      Object item = array.get(i);
                      if(item instanceof JSONObject){
                          printJSONObject((JSONObject)item, indentation+1);
                      } else if(isPrintable(item)){
                          addLine(String.valueOf(item), indentation);
                      } else {
                          Logger.d(TAG, "Unknown object " + object);
                      }
                    }
                } else {
                    Logger.d(TAG, "Unknown object " + object);
                }
            }
        } catch (Exception e){
            Logger.e(TAG, "Error while reading JSON " + e);
        }
    }

    private boolean isPrintable(Object object){
        return object instanceof String
                || object instanceof Integer
                || object instanceof Long
                || object instanceof Float
                || object instanceof Double
                || object instanceof Boolean;
    }

    private void addLine(String line, int indentation){
        String padding = Util.repeat("\t", indentation);
        builder.append(padding).append(line).append("\n");
    }

    private void processSampleJSON(String json){
        try {
            builder = new StringBuilder();
            JSONObject root = new JSONObject(json);
            printJSONObject(root, 0);
            getActivity().runOnUiThread(() -> {
                sampleJson.setText(builder.toString());
            });
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
