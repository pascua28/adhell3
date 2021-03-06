package com.fusionjack.adhell3.utils.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.utils.PasswordStorage;

public class EnterPasswordDialog {

    private static EnterPasswordDialog instance;

    private AlertDialog dialog;

    private EnterPasswordDialog(View view, Runnable callback) {
        if (view == null || view.getContext() == null) {
            return;
        }

        Context context = view.getContext();
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_enter_password, (ViewGroup) view, false);

        this.dialog = new AlertDialog.Builder(context, R.style.DialogStyle)
                .setView(dialogView)
                .setPositiveButton(android.R.string.yes, null)
                .setCancelable(false)
                .create();

        dialog.setOnShowListener(d -> {
            TextView infoTextView = dialogView.findViewById(R.id.infoTextView);
            infoTextView.setText(R.string.dialog_enter_password_summary);

            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                EditText passwordEditText = dialogView.findViewById(R.id.passwordEditText);
                String password = passwordEditText.getText().toString();
                try {
                    String passwordHash = AppPreferences.getInstance().getPasswordHash();
                    if (PasswordStorage.verifyPassword(password, passwordHash)) {
                        passwordEditText.setText("");
                        dialog.dismiss();
                        callback.run();
                    } else {
                        infoTextView.setText(R.string.dialog_wrong_password);
                    }
                } catch (PasswordStorage.CannotPerformOperationException | PasswordStorage.InvalidHashException e) {
                    LogUtils.error(e.getMessage());
                    infoTextView.setText(R.string.dialog_wrong_password);
                }
            });
        });
    }

    public synchronized static EnterPasswordDialog getInstance(View view, Runnable callback) {
        if (instance == null) {
            instance = new EnterPasswordDialog(view, callback);
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
            LogUtils.info("Destroying EnterPasswordDialog ...");
            instance.dialog = null;
            instance = null;
        }
    }

}
