package com.fusionjack.adhell3.model;

public class ReceiverInfo implements IComponentInfo {
    private final String packageName;
    private final String name;
    private final String permission;

    ReceiverInfo(String packageName, String name, String permission) {
        this.packageName = packageName;
        this.name = name;
        this.permission = permission;
    }

    @Override
    public String getPackageName() {
        return packageName;
    }

    public String getName() {
        return name;
    }

    public String getPermission() {
        return permission == null ? "No permission" : permission;
    }
}
