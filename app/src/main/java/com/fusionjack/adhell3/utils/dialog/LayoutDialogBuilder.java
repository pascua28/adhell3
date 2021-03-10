package com.fusionjack.adhell3.utils.dialog;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AlertDialog;

import com.fusionjack.adhell3.R;

import java.util.function.Consumer;

public final class LayoutDialogBuilder {

    private static final Runnable EMPTY_RUNNABLE = () -> {};

    private final View view;
    private View dialogView;

    public LayoutDialogBuilder(View view) {
        this.view = view;
    }

    public LayoutDialogBuilder setLayout(@LayoutRes int layout) {
        if (view != null && view.getContext() != null) {
            this.dialogView = LayoutInflater.from(view.getContext()).inflate(layout, (ViewGroup) view, false);
        }
        return this;
    }

    public LayoutDialogBuilder customize(Consumer<View> onCustomize) {
        if (dialogView != null) {
            onCustomize.accept(dialogView);
        }
        return this;
    }

    public void show(Consumer<View> onPositiveButton) {
        show(onPositiveButton, EMPTY_RUNNABLE);
    }

    public void show(Consumer<View> onPositiveButton, Runnable onNegativeButton) {
        AlertDialog dialog = create(onPositiveButton, onNegativeButton);
        if (dialog != null) {
            dialog.show();
        }
    }

    public AlertDialog create(Consumer<View> onPositiveButton, Runnable onNegativeButton) {
        if (dialogView != null) {
            return new AlertDialog.Builder(view.getContext(), R.style.DialogStyle)
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.yes, (d, whichButton) -> onPositiveButton.accept(dialogView))
                    .setNegativeButton(android.R.string.no, (d, whichButton) -> onNegativeButton.run())
                    .create();
        }
        return null;
    }

}
