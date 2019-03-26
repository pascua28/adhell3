package com.fusionjack.adhell3.fragments;

public class FilterAppInfo {
    private boolean FILTER_MENU_SYSTEM_APPS;
    private boolean FILTER_MENU_USERS_APPS;
    private boolean FILTER_MENU_RUNNING_APPS;
    private boolean FILTER_MENU_STOPPED_APPS;

    FilterAppInfo() {
        this.FILTER_MENU_SYSTEM_APPS = true;
        this.FILTER_MENU_USERS_APPS = true;
        this.FILTER_MENU_RUNNING_APPS = true;
        this.FILTER_MENU_STOPPED_APPS = true;
    }

    public boolean getSystemAppsFilter() {
        return this.FILTER_MENU_SYSTEM_APPS;
    }

    void setSystemAppsFilter(boolean filterState) {
        this.FILTER_MENU_SYSTEM_APPS = filterState;
    }

    public boolean getUserAppsFilter() {
        return this.FILTER_MENU_USERS_APPS;
    }

    void setUserAppsFilter(boolean filterState) {
        this.FILTER_MENU_USERS_APPS = filterState;
    }

    public boolean getRunningAppsFilter() {
        return this.FILTER_MENU_RUNNING_APPS;
    }

    void setRunningAppsFilter(boolean filterState) {
        this.FILTER_MENU_RUNNING_APPS = filterState;
    }

    public boolean getStoppedAppsFilter() {
        return this.FILTER_MENU_STOPPED_APPS;
    }

    void setStoppedAppsFilter(boolean filterState) {
        this.FILTER_MENU_STOPPED_APPS = filterState;
    }
}
