package edu.berkeley.cs.amplab.carat.android.model_classes;

import edu.berkeley.cs.amplab.carat.android.R;

/**
 * Static actions
 * Created by Jonatan Hamberg on 23.3.2016.
 */
public class CustomAction {
    private ActionType type;
    private Integer id;
    private int icon;
    private String title;
    private String subtitle;
    private boolean expandable;
    private int expandedTitle;
    private int expandedText;
    public enum ActionType{
        GOOGLE_SURVEY, QUESTIONNAIRE, COLLECT
    }

    /**
     * Creates a default static action which is not expandable.
     * @param type Action type
     * @param title Title resource id
     * @param subtitle Subtitle resource id
     * @param id Resource id
     */
    public CustomAction(ActionType type, String title, String subtitle, Integer id){
        this.type = type;
        this.title = title;
        this.subtitle = subtitle;
        this.expandable = false;
        this.icon = R.drawable.ic_launcher_transp;
        this.id = id;
    }

    public CustomAction(ActionType type, String title, String subtitle){
        this(type, title, subtitle, null);
    }

    /**
     * Makes action expandable and adds content to it
     * @param title Title resource id
     * @param text Text resource id
     * @return Static action instance
     */
    public CustomAction makeExpandable(int title, int text){
        this.expandable = true;
        this.expandedTitle = title;
        this.expandedText = text;
        return this;
    }

    /**
     * Adds an icon to the action, default is the application icon.
     * @param resId Action icon resource id
     * @return Static action instance
     */
    public CustomAction addIcon(int resId){
        this.icon = resId;
        return this;
    }

    /**
     * @return True if action can be expanded
     */
    public boolean isExpandable(){
        return expandable;
    }

    public ActionType getType() {
        return type;
    }

    public int getIcon() {
        return icon;
    }

    public String getTitle() {
        return title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public int getExpandedText() {
        return expandedText;
    }

    public int getExpandedTitle(){
        return expandedTitle;
    }

    public int getId(){
        return id;
    }
}
