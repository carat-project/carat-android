package edu.berkeley.cs.amplab.carat.android.fragments;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import edu.berkeley.cs.amplab.carat.android.R;

/**
 * Created by Jonatan Hamberg on 27.2.2018
 */
public class SamplePreviewFragment extends Fragment {
    private ScrollView mainView;
    private TextView sampleJson;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mainView = (ScrollView) inflater.inflate(R.layout.fragment_sample_preview, container, false);
        return mainView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    /*@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mainFrame = (RelativeLayout) inflater.inflate(R.layout.fragment_device, container, false);
        Logger.d("debug", "*** : " + "ONCREATEVIEW");
        return mainFrame;
    }*/
}
