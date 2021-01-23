package com.fusionjack.adhell3.utils.rx;

import android.app.ProgressDialog;
import android.content.Context;

import java.lang.ref.WeakReference;
import java.util.Optional;

import io.reactivex.Completable;
import io.reactivex.Scheduler;

public class RxCompletableBuilder {

    private static final Runnable EMPTY_RUNNABLE = () -> {};

    private final Scheduler scheduler;

    private String dialogMessage;
    private boolean showDialog;

    private WeakReference<Context> weakReference;

    RxCompletableBuilder(Scheduler scheduler) {
        this.scheduler = scheduler;
        this.showDialog = false;
    }

    public RxCompletableBuilder showErrorAlert(Context context) {
        if (weakReference == null) {
            weakReference = new WeakReference<>(context);
        }
        return this;
    }

    public RxCompletableBuilder setShowDialog(String dialogMessage, Context context) {
        if (weakReference == null) {
            weakReference = new WeakReference<>(context);
        }
        this.showDialog = true;
        this.dialogMessage = dialogMessage;
        return this;
    }

    public void async(Completable observable) {
        async(observable, EMPTY_RUNNABLE, EMPTY_RUNNABLE, EMPTY_RUNNABLE);
    }

    public void async(Completable observable, Runnable onCompletableCallback) {
        async(observable, EMPTY_RUNNABLE, onCompletableCallback, EMPTY_RUNNABLE);
    }

    public void async(Completable observable, Runnable onSubscribeCallback, Runnable onCompletableCallback, Runnable onErrorCallback) {
        Context context = Optional.ofNullable(weakReference).map(WeakReference::get).orElse(null);
        if (showDialog) {
            ProgressDialog dg = Optional.ofNullable(context).map(ProgressDialog::new).orElse(null);
            Optional.ofNullable(dg).ifPresent(dialog -> {
                Runnable onSubscribe = () -> {
                    dialog.setMessage(dialogMessage);
                    dialog.setCancelable(false);
                    dialog.show();
                };
                Runnable onComplete = dialog::dismiss;
                Runnable onError = dialog::dismiss;
                RxFactory.async(observable, scheduler, onSubscribe, onComplete, onError, context);
            });
        } else {
            RxFactory.async(observable, scheduler, onSubscribeCallback, onCompletableCallback, onErrorCallback, context);
        }
    }

}
