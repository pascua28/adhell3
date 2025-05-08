package com.fusionjack.adhell3.utils.dialog;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.utils.DeviceAdminInteractor;
import com.fusionjack.adhell3.utils.LogUtils;
import com.google.android.material.textfield.TextInputEditText;

public class TransferDeviceOwnerDialog {
    View view;

    private AlertDialog dialog;

    public void show() {
        if (dialog != null && !dialog.isShowing()) {
            dialog.show();
        }
    }

    public TransferDeviceOwnerDialog(View view) {
        if (view == null || view.getContext() == null) {
            return;
        }

        this.view = view;
        Context context = view.getContext();

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_transfer_device_owner, (ViewGroup) view, false);

        dialog = new AlertDialog.Builder(context, R.style.DialogStyle)
                .setView(dialogView)
                .setPositiveButton(R.string.transfer_do, null)
                .setCancelable(true)
                .create();

        dialog.setOnShowListener(dialogInterface -> {
           Button button = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
           button.setOnClickListener(v -> onPositiveButtonClick(dialogInterface));
        });
    }

    private void onPositiveButtonClick(DialogInterface dialogInterface) {
        if (dialog == null) {
            LogUtils.error("dialog is null");
            return;
        }

        TextInputEditText editText = dialog.findViewById(R.id.transferDeviceOwnerEditText);

        String rawComponentName = editText.getText().toString();

        String[] splitComponentName = rawComponentName.split("/");

        if (splitComponentName.length != 2) {
            Toast.makeText(view.getContext(), "Invalid input", Toast.LENGTH_SHORT).show();
            return;
        }

        if (splitComponentName[1].startsWith(".")) { // Append package name to short class name
            splitComponentName[1] = splitComponentName[0] + splitComponentName[1];
        }

        ComponentName componentName = new ComponentName(splitComponentName[0], splitComponentName[1]);
        DeviceAdminInteractor dai = DeviceAdminInteractor.getInstance();

        boolean result = dai.transferDeviceOwner(dialog.getContext(), componentName);
        if (result) {
            if (!dai.isDeviceOwner()) {
                Toast.makeText(view.getContext(), "Successfully transferred", Toast.LENGTH_SHORT).show();
                dialogInterface.dismiss();
            } else {
                Toast.makeText(view.getContext(), "Failed to finish transfer", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(view.getContext(), "Failed to send transfer", Toast.LENGTH_SHORT).show();
        }
    }
}
