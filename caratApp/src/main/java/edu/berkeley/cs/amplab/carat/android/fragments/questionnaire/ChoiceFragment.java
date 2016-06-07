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
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.MainActivity;
import edu.berkeley.cs.amplab.carat.android.R;
import edu.berkeley.cs.amplab.carat.android.fragments.ActionsFragment;
import edu.berkeley.cs.amplab.carat.android.views.adapters.QuestionnaireItemAdapter;
import edu.berkeley.cs.amplab.carat.thrift.QuestionnaireAnswer;
import edu.berkeley.cs.amplab.carat.thrift.QuestionnaireItem;

/**
 * Single choice fragment logic for questionnaire.
 */
public class ChoiceFragment extends Fragment {
    private MainActivity mainActivity;
    private QuestionnaireItemAdapter adapter;

    private int index, id;
    private String text, subtext;
    private List<String> choices;
    private boolean other, numeric;

    private RelativeLayout mainFrame;
    private TextView footerView;
    private RadioGroup buttonGroup;
    private EditText otherInput;
    private Button proceedButton;

    public ChoiceFragment() {
        adapter = QuestionnaireItemAdapter.getInstance();
    }

    public static ChoiceFragment from(QuestionnaireItem item, int index){
        ChoiceFragment fragment = new ChoiceFragment();
        fragment.index = index;
        fragment.id = item.getQuestionId();
        fragment.text = item.getTitle();
        fragment.subtext = item.getContent();
        fragment.choices = item.getChoices();
        fragment.other = item.other;
        fragment.numeric = item.numeric;
        return fragment;

    }

    @Override
    public void onResume(){
        super.onResume();
        preselectRadioButton();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mainFrame = (RelativeLayout) inflater.inflate(R.layout.questionnaire_choice, container, false);
        setActionbarTitle();
        setupViewReferences();
        populateRadioGroup();
        setupListeners();
        return mainFrame;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
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
        otherInput = (EditText) mainFrame.findViewById(R.id.specify_other);
        proceedButton = (Button) mainFrame.findViewById(R.id.proceed_button);
        footerView = (TextView) mainFrame.findViewById(R.id.exit_button);
        buttonGroup = (RadioGroup) mainFrame.findViewById(R.id.button_container);

        textView.setText(text);
        subtextView.setText(subtext);
        proceedButton.setText(R.string.nextQuestion);
        footerView.setText(R.string.backToApp);
        if(other) otherInput.setVisibility(View.VISIBLE);
        if(numeric) otherInput.setInputType(InputType.TYPE_CLASS_NUMBER);
    }

    public void populateRadioGroup(){
        RadioButton button;
        for(int i=0; i<choices.size(); i++){
            button = new RadioButton(getContext());
            button.setText(choices.get(i));
            button.setTag(i);
            buttonGroup.addView(button);
        }
    }

    public void preselectRadioButton(){
        Integer selection = getPreselectedChoice();
        if(selection == null) return;
        int childCount = buttonGroup.getChildCount();
        for(int i=0; i < childCount; i++){
            RadioButton button = (RadioButton) buttonGroup.getChildAt(i);
            if(button.getTag() != null && button.getTag().equals(selection)){
                buttonGroup.check(button.getId());
                if(other && i == childCount -1){
                    checkInput(otherInput.toString());
                } else {
                    proceedButton.setEnabled(true);
                }
            }
        }
    }

    public void setupListeners(){
        setupRadioGroupListener();
        setupProceedButtonListener();
        setupInputListeners();
        setupExitButtonListener();
    }

    public void setupRadioGroupListener(){
        buttonGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                RadioButton button = (RadioButton) buttonGroup.findViewById(checkedId);

                // Focus input and show keyboard when 'other' is selected
                // otherwise hide the keyboard and clear focus.
                int lastChoice = choices.size()-1;
                if(other && button.getTag().equals(lastChoice)){
                    otherInput.requestFocus();
                    mainActivity.showKeyboard(otherInput);
                    checkInput(otherInput.getText().toString());
                } else {
                    otherInput.clearFocus();
                    mainActivity.hideKeyboard(otherInput);
                    proceedButton.setEnabled(true);
                }
            }
        });
    }

    public void setupProceedButtonListener(){
        proceedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                QuestionnaireAnswer answer = getAnswer();
                adapter.saveAnswer(answer);
                adapter.loadItem(mainActivity, index + 1);
            }
        });
    }

    public void setupInputListeners(){
        otherInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus){

                // Hide keyboard when user clicks outside the input,
                // otherwise select the radio button for 'other'
                if(!hasFocus) mainActivity.hideKeyboard(mainFrame);
                else {
                    int count = buttonGroup.getChildCount();
                    RadioButton other = (RadioButton) buttonGroup.getChildAt(count-1);
                    buttonGroup.check(other.getId());
                }
            }
        });
        otherInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                checkInput(s.toString());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    public void setupExitButtonListener(){
        footerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActionsFragment fragment = new ActionsFragment();
                mainActivity.replaceFragment(fragment, Constants.FRAGMENT_ACTIONS_TAG);
            }
        });
    }

    public Integer getPreselectedChoice(){
        QuestionnaireAnswer answer = adapter.getAnswer(id);
        if(answer == null) return null;
        List<Integer> answers = answer.getAnswers();
        if(answers == null || answers.size() <= 0) return null;
        return answers.get(0);
    }

    public QuestionnaireAnswer getAnswer(){
        int checked = buttonGroup.getCheckedRadioButtonId();
        RadioButton button = (RadioButton) buttonGroup.findViewById(checked);

        List<Integer> answerList = new ArrayList<>();
        answerList.add((int)button.getTag());
        QuestionnaireAnswer answer = new QuestionnaireAnswer()
                .setQuestionId(id)
                .setAnswers(answerList);

        // Other answer
        if(button.getTag().equals(buttonGroup.getChildCount()-1)){
            answer.setInput(otherInput.toString());
        }
        return answer;
    }

    /**
     * Check for empty input or whitespaces
     * @param text input
     */
    public void checkInput(String text){
        if(text == null || text.isEmpty() || text.trim().isEmpty()){
            proceedButton.setEnabled(false);
        } else {
            proceedButton.setEnabled(true);
        }
    }
}
