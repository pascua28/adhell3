package com.fusionjack.adhell3.model;

import java.util.Objects;

public class ReceiverInfo implements IComponentInfo {
    private static final String NO_PERMISSION = "No permission";

    private final String packageName;
    private final String name;
    private final String permission;

    ReceiverInfo(String packageName, String name, String permission) {
        this.packageName = packageName;
        this.name = name;
        this.permission = permission == null ? NO_PERMISSION : permission;
    }

    @Override
    public String getPackageName() {
        return packageName;
    }

    public String getName() {
        return name;
    }

    public String getPermission() {
        return permission;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReceiverInfo that = (ReceiverInfo) o;
        return Objects.equals(packageName, that.packageName) &&
                Objects.equals(name, that.name) &&
                Objects.equals(permission, that.permission);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageName, name, permission);
    }
}
