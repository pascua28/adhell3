package com.fusionjack.adhell3.db.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.fusionjack.adhell3.db.entity.FirewallWhitelistedPackage;

import java.util.List;

@Dao
public interface FirewallWhitelistedPackageDao {

    @Query("SELECT * FROM FirewallWhitelistedPackage")
    List<FirewallWhitelistedPackage> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<FirewallWhitelistedPackage> firewallWhitelistedPackages);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FirewallWhitelistedPackage whitelistedPackage);

    @Query("DELETE FROM FirewallWhitelistedPackage")
    void deleteAll();

    @Query("DELETE FROM FirewallWhitelistedPackage WHERE packageName = :packageName")
    void deleteByPackageName(String packageName);
}
