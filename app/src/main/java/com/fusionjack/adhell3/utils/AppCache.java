package com.fusionjack.adhell3.utils;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import com.fusionjack.adhell3.utils.rx.RxSingleComputationBuilder;

import java.util.Map;
import java.util.function.Consumer;

public class AppCache {
    private final AppInfoResult result;

    private static AppCache instance;
    private static boolean appCached;

    private AppCache() {
        this.result = new AppInfoResult();
    }

    public static synchronized AppCache getInstance() {
        if (instance == null) {
            instance = new AppCache();
        }
        return instance;
    }

    public synchronized void inject(ApplicationInfo appInfo) {
        String packageName = appInfo.packageName;
        PackageManager packageManager = AdhellFactory.getInstance().getPackageManager();

        Drawable icon;
        try {
            icon = packageManager.getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            icon = null;
        }
        result.addAppIcon(appInfo.packageName, icon);

        String appName = packageManager.getApplicationLabel(appInfo).toString();
        result.addAppName(packageName, appName);

        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            result.addVersionName(packageName, packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException ignore) {
        }
    }

    public synchronized void cacheApps(Context context, Runnable callerCallback) {
        if (appCached) {
            callerCallback.run();
        } else {
            LogUtils.info("Caching apps ...");
            Consumer<AppInfoResult> callback = appInfoResult -> {
                result.addAppIcons(appInfoResult.getAppsIcons());
                result.addAppNames(appInfoResult.getAppsNames());
                result.addVersionNames(appInfoResult.getVersionNames());
                callerCallback.run();
            };
            new RxSingleComputationBuilder()
                    .setShowDialog("Caching apps ...", context)
                    .async(AppDatabaseFactory.getInstalledApps(), callback);
            appCached = true;
        }
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
