package edu.berkeley.cs.amplab.carat.android.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import edu.berkeley.cs.amplab.carat.android.CaratApplication;
import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.MainActivity;
import edu.berkeley.cs.amplab.carat.android.R;
import edu.berkeley.cs.amplab.carat.android.UsageManager;
import edu.berkeley.cs.amplab.carat.android.receivers.AsyncSuccess;
import edu.berkeley.cs.amplab.carat.android.sampling.SamplingLibrary;
import edu.berkeley.cs.amplab.carat.android.adapters.ProcessExpandListAdapter;
import edu.berkeley.cs.amplab.carat.thrift.ProcessInfo;

/**
 * Created by Valto on 7.10.2015.
 * Modified by Jonatan Hamberg on 28.6.2017
 */
public class ProcessListFragment extends Fragment {
    private MainActivity mainActivity;
    private LinearLayout mainFrame;
    private RelativeLayout processHeader;
    private ExpandableListView expandableListView;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mainActivity = (MainActivity) activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mainFrame = (LinearLayout) inflater.inflate(R.layout.fragment_process_list, container, false);
        return mainFrame;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }

    @Override
    public void onResume() {
        super.onResume();
        mainActivity.setUpActionBar(R.string.process_list_title, true);
        initViewRefs();
        refresh();
    }

    private void initViewRefs() {
        processHeader = (RelativeLayout) mainFrame.findViewById(R.id.process_header);
        expandableListView = (ExpandableListView) mainFrame.findViewById(R.id.expandable_process_list);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            Context context = getContext();
            TextView description = (TextView) mainFrame.findViewById(R.id.processlistDescription);
            description.setText(R.string.process_list_message_lp);
            if(!UsageManager.isPermissionGranted(context)){
                UsageManager.promptPermission(context, new AsyncSuccess() {
                    @Override
                    public void complete(boolean success) {
                        if(!success){
                            Toast.makeText(context, "Only services will be shown.", Toast.LENGTH_LONG).show();
                        }
                    }
                });
            }
        }
    }

    private void refresh() {
        CaratApplication app = (CaratApplication) getActivity().getApplication();
        SamplingLibrary.resetRunningProcessInfo();
        Context context = getContext();
        long recent = System.currentTimeMillis() - Constants.FRESHNESS_RUNNING_PROCESS;
        List<ProcessInfo> searchResults = SamplingLibrary.getRunningProcessInfoForSample(context, recent);
        expandableListView.setAdapter(new ProcessExpandListAdapter((MainActivity) getActivity(),
                expandableListView, app, searchResults));
    }

}
