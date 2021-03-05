package com.fusionjack.adhell3.utils.dialog;

import android.content.Context;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

public final class DialogBuilder {

    private DialogBuilder() {
    }

    public static void showDialog(@StringRes int title, @StringRes int message, Context context) {
        if (context != null) {
            new AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .show();
        }
    }

    public static void showDialog(@StringRes int title, String message, Context context) {
        if (context != null) {
            new AlertDialog.Builder(context)
                    .setTitle(title)
                    .setMessage(message)
                    .show();
        }
    }

}
