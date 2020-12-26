package com.fusionjack.adhell3.utils;

import android.content.pm.ApplicationInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AppDiff {

    private final List<ApplicationInfo> newApps;
    private final List<String> deletedApps;

    public AppDiff() {
        this.newApps = new ArrayList<>();
        this.deletedApps = new ArrayList<>();
    }

    public boolean isEmpty() {
        return newApps.isEmpty() && deletedApps.isEmpty();
    }

    public void putNewApps(List<ApplicationInfo> newApps) {
        this.newApps.addAll(newApps);
    }

    public void putDeletedApps(List<String> deletedApps) {
        this.deletedApps.addAll(deletedApps);
    }

    public List<ApplicationInfo> getNewApps() {
        return Collections.unmodifiableList(newApps);
    }

    public List<String> getDeletedApps() {
        return Collections.unmodifiableList(deletedApps);
    }
}
