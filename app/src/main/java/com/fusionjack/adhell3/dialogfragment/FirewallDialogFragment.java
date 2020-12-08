package com.fusionjack.adhell3.dialogfragment;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.databinding.DialogFragmentFirewallBinding;

import java.util.Objects;

public class FirewallDialogFragment extends DialogFragment {
    private String text = "";
    private DialogFragmentFirewallBinding binding;

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
        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().getAttributes().windowAnimations = R.style.FragmentDialogAnimation;
        }
        binding = DialogFragmentFirewallBinding.inflate(inflater);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle bundle = getArguments();
        if (bundle != null) {
            binding.titleTextView.setText(bundle.getString("title"));
        }

        binding.closeButton.setOnClickListener(v -> dismiss());
        binding.closeButton.setEnabled(false);

        binding.scrollView.getLayoutParams().height = (int) (getResources().getDisplayMetrics().heightPixels * 0.675);
    }

    public void appendText(String text) {
        this.text += text + "\n";
        binding.logTextView.setText(this.text);
        scrollToBottom();
    }

    private void scrollToBottom() {
        binding.scrollView.post(() -> binding.scrollView.smoothScrollTo(0, binding.logTextView.getBottom()));
    }

    public void enableCloseButton() {
        binding.closeButton.setEnabled(true);
    }
}
