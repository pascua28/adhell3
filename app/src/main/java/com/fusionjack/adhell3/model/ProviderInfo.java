package com.fusionjack.adhell3.model;

import java.util.Objects;

public class ProviderInfo implements IComponentInfo {

    private final String packageName;
    private final String name;

    ProviderInfo(String packageName, String name) {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProviderInfo that = (ProviderInfo) o;
        return Objects.equals(packageName, that.packageName) &&
                Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageName, name);
    }
}
