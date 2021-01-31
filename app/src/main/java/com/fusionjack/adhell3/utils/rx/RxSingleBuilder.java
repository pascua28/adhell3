package com.fusionjack.adhell3.utils.rx;

import android.app.ProgressDialog;
import android.content.Context;

import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.function.Consumer;

import io.reactivex.Scheduler;
import io.reactivex.Single;

public class RxSingleBuilder {

    private static final Runnable EMPTY_RUNNABLE = () -> {};

    private final Scheduler scheduler;

    private String dialogMessage;
    private boolean showDialog;

    private WeakReference<Context> weakReference;

    RxSingleBuilder(Scheduler scheduler) {
        this.scheduler = scheduler;
        this.showDialog = false;
    }

    public RxSingleBuilder setShowErrorAlert(Context context) {
        if (weakReference == null) {
            weakReference = new WeakReference<>(context);
        }
        return this;
    }

    public RxSingleBuilder setShowDialog(String dialogMessage, Context context) {
        if (weakReference == null) {
            weakReference = new WeakReference<>(context);
        }
        this.showDialog = true;
        this.dialogMessage = dialogMessage;
        return this;
    }

    public <T> void async(Single<T> observable) {
        async(observable, EMPTY_RUNNABLE, t -> {}, EMPTY_RUNNABLE);
    }

    public <T> void async(Single<T> observable, Consumer<T> onCompletableCallback) {
        async(observable, EMPTY_RUNNABLE, onCompletableCallback, EMPTY_RUNNABLE);
    }

    public <T> void async(Single<T> observable, Runnable onSubscribeCallback, Consumer<T> onSuccessCallback, Runnable onErrorCallback) {
        Context context = Optional.ofNullable(weakReference).map(WeakReference::get).orElse(null);
        if (showDialog) {
            Optional.ofNullable(context).map(ProgressDialog::new).ifPresent(dialog -> {
                Runnable onSubscribe = () -> {
                    dialog.setMessage(dialogMessage);
                    dialog.setCancelable(false);
                    dialog.show();
                };
                Consumer<T> onSuccess = t -> dialog.dismiss();
                Runnable onError = dialog::dismiss;
                RxFactory.async(observable, scheduler, onSubscribe, onSuccess, onError, context);
            });
        } else {
            RxFactory.async(observable, scheduler, onSubscribeCallback, onSuccessCallback, onErrorCallback, context);
        }
    }

}
