package com.fusionjack.adhell3.utils.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.utils.PasswordStorage;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Objects;
import java.util.Optional;

public final class SetPasswordDialog {

    private SetPasswordDialog() {
    }

    public static void show(View view, Runnable onNegativeButton) {
        if (view == null || view.getContext() == null) {
            return;
        }
        Context context = view.getContext();
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_set_password, (ViewGroup) view, false);

        AlertDialog dialog = new AlertDialog.Builder(context, R.style.DialogStyle)
                .setView(dialogView)
                .setPositiveButton(android.R.string.yes, null)
                .setNegativeButton(android.R.string.no, null)
                .setCancelable(false)
                .create();

        dialog.setOnShowListener(d -> {
            TextView infoTextView = dialogView.findViewById(R.id.infoTextView);
            infoTextView.setText(R.string.set_password_title);
            SwitchCompat enableBiometricSwitch = dialogView.findViewById(R.id.switchBiometric);
            TextView biometricTextView = dialogView.findViewById(R.id.infoBiometricTextView);

            if (PasswordStorage.isBiometricSupported(context)) {
                enableBiometricSwitch.setOnClickListener(v -> {
                    if (enableBiometricSwitch.isChecked()) {
                        PasswordStorage.authBiometric(
                            () -> { // onSuccess
                                enableBiometricSwitch.setChecked(true);
                                LogUtils.info("Biometric authentication is enabled");
                            }, () -> { // onFailure
                                enableBiometricSwitch.setChecked(false);
                                LogUtils.error("Biometric authentication is not enabled");
                            }
                        );
                    }
                });
            } else {
                enableBiometricSwitch.setVisibility(View.GONE);
                biometricTextView.setVisibility(View.GONE);
            }


            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                TextInputEditText passwordEditText = dialogView.findViewById(R.id.passwordEditText);
                String password = Optional.ofNullable(passwordEditText.getText()).map(Object::toString).orElse("");
                if (!password.isEmpty()) {
                    TextInputEditText passwordConfirmEditText = dialogView.findViewById(R.id.confirmPasswordEditText);
                    String passwordConfirm = Optional.ofNullable(passwordConfirmEditText.getText()).map(Objects::toString).orElse("");
                    if (password.equals(passwordConfirm)) {
                        try {
                            AppPreferences.getInstance().setPassword(password);
                            if (enableBiometricSwitch.isChecked()) {
                                AppPreferences.getInstance().setBiometric(true);
                            }
                            dialog.dismiss();
                        } catch (PasswordStorage.CannotPerformOperationException e) {
                            LogUtils.error(e.getMessage());
                            DialogBuilder.showDialog(R.string.error, e.getMessage(), context);
                        }
                    } else {
                        infoTextView.setText(R.string.dialog_password_not_matched);
                    }
                } else {
                    infoTextView.setText(R.string.dialog_empty_password);
                }
            });

            Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            negativeButton.setOnClickListener(v -> {
                dialog.dismiss();
                onNegativeButton.run();
            });
        });

        dialog.show();
    }

}
