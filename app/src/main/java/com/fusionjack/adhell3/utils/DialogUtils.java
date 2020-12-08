package com.fusionjack.adhell3.utils;

import android.content.Context;
import android.view.LayoutInflater;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.databinding.DialogProgressBinding;

public class DialogUtils {

    public static AlertDialog getProgressDialog(String progressText, Context context) {
        DialogProgressBinding dialogProgressBinding = DialogProgressBinding.inflate(LayoutInflater.from(context));

        dialogProgressBinding.loadingMsg.setText(progressText);

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
        builder.setCancelable(true);
        builder.setView(dialogProgressBinding.getRoot());

        return builder.create();
    }

    public static void setProgressDialogMessage(AlertDialog dialog, String message) {
        TextView loadingTextView = dialog.findViewById(R.id.loading_msg);
        if (loadingTextView != null) {
            loadingTextView.setText(message);
        }
    }
}
