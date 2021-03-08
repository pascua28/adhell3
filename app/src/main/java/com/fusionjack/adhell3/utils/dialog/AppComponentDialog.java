package com.fusionjack.adhell3.utils.dialog;

import android.view.View;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.utils.LogUtils;
import com.google.android.material.snackbar.Snackbar;

public final class AppComponentDialog {

    private static AppComponentDialog instance;

    private Snackbar snackbar;
    private boolean isShown = false;

    public AppComponentDialog(View view) {
        View rootView = view.findViewById(R.id.appComponentCoordinatorLayout);
        this.snackbar = Snackbar.make(rootView, R.string.dialog_system_app_components_info, Snackbar.LENGTH_LONG);
        snackbar.setDuration(10000);
        snackbar.setAction("Close", v -> snackbar.dismiss());

        View snackBarView = snackbar.getView();
        snackBarView.setBackgroundColor(ContextCompat.getColor(view.getContext(), R.color.colorPrimaryDark));
        TextView snackTextView = snackBarView.findViewById(com.google.android.material.R.id.snackbar_text);
        snackTextView.setMaxLines(3);
    }

    public synchronized static AppComponentDialog getInstance(View view) {
        if (instance == null) {
            LogUtils.info("Creating AppComponentDialog ...");
            instance = new AppComponentDialog(view);
        }
        return instance;
    }

    public synchronized void show() {
        if (!isShown && BuildConfig.SHOW_SYSTEM_APP_COMPONENT) {
            snackbar.show();
            isShown = true;
        }
    }

    public synchronized static void destroy() {
        if (instance != null) {
            LogUtils.info("Destroying AppComponentDialog ...");
            instance.snackbar = null;
            instance = null;
        }
    }

}
