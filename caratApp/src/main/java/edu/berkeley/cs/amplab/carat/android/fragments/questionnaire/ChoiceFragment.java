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
 * Questionnaire fragment with radio buttons
 */
public class ChoiceFragment extends Fragment {
    private MainActivity mainActivity;
    private QuestionnaireItemAdapter adapter;
    private QuestionnaireAnswer saved;

    private int index, id, lastIndex;
    private String text, subtext;
    private List<String> choices;
    private boolean other, numeric, last;

    private RelativeLayout mainFrame;
    private TextView textView, subtextView, footerView;
    private RadioGroup buttonGroup;
    private EditText otherInput;
    private Button proceedButton;

    public ChoiceFragment() {
        adapter = QuestionnaireItemAdapter.getInstance();
    }

    public static ChoiceFragment from(QuestionnaireItem item, int index, boolean last){
        ChoiceFragment fragment = new ChoiceFragment();
        fragment.index = index;
        fragment.last = last;
        fragment.id = item.getQuestionId();
        fragment.text = item.getTitle();
        fragment.subtext = item.getContent();
        fragment.choices = item.getChoices();
        fragment.other = item.other;
        fragment.numeric = item.numeric;
        fragment.lastIndex = fragment.choices.size()-1;
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mainFrame = (RelativeLayout) inflater.inflate(R.layout.questionnaire_choice, container, false);
        saved = adapter.getAnswer(id);
        setActionbarTitle();
        setupViewReferences();
        setupViewValues();
        populateRadioGroup();
        setupListeners();
        // loadSavedValues()
        return mainFrame;
    }

    @Override
    public void onResume(){
        super.onResume();
        // These need to happen on resume so saved values are
        // properly set when the view is popped from backstack.
        saved = adapter.getAnswer(id);
        preselectRadioButton();
        prefillInput();
        mainActivity.hideKeyboard(otherInput);
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
        textView = (TextView) mainFrame.findViewById(R.id.content_text);
        subtextView = (TextView) mainFrame.findViewById(R.id.content_subtext);
        otherInput = (EditText) mainFrame.findViewById(R.id.specify_other);
        proceedButton = (Button) mainFrame.findViewById(R.id.proceed_button);
        footerView = (TextView) mainFrame.findViewById(R.id.exit_button);
        buttonGroup = (RadioGroup) mainFrame.findViewById(R.id.button_container);
        buttonGroup.removeAllViews(); // Remove placeholders
    }

    public void setupViewValues(){
        textView.setText(text);
        subtextView.setText(subtext);
        proceedButton.setText(last ? R.string.submit : R.string.nextQuestion);
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
        int savedSelection = -1;
        if(saved != null && saved.getAnswers() != null
                && saved.getAnswers().size() >0){
            savedSelection = saved.getAnswers().get(0);
        }
        selectRadioButton(savedSelection);
    }

    public void prefillInput(){
        String input = "";
        if(saved != null && saved.getInput() != null){
            input = saved.getInput();
        }
        otherInput.setText(input);
    }

    public void selectRadioButton(int selection){
        int childCount = buttonGroup.getChildCount();
        for(int i=0; i < childCount; i++){
            RadioButton button = (RadioButton) buttonGroup.getChildAt(i);
            if(button.getTag() != null && button.getTag().equals(selection)){
                // This will indirectly call the listener and check
                // if proceed button should be enabled
                buttonGroup.check(button.getId());
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

                // Cache the answer here in case the user navigates to the previous view
                // and wants to continue where they left off when coming back here.
                // This should not cause problems with empty inputs since the user will
                // be unable to continue/submit as long as the field is empty.
                if(button.isChecked()){
                    adapter.cacheInMemory(getAnswer());
                }

                // Focus input and show keyboard when 'other' is selected
                // otherwise hide the keyboard and clear focus.
                if(other && button.getTag().equals(lastIndex)){
                    otherInput.requestFocus();
                    mainActivity.showKeyboard(otherInput);
                    String input = otherInput.getText().toString();
                    boolean valid = validateInput(input);
                    proceedButton.setEnabled(valid);
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
                saved = answer; // In case of recycling
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
                    RadioButton other = (RadioButton) buttonGroup.getChildAt(lastIndex);
                    buttonGroup.check(other.getId());
                }
            }
        });
        otherInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                // Need to make sure the button is checked since this method
                // gets called when a fragment is popped from the backstack
                RadioButton button = (RadioButton) buttonGroup.getChildAt(lastIndex);
                if(button.isChecked()){
                    String text = s.toString();
                    boolean valid = validateInput(text);
                    proceedButton.setEnabled(valid);
                    adapter.cacheInMemory(getAnswer());
                }
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
                // Exit questionnaire and return to actions view
                ActionsFragment fragment = new ActionsFragment();
                mainActivity.replaceFragment(fragment, Constants.FRAGMENT_ACTIONS_TAG);
            }
        });
    }

    public QuestionnaireAnswer getAnswer(){
        int checked = buttonGroup.getCheckedRadioButtonId();
        RadioButton button = (RadioButton) buttonGroup.findViewById(checked);

        List<Integer> answerList = new ArrayList<>();
        answerList.add((int)button.getTag());
        QuestionnaireAnswer answer = new QuestionnaireAnswer()
                .setQuestionId(id)
                .setAnswers(answerList);

        // Only save the input if other option has been selected
        if(other && button.getTag().equals(lastIndex)){
            answer.setInput(otherInput.getText().toString());
        }
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
