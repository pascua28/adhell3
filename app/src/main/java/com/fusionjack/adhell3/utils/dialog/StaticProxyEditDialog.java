package com.fusionjack.adhell3.utils.dialog;

import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.db.entity.StaticProxy;

import java.util.function.Consumer;

public final class StaticProxyEditDialog {

    private StaticProxyEditDialog() {
    }

    public static void show(View view, StaticProxy staticProxy, Consumer<View> onPositiveButton) {
        if (view == null || view.getContext() == null) {
            return;
        }

        Consumer<View> onCustomize = dialogView -> {
            TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
            titleTextView.setText(R.string.edit_proxy_title);

            TextView orginNameTextView = dialogView.findViewById(R.id.originName);
            EditText nameEditText = dialogView.findViewById(R.id.nameEditText);
            EditText hostnameEditText = dialogView.findViewById(R.id.hostnameEditText);
            EditText portEditText = dialogView.findViewById(R.id.portEditText);
            EditText exclusionEditText = dialogView.findViewById(R.id.exclusionEditText);
            EditText userEditText = dialogView.findViewById(R.id.userEditText);
            EditText passwordEditText = dialogView.findViewById(R.id.passwordEditText);

            orginNameTextView.setText(staticProxy.name);
            nameEditText.setText(staticProxy.name);
            hostnameEditText.setText(staticProxy.hostname);
            portEditText.setText(String.valueOf(staticProxy.port));
            exclusionEditText.setText(staticProxy.exclusionList);
            userEditText.setText(staticProxy.user);
            passwordEditText.setText(staticProxy.password);
        };

        new LayoutDialogBuilder(view)
                .setLayout(R.layout.dialog_static_proxy)
                .customize(onCustomize)
                .show(onPositiveButton);
    }

}
