package com.fusionjack.adhell3.utils;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.fusionjack.adhell3.R;

public class DialogUtils {

    public static AlertDialog getProgressDialog(String progressText, Context context) {
        View view = null;
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        if (inflater != null) {
            view = inflater.inflate(R.layout.dialog_progress, new RelativeLayout(context), false);

            TextView loadingTextView = view.findViewById(R.id.loading_msg);
            loadingTextView.setText(progressText);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.ThemeOverlay_AlertDialog);
        builder.setCancelable(true);
        builder.setView(view);

        AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null)
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        return dialog;
    }

    public static void setProgressDialogMessage(AlertDialog dialog, String message) {
        TextView loadingTextView = dialog.findViewById(R.id.loading_msg);
        if (loadingTextView != null) {
            loadingTextView.setText(message);
        }
    }
}
