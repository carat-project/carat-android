package edu.berkeley.cs.amplab.carat.android.components;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.support.v7.app.AlertDialog;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.CheckBox;

import edu.berkeley.cs.amplab.carat.android.R;

/**
 * Created by Jonatan Hamberg on 7.2.2018.
 */
public class CaratDialogs {
    public static abstract class Callback {
        protected abstract void run(boolean success, boolean remember);
    }

    public static void permissionRequest(Context context, int message, Callback cb){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.permission_request);
        builder.setView(R.layout.permission_prompt);
        builder.setMessage(message);
        builder.setIcon(R.drawable.carat_material_icon);

        builder.setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
            cb.run(true, isRememberChecked(dialog));
        });
        builder.setNegativeButton(R.string.dialog_cancel, (dialog, which) -> {
            cb.run(false, isRememberChecked(dialog));
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(Dialog.BUTTON_NEGATIVE).setTextColor(Color.rgb(248, 176, 58));
        dialog.getButton(Dialog.BUTTON_POSITIVE).setTextColor(Color.rgb(248, 176, 58));
        dialog.getButton(Dialog.BUTTON_POSITIVE).setShadowLayer(0,0,0,0);
        dialog.getButton(Dialog.BUTTON_NEGATIVE).setShadowLayer(0,0,0,0);
        Window window = dialog.getWindow();
        if(window != null){
            View custom = window.findViewById(R.id.custom);
            if(custom != null){
                View parent = (View)custom.getParent();
                parent.setMinimumHeight(0);
            }
        }
    }

    public static void choiceDialog(Context context, String title, String message, String secondaryOption, Runnable onSecondaryOption){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.dialog_ok, (dialog, which) -> {
            dialog.dismiss();
        });
        builder.setNegativeButton(secondaryOption, (dialog, which) -> {
            onSecondaryOption.run();
        });

        AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(Dialog.BUTTON_NEGATIVE).setTextColor(Color.rgb(248, 176, 58));
        dialog.getButton(Dialog.BUTTON_POSITIVE).setTextColor(Color.rgb(248, 176, 58));
        dialog.getButton(Dialog.BUTTON_POSITIVE).setShadowLayer(0,0,0,0);
        dialog.getButton(Dialog.BUTTON_NEGATIVE).setShadowLayer(0,0,0,0);
        Window window = dialog.getWindow();
        if(window != null){
            View custom = window.findViewById(R.id.custom);
            if(custom != null){
                View parent = (View)custom.getParent();
                parent.setMinimumHeight(0);
            }
        }
    }

    public static void informationDialog(Context context, String title, String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setPositiveButton(android.R.string.yes, (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();

        Button button = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
        if(button != null){
            button.setTextColor(context.getResources().getColor(R.color.orange));
        }
    }

    private static boolean isRememberChecked(DialogInterface dialog){
        Window window = ((AlertDialog) dialog).getWindow();
        if(window != null){
            CheckBox remindCheckbox = (CheckBox)window.findViewById(R.id.remember_checkbox);
            return remindCheckbox.isChecked();
        }
        return false;
    }
}
