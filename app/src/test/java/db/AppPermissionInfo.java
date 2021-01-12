package db;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.Objects;

class AppPermissionInfo {
    private String packageName;
    private String permissionName;
    private int permissionStatus;
    private String policyPackageId;

    AppPermissionInfo(String packageName, String permissionName, int permissionStatus) {
        this.packageName = packageName;
        this.permissionName = permissionName;
        this.permissionStatus = permissionStatus;
    }

    AppPermissionInfo() {
    }

    void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }

    void setPermissionStatus(int permissionStatus) {
        this.permissionStatus = permissionStatus;
    }

    void setPolicyPackageId(String policyPackageId) {
        this.policyPackageId = policyPackageId;
    }

    String getPackageName() {
        return packageName;
    }

    String getPermissionName() {
        return permissionName;
    }

    int getPermissionStatus() {
        return permissionStatus;
    }

    String getPolicyPackageId() {
        return policyPackageId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(packageName, permissionName, permissionStatus);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof AppPermissionInfo)) {
            return false;
        }
        AppPermissionInfo other = (AppPermissionInfo) o;
        return Objects.equals(packageName, other.packageName)
                && Objects.equals(permissionName, other.permissionName)
                && permissionStatus == other.permissionStatus;
    }

    @NonNull
    @Override
    public String toString() {
        return packageName + "|" + permissionName + "|" + permissionStatus;
    }
}
