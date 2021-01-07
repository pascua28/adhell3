package com.fusionjack.adhell3.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.TypeConverters;

import com.fusionjack.adhell3.db.DateConverter;
import com.fusionjack.adhell3.db.entity.DisabledPackage;

import java.util.List;

@Dao
@TypeConverters(DateConverter.class)
public interface DisabledPackageDao {

    @Query("SELECT * FROM DisabledPackage")
    List<DisabledPackage> getAll();

    @Query("SELECT COUNT(*) FROM DisabledPackage")
    int getSize();

    @Insert
    void insertAll(List<DisabledPackage> disabledPackages);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DisabledPackage disabledPackage);

    @Query("DELETE FROM DisabledPackage WHERE packageName = :packageName")
    void deleteByPackageName(String packageName);

    @Query("DELETE FROM DisabledPackage")
    void deleteAll();
}
