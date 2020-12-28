package com.fusionjack.adhell3.utils;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import java.util.Map;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.annotations.NonNull;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;


public class AppCache {
    private final AppInfoResult result;

    private static AppCache instance;
    private static boolean appCached;

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

    public boolean getAppCached() {
        return appCached;
    }

    private AppCache() {
        this.result = new AppInfoResult();
    }

    public static synchronized AppCache getInstance(CompletableObserver callerObserver) {
        if (instance == null) {
            instance = new AppCache();
        }
        if (!appCached) {
            instance.cacheApps(callerObserver);
            appCached = true;
        }
        return instance;
    }

    public synchronized static void inject(ApplicationInfo appInfo) {
        if (instance == null) {
            instance = new AppCache();
        }
        inject(instance, appInfo);
    }

    private static void inject(AppCache appCache, ApplicationInfo appInfo) {
        String packageName = appInfo.packageName;
        PackageManager packageManager = AdhellFactory.getInstance().getPackageManager();

        Drawable icon;
        try {
            icon = packageManager.getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            icon = null;
        }
        appCache.result.addAppIcon(appInfo.packageName, icon);

        String appName = packageManager.getApplicationLabel(appInfo).toString();
        appCache.result.addAppName(packageName, appName);

        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            appCache.result.addVersionName(packageName, packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException ignore) {
        }
    }

    private void cacheApps(CompletableObserver callerObserver) {
        AppDatabaseFactory.getInstalledApps()
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
