package com.fusionjack.adhell3.utils;

import android.content.SharedPreferences;

public final class AppPreferences {
    private static final String BLOCKED_DOMAINS_COUNT = "blockedDomainsCount";
    private static final String DISABLER_TOGGLE = "disablerToggle";
    private static final String APP_COMPONENT_TOGGLE = "appComponentToggle";
    private static final String DNS_ALL_APPS_ENABLED = "dnsAllAppsEnabled";
    private static final String DNS1 = "dns1";
    private static final String DNS2 = "dns2";
    private static final String PASSWORD = "password";
    private static final String WARNING_DIALOG_APP_COMPONENT = "warningDialogAppComponent";
    private static final String AUTO_UPDATE_INTERVAL = "autoUpdateInterval";
    private static final String AUTO_UPDATE_LOG = "autoUpdateLog";
    private static final String AUTO_UPDATE_CONSTRAINT_LOW_BATTERY = "autoUpdateConstraintLowBattery";
    private static final String AUTO_UPDATE_CONSTRAINT_MOBILE_DATA = "autoUpdateConstraintMobileData";
    private static final String STORAGE_TREE_PATH = "storageTreePath";
    private static AppPreferences instance;
    private final SharedPreferences sharedPreferences;

    private AppPreferences() {
        sharedPreferences = AdhellFactory.getInstance().getSharedPreferences();
    }

    public static AppPreferences getInstance() {
        if (instance == null) {
            instance = new AppPreferences();
        }
        return instance;
    }

    public boolean isAppDisablerToggleEnabled() {
        return sharedPreferences.getBoolean(DISABLER_TOGGLE, true);
    }

    void setAppDisablerToggle(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(DISABLER_TOGGLE, enabled);
        editor.apply();
    }

    public boolean isAppComponentToggleEnabled() {
        return sharedPreferences.getBoolean(APP_COMPONENT_TOGGLE, true);
    }

    void setAppComponentToggle(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(APP_COMPONENT_TOGGLE, enabled);
        editor.apply();
    }

    public boolean isDnsAllAppsEnabled() {
        return sharedPreferences.getBoolean(DNS_ALL_APPS_ENABLED, false);
    }

    public void setDnsAllApps(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(DNS_ALL_APPS_ENABLED, enabled);
        editor.apply();
    }

    public String getDns1() {
        return sharedPreferences.getString(DNS1, "0.0.0.0");
    }

    public String getDns2() {
        return sharedPreferences.getString(DNS2, "0.0.0.0");
    }

    public boolean isDnsNotEmpty() {
        return sharedPreferences.contains(DNS1) && sharedPreferences.contains(DNS2);
    }

    void setDns(String dns1, String dns2) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(DNS1, dns1);
        editor.putString(DNS2, dns2);
        editor.apply();
    }

    void removeDns() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(DNS1);
        editor.remove(DNS2);
        editor.apply();
    }

    public int getBlockedDomainsCount() {
        return sharedPreferences.getInt(BLOCKED_DOMAINS_COUNT, 0);
    }

    public void setBlockedDomainsCount(int count) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(BLOCKED_DOMAINS_COUNT, count);
        editor.apply();
    }

    public void resetBlockedDomainsCount() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(BLOCKED_DOMAINS_COUNT);
        editor.apply();
    }

    public void resetPassword() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PASSWORD, "");
        editor.apply();
    }

    public void setPassword(String password) throws PasswordStorage.CannotPerformOperationException {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PASSWORD, PasswordStorage.createHash(password));
        editor.apply();
    }

    public String getPasswordHash() {
        return sharedPreferences.getString(PASSWORD, "");
    }

    public boolean getWarningDialogAppComponentDontShow() {
        return sharedPreferences.getBoolean(WARNING_DIALOG_APP_COMPONENT, false);
    }

    public void setWarningDialogAppComponentDontShow(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(WARNING_DIALOG_APP_COMPONENT, enabled);
        editor.apply();
    }

    public int getAutoUpdateInterval() {
        return sharedPreferences.getInt(AUTO_UPDATE_INTERVAL, 5);
    }

    public void setAutoUpdateInterval(int interval) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(AUTO_UPDATE_INTERVAL, interval);
        editor.apply();
    }

    public boolean getCreateLogOnAutoUpdate() {
        return sharedPreferences.getBoolean(AUTO_UPDATE_LOG, true);
    }

    public void setCreateLogOnAutoUpdate(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(AUTO_UPDATE_LOG, enabled);
        editor.apply();
    }

    public boolean getAutoUpdateConstraintLowBattery() {
        return sharedPreferences.getBoolean(AUTO_UPDATE_CONSTRAINT_LOW_BATTERY, true);
    }

    public void setAutoUpdateConstraintLowBattery(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(AUTO_UPDATE_CONSTRAINT_LOW_BATTERY, enabled);
        editor.apply();
    }

    public boolean getAutoUpdateConstraintMobileData() {
        return sharedPreferences.getBoolean(AUTO_UPDATE_CONSTRAINT_MOBILE_DATA, true);
    }

    public void setAutoUpdateConstraintMobileData(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(AUTO_UPDATE_CONSTRAINT_MOBILE_DATA, enabled);
        editor.apply();
    }

    public void setStorageTreePath(String treePath) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(STORAGE_TREE_PATH, treePath);
        editor.apply();
    }

    public String getStorageTreePath() {
        return sharedPreferences.getString(STORAGE_TREE_PATH, "");
    }
}
