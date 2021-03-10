package com.fusionjack.adhell3.utils.dialog;

import android.view.View;

import androidx.appcompat.app.AlertDialog;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.utils.LogUtils;

import java.util.function.Consumer;

public final class HostsFileDialog {

    private static HostsFileDialog instance;

    private AlertDialog dialog;
    private final Consumer<View> onPositiveButton;
    private boolean isShown = false;

    public HostsFileDialog(View view, Consumer<View> onPositiveButton) {
        this.onPositiveButton = onPositiveButton;
        this.dialog = new LayoutDialogBuilder(view)
                .setLayout(R.layout.dialog_add_hosts_file)
                .create(onPositiveButton, () -> {});
    }

    public synchronized static HostsFileDialog getInstance(View view, Consumer<View> onPositiveButton) {
        if (instance == null) {
            LogUtils.info("Creating HostsFileDialog ...");
            instance = new HostsFileDialog(view, onPositiveButton);
        }
        return instance;
    }

    public synchronized void show() {
        if (isShown) {
            onPositiveButton.accept(null);
        } else {
            if (dialog != null && !dialog.isShowing()) {
                dialog.show();
                isShown = true;
            }
        }
    }

    public synchronized static void destroy() {
        if (instance != null) {
            LogUtils.info("Destroying HostsFileDialog ...");
            instance.dialog = null;
            instance = null;
        }
    }

}
