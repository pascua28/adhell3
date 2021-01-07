package com.fusionjack.adhell3.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.fusionjack.adhell3.db.entity.AppInfo;

import java.util.List;

@Dao
public interface AppInfoDao {
    // Insert
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AppInfo> apps);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AppInfo info);

    // Delete
    @Query("DELETE FROM AppInfo")
    void deleteAll();

    @Query("DELETE FROM AppInfo WHERE packageName = :packageName")
    void deleteByPackageName(String packageName);


    // Update
    @Update
    void update(AppInfo appInfo);


    // Get all package names
    @Query("SELECT packageName FROM AppInfo")
    List<String> getAllPackageNames();

    // Get app based on a package name
    @Query("SELECT * FROM AppInfo WHERE packageName = :packageName")
    AppInfo getAppByPackageName(String packageName);


    // Get app size
    @Query("SELECT COUNT(*) FROM AppInfo")
    int getAppSize();


    // Get last app id
    @Query("SELECT MAX(id) FROM AppInfo")
    long getLastAppId();


    // Disabled apps
    @Query("SELECT * FROM AppInfo WHERE disabled = 1 ORDER BY appName ASC")
    List<AppInfo> getDisabledApps();

    @Query("SELECT * FROM AppInfo ORDER BY disabled DESC, appName ASC")
    LiveData<List<AppInfo>> getAppsInDisabledOrder();


    // Mobile restricted apps (only enabled apps)
    @Query("SELECT * FROM AppInfo WHERE mobileRestricted = 1 AND disabled = 0")
    List<AppInfo> getMobileRestrictedApps();

    @Query("SELECT * FROM AppInfo WHERE disabled = 0 ORDER BY mobileRestricted DESC, appName ASC")
    LiveData<List<AppInfo>> getAppsInMobileRestrictedOrder();


    // Wifi restricted apps (only enabled apps)
    @Query("SELECT * FROM AppInfo WHERE wifiRestricted = 1 AND disabled = 0")
    List<AppInfo> getWifiRestrictedApps();

    @Query("SELECT * FROM AppInfo WHERE disabled = 0 ORDER BY wifiRestricted DESC, appName ASC")
    LiveData<List<AppInfo>> getAppsInWifiRestrictedOrder();


    // Whitelisted apps (only enabled apps)
    @Query("SELECT * FROM AppInfo WHERE adhellWhitelisted = 1 ORDER BY appName ASC")
    List<AppInfo> getWhitelistedApps();

    @Query("SELECT * FROM AppInfo WHERE disabled = 0 ORDER BY adhellWhitelisted DESC, appName ASC")
    LiveData<List<AppInfo>> getAppsInWhitelistedOrder();


    // User apps
    @Query("SELECT * FROM AppInfo WHERE system = 0 AND disabled = 0 ORDER BY appName ASC")
    LiveData<List<AppInfo>> getUserApps();

    @Query("SELECT * FROM AppInfo WHERE system = 0 ORDER BY appName ASC")
    List<AppInfo> getUserAndDisabledApps();


    // System apps
    @Query("SELECT * FROM AppInfo WHERE system = 1 AND disabled = 0 ORDER BY appName ASC")
    LiveData<List<AppInfo>> getSystemApps();


    // Enabled apps
    @Query("SELECT * FROM AppInfo WHERE disabled = 0 ORDER BY appName ASC")
    LiveData<List<AppInfo>> getEnabledApps();


    // DNS apps
    @Query("SELECT * FROM AppInfo WHERE hasCustomDns = 1 ORDER BY appName ASC")
    List<AppInfo> getDnsApps();

    @Query("SELECT * FROM AppInfo WHERE disabled = 0 ORDER BY hasCustomDns DESC, appName ASC")
    LiveData<List<AppInfo>> getAppsInDnsOrder();


    // All user apps
    @Query("SELECT * FROM AppInfo WHERE system = 0 ORDER BY disabled DESC, appName ASC")
    LiveData<List<AppInfo>> getAllUserAppsInDisabledOrder();

    @Query("SELECT * FROM AppInfo WHERE system = 0 ORDER BY mobileRestricted DESC, appName ASC")
    LiveData<List<AppInfo>> getAllUserAppsInMobileRestrictedOrder();

    @Query("SELECT * FROM AppInfo WHERE system = 0 ORDER BY wifiRestricted DESC, appName ASC")
    LiveData<List<AppInfo>> getAllUserAppsInWifiRestrictedOrder();

    @Query("SELECT * FROM AppInfo WHERE system = 0 ORDER BY adhellWhitelisted DESC, appName ASC")
    LiveData<List<AppInfo>> getAllUserAppsInWhitelistedOrder();

    @Query("SELECT * FROM AppInfo WHERE system = 0 ORDER BY hasCustomDns DESC, appName ASC")
    LiveData<List<AppInfo>> getAllUserAppsInDnsOrder();


    // All system apps
    @Query("SELECT * FROM AppInfo WHERE system = 1 ORDER BY disabled DESC, appName ASC")
    LiveData<List<AppInfo>> getAllSystemAppsInDisabledOrder();
    // All system apps
    @Query("SELECT * FROM AppInfo WHERE system = 1 ORDER BY mobileRestricted DESC, appName ASC")
    LiveData<List<AppInfo>> getAllSystemAppsInMobileRestrictedOrder();
    // All system apps
    @Query("SELECT * FROM AppInfo WHERE system = 1 ORDER BY wifiRestricted DESC, appName ASC")
    LiveData<List<AppInfo>> getAllSystemAppsInWifiRestrictedOrder();
    // All system apps
    @Query("SELECT * FROM AppInfo WHERE system = 1 ORDER BY adhellWhitelisted DESC, appName ASC")
    LiveData<List<AppInfo>> getAllSystemAppsInWhitelistedOrder();
    // All system apps
    @Query("SELECT * FROM AppInfo WHERE system = 1 ORDER BY hasCustomDns DESC, appName ASC")
    LiveData<List<AppInfo>> getAllSystemAppsInDnsOrder();
}