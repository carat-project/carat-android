package edu.berkeley.cs.amplab.carat.android.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.SortedMap;

import edu.berkeley.cs.amplab.carat.android.CaratActions;
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
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
              switch(intent.getAction()){
                  case CaratActions.CHARGING_NEW:
                  case CaratActions.CHARGING_UPDATE:
                      setChargingSessions(sessionManager.getSavedSessions());
              }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_charging, container, false);
        sessionManager = ChargingSessionManager.getInstance();
        new Handler().post(() -> {
            setChargingSessions(sessionManager.getSavedSessions());
        });
        getContext().registerReceiver(receiver, new IntentFilter(CaratActions.CHARGING_NEW));
        getContext().registerReceiver(receiver, new IntentFilter(CaratActions.CHARGING_UPDATE));
        return binding.getRoot();
    }

    private void setChargingSessions(SortedMap<Long, ChargingSession> sessions){
        Logger.d(TAG, "Got " + sessions.size() + " sessions");
        StringBuilder sb = new StringBuilder();
        boolean captureFirst = true;
        for(Long timestamp : sessions.keySet()){
            if(captureFirst){
                captureFirst = false;
            }
            sb.append(sessions.get(timestamp)).append("\n");
        }
        binding.chargingDebug.setText(sb.toString());
    }

    @Override
    public void onResume() {
        super.onResume();
    }
}
