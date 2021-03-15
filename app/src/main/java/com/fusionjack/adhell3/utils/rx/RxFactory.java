package com.fusionjack.adhell3.utils.rx;

import android.content.Context;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.utils.dialog.DialogBuilder;

import java.util.function.Consumer;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.core.Scheduler;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;

final class RxFactory {

    private RxFactory() {
    }

    static void async(Completable observable, Scheduler scheduler, Runnable onSubscribeCallback, Runnable onCompleteCallback, Runnable onErrorCallback, Context context) {
        observable.subscribeOn(scheduler)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(completableObserver(onSubscribeCallback, onCompleteCallback, onErrorCallback, context));
    }

    private static CompletableObserver completableObserver(Runnable onSubscribeCallback, Runnable onCompleteCallback, Runnable onErrorCallback, Context context) {
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
                    DialogBuilder.showDialog(R.string.error, e.getMessage(), context);
                }
            }
        };
    }

    static <T> void async(Single<T> observable, Scheduler scheduler, Runnable onSubscribeCallback, Consumer<T> onSuccessCallback, Runnable onErrorCallback, Context context) {
        observable.subscribeOn(scheduler)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(singleObserver(onSubscribeCallback, onSuccessCallback, onErrorCallback, context));
    }

    private static <T> SingleObserver<T> singleObserver(Runnable onSubscribeCallback, Consumer<T> onSuccessCallback, Runnable onErrorCallback, Context context) {
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
                    DialogBuilder.showDialog(R.string.error, e.getMessage(), context);
                }
            }
        };
    }

}
