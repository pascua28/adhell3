package com.fusionjack.adhell3.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.TypeConverters;

import com.fusionjack.adhell3.db.DateConverter;
import com.fusionjack.adhell3.db.entity.ReportBlockedUrl;

import java.util.List;

@Dao
@TypeConverters(DateConverter.class)
public interface ReportBlockedUrlDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ReportBlockedUrl reportBlockedUrl);

    @Insert
    void insertAll(List<ReportBlockedUrl> reportBlockedUrls);

    @Query("SELECT * FROM ReportBlockedUrl WHERE blockDate > :startDate ORDER BY _id DESC")
    LiveData<List<ReportBlockedUrl>> getReportBlockUrlAfterAsLiveData(long startDate);

    @Query("SELECT * FROM ReportBlockedUrl WHERE blockDate > :startDate ORDER BY _id DESC")
    List<ReportBlockedUrl> getReportBlockUrlAfter(long startDate);

    @Query("DELETE FROM ReportBlockedUrl WHERE blockDate < :blockDate")
    void deleteBefore(long blockDate);

    @Query("SELECT * FROM ReportBlockedUrl ORDER BY blockDate DESC LIMIT 1")
    ReportBlockedUrl getLastBlockedDomain();

    @Query("DELETE FROM ReportBlockedUrl WHERE packageName = :packageName")
    void deleteByPackageName(String packageName);

}
