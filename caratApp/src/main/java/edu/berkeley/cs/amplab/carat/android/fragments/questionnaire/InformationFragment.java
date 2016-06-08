package edu.berkeley.cs.amplab.carat.android.fragments.questionnaire;


import android.app.Activity;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.TextView;

import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.MainActivity;
import edu.berkeley.cs.amplab.carat.android.R;
import edu.berkeley.cs.amplab.carat.android.fragments.ActionsFragment;
import edu.berkeley.cs.amplab.carat.android.views.adapters.QuestionnaireItemAdapter;
import edu.berkeley.cs.amplab.carat.thrift.QuestionnaireAnswer;
import edu.berkeley.cs.amplab.carat.thrift.QuestionnaireItem;

/**
 * Questionnaire fragment with a webview for information
 */
public class InformationFragment extends Fragment {
    private MainActivity mainActivity;
    private QuestionnaireItemAdapter adapter;

    private int index;
    private String content;
    private boolean last;

    private RelativeLayout mainFrame;
    private WebView webView;
    private Button proceedButton;
    private TextView footerView;

    public InformationFragment() {
        this.adapter = QuestionnaireItemAdapter.getInstance();
    }

    public static InformationFragment from(QuestionnaireItem item, int index, boolean last){
        InformationFragment fragment = new InformationFragment();
        fragment.index = index;
        fragment.last = last;
        fragment.content = item.getContent();
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mainFrame = (RelativeLayout) inflater.inflate(R.layout.questionnaire_information, container, false);
        setActionbarTitle();
        setupViewReferences();
        setupViewValues();
        setupProceedButtonListener();
        return mainFrame;
    }

    @Override
    public void onResume(){
        super.onResume();
        mainActivity.hideKeyboard(mainFrame);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mainActivity = (MainActivity) activity;
    }

    public void setActionbarTitle(){
        String information = getString(R.string.information).toUpperCase();
        mainActivity.setUpActionBar(information, true);
    }

    public void setupViewReferences(){
        webView = (WebView) mainFrame.findViewById(R.id.webView);
        proceedButton = (Button) mainFrame.findViewById(R.id.proceed_button);
    }

    public void setupViewValues(){
        proceedButton.setText(last ? R.string.submit : R.string.continueText);

        // Optimizations for webview loading speed
        if(Build.VERSION.SDK_INT >= 19){
            webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        } else if(Build.VERSION.SDK_INT >= 11){
            webView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        }
        webView.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        webView.loadData(content, "text/html", "UTF-8");
    }

    public void setupProceedButtonListener(){
        proceedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter.loadItem(mainActivity, index + 1);
            }
        });
    }
}
