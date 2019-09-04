package com.fusionjack.adhell3.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import androidx.annotation.NonNull;

import com.fusionjack.adhell3.db.DateConverter;

import java.util.Date;

@Entity(
        tableName = "PolicyPackage"
)
@TypeConverters(DateConverter.class)
public class PolicyPackage {

    @NonNull
    @PrimaryKey
    @ColumnInfo(name = "id")
    public String id = "";

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "description")
    public String description;

    @ColumnInfo(name = "active")
    public boolean active;

    @ColumnInfo(name = "createdAt")
    public Date createdAt;

    @ColumnInfo(name = "updatedAt")
    public Date updatedAt;

    @ColumnInfo(name = "numberOfDisabledPackages")
    public int numberOfDisabledPackages;

    @ColumnInfo(name = "numberOfHosts")
    public int numberOfHosts;

    @ColumnInfo(name = "numberOfUserBlockedDomains")
    public int numberOfUserBlockedDomains;

    @ColumnInfo(name = "numberOfUserWhitelistedDomains")
    public int numberOfUserWhitelistedDomains;

    @ColumnInfo(name = "numberOfChangedPermissions")
    public int numberOfChangedPermissions;

}
