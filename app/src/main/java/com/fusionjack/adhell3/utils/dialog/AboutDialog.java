package com.fusionjack.adhell3.utils.dialog;

import android.os.Build;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.R;
import com.samsung.android.knox.EnterpriseDeviceManager;

import java.util.function.Consumer;

public final class AboutDialog {

    private AboutDialog() {
    }

    public static void show(View view) {
        if (view == null || view.getContext() == null) {
            return;
        }

        Consumer<View> onCustomize = dialogView -> {
            TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
            titleTextView.setText(R.string.about_title);

            TextView infoTextView = dialogView.findViewById(R.id.infoTextView);
            infoTextView.setText(R.string.about_content);
            infoTextView.setMovementMethod(LinkMovementMethod.getInstance());

            String subInfoPlaceholder = view.getContext().getResources().getString(R.string.about_sub_content);
            String subInfo = String.format(subInfoPlaceholder,
                    BuildConfig.VERSION_NAME, BuildConfig.BUILD_DATE,
                    EnterpriseDeviceManager.getAPILevel(), Build.VERSION.SDK_INT);

            TextView subInfoTextView = dialogView.findViewById(R.id.subInfoTextView);
            subInfoTextView.setText(subInfo);
        };

        new LayoutDialogBuilder(view)
                .setLayout(R.layout.dialog_about)
                .customize(onCustomize)
                .show(d -> {});
    }
}
