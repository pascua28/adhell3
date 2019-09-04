package com.fusionjack.adhell3.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.TypeConverters;

import com.fusionjack.adhell3.db.DateConverter;
import com.fusionjack.adhell3.db.entity.UserBlockUrl;

import java.util.List;

@Dao
@TypeConverters(DateConverter.class)
public interface UserBlockUrlDao {
    @Query("SELECT url FROM UserBlockUrl")
    LiveData<List<String>> getAll();

    @Query("SELECT * FROM UserBlockUrl")
    List<UserBlockUrl> getAll2();

    @Query("SELECT url FROM UserBlockUrl")
    List<String> getAll3();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserBlockUrl userBlockUrl);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<UserBlockUrl> userBlockUrls);

    @Query("DELETE FROM UserBlockUrl WHERE url = :url")
    void deleteByUrl(String url);

    @Query("DELETE FROM UserBlockUrl")
    void deleteAll();

    @Query("SELECT * FROM UserBlockUrl WHERE url LIKE :url")
    List<UserBlockUrl> getByUrl(String url);
}
