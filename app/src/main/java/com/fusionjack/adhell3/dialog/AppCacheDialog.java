package com.fusionjack.adhell3.dialog;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import com.fusionjack.adhell3.utils.DialogUtils;

import java.lang.ref.WeakReference;


public class AppCacheDialog {

    private final WeakReference<Context> contextWeakReference;
    private AlertDialog dialog;

    public AppCacheDialog(Context context) {
        this.contextWeakReference = new WeakReference<>(context);
        createDialog("Caching apps, please wait...");
    }

    public AppCacheDialog(Context context, String message) {
        this.contextWeakReference = new WeakReference<>(context);
        createDialog(message);
    }

    private void createDialog(String message) {
        Context context = contextWeakReference.get();
        if (context != null) {
            dialog = DialogUtils.getProgressDialog(message, context);
            dialog.setCancelable(false);
        }
    }

    public void showDialog() {
        if (dialog != null) {
            dialog.show();
        }
    }

    public void dismissDialog() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
