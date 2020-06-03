package com.fusionjack.adhell3.model;

public class ActivityInfo implements IComponentInfo {

    private final String packageName;
    private final String name;

    ActivityInfo(String packageName, String name) {
        this.packageName = packageName;
        this.name = name;
    }

    @Override
    public String getPackageName() {
        return packageName;
    }

    public String getName() {
        return name;
    }
}
