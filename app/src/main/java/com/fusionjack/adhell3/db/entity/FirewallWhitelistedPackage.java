package com.fusionjack.adhell3.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "FirewallWhitelistedPackage",
        indices = {@Index("policyPackageId")},
        foreignKeys = @ForeignKey(entity = PolicyPackage.class,
                parentColumns = "id",
                childColumns = "policyPackageId")
)
public class FirewallWhitelistedPackage {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @ColumnInfo(name = "packageName")
    public String packageName;

    @ColumnInfo(name = "policyPackageId")
    public String policyPackageId;

    public FirewallWhitelistedPackage() {
    }

    @Ignore
    public FirewallWhitelistedPackage(String packageName, String policyPackageId) {
        this.packageName = packageName;
        this.policyPackageId = policyPackageId;
    }

}
