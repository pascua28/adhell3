package com.fusionjack.adhell3.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;


@Entity(
        tableName = "AppPermission",
        indices = {@Index("policyPackageId")},
        foreignKeys = @ForeignKey(entity = PolicyPackage.class,
                parentColumns = "id",
                childColumns = "policyPackageId")
)
public class AppPermission {

    @Ignore
    public static final int UNKNOWN_TYPE = -2;

    @Ignore
    public static final int STATUS_PERMISSION = -1;

    @Ignore
    public static final int STATUS_ACTIVITY = 1;

    @Ignore
    public static final int STATUS_SERVICE = 2;

    @Ignore
    public static final int STATUS_RECEIVER = 5;

    @Ignore
    public static final int STATUS_PROVIDER = 3;

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @ColumnInfo(name = "packageName")
    public String packageName;

    @ColumnInfo(name = "permissionName")
    public String permissionName;

    @ColumnInfo(name = "permissionStatus")
    public int permissionStatus;

    @ColumnInfo(name = "policyPackageId")
    public String policyPackageId;

    // TwentyNine78's component types compatibility
    @Ignore private static final int ACTIVITY_TYPE = 8;
    @Ignore private static final int PROVIDER_TYPE = 11;

    @Ignore
    public static int convertType(int type) {
        if (type == STATUS_PERMISSION || type == STATUS_ACTIVITY || type == STATUS_SERVICE || type == STATUS_RECEIVER || type == STATUS_PROVIDER) return type;
        if (type == ACTIVITY_TYPE) return STATUS_ACTIVITY;
        if (type == PROVIDER_TYPE) return STATUS_PROVIDER;
        return UNKNOWN_TYPE;
    }
}
