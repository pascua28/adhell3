package com.fusionjack.adhell3.model;

import com.fusionjack.adhell3.utils.AppPermissionUtils;

import java.util.Objects;

public class PermissionInfo implements IComponentInfo {
    public final static PermissionInfo EMPTY_PERMISSION = new PermissionInfo("Empty", null, 0, "Empty");

    private final String name;
    private final String label;
    private final int level;
    private final String packageName;

    PermissionInfo(String name, CharSequence label, int level, String packageName) {
        this.name = name;
        this.label = label == null ? "No description" : label.toString();
        this.level = AppPermissionUtils.fixProtectionLevel(level);
        this.packageName = packageName;
    }

    @Override
    public String getPackageName() {
        return packageName;
    }

    @Override
    public String getName() {
        return name;
    }

    public String getLabel() {
        return label;
    }

    public int getLevel() {
        return level;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PermissionInfo that = (PermissionInfo) o;
        return level == that.level &&
                Objects.equals(name, that.name) &&
                Objects.equals(label, that.label) &&
                Objects.equals(packageName, that.packageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, label, level, packageName);
    }
}
