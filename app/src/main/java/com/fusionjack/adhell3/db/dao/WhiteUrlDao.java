package com.fusionjack.adhell3.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.TypeConverters;

import com.fusionjack.adhell3.db.DateConverter;
import com.fusionjack.adhell3.db.entity.WhiteUrl;

import java.util.List;

@Dao
@TypeConverters(DateConverter.class)
public interface WhiteUrlDao {

    @Query("SELECT url FROM WhiteUrl")
    LiveData<List<String>> getAll();

    @Query("SELECT * FROM WhiteUrl")
    List<WhiteUrl> getAll2();

    @Query("SELECT url FROM WhiteUrl")
    List<String> getAll3();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(WhiteUrl whiteUrl);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<WhiteUrl> whiteUrls);

    @Query("DELETE FROM WhiteUrl WHERE url = :url")
    void deleteByUrl(String url);

    @Delete
    void delete(WhiteUrl whiteUrl);

    @Query("DELETE FROM WhiteUrl")
    void deleteAll();

}
