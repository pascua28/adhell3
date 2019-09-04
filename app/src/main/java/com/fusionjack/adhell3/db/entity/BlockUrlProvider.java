package com.fusionjack.adhell3.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.fusionjack.adhell3.db.DateConverter;

import java.util.Date;

@Entity(tableName = "BlockUrlProviders",
        indices = {@Index("policyPackageId")},
        foreignKeys = @ForeignKey(entity = PolicyPackage.class,
                parentColumns = "id",
                childColumns = "policyPackageId"))
@TypeConverters(DateConverter.class)
public class BlockUrlProvider {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public long id;

    @ColumnInfo(name = "url")
    public String url;

    @ColumnInfo(name = "count")
    public int count;

    @ColumnInfo(name = "lastUpdated")
    public Date lastUpdated;

    @ColumnInfo(name = "deletable")
    public boolean deletable;

    @ColumnInfo(name = "selected")
    public boolean selected;

    @ColumnInfo(name = "policyPackageId")
    public String policyPackageId;
}
