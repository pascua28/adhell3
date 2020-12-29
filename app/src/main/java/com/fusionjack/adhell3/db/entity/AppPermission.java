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
    public static final int STATUS_PERMISSION = -1;

    @Ignore
    public static final int STATUS_SERVICE = 2;

    @Ignore
    public static final int STATUS_RECEIVER = 5;

    @Ignore
    public static final int STATUS_ACTIVITY = 8;

    @Ignore
    public static final int STATUS_PROVIDER = 11;

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
}
