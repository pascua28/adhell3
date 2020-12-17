package com.fusionjack.adhell3.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.fusionjack.adhell3.db.entity.DnsPackage;

import java.util.List;

@Dao
public interface DnsPackageDao {
    @Query("SELECT * FROM DnsPackage")
    List<DnsPackage> getAll();

    @Insert
    void insertAll(List<DnsPackage> dnsPackageList);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DnsPackage dnsPackage);

    @Query("DELETE FROM DnsPackage WHERE packageName = :packageName")
    void deleteByPackageName(String packageName);

    @Query("DELETE FROM DnsPackage")
    void deleteAll();
}
