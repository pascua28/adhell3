package com.fusionjack.adhell3.utils.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.utils.LogUtils;

public final class DeviceAdminDialog {

    private static DeviceAdminDialog instance;

    private AlertDialog dialog;

    public DeviceAdminDialog(View view, Runnable requestDeviceAdminAction) {
        if (view == null || view.getContext() == null) {
            return;
        }

        Context context = view.getContext();
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_question, (ViewGroup) view, false);

        this.dialog = new AlertDialog.Builder(context, R.style.DialogStyle)
                .setView(dialogView)
                .setPositiveButton(android.R.string.yes, null)
                .setCancelable(false)
                .create();

        dialog.setOnShowListener(d -> {
            TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
            titleTextView.setText(R.string.dialog_device_admin_title);

            TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
            questionTextView.setText(R.string.dialog_device_admin_info);

            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setText(R.string.turn_on_admin);
            positiveButton.setOnClickListener(v -> {
                LogUtils.info("Showing device admin ...");
                requestDeviceAdminAction.run();
            });
        });
    }

    public synchronized static DeviceAdminDialog getInstance(View view, Runnable requestDeviceAdminAction) {
        if (instance == null) {
            LogUtils.info("Creating DeviceAdminDialog ...");
            instance = new DeviceAdminDialog(view, requestDeviceAdminAction);
        }
        return instance;
    }

    public void show() {
        if (dialog != null && !dialog.isShowing()) {
            dialog.show();
            DialogBuilder.styleDialog(dialog);
        }
    }

    public void dismiss() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    public synchronized static void destroy() {
        if (instance != null) {
            LogUtils.info("Destroying DeviceAdminDialog ...");
            instance.dialog = null;
            instance = null;
        }
    }

}
