package edu.berkeley.cs.amplab.carat.android.fragments;

import android.databinding.DataBindingUtil;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.SortedMap;

import edu.berkeley.cs.amplab.carat.android.R;
import edu.berkeley.cs.amplab.carat.android.databinding.FragmentChargingBinding;
import edu.berkeley.cs.amplab.carat.android.models.ChargingSession;
import edu.berkeley.cs.amplab.carat.android.sampling.ChargingSessionManager;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;

/**
 * Created by Jonatan on 21.11.2017.
 */

public class ChargingFragment extends Fragment {
    private static final String TAG = ChargingFragment.class.getSimpleName();
    private FragmentChargingBinding binding;
    private ChargingSessionManager sessionManager;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_charging, container, false);
        sessionManager = ChargingSessionManager.getInstance();
        new Handler().post(() -> setChargingSessions(sessionManager.getSavedSessions()));
        return binding.getRoot();
    }

    private void setChargingSessions(SortedMap<Long, ChargingSession> sessions){
        Logger.d(TAG, "Got " + sessions.size() + " sessions");
        StringBuilder sb = new StringBuilder();
        for(Long timestamp : sessions.keySet()){
            sb.append(sessions.get(timestamp)).append("\n");
        }
        binding.chargingDebug.setText(sb.toString());
    }
}
