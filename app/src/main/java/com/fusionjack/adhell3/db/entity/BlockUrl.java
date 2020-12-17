package com.fusionjack.adhell3.db.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;


@Entity(
        tableName = "BlockUrl",
        indices = {@Index("urlProviderId")},
        foreignKeys = @ForeignKey(
                entity = BlockUrlProvider.class,
                parentColumns = "_id",
                childColumns = "urlProviderId",
                onDelete = ForeignKey.CASCADE
        )
)
public class BlockUrl {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "_id")
    public long id;

    @ColumnInfo(name = "url")
    public String url;

    @ColumnInfo(name = "urlProviderId")
    public long urlProviderId;

    public BlockUrl(String url, long urlProviderId) {
        this.url = url;
        this.urlProviderId = urlProviderId;
    }
}
