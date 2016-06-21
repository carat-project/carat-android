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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.MainActivity;
import edu.berkeley.cs.amplab.carat.android.R;
import edu.berkeley.cs.amplab.carat.android.fragments.*;
import edu.berkeley.cs.amplab.carat.android.sampling.SamplingLibrary;
import edu.berkeley.cs.amplab.carat.android.views.adapters.QuestionnaireItemAdapter;
import edu.berkeley.cs.amplab.carat.thrift.Answers;
import edu.berkeley.cs.amplab.carat.thrift.QuestionnaireAnswer;
import edu.berkeley.cs.amplab.carat.thrift.QuestionnaireItem;

/**
 * Questionnaire fragment with checkboxes
 */
public class MultichoiceFragment extends Fragment {
    private MainActivity mainActivity;
    private QuestionnaireItemAdapter adapter;
    private QuestionnaireAnswer saved;

    private int index, id, lastIndex;
    private String text, subtext;
    private List<String> choices;
    private List<Integer> location;
    private boolean other, numeric, last;

    private RelativeLayout mainFrame;
    private TextView textView, subtextView, footerView;
    private LinearLayout buttonContainer;
    private EditText otherInput;
    private Button proceedButton;

    public MultichoiceFragment() {
        // Empty constructor required by Android API
    }

    public static MultichoiceFragment from(QuestionnaireItem item, QuestionnaireItemAdapter adapter, int index, boolean last){
        MultichoiceFragment fragment = new MultichoiceFragment();
        fragment.index = index;
        fragment.adapter = adapter;
        fragment.last = last;
        fragment.id = item.getQuestionId();
        fragment.text = item.getTitle();
        fragment.subtext = item.getContent();
        fragment.choices = item.getChoices();
        fragment.other = item.isOtherOption();
        fragment.numeric = item.isInputNumeric();
        fragment.lastIndex = fragment.choices.size()-1;
        fragment.location = item.getLocation();
        return fragment;
    }

    @Override
    public void onResume(){
        super.onResume();
        // These needs to happen on resume so saved values are
        // properly set when the view is popped from backstack.
        preselectCheckboxes();
        prefillInput();
        mainActivity.hideKeyboard(otherInput);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mainFrame = (RelativeLayout) inflater.inflate(R.layout.questionnaire_multichoice, container, false);
        setActionbarTitle();
        setupViewReferences();
        setupViewValues();
        populateCheckboxes();

        // Unlike with radio buttons, saved values need to be loaded
        // before listeners to ensure that prefilling data does not
        // trigger cache saves.
        saved = adapter.getAnswer(id);
        preselectCheckboxes();
        prefillInput();

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
        textView = (TextView) mainFrame.findViewById(R.id.content_text);
        subtextView = (TextView) mainFrame.findViewById(R.id.content_subtext);
        otherInput = (EditText) mainFrame.findViewById(R.id.specify_other);
        proceedButton = (Button) mainFrame.findViewById(R.id.proceed_button);
        footerView = (TextView) mainFrame.findViewById(R.id.exit_button);
        buttonContainer = (LinearLayout) mainFrame.findViewById(R.id.button_container);
        buttonContainer.removeAllViews(); // Remove placeholders
    }

    public void setupViewValues(){
        textView.setText(text);
        subtextView.setText(subtext);
        proceedButton.setText(R.string.nextQuestion);
        proceedButton.setText(last ? R.string.submit : R.string.nextQuestion);
        footerView.setText(R.string.backToApp);
        if(other) otherInput.setVisibility(View.VISIBLE);
        if(numeric) otherInput.setInputType(InputType.TYPE_CLASS_NUMBER);
    }

    public void populateCheckboxes(){
        CheckBox button;
        for(int i=0; i<choices.size(); i++){
            button = new CheckBox(getContext());
            button.setText(choices.get(i));
            button.setTag(i);
            buttonContainer.addView(button);
        }
    }

    public void prefillInput(){
        String input = "";
        if(saved != null && saved.getInput() != null){
            input = saved.getInput();
        }
        otherInput.setText(input);
    }

    public void preselectCheckboxes(){
        List<Integer> selections = new ArrayList<>();
        if(saved != null && saved.getAnswers() != null){
            selections = saved.getAnswers();
        }
        selectCheckboxes(selections);
    }

    public void selectCheckboxes(List<Integer> preselected){
        if(preselected == null) return;
        int childCount = buttonContainer.getChildCount();
        for(int i=0; i < childCount; i++){
            CheckBox button = (CheckBox) buttonContainer.getChildAt(i);
            int tag = (int) button.getTag();
            if(button.getTag() != null && preselected.contains(tag)){
                button.setChecked(true);
                proceedButton.setEnabled(checkButtonStates());
            }
        }
    }

    public void setupListeners(){
        setupCheckboxListeners();
        setupProceedButtonListener();
        setupInputListeners();
        setupExitButtonListener();
    }

    public void setupCheckboxListeners(){
        final int buttonCount = buttonContainer.getChildCount();
        for(int i=0; i< buttonCount; i++){
            final int index = i;
            final CheckBox button = (CheckBox) buttonContainer.getChildAt(index);
            button.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    // Cache the answers here in case the user navigates to the previous view
                    // and wants to continue where they left off when coming back here.
                    // This should not cause problems with empty inputs since the user will
                    // be unable to continue/submit as long as the field is empty.
                    adapter.cacheInMemory(getAnswer());

                    proceedButton.setEnabled(checkButtonStates());
                    if(other && index == lastIndex && isChecked){
                        otherInput.requestFocus();
                        mainActivity.showKeyboard(otherInput);
                    } else {
                        otherInput.clearFocus();
                        mainActivity.hideKeyboard(otherInput);
                    }
                }
            });
        }
    }

    /**
     * Loops through the list of checkboxes in a grid and returns
     * true if any of them is checked. Performs an additional check
     * when the checkbox has an input.
     * @return True if a button is checked, false if not.
     */
    private boolean checkButtonStates(){
        int buttonCount = buttonContainer.getChildCount();
        boolean checked = false;
        for(int i=0; i<buttonCount; i++){
            CheckBox button = (CheckBox) buttonContainer.getChildAt(i);
            if(button.isChecked()){
                checked = true;
                if(other && i == lastIndex){
                   return validateInput(otherInput.getText().toString());
                }
            }
        }
        return checked;
    }

    // This can only happen when the button is enabled
    // so no need to recheck conditions here, we can assume
    // that they are true.
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

    private void setupInputListeners(){
        otherInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus){
                // Hide keyboard when user clicks outside the input,
                // otherwise check the box for 'other'
                if(!hasFocus) mainActivity.hideKeyboard(mainFrame);
                else {
                    CheckBox other = (CheckBox) buttonContainer.getChildAt(lastIndex);
                    other.setChecked(true);
                }
            }
        });
        otherInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                adapter.cacheInMemory(getAnswer());
                proceedButton.setEnabled(checkButtonStates());
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }

    private void setupExitButtonListener(){
        footerView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ActionsFragment fragment = new ActionsFragment();
                mainActivity.replaceFragment(fragment, Constants.FRAGMENT_ACTIONS_TAG);
            }
        });
    }

    private QuestionnaireAnswer getAnswer(){
        List<Integer> answers = getSelectedChoices();
        QuestionnaireAnswer answer = new QuestionnaireAnswer()
                .setQuestionId(id)
                .setAnswers(answers);
        if(other && answers.contains(lastIndex)){
            answer.setInput(otherInput.getText().toString());
        }
        if(location != null && locationAllowed(answers)){
            String location = SamplingLibrary.getCoarseLocation(getContext());
            answer.setLocation(location);
        }
        return answer;
    }

    private boolean locationAllowed(List<Integer> answers){
        for(int id : answers){
            if(location.contains(id)){
                return true;
            }
        }
        return false;
    }

    private List<Integer> getSelectedChoices(){
        List<Integer> checked = new ArrayList<>();
        int buttonCount = buttonContainer.getChildCount();
        for(int i=0; i<buttonCount; i++){
            CheckBox button = (CheckBox) buttonContainer.getChildAt(i);
            if(button.isChecked()){
                checked.add((int)button.getTag());
            }
        }
        return checked;
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
