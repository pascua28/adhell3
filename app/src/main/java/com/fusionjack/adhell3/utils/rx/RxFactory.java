package com.fusionjack.adhell3.utils.rx;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;

import com.fusionjack.adhell3.utils.LogUtils;

import java.util.function.Consumer;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;

public final class RxFactory {

    private RxFactory() {
    }

    public static void async(Completable observable, Scheduler scheduler, Runnable onSubscribeCallback, Runnable onCompleteCallback, Runnable onErrorCallback, Context context) {
        observable.subscribeOn(scheduler)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(completableObserver(onSubscribeCallback, onCompleteCallback, onErrorCallback, context));
    }

    public static CompletableObserver completableObserver(Runnable onSubscribeCallback, Runnable onCompleteCallback, Runnable onErrorCallback, Context context) {
        return new CompletableObserver() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                onSubscribeCallback.run();
            }

            @Override
            public void onComplete() {
                onCompleteCallback.run();
            }

            @Override
            public void onError(@NonNull Throwable e) {
                onErrorCallback.run();
                LogUtils.error(e.getMessage(), e);
                if (context != null) {
                    new AlertDialog.Builder(context)
                            .setTitle("Error")
                            .setMessage(e.getMessage())
                            .show();
                }
            }
        };
    }

    public static <T> void async(Single<T> observable, Scheduler scheduler, Runnable onSubscribeCallback, Consumer<T> onSuccessCallback, Runnable onErrorCallback, Context context) {
        observable.subscribeOn(scheduler)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(singleObserver(onSubscribeCallback, onSuccessCallback, onErrorCallback, context));
    }

    public static <T> SingleObserver<T> singleObserver(Runnable onSubscribeCallback, Consumer<T> onSuccessCallback, Runnable onErrorCallback, Context context) {
        return new SingleObserver<T>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                onSubscribeCallback.run();
            }

            @Override
            public void onSuccess(@NonNull T t) {
                onSuccessCallback.accept(t);
            }

            @Override
            public void onError(@NonNull Throwable e) {
                onErrorCallback.run();
                LogUtils.error(e.getMessage(), e);
                if (context != null) {
                    new AlertDialog.Builder(context)
                            .setTitle("Error")
                            .setMessage(e.getMessage())
                            .show();
                }
            }
        };
    }

}
