package edu.berkeley.cs.amplab.carat.android.adapters;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import edu.berkeley.cs.amplab.carat.android.CaratApplication;
import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.MainActivity;
import edu.berkeley.cs.amplab.carat.android.R;
import edu.berkeley.cs.amplab.carat.android.UsageManager;
import edu.berkeley.cs.amplab.carat.android.components.BaseDialog;
import edu.berkeley.cs.amplab.carat.android.models.SimpleProcessInfo;
import edu.berkeley.cs.amplab.carat.android.sampling.SamplingLibrary;
import edu.berkeley.cs.amplab.carat.android.utils.Logger;
import edu.berkeley.cs.amplab.carat.android.utils.ProcessUtil;
import edu.berkeley.cs.amplab.carat.thrift.PackageProcess;
import edu.berkeley.cs.amplab.carat.thrift.ProcessInfo;

/**
 * Created by Valto on 7.10.2015.
 */
public class ProcessExpandListAdapter extends BaseExpandableListAdapter implements View.OnClickListener, ExpandableListView.OnGroupClickListener {
    private static final String TAG = ProcessExpandListAdapter.class.getSimpleName();
    private MainActivity mainActivity;
    private CaratApplication caratApplication;
    private SimpleProcessInfo[] processInfoList;
    private LayoutInflater mInflater;

    public ProcessExpandListAdapter(MainActivity mainActivity, ExpandableListView listView,
                                    final CaratApplication caratApplication) {

        this.caratApplication = caratApplication;
        this.mainActivity = mainActivity;

        processInfoList = new SimpleProcessInfo[]{};
        Context context = mainActivity.getApplicationContext();
        long recent = System.currentTimeMillis() - Constants.FRESHNESS_RUNNING_PROCESS;
        ProcessExpandListAdapter adapter = this;
        new Thread(() -> {
            List<ProcessInfo> processes = SamplingLibrary.getRunningProcesses(context, recent, false);
            SimpleProcessInfo[] convertedResults = convertProcessInfo(processes);
            Arrays.sort(convertedResults);
            processInfoList = convertedResults;
            mainActivity.runOnUiThread(listView::invalidateViews);
            mainActivity.runOnUiThread(adapter::notifyDataSetChanged);
        }).start();
        mInflater = LayoutInflater.from(caratApplication);
        listView.setOnGroupClickListener(this);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.process_list_child_item, null);
        }

        SimpleProcessInfo item = processInfoList[groupPosition];
        setViewsInChild(convertView, item);
        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return 1;
    }

    @Override
    public Object getGroup(int groupPosition) {
        return processInfoList[groupPosition];
    }

    @Override
    public int getGroupCount() {
        return processInfoList.length;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.process_list_header, null);
        }

        if (processInfoList == null || groupPosition < 0
                || groupPosition >= processInfoList.length)
            return convertView;

        SimpleProcessInfo item = processInfoList[groupPosition];
        if (item == null)
            return convertView;

        setItemViews(convertView, item, groupPosition);

        return convertView;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    private void setViewsInChild(View v, SimpleProcessInfo item) {
        TextView priorityValue = (TextView) v.findViewById(R.id.priority_value);
        TextView processVersion = (TextView) v.findViewById(R.id.version_amount);
        TextView packageName = (TextView) v.findViewById(R.id.package_name);
        processVersion.setText(item.getVersionName());
        priorityValue.setText(item.getImportance());
        packageName.setText(item.getPackageName());

    }

    private void setItemViews(View v, SimpleProcessInfo item, int groupPosition) {
        ImageView processIcon = (ImageView) v.findViewById(R.id.process_icon);
        TextView processName = (TextView) v.findViewById(R.id.process_name);
        TextView serviceCount = (TextView) v.findViewById(R.id.serviceCount);
        TextView activityCount = (TextView) v.findViewById(R.id.activityCount);
        TextView serviceDesc = (TextView) v.findViewById(R.id.serviceText);
        TextView activityDesc = (TextView) v.findViewById(R.id.activityText);
        View activityIndicator = (View) v.findViewById(R.id.activityIndicator);

        processIcon.setImageDrawable(item.getIcon());
        processName.setText(item.getLocalizedName());

        int aCount = item.getActivityCount();
        int sCount = item.getServiceCount();

        if(aCount <= 0){
            activityCount.setVisibility(View.GONE);
            activityDesc.setVisibility(View.GONE);
            activityIndicator.setBackgroundColor(v.getResources().getColor(R.color.gray));
        } else {
            activityCount.setVisibility(View.VISIBLE);
            activityDesc.setVisibility(View.VISIBLE);
            activityIndicator.setBackgroundColor(v.getResources().getColor(R.color.accent));
            activityDesc.setText(aCount > 1 ? R.string.activities : R.string.activity);
            activityCount.setText(String.valueOf(aCount));
        }

        if(sCount <= 0){
            serviceCount.setVisibility(View.GONE);
            serviceDesc.setVisibility(View.GONE);
        } else {
            serviceCount.setVisibility(View.VISIBLE);
            serviceDesc.setVisibility(View.VISIBLE);
            serviceDesc.setText(sCount > 1 ? R.string.services : R.string.service);
            serviceCount.setText(String.valueOf(sCount));
        }

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.what_are_these_numbers:
                BaseDialog dialog = new BaseDialog(mainActivity,
                        mainActivity.getString(R.string.what_are_these_numbers_title),
                        mainActivity.getString(R.string.what_are_these_numbers_explanation),
                        "detailinfo");
                dialog.showDialog();
                break;
        }
    }

    @Override
    public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
        if (parent.isGroupExpanded(groupPosition)) {
            ImageView collapseIcon = (ImageView) v.findViewById(R.id.collapse_icon);
            collapseIcon.setImageResource(R.drawable.collapse_down);
        } else {
            ImageView collapseIcon = (ImageView) v.findViewById(R.id.collapse_icon);
            collapseIcon.setImageResource(R.drawable.collapse_up);
        }
        return false;
    }

    private SimpleProcessInfo[] convertProcessInfo(List<ProcessInfo> list){
        List<SimpleProcessInfo> result = new LinkedList<>();
        Context context = caratApplication.getApplicationContext();
        for(ProcessInfo pi : list){
            String pName = pi.getPName();
            String localizedName = CaratApplication.labelForApp(context, pName);
            String importance = ProcessUtil.mostRecentPriority(context, pName);
            int serviceCount = 0;
            int activityCount = 0;
            if(pi.isSetProcesses()){
                for(PackageProcess p : pi.getProcesses()){
                    String processName = p.getProcessName();
                    int processCount = p.getProcessCount();
                    processCount = processCount <= 0 ? 1 : processCount;
                    if(processName.contains("@")){
                        serviceCount += processCount;
                    } else {
                        activityCount += processCount;
                    }
                }
            }
            Drawable icon = CaratApplication.iconForApp(context, pName);
            PackageInfo pInfo = SamplingLibrary.getPackageInfo(context, pName);
            String versionName;
            if (pInfo != null) {
                versionName = pInfo.versionName;
                if (versionName == null) {
                    versionName = pInfo.versionCode + "";
                }
            } else {
                versionName = "N/A";
            }
            SimpleProcessInfo spi = new SimpleProcessInfo()
                    .setPackageName(pName)
                    .setLocalizedName(localizedName)
                    .setImportance(importance)
                    .setVersionName(versionName)
                    .setIcon(icon)
                    .setActivityCount(activityCount)
                    .setServiceCount(serviceCount);
            result.add(spi);
        }
        return result.toArray(new SimpleProcessInfo[result.size()]);
    }
}
