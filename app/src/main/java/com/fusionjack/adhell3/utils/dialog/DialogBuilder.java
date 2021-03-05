package com.fusionjack.adhell3.utils.dialog;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.WindowManager;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.fusionjack.adhell3.R;

public final class DialogBuilder {

    private DialogBuilder() {
    }

    public static void showDialog(@StringRes int title, @StringRes int message, Context context) {
        if (context != null) {
            AlertDialog dialog = new AlertDialog.Builder(context, R.style.DialogStyle)
                    .setTitle(title)
                    .setMessage(message)
                    .create();
            dialog.show();
            styleDialog(dialog);
        }
    }

    public static void showDialog(@StringRes int title, String message, Context context) {
        if (context != null) {
            AlertDialog dialog = new AlertDialog.Builder(context, R.style.DialogStyle)
                    .setTitle(title)
                    .setMessage(message)
                    .create();
            dialog.show();
            styleDialog(dialog);
        }
    }

    protected static void styleDialog(AlertDialog dialog) {
        int color = ContextCompat.getColor(dialog.getContext(), R.color.colorAccent);
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setTextColor(color);
        dialog.getButton(DialogInterface.BUTTON_NEGATIVE).setTextColor(color);
        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setTextColor(color);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.getWindow().setDimAmount(0.75f);
    }

    public static void styleDialog(ProgressDialog dialog) {
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.getWindow().setDimAmount(0.75f);
    }


}
