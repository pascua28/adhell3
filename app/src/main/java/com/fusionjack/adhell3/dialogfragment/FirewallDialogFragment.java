package com.fusionjack.adhell3.dialogfragment;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import com.fusionjack.adhell3.R;

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
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NORMAL, R.style.DialogStyle);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);

        int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.9);
        int height = (int)(getResources().getDisplayMetrics().heightPixels * 0.9);
        dialog.getWindow().setLayout(width, height);

        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        dialog.getWindow().setDimAmount(0.75f);

        return dialog;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_fragment_firewall, container);

        TextView titleTextView = view.findViewById(R.id.titleTextView);
        Bundle bundle = getArguments();
        if (bundle != null) {
            titleTextView.setText(bundle.getString("title"));
        }

        scrollView  = view.findViewById(R.id.scrollView);
        logTextView = view.findViewById(R.id.logTextView);
        closeButton = view.findViewById(R.id.closeButton);
        closeButton.setOnClickListener(v -> dismiss());
        closeButton.setEnabled(false);
        closeButton.setTextColor(Color.GRAY);

        scrollView.getLayoutParams().height = (int)(getResources().getDisplayMetrics().heightPixels * 0.7);

        return view;
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
        closeButton.setTextColor(ContextCompat.getColor(getContext(), R.color.colorAccent));
    }
}
