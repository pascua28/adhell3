package com.fusionjack.adhell3.utils.dialog;

import android.content.Context;
import android.os.Build;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.R;
import com.samsung.android.knox.EnterpriseDeviceManager;

public final class AboutDialogBuilder {

    private AboutDialogBuilder() {
    }

    public static void show(View view) {
        if (view == null || view.getContext() == null) {
            return;
        }
        Context context = view.getContext();
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_about, (ViewGroup) view, false);
        TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
        titleTextView.setText(R.string.about_title);

        TextView infoTextView = dialogView.findViewById(R.id.infoTextView);
        infoTextView.setText(R.string.about_content);
        infoTextView.setMovementMethod(LinkMovementMethod.getInstance());

        String subInfoPlaceholder = context.getResources().getString(R.string.about_sub_content);
        String subInfo = String.format(subInfoPlaceholder,
                BuildConfig.VERSION_NAME, BuildConfig.BUILD_DATE,
                EnterpriseDeviceManager.getAPILevel(), Build.VERSION.SDK_INT);

        TextView subInfoTextView = dialogView.findViewById(R.id.subInfoTextView);
        subInfoTextView.setText(subInfo);

        AlertDialog dialog = new AlertDialog.Builder(context, R.style.DialogStyle)
                .setView(dialogView)
                .setPositiveButton(android.R.string.yes, null)
                .create();
        dialog.show();
        DialogBuilder.styleDialog(dialog);
    }
}
