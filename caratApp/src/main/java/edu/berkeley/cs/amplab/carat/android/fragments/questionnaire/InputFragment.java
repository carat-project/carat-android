package edu.berkeley.cs.amplab.carat.android.fragments.questionnaire;


import android.app.Activity;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.List;

import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.MainActivity;
import edu.berkeley.cs.amplab.carat.android.R;
import edu.berkeley.cs.amplab.carat.android.fragments.ActionsFragment;
import edu.berkeley.cs.amplab.carat.android.views.adapters.QuestionnaireItemAdapter;
import edu.berkeley.cs.amplab.carat.thrift.QuestionnaireAnswer;
import edu.berkeley.cs.amplab.carat.thrift.QuestionnaireItem;

/**
 * Questionnaire fragment with a text input
 */
public class InputFragment extends Fragment {
    private MainActivity mainActivity;
    private QuestionnaireItemAdapter adapter;
    private QuestionnaireAnswer saved;

    private int index, id;
    private String text, subtext;
    private boolean last, numeric;

    private RelativeLayout mainFrame;
    private TextView footerView;
    private EditText input;
    private Button proceedButton;

    public InputFragment() {
        this.adapter = QuestionnaireItemAdapter.getInstance();
    }

    public static InputFragment from(QuestionnaireItem item, int index, boolean last){
        InputFragment fragment = new InputFragment();
        fragment.index = index;
        fragment.last = last;
        fragment.id = item.getQuestionId();
        fragment.text = item.getTitle();
        fragment.subtext = item.getContent();
        fragment.numeric = item.numeric;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mainFrame = (RelativeLayout) inflater.inflate(R.layout.questionnaire_input, container, false);
        setupViewReferences();
        setActionbarTitle();
        setupListeners();
        return mainFrame;
    }

    @Override
    public void onResume(){
        super.onResume();
        // This needs to happen on resume so saved values are
        // properly set when the view is popped from backstack.
        loadSavedValues();
        mainActivity.hideKeyboard(input);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        this.mainActivity = (MainActivity) activity;
    }

    public void setActionbarTitle(){
        String question = getString(R.string.question);
        int count = adapter.getQuestionCount();
        String title = question + " " + id + "/" + count;
        mainActivity.setUpActionBar(title, true);
    }

    public void setupViewReferences(){
        TextView textView = (TextView) mainFrame.findViewById(R.id.content_text);
        TextView subtextView = (TextView) mainFrame.findViewById(R.id.content_subtext);
        input = (EditText) mainFrame.findViewById(R.id.answer_input);
        proceedButton = (Button) mainFrame.findViewById(R.id.proceed_button);
        footerView = (TextView) mainFrame.findViewById(R.id.exit_button);

        textView.setText(text);
        subtextView.setText(subtext);
        if(last){
            // Allow proceeding with an empty input when submitting
            proceedButton.setText(R.string.submit);
            proceedButton.setEnabled(true);
        } else {
            proceedButton.setText(R.string.nextQuestion);
        }
        footerView.setText(R.string.backToApp);
        if(numeric) input.setInputType(InputType.TYPE_CLASS_NUMBER);
    }

    public void setupListeners(){
        setupProceedButtonListener();
        setupInputListener();
        setupExitButtonListener();
    }

    public void loadSavedValues(){
        saved = adapter.getAnswer(id);
        if(saved == null || saved.getInput() == null){
            return;
        }

        String savedInput = saved.getInput();
        input.setText(savedInput);
    }

    private void setupInputListener(){
        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.cacheInMemory(getAnswer());
                if(!last){
                    boolean validity = validateInput(input.getText().toString());
                    proceedButton.setEnabled(validity);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No operation
            }

            @Override
            public void afterTextChanged(Editable s) {
                // No operation
            }
        });

        input.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus){
                // Hide keyboard when user clicks outside the input
                if(!hasFocus) mainActivity.hideKeyboard(mainFrame);
            }
        });
    }

    private void setupProceedButtonListener(){
        proceedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                QuestionnaireAnswer answer = getAnswer();
                adapter.saveAnswer(answer);
                adapter.loadItem(mainActivity, index+1);
                saved = answer;
            }
        });
    }

    public void setupExitButtonListener(){
        footerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Exit questionnaire and return to actions view
                ActionsFragment fragment = new ActionsFragment();
                mainActivity.replaceFragment(fragment, Constants.FRAGMENT_ACTIONS_TAG);
            }
        });
    }

    private QuestionnaireAnswer getAnswer(){
        String value = input.getText().toString();
        QuestionnaireAnswer answer = new QuestionnaireAnswer()
                .setQuestionId(id)
                .setInput(value);
        return answer;
    }

    /**
     * Validate input text by checking for null or empty strings.
     * @param text Text input
     * @return True when validation is successful
     */
    private boolean validateInput(String text){
        return !(text == null || text.isEmpty() || text.trim().isEmpty());
    }

}
