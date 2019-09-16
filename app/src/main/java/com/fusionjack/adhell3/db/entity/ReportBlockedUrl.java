package com.fusionjack.adhell3.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.fusionjack.adhell3.db.DateConverter;

@Entity(tableName = "ReportBlockedUrl")
@TypeConverters(DateConverter.class)
public class ReportBlockedUrl {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public long id;
    public String url;
    public String packageName;
    public long blockDate;

    public ReportBlockedUrl() {
    }

    @Ignore
    public ReportBlockedUrl(String url, String packageName, long timestamp) {
        this.url = url;
        this.packageName = packageName;
        this.blockDate = timestamp;
    }

    @NonNull
    @Override
    public String toString() {
        return "ReportBlockedUrl{" +
                "id=" + id +
                ", url='" + url + '\'' +
                ", packageName='" + packageName + '\'' +
                ", blockDate=" + blockDate +
                '}';
    }
}
