package com.fusionjack.adhell3.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import com.fusionjack.adhell3.db.DateConverter;

import java.util.Date;

@Entity(
        tableName = "WhiteUrl"
)
@TypeConverters(DateConverter.class)
public class WhiteUrl {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public long id;

    @ColumnInfo(name = "url")
    public final String url;

    public final Date insertedAt;

    public WhiteUrl(String url, Date insertedAt) {
        this.url = url;
        this.insertedAt = insertedAt;
    }
}
