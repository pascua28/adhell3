package com.fusionjack.adhell3.fragments;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import javax.annotation.Nullable;

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

    void setHighlightRunningApps(boolean filterState) {
        this.HIGHLIGHT_RUNNING_APPS = filterState;
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

    @Override
    public boolean equals(@Nullable Object o) {
        if (!(o instanceof FilterAppInfo)) {
            return false;
        }
        FilterAppInfo other = (FilterAppInfo) o;
        return Objects.equals(HIGHLIGHT_RUNNING_APPS, other.HIGHLIGHT_RUNNING_APPS)
                && Objects.equals(FILTER_MENU_SYSTEM_APPS, other.FILTER_MENU_SYSTEM_APPS)
                && Objects.equals(FILTER_MENU_USERS_APPS, other.FILTER_MENU_USERS_APPS)
                && Objects.equals(FILTER_MENU_RUNNING_APPS, other.FILTER_MENU_RUNNING_APPS)
                && Objects.equals(FILTER_MENU_STOPPED_APPS, other.FILTER_MENU_STOPPED_APPS);
    }

    @Override
    public int hashCode() {
        int result = 31;
        List<Boolean> booleanList = Arrays.asList(
                this.HIGHLIGHT_RUNNING_APPS,
                this.FILTER_MENU_SYSTEM_APPS,
                this.FILTER_MENU_USERS_APPS,
                this.FILTER_MENU_RUNNING_APPS,
                this.FILTER_MENU_STOPPED_APPS
        );
        for(boolean val : booleanList) {
            result = 31 * result + Boolean.hashCode(val);
        }
        return result;
    }
}
