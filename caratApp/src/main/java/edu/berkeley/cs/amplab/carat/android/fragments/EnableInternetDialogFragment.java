package edu.berkeley.cs.amplab.carat.android.fragments;

import edu.berkeley.cs.amplab.carat.android.MainActivity;
import edu.berkeley.cs.amplab.carat.android.R;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;

public class EnableInternetDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.dialog_text)
               .setPositiveButton(R.string.dialog_enable, (dialog, id) -> {
                   MainActivity mainActivity = (MainActivity) getActivity();
                   mainActivity.GoToWifiScreen();
               })
               .setNegativeButton(R.string.dialog_cancel, (dialog, id) -> {
                   EnableInternetDialogFragment.this.getDialog().cancel();
               });

        // Create the AlertDialog object and return it
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(dialogInterface -> {
            dialog.getButton(Dialog.BUTTON_NEGATIVE).setTextColor(Color.rgb(248, 176, 58));
            dialog.getButton(Dialog.BUTTON_POSITIVE).setTextColor(Color.rgb(248, 176, 58));
            dialog.getButton(Dialog.BUTTON_POSITIVE).setShadowLayer(0,0,0,0);
            dialog.getButton(Dialog.BUTTON_NEGATIVE).setShadowLayer(0,0,0,0);
        });
        return dialog;
    }
}