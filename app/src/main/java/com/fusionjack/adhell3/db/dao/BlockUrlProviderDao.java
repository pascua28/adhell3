package com.fusionjack.adhell3.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.fusionjack.adhell3.db.entity.BlockUrlProvider;

import java.util.List;

@Dao
public interface BlockUrlProviderDao {

    @Query("SELECT * FROM BlockUrlProviders ORDER BY deletable ASC")
    List<BlockUrlProvider> getAll2();

    @Query("SELECT * FROM BlockUrlProviders ORDER BY deletable ASC")
    LiveData<List<BlockUrlProvider>> getAll();

    @Query("SELECT * FROM BlockUrlProviders WHERE selected = :selected")
    List<BlockUrlProvider> getBlockUrlProviderBySelectedFlag(int selected);

    @Query("SELECT DISTINCT url FROM BlockUrl WHERE urlProviderId IN (SELECT _id FROM BlockUrlProviders WHERE selected = 1) ORDER BY url ASC")
    List<String> getUniqueBlockedUrls();

    @Query("SELECT COUNT(DISTINCT url) AS unique_count FROM BlockUrl WHERE urlProviderId IN (SELECT _id FROM BlockUrlProviders WHERE selected = 1)")
    int getUniqueBlockedUrlsCount();

    @Query("SELECT * FROM BlockUrlProviders WHERE url = :url")
    BlockUrlProvider getByUrl(String url);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long[] insertAll(BlockUrlProvider... urlProviders);

    @Update
    void updateBlockUrlProviders(BlockUrlProvider... blockUrlProviders);

    @Delete
    void delete(BlockUrlProvider blockUrlProvider);

    @Query("SELECT * FROM BlockUrlProviders WHERE deletable = 0")
    List<BlockUrlProvider> getDefault();

    @Query("DELETE FROM BlockUrlProviders WHERE deletable = 0")
    void deleteDefault();

    @Query("DELETE FROM BlockUrlProviders")
    void deleteAll();

}
