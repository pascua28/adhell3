package com.fusionjack.adhell3.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.TypeConverters;

import com.fusionjack.adhell3.db.DateConverter;
import com.fusionjack.adhell3.db.entity.RestrictedPackage;

import java.util.List;

@Dao
@TypeConverters(DateConverter.class)
public interface RestrictedPackageDao {

    @Query("SELECT * FROM RestrictedPackage")
    LiveData<List<RestrictedPackage>> getAllAsLiveData();

    @Query("SELECT * FROM RestrictedPackage")
    List<RestrictedPackage> getAll();

    @Insert
    void insertAll(List<RestrictedPackage> restrictedPackages);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(RestrictedPackage restrictedPackage);

    @Query("DELETE FROM RestrictedPackage WHERE packageName = :packageName")
    void deleteByPackageName(String packageName);

    @Query("DELETE FROM RestrictedPackage WHERE packageName = :packageName AND type = :type")
    void deleteByPackageName(String packageName, String type);

    @Query("DELETE FROM RestrictedPackage WHERE type = :type")
    void deleteByType(String type);

    @Query("DELETE FROM RestrictedPackage")
    void deleteAll();
}
