package com.fusionjack.adhell3.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.fusionjack.adhell3.db.DateConverter;


@Entity(
        tableName = "AppInfo",
        indices = {@Index("appName"), @Index("installTime"), @Index("disabled"), @Index("mobileRestricted")}
)
@TypeConverters(DateConverter.class)
public class AppInfo {
    @PrimaryKey
    public long id;

    @ColumnInfo(name = "packageName")
    public String packageName;

    @ColumnInfo(name = "appName")
    public String appName;

    @ColumnInfo(name = "installTime")
    public long installTime;

    @ColumnInfo(name = "system")
    public boolean system;

    @ColumnInfo(name = "adhellWhitelisted")
    public boolean adhellWhitelisted;

    @ColumnInfo(name = "disabled")
    public boolean disabled;

    @ColumnInfo(name = "mobileRestricted")
    public boolean mobileRestricted;

    @ColumnInfo(name = "wifiRestricted")
    public boolean wifiRestricted;

    @ColumnInfo(name = "hasCustomDns")
    public boolean hasCustomDns;
}
