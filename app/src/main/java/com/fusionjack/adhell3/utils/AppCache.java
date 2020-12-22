package com.fusionjack.adhell3.utils;

import android.graphics.drawable.Drawable;

import java.util.Map;

import io.reactivex.CompletableObserver;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class AppCache {
    private final AppInfoResult result;

    private static AppCache instance;

    private static final CompletableObserver EMPTY_OBSERVER = new CompletableObserver() {
        @Override
        public void onSubscribe(@NonNull Disposable d) {
        }

        @Override
        public void onComplete() {
        }

        @Override
        public void onError(@NonNull Throwable e) {
        }
    };

    private AppCache() {
        this.result = new AppInfoResult();
    }

    public static synchronized AppCache getInstance(CompletableObserver callerObserver) {
        if (instance == null) {
            instance = new AppCache();
            instance.cacheApps(callerObserver);
        }
        return instance;
    }

    public static synchronized void load() {
        getInstance(EMPTY_OBSERVER);
    }

    private void cacheApps(CompletableObserver callerObserver) {
        AppDatabaseFactory.refreshInstalledApps()
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(handleResult(callerObserver == null ? EMPTY_OBSERVER : callerObserver));
    }

    private SingleObserver<AppInfoResult> handleResult(CompletableObserver callerObserver) {
        return new SingleObserver<AppInfoResult>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                callerObserver.onSubscribe(d);
            }

            @Override
            public void onSuccess(@NonNull AppInfoResult appInfoResult) {
                result.addAppIcons(appInfoResult.getAppsIcons());
                result.addAppNames(appInfoResult.getAppsNames());
                result.addVersionNames(appInfoResult.getVersionNames());
                callerObserver.onComplete();
            }

            @Override
            public void onError(@NonNull Throwable e) {
                callerObserver.onError(e);
            }
        };
    }

    public Map<String, Drawable> getIcons() {
        return result.getAppsIcons();
    }

    public Map<String, String> getNames() {
        return result.getAppsNames();
    }

    public Map<String, String> getVersionNames() {
        return result.getVersionNames();
    }

}
