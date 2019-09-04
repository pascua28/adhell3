package com.fusionjack.adhell3.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.TypeConverters;

import com.fusionjack.adhell3.db.DateConverter;
import com.fusionjack.adhell3.db.entity.PolicyPackage;

@Dao
@TypeConverters(DateConverter.class)
public interface PolicyPackageDao {

    @Query("SELECT * FROM PolicyPackage WHERE id = :id")
    PolicyPackage getPolicyById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(PolicyPackage policyPackage);
}
