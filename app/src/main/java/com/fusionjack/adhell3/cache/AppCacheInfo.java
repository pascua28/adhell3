package com.fusionjack.adhell3.cache;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import com.fusionjack.adhell3.utils.AdhellFactory;

public class AppCacheInfo {

    public static final AppCacheInfo EMPTY = new AppCacheInfo(null, "(unknown)", "0.0.0");

    private Drawable drawable;
    private String appName;
    private String appVersion;

    public AppCacheInfo() {
    }

    private AppCacheInfo(Drawable drawable, String appName, String appVersion) {
        this.drawable = drawable;
        this.appName = appName;
        this.appVersion = appVersion;
    }

    public Drawable getDrawable() {
        return drawable;
    }

    public void setDrawable(Drawable drawable) {
        this.drawable = drawable;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getAppVersion() {
        return appVersion;
    }

    public void setAppVersion(String appVersion) {
        this.appVersion = appVersion;
    }

    public static AppCacheInfo toAppCacheInfo(ApplicationInfo appInfo) {
        String packageName = appInfo.packageName;
        PackageManager packageManager = AdhellFactory.getInstance().getPackageManager();

        AppCacheInfo appCacheInfo = new AppCacheInfo();

        Drawable icon;
        try {
            icon = packageManager.getApplicationIcon(packageName);
        } catch (PackageManager.NameNotFoundException e) {
            icon = null;
        }
        appCacheInfo.setDrawable(icon);

        String appName = packageManager.getApplicationLabel(appInfo).toString();
        appCacheInfo.setAppName(appName == null ? "(unknown)" : appName);

        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            appCacheInfo.setAppVersion(packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException ignore) {
            appCacheInfo.setAppVersion("0.0.0");
        }
        return appCacheInfo;
    }

}
