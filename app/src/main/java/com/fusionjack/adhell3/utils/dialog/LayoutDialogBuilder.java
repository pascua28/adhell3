package com.fusionjack.adhell3.utils.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.LayoutRes;
import androidx.appcompat.app.AlertDialog;

import java.util.function.Consumer;

public final class LayoutDialogBuilder {

    private static final Runnable EMPTY_RUNNABLE = () -> {};
    private static final int EMPTY_ID = -1;

    private final View view;

    @LayoutRes private int layout;

    public LayoutDialogBuilder(View view) {
        this.view = view;
        this.layout = EMPTY_ID;
    }

    public LayoutDialogBuilder setLayout(@LayoutRes int layout) {
        this.layout = layout;
        return this;
    }

    public void show(Consumer<View> onPositiveButton) {
        show(onPositiveButton, EMPTY_RUNNABLE);
    }

    public void show(Consumer<View> onPositiveButton, Runnable onNegativeButton) {
        if (view != null && view.getContext() != null && layout != EMPTY_ID) {
            Context context = view.getContext();
            View dialogView = LayoutInflater.from(context).inflate(layout, (ViewGroup) view, false);
            new AlertDialog.Builder(context)
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> onPositiveButton.accept(dialogView))
                    .setNegativeButton(android.R.string.no, (dialog, whichButton) -> onNegativeButton.run())
                    .show();
        }
    }

}
