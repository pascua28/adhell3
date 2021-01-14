package com.fusionjack.adhell3.model;

import java.util.Objects;

public class FilterAppInfo {
    private boolean HIGHLIGHT_RUNNING_APPS;
    private boolean FILTER_MENU_SYSTEM_APPS;
    private boolean FILTER_MENU_USERS_APPS;
    private boolean FILTER_MENU_RUNNING_APPS;
    private boolean FILTER_MENU_STOPPED_APPS;

    public FilterAppInfo() {
        this.HIGHLIGHT_RUNNING_APPS = false;
        this.FILTER_MENU_SYSTEM_APPS = true;
        this.FILTER_MENU_USERS_APPS = true;
        this.FILTER_MENU_RUNNING_APPS = true;
        this.FILTER_MENU_STOPPED_APPS = true;
    }

    public boolean getHighlightRunningApps() {
        return this.HIGHLIGHT_RUNNING_APPS;
    }

    public void setHighlightRunningApps(boolean filterState) {
        this.HIGHLIGHT_RUNNING_APPS = filterState;
    }

    public boolean getSystemAppsFilter() {
        return this.FILTER_MENU_SYSTEM_APPS;
    }

    public void setSystemAppsFilter(boolean filterState) {
        this.FILTER_MENU_SYSTEM_APPS = filterState;
    }

    public boolean getUserAppsFilter() {
        return this.FILTER_MENU_USERS_APPS;
    }

    public void setUserAppsFilter(boolean filterState) {
        this.FILTER_MENU_USERS_APPS = filterState;
    }

    public boolean getRunningAppsFilter() {
        return this.FILTER_MENU_RUNNING_APPS;
    }

    public void setRunningAppsFilter(boolean filterState) {
        this.FILTER_MENU_RUNNING_APPS = filterState;
    }

    public boolean getStoppedAppsFilter() {
        return this.FILTER_MENU_STOPPED_APPS;
    }

    public void setStoppedAppsFilter(boolean filterState) {
        this.FILTER_MENU_STOPPED_APPS = filterState;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FilterAppInfo that = (FilterAppInfo) o;
        return HIGHLIGHT_RUNNING_APPS == that.HIGHLIGHT_RUNNING_APPS &&
                FILTER_MENU_SYSTEM_APPS == that.FILTER_MENU_SYSTEM_APPS &&
                FILTER_MENU_USERS_APPS == that.FILTER_MENU_USERS_APPS &&
                FILTER_MENU_RUNNING_APPS == that.FILTER_MENU_RUNNING_APPS &&
                FILTER_MENU_STOPPED_APPS == that.FILTER_MENU_STOPPED_APPS;
    }

    @Override
    public int hashCode() {
        return Objects.hash(HIGHLIGHT_RUNNING_APPS, FILTER_MENU_SYSTEM_APPS, FILTER_MENU_USERS_APPS, FILTER_MENU_RUNNING_APPS, FILTER_MENU_STOPPED_APPS);
    }
}
