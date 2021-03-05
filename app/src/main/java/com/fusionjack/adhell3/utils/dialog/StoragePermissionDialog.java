package com.fusionjack.adhell3.utils.dialog;

import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.utils.LogUtils;

public final class StoragePermissionDialog {

    private static StoragePermissionDialog instance;

    private AlertDialog dialog;

    public StoragePermissionDialog(View view, Runnable onPositiveButton, Runnable onNegativeButton) {
        this.dialog = new QuestionDialogBuilder(view)
                .setTitle(R.string.dialog_storage_permission_title)
                .setQuestion(R.string.dialog_storage_permission_summary)
                .create(onPositiveButton, onNegativeButton, () -> {});
    }

    public synchronized static StoragePermissionDialog getInstance(View view, Runnable onPositiveButton, Runnable onNegativeButton) {
        if (instance == null) {
            LogUtils.info("Creating StoragePermissionDialog ...");
            instance = new StoragePermissionDialog(view, onPositiveButton, onNegativeButton);
        }
        return instance;
    }

    public void show() {
        if (dialog != null && !dialog.isShowing()) {
            dialog.show();
            DialogBuilder.styleDialog(dialog);
        }
    }

    public synchronized static void destroy() {
        if (instance != null) {
            LogUtils.info("Destroying StoragePermissionDialog ...");
            instance.dialog = null;
            instance = null;
        }
    }

}
