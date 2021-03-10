package com.fusionjack.adhell3.utils.dialog;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.tasks.BackupDatabaseRxTask;
import com.fusionjack.adhell3.utils.DeviceAdminInteractor;
import com.fusionjack.adhell3.utils.LicenseHandler;
import com.fusionjack.adhell3.utils.LogUtils;

import java.util.function.Consumer;

public final class LicenseDialog {

    private static LicenseDialog instance;

    private AlertDialog dialog;

    // Change license
    public LicenseDialog(View view, SharedPreferences sharedPreferences) {
        if (view == null || view.getContext() == null) {
            return;
        }

        Context context = view.getContext();
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_activate_license, (ViewGroup) view, false);

        this.dialog = new AlertDialog.Builder(context, R.style.DialogStyle)
                .setView(dialogView)
                .setPositiveButton(android.R.string.yes, null)
                .setCancelable(true)
                .create();

        dialog.setOnShowListener(d -> {
            TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
            titleTextView.setText(R.string.change_license);

            init(dialogView, sharedPreferences, context);
            initActivationButton(view, dialogView, () -> {});
        });
    }

    // Activate license
    public LicenseDialog(View view, SharedPreferences sharedPreferences, Runnable onActivationCallback, Runnable uninstallAction) {
        if (view == null || view.getContext() == null) {
            return;
        }

        Context context = view.getContext();
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_activate_license, (ViewGroup) view, false);

        this.dialog = new AlertDialog.Builder(context, R.style.DialogStyle)
                .setView(dialogView)
                .setPositiveButton(android.R.string.yes, null)
                .setNegativeButton(R.string.backup, null)
                .setNeutralButton(R.string.uninstall, null)
                .setCancelable(false)
                .create();

        dialog.setOnShowListener(d -> {
            boolean knoxEnabled = DeviceAdminInteractor.getInstance().isKnoxEnabled(context);
            TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
            titleTextView.setText(knoxEnabled ? R.string.deactivate_license : R.string.activate_license);

            init(dialogView, sharedPreferences, context);
            initActivationButton(view, dialogView, onActivationCallback);
            initBackupButton(view);
            initUninstallButton(uninstallAction);
        });
    }

    public synchronized static LicenseDialog getChangeInstance(View view, SharedPreferences sharedPreferences) {
        if (instance == null) {
            LogUtils.info("Creating Change LicenseDialog ...");
            instance = new LicenseDialog(view, sharedPreferences);
        }
        return instance;
    }

    public synchronized static LicenseDialog getActivationInstance(View view, SharedPreferences sharedPreferences, Runnable onActivationCallback, Runnable uninstallAction) {
        if (instance == null) {
            LogUtils.info("Creating Activation LicenseDialog ...");
            instance = new LicenseDialog(view, sharedPreferences, onActivationCallback, uninstallAction);
        }
        return instance;
    }

    private void initActivationButton(View view, View dialogView, Runnable callback) {
        Context context = view.getContext();
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setOnClickListener(v -> {
            boolean knoxEnabled = DeviceAdminInteractor.getInstance().isKnoxEnabled(context);
            positiveButton.setText(knoxEnabled ? R.string.deactivating_knox_license : R.string.activating_knox_license);
            positiveButton.setEnabled(false);
            positiveButton.setTextColor(Color.GRAY);

            Runnable onSuccessActivation = () -> {
                dialog.dismiss();
                Toast.makeText(context, "License activated", Toast.LENGTH_LONG).show();
                callback.run();
            };
            Runnable onSuccessDeactivation = () -> {
                Toast.makeText(context, "License deactivated", Toast.LENGTH_LONG).show();
                reinit(dialogView);
            };
            Consumer<String> onError = message -> {
                positiveButton.setEnabled(true);
                DialogBuilder.showDialog(R.string.error, message, context);
            };

            EditText licenseKeyEditText = dialogView.findViewById(R.id.licenseKeyEditText);
            String knoxKey = licenseKeyEditText.getText().toString();
            if (knoxKey.isEmpty()) {
                Toast.makeText(context, "License key cannot be empty!", Toast.LENGTH_LONG).show();
            } else {
                LicenseHandler.getInstance().activeOrDeactivateLicense(knoxKey, onSuccessActivation, onSuccessDeactivation, onError);
            }
        });
    }

    private void initBackupButton(View view) {
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        negativeButton.setOnClickListener(v -> backupDatabase(view));
    }

    private void initUninstallButton(Runnable uninstallAction) {
        Button neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
        neutralButton.setOnClickListener(v -> uninstallAction.run());
    }

    private void init(View dialogView, SharedPreferences sharedPreferences, Context context) {
        boolean knoxEnabled = DeviceAdminInteractor.getInstance().isKnoxEnabled(context);
        LogUtils.info("Knox is " + (knoxEnabled ? "enabled" : "disabled"));

        String knoxKey = DeviceAdminInteractor.getInstance().getKnoxKey(sharedPreferences);
        EditText licenseKeyEditText = dialogView.findViewById(R.id.licenseKeyEditText);
        licenseKeyEditText.setText(knoxKey);
        licenseKeyEditText.setTextColor(knoxEnabled ? Color.GRAY : ContextCompat.getColor(dialog.getContext(), R.color.colorText));
        licenseKeyEditText.setEnabled(!knoxEnabled);

        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setText(knoxEnabled ? R.string.deactivate : R.string.activate);
        positiveButton.setEnabled(true);
    }

    private void reinit(View dialogView) {
        TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
        titleTextView.setText(R.string.activate_license);

        EditText licenseKeyEditText = dialogView.findViewById(R.id.licenseKeyEditText);
        licenseKeyEditText.setTextColor(ContextCompat.getColor(dialog.getContext(), R.color.colorText));
        licenseKeyEditText.setEnabled(true);

        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        positiveButton.setText(R.string.activate_license);
        positiveButton.setEnabled(true);
        positiveButton.setTextColor(ContextCompat.getColor(dialog.getContext(), R.color.colorAccent));
    }

    private void backupDatabase(View view) {
        new QuestionDialogBuilder(view)
                .setTitle(R.string.backup_database_dialog_title)
                .setQuestion(R.string.backup_database_dialog_text)
                .show(() -> new BackupDatabaseRxTask(view.getContext()).run());
    }

    public void show() {
        if (dialog != null && !dialog.isShowing()) {
            dialog.show();
        }
    }

    public synchronized static void destroy() {
        if (instance != null) {
            LogUtils.info("Destroying LicenseDialog ...");
            instance.dialog = null;
            instance = null;
        }
    }

}
