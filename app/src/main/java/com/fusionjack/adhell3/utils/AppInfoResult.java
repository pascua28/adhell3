package com.fusionjack.adhell3.utils;

import android.graphics.drawable.Drawable;

import java.util.HashMap;
import java.util.Map;

class AppInfoResult {

    private final Map<String, Drawable> appsIcons;
    private final Map<String, String> appsNames;
    private final Map<String, String> versionNames;

    AppInfoResult() {
        this.appsIcons = new HashMap<>();
        this.appsNames = new HashMap<>();
        this.versionNames = new HashMap<>();
    }

    void addAppIcon(String packageName, Drawable icon) {
        appsIcons.put(packageName, icon);
    }

    void addAppName(String packageName, String appName) {
        appsNames.put(packageName, appName);
    }

    void addVersionName(String packageName, String versionName) {
        versionNames.put(packageName, versionName);
    }

    void addAppIcons(Map<String, Drawable> appsIcons) {
        this.appsIcons.putAll(appsIcons);
    }

    void addAppNames(Map<String, String> appsNames) {
        this.appsNames.putAll(appsNames);
    }

    void addVersionNames(Map<String, String> versionNames) {
        this.versionNames.putAll(versionNames);
    }

    Map<String, Drawable> getAppsIcons() {
        return appsIcons;
    }

    Map<String, String> getAppsNames() {
        return appsNames;
    }

    Map<String, String> getVersionNames() {
        return versionNames;
    }
}
