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
import com.fusionjack.adhell3.utils.PasswordStorage;

public final class PasswordDialogBuilder {

    private PasswordDialogBuilder() {
    }

    public static AlertDialog showEnterPassword(View view, Runnable callback) {
        if (view == null || view.getContext() == null) {
            return null;
        }

        Context context = view.getContext();
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_enter_password, (ViewGroup) view, false);

        AlertDialog passwordDialog = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setPositiveButton(android.R.string.yes, null)
                .setCancelable(false)
                .create();

        passwordDialog.setOnShowListener(dialog -> {
            Button positiveButton = passwordDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                EditText passwordEditText = dialogView.findViewById(R.id.passwordEditText);
                String password = passwordEditText.getText().toString();
                try {
                    TextView infoTextView = dialogView.findViewById(R.id.infoTextView);
                    String passwordHash = AppPreferences.getInstance().getPasswordHash();
                    if (PasswordStorage.verifyPassword(password, passwordHash)) {
                        infoTextView.setText(R.string.dialog_enter_password_summary);
                        passwordEditText.setText("");
                        passwordDialog.dismiss();
                        callback.run();
                    } else {
                        infoTextView.setText(R.string.dialog_wrong_password);
                    }
                } catch (PasswordStorage.CannotPerformOperationException | PasswordStorage.InvalidHashException e) {
                    e.printStackTrace();
                }
            });
        });

        return passwordDialog;
    }

    public static void showSetPassword(View view, Runnable onNegativeButton) {
        if (view == null || view.getContext() == null) {
            return;
        }

        Context context = view.getContext();
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_set_password, (ViewGroup) view, false);

        new AlertDialog.Builder(context)
                .setView(dialogView)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                    EditText passwordEditText = dialogView.findViewById(R.id.passwordEditText);
                    String password = passwordEditText.getText().toString();
                    if (!password.isEmpty()) {
                        try {
                            AppPreferences.getInstance().setPassword(password);
                        } catch (PasswordStorage.CannotPerformOperationException e) {
                            e.printStackTrace();
                        }
                    } else {
                        TextView infoTextView = dialogView.findViewById(R.id.infoTextView);
                        infoTextView.setText(R.string.dialog_empty_password);
                    }
                })
                .setNegativeButton(android.R.string.no, (dialog, whichButton) -> onNegativeButton.run())
                .setCancelable(false)
                .show();
    }

}
