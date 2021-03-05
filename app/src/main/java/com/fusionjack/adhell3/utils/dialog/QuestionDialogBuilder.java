package com.fusionjack.adhell3.utils.dialog;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;

import com.fusionjack.adhell3.R;

public final class QuestionDialogBuilder {

    private static final Runnable EMPTY_RUNNABLE = () -> {};
    private static final int EMPTY_ID = -1;

    private final View view;

    @StringRes private int titleId;
    @StringRes private int questionId;
    private String title;
    private String question;

    @StringRes private int positiveButtonText;
    @StringRes private int negativeButtonText;
    @StringRes private int neutralButtonText;

    public QuestionDialogBuilder(View view) {
        this.view = view;
        this.titleId = EMPTY_ID;
        this.questionId = EMPTY_ID;
        this.positiveButtonText = android.R.string.yes;
        this.negativeButtonText = android.R.string.no;
        this.neutralButtonText = android.R.string.no;
    }

    public QuestionDialogBuilder setTitle(@StringRes int titleId) {
        this.titleId = titleId;
        return this;
    }

    public QuestionDialogBuilder setQuestion(@StringRes int questionId) {
        this.questionId = questionId;
        return this;
    }

    public QuestionDialogBuilder setTitle(String title) {
        this.title = title;
        return this;
    }

    public QuestionDialogBuilder setQuestion(String question) {
        this.question = question;
        return this;
    }

    public QuestionDialogBuilder setPositiveButtonText(int positiveButtonText) {
        this.positiveButtonText = positiveButtonText;
        return this;
    }

    public QuestionDialogBuilder setNegativeButtonText(int negativeButtonText) {
        this.negativeButtonText = negativeButtonText;
        return this;
    }

    public QuestionDialogBuilder setNeutralButtonText(int neutralButtonText) {
        this.neutralButtonText = neutralButtonText;
        return this;
    }

    public void show(Runnable onPositiveButton) {
        show(onPositiveButton, EMPTY_RUNNABLE, EMPTY_RUNNABLE);
    }

    public void show(Runnable onPositiveButton, Runnable onNegativeButton) {
        show(onPositiveButton, onNegativeButton, EMPTY_RUNNABLE);
    }

    public void show(Runnable onPositiveButton, Runnable onNegativeButton, Runnable onNeutralButton) {
        AlertDialog dialog = create(onPositiveButton, onNegativeButton, onNeutralButton);
        if (dialog != null) {
            dialog.show();
        }
    }

    public AlertDialog create(Runnable onPositiveButton, Runnable onNegativeButton, Runnable onNeutralButton) {
        if (view == null || view.getContext() == null) {
            return null;
        }
        Context context = view.getContext();
        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_question, (ViewGroup) view, false);
        TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
        TextView questionTextView = dialogView.findViewById(R.id.questionTextView);

        if (titleId == EMPTY_ID) {
            titleTextView.setText(title);
        } else {
            titleTextView.setText(titleId);
        }
        if (questionId == EMPTY_ID) {
            questionTextView.setText(question);
        } else {
            questionTextView.setText(questionId);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setView(dialogView)
                .setPositiveButton(positiveButtonText, (dialog, whichButton) -> onPositiveButton.run())
                .setNegativeButton(negativeButtonText, (dialog, whichButton) -> onNegativeButton.run());
        if (onNeutralButton != null && onNeutralButton != EMPTY_RUNNABLE) {
            builder.setNeutralButton(neutralButtonText, (dialog, whichButton) -> onNeutralButton.run());
        }

        return builder.create();
    }

}
