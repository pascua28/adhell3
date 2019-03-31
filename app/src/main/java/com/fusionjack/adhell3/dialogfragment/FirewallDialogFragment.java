package com.fusionjack.adhell3.dialogfragment;

import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fusionjack.adhell3.R;

import java.util.Objects;

public class FirewallDialogFragment extends DialogFragment {
    private TextView logTextView;
    private ScrollView scrollView;
    private Button closeButton;
    private String text = "";

    public static FirewallDialogFragment newInstance(String title) {
        FirewallDialogFragment fragment = new FirewallDialogFragment();
        Bundle args = new Bundle();
        args.putString("title", title);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
            int height = (int) (getResources().getDisplayMetrics().heightPixels * 0.9);
            Objects.requireNonNull(dialog.getWindow()).setLayout(width, height);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_fragment_firewall, container);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TextView titleTextView = view.findViewById(R.id.titleTextView);
        Bundle bundle = getArguments();
        if (bundle != null) {
            titleTextView.setText(bundle.getString("title"));
        }

        scrollView = view.findViewById(R.id.scrollView);
        logTextView = view.findViewById(R.id.logTextView);
        closeButton = view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dismiss());
        closeButton.setEnabled(false);

        scrollView.getLayoutParams().height = (int) (getResources().getDisplayMetrics().heightPixels * 0.675);
    }

    public void appendText(String text) {
        this.text += text + "\n";
        logTextView.setText(this.text);
        scrollToBottom();
    }

    private void scrollToBottom() {
        scrollView.post(() -> scrollView.smoothScrollTo(0, logTextView.getBottom()));
    }

    public void enableCloseButton() {
        closeButton.setEnabled(true);
    }
}
