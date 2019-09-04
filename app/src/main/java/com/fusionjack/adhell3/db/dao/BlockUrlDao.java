package com.fusionjack.adhell3.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.fusionjack.adhell3.db.entity.BlockUrl;

import java.util.List;

@Dao
public interface BlockUrlDao {

    @Query("SELECT url FROM BlockUrl WHERE urlProviderId = :urlProviderId")
    List<String> getUrlsByProviderId(long urlProviderId);

    @Query("DELETE FROM BlockUrl WHERE urlProviderId IN (Select _id from BlockUrlProviders WHERE selected = 1)")
    void deleteBlockUrlsBySelectedProvider();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<BlockUrl> blockUrls);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BlockUrl... blockUrls);

    @Query("SELECT * FROM BlockUrl WHERE urlProviderId = :urlProviderId AND url LIKE :url")
    List<BlockUrl> getByUrl(long urlProviderId, String url);
}