package edu.berkeley.cs.amplab.carat.android.views.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import edu.berkeley.cs.amplab.carat.android.CaratApplication;
import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.MainActivity;
import edu.berkeley.cs.amplab.carat.android.R;
import edu.berkeley.cs.amplab.carat.android.fragments.questionnaire.ChoiceFragment;
import edu.berkeley.cs.amplab.carat.android.fragments.questionnaire.InformationFragment;
import edu.berkeley.cs.amplab.carat.android.fragments.questionnaire.InputFragment;
import edu.berkeley.cs.amplab.carat.android.fragments.questionnaire.MultichoiceFragment;
import edu.berkeley.cs.amplab.carat.thrift.Answers;
import edu.berkeley.cs.amplab.carat.thrift.Questionnaire;
import edu.berkeley.cs.amplab.carat.thrift.QuestionnaireAnswer;
import edu.berkeley.cs.amplab.carat.thrift.QuestionnaireItem;

/**
 * Created by Jonatan Hamberg on 20.4.2016.
 */
public class QuestionnaireItemAdapter {
    private int questionCount;
    private List<QuestionnaireItem> items;
    private HashMap<Integer, QuestionnaireAnswer> answers;
    private static QuestionnaireItemAdapter instance = null;

    // TODO: Allow creating instances for different questionnaire ids
    private QuestionnaireItemAdapter(){
        Questionnaire questionnaire = CaratApplication.getStorage().getQuestionnaire(0);
        if(questionnaire == null) return;
        this.items = questionnaire.getItems();
        this.answers = new HashMap<>();
        questionCount = 0;
        for(QuestionnaireItem item : items){
            if(item.isSetQuestionId()) questionCount++;
        }
    }

    public static QuestionnaireItemAdapter getInstance() {
        if (instance == null) {
            instance = new QuestionnaireItemAdapter();
        }
        return instance;
    }

    public void storeAnswers(){
        if(this.answers == null) return;
        Answers answers = new Answers();
        answers.setId(0);
        answers.setUuId(CaratApplication.myDeviceData.getCaratId());
        answers.setTimestamp(System.currentTimeMillis() / 1000);
        answers.setAnswers(new ArrayList<>(this.answers.values()));

        CaratApplication.getStorage().writeAnswers(answers);
    }

    public void loadStoredAnswers(){
        Answers answers = CaratApplication.getStorage().getAnswers(0);
        if(answers != null && answers.getAnswers() != null){
            List<QuestionnaireAnswer> answerList  = answers.getAnswers();
            for(QuestionnaireAnswer answer : answerList){
                this.answers.put(answer.getQuestionId(), answer);
            }
        }
    }

    public void cacheInMemory(QuestionnaireAnswer answer){
        answers.put(answer.getQuestionId(), answer);
    }

    public void saveAnswer(QuestionnaireAnswer answer){
        answers.put(answer.getQuestionId(), answer);
        storeAnswers();
    }

    public QuestionnaireAnswer getAnswer(int questionId){
        return answers.get(questionId);
    }

    public int getQuestionCount(){
        return questionCount;
    }

    public void loadItem(MainActivity mainActivity, int index){
        int itemCount = items.size();
        if(index >= itemCount){
            completeQuestionnaire(mainActivity);
            return;
        }
        QuestionnaireItem item = items.get(index);
        boolean last = index == itemCount-1;
        Fragment next;
        String tag;

        // Tags need to be appended with an identifier so fragment manager
        // doesn't accidentally use the backstack for replacing the current
        // fragment.
        switch(item.type){
            case INFORMATION:
                next = InformationFragment.from(item, index, last);
                tag = Constants.FRAGMENT_QUESTIONNAIRE_INFORMATION + index;
                break;
            case CHOICE:
                next = ChoiceFragment.from(item, index, last);
                tag = Constants.FRAGMENT_QUESTIONNAIRE_CHOICE + index;
                break;
            case MULTICHOICE:
                next = MultichoiceFragment.from(item, index, last);
                tag = Constants.FRAGMENT_QUESTIONNAIRE_MULTICHOICE + index;
                break;
            case INPUT:
                next = InputFragment.from(item, index, last);
                tag = Constants.FRAGMENT_QUESTIONNAIRE_INPUT + index;
                break;
            default:
                return;
        }
        // Replace current fragment
        mainActivity.replaceFragment(next, tag);
    }

    public void completeQuestionnaire(MainActivity mainActivity){
        // TODO: Send results, set answered flag
        exitQuestionnaire(mainActivity);
    }

    public void exitQuestionnaire(MainActivity mainActivity){
        Context context = mainActivity.getApplicationContext();
        mainActivity.loadHomeScreen();
        String thanksMessage = mainActivity.getString(R.string.participationThanks);
        Toast.makeText(context, thanksMessage, Toast.LENGTH_LONG).show();
    }

}

