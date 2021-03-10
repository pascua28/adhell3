package com.fusionjack.adhell3.utils.dialog;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

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
        }
    }

    public static void showDialog(@StringRes int title, String message, Context context) {
        if (context != null) {
            AlertDialog dialog = new AlertDialog.Builder(context, R.style.DialogStyle)
                    .setTitle(title)
                    .setMessage(message)
                    .create();
            dialog.show();
        }
    }

}
