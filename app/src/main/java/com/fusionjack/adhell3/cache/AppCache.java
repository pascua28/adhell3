package com.fusionjack.adhell3.cache;

import android.content.Context;
import android.content.pm.ApplicationInfo;

import com.fusionjack.adhell3.utils.AppDatabaseFactory;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.utils.rx.RxSingleComputationBuilder;

import java.util.function.Consumer;

public class AppCache {
    private final AppCacheResult result;

    private static AppCache instance;
    private static boolean appCached;

    private AppCache() {
        this.result = new AppCacheResult();
    }

    public static synchronized AppCache getInstance() {
        if (instance == null) {
            instance = new AppCache();
        }
        return instance;
    }

    public synchronized void inject(ApplicationInfo appInfo) {
        String packageName = appInfo.packageName;
        result.addAppCacheInfo(packageName, AppCacheInfo.toAppCacheInfo(appInfo));
    }

    public synchronized void cacheApps(Context context, Runnable callerCallback) {
        if (appCached) {
            callerCallback.run();
        } else {
            LogUtils.info("Caching apps ...");
            Consumer<AppCacheResult> callback = appCacheResult -> {
                result.merge(appCacheResult);
                callerCallback.run();
            };
            new RxSingleComputationBuilder()
                    .setShowDialog("Caching apps ...", context)
                    .async(AppDatabaseFactory.getInstalledApps(), callback);
            appCached = true;
        }
    }

    public AppCacheInfo getAppCacheInfo(String packageName) {
        return result.getAppCacheInfo(packageName);
    }

}
