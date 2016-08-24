package edu.berkeley.cs.amplab.carat.android.adapters;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IllegalFormatCodePointException;
import java.util.List;

import edu.berkeley.cs.amplab.carat.android.CaratApplication;
import edu.berkeley.cs.amplab.carat.android.Constants;
import edu.berkeley.cs.amplab.carat.android.MainActivity;
import edu.berkeley.cs.amplab.carat.android.R;
import edu.berkeley.cs.amplab.carat.android.fragments.questionnaire.ChoiceFragment;
import edu.berkeley.cs.amplab.carat.android.fragments.questionnaire.InformationFragment;
import edu.berkeley.cs.amplab.carat.android.fragments.questionnaire.InputFragment;
import edu.berkeley.cs.amplab.carat.android.fragments.questionnaire.MultichoiceFragment;
import edu.berkeley.cs.amplab.carat.android.protocol.CommunicationManager;
import edu.berkeley.cs.amplab.carat.thrift.Answers;
import edu.berkeley.cs.amplab.carat.thrift.Questionnaire;
import edu.berkeley.cs.amplab.carat.thrift.QuestionnaireAnswer;
import edu.berkeley.cs.amplab.carat.thrift.QuestionnaireItem;

/**
 * Created by Jonatan Hamberg on 20.4.2016.
 */
public class QuestionnaireItemAdapter {
    private int id, questionCount;
    private List<QuestionnaireItem> items;
    private HashMap<Integer, QuestionnaireAnswer> answers;

    public QuestionnaireItemAdapter(Questionnaire questionnaire){
        if(questionnaire == null) return;
        this.id = questionnaire.getId();
        this.items = questionnaire.getItems();
        this.answers = new HashMap<>();
        questionCount = 0;
        for(QuestionnaireItem item : items){
            if(item.isSetQuestionId()) questionCount++;
        }
    }

    public void storeAnswers(boolean submitted){
        if(this.answers == null) return;
        Answers answers = new Answers();
        answers.setId(id);
        answers.setComplete(submitted);
        answers.setUuId(CaratApplication.myDeviceData.getCaratId());
        answers.setTimestamp(System.currentTimeMillis());
        answers.setAnswers(new ArrayList<>(this.answers.values()));
        CaratApplication.getStorage().writeAnswers(answers);
        if(submitted){
            CaratApplication.getStorage().deleteQuestionnaire(id);
        }
    }

    public void loadStoredAnswers(){
        Answers answers = CaratApplication.getStorage().getAnswers(id);
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
        storeAnswers(false);
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
        switch(item.type.toLowerCase()){
            case "information":
                next = InformationFragment.from(item, this, index, last);
                tag = Constants.FRAGMENT_QUESTIONNAIRE_INFORMATION + index;
                break;
            case "choice":
                next = ChoiceFragment.from(item, this, index, last);
                tag = Constants.FRAGMENT_QUESTIONNAIRE_CHOICE + index;
                break;
            case "multichoice":
                next = MultichoiceFragment.from(item, this, index, last);
                tag = Constants.FRAGMENT_QUESTIONNAIRE_MULTICHOICE + index;
                break;
            case "input":
                next = InputFragment.from(item, this, index, last);
                tag = Constants.FRAGMENT_QUESTIONNAIRE_INPUT + index;
                break;
            default:
                return;
        }
        // Replace current fragment
        mainActivity.replaceFragment(next, tag);
    }

    public void completeQuestionnaire(MainActivity mainActivity){
        storeAnswers(true);
        Context context = mainActivity.getApplicationContext();
        mainActivity.loadHomeScreen();
        String thanksMessage = mainActivity.getString(R.string.participationThanks);
        Toast.makeText(context, thanksMessage, Toast.LENGTH_LONG).show();
    }
}

