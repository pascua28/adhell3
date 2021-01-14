package com.fusionjack.adhell3.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.fusionjack.adhell3.db.DateConverter;

import java.util.Objects;

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

    @Ignore
    public String getPackageName() {
        return packageName;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReportBlockedUrl that = (ReportBlockedUrl) o;
        return id == that.id &&
                blockDate == that.blockDate &&
                url.equals(that.url) &&
                packageName.equals(that.packageName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, url, packageName, blockDate);
    }
}
