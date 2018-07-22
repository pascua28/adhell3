package com.fusionjack.adhell3.db.dao;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.OnConflictStrategy;
import android.arch.persistence.room.Query;

import com.fusionjack.adhell3.db.entity.BlockUrl;

import java.util.List;

@Dao
public interface BlockUrlDao {

    @Query("SELECT * FROM BlockUrl")
    List<BlockUrl> getAll();

    @Query("SELECT url FROM BlockUrl WHERE urlProviderId = :urlProviderId")
    List<String> getUrlsByProviderId(long urlProviderId);

    @Query("SELECT * FROM BlockUrl WHERE url LIKE '@dhell%' GROUP BY url")
    List<BlockUrl> getAdhellFilters();

    @Query("DELETE FROM BlockUrl")
    void deleteAll();

    @Query("DELETE FROM BlockUrl WHERE url = :url")
    void deleteBlockUrl(String url);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<BlockUrl> blockUrls);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(BlockUrl... blockUrls);

    @Query("SELECT * FROM BlockUrl WHERE urlProviderId = :urlProviderId AND url LIKE :url")
    List<BlockUrl> getByUrl(long urlProviderId, String url);
}
