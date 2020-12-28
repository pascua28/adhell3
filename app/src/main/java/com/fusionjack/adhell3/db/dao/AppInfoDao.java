package com.fusionjack.adhell3.db.dao;

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


    // Get app size
    @Query("SELECT COUNT(*) FROM AppInfo")
    int getAppSize();


    // Get last app id
    @Query("SELECT MAX(id) FROM AppInfo")
    long getLastAppId();

    // Get based on appName/packageName
    @Query("SELECT * FROM AppInfo ORDER BY appName ASC")
    List<AppInfo> getAppsAlphabetically();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) ORDER BY appName ASC")
    List<AppInfo> getAppsAlphabetically(String str);

    @Query("SELECT * FROM AppInfo WHERE packageName = :packageName")
    AppInfo getAppByPackageName(String packageName);


    // Get apps
    @Query("SELECT * FROM AppInfo ORDER BY appName ASC")
    List<AppInfo> getApps();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) ORDER BY appName ASC")
    List<AppInfo> getApps(String str);


    // Disabled apps
    @Query("SELECT * FROM AppInfo WHERE disabled = 1 ORDER BY appName ASC")
    List<AppInfo> getDisabledApps();

    @Query("SELECT * FROM AppInfo ORDER BY disabled DESC, appName ASC")
    List<AppInfo> getAppsInDisabledOrder();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) ORDER BY disabled DESC, appName ASC")
    List<AppInfo> getAppsInDisabledOrder(String str);


    // Mobile restricted apps (only enabled apps)
    @Query("SELECT * FROM AppInfo WHERE mobileRestricted = 1 AND disabled = 0")
    List<AppInfo> getMobileRestrictedApps();

    @Query("SELECT * FROM AppInfo WHERE disabled = 0 ORDER BY mobileRestricted DESC, appName ASC")
    List<AppInfo> getAppsInMobileRestrictedOrder();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) AND disabled = 0 ORDER BY mobileRestricted DESC, appName ASC")
    List<AppInfo> getAppsInMobileRestrictedOrder(String str);


    // Wifi restricted apps (only enabled apps)
    @Query("SELECT * FROM AppInfo WHERE wifiRestricted = 1 AND disabled = 0")
    List<AppInfo> getWifiRestrictedApps();

    @Query("SELECT * FROM AppInfo WHERE disabled = 0 ORDER BY wifiRestricted DESC, appName ASC")
    List<AppInfo> getAppsInWifiRestrictedOrder();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) AND disabled = 0 ORDER BY wifiRestricted DESC, appName ASC")
    List<AppInfo> getAppsInWifiRestrictedOrder(String str);


    // Whitelisted apps (only enabled apps)
    @Query("SELECT * FROM AppInfo WHERE adhellWhitelisted = 1 ORDER BY appName ASC")
    List<AppInfo> getWhitelistedApps();

    @Query("SELECT * FROM AppInfo WHERE disabled = 0 ORDER BY adhellWhitelisted DESC, appName ASC")
    List<AppInfo> getAppsInWhitelistedOrder();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) AND disabled = 0 ORDER BY adhellWhitelisted DESC, appName ASC")
    List<AppInfo> getAppsInWhitelistedOrder(String str);


    // User apps
    @Query("SELECT * FROM AppInfo WHERE system = 0 AND disabled = 0 ORDER BY appName ASC")
    List<AppInfo> getUserApps();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) AND system = 0 AND disabled = 0 ORDER BY appName ASC")
    List<AppInfo> getUserApps(String str);

    @Query("SELECT * FROM AppInfo WHERE system = 0 ORDER BY appName ASC")
    List<AppInfo> getUserAndDisabledApps();


    // System apps
    @Query("SELECT * FROM AppInfo WHERE system = 1 AND disabled = 0 ORDER BY appName ASC")
    List<AppInfo> getSystemApps();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) AND system = 1 AND disabled = 0 ORDER BY appName ASC")
    List<AppInfo> getSystemApps(String str);


    // Enabled apps
    @Query("SELECT * FROM AppInfo WHERE disabled = 0 ORDER BY appName ASC")
    List<AppInfo> getEnabledAppsAlphabetically();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) AND disabled = 0 ORDER BY appName ASC")
    List<AppInfo> getEnabledAppsAlphabetically(String str);


    // DNS apps
    @Query("SELECT * FROM AppInfo WHERE hasCustomDns = 1 ORDER BY appName ASC")
    List<AppInfo> getDnsApps();

    @Query("SELECT * FROM AppInfo WHERE disabled = 0 ORDER BY hasCustomDns DESC, appName ASC")
    List<AppInfo> getAppsInDnsOrder();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) AND disabled = 0 ORDER BY hasCustomDns DESC, appName ASC")
    List<AppInfo> getAppsInDnsOrder(String str);

    @Query("SELECT * FROM AppInfo WHERE adhellWhitelisted = 1 OR disabled = 1 OR mobileRestricted = 1 OR wifiRestricted = 1 OR hasCustomDns = 1")
    List<AppInfo> getModifiedApps();


    // All user apps
    @Query("SELECT * FROM AppInfo WHERE system = 0 ORDER BY disabled DESC, appName ASC")
    List<AppInfo> getAllUserAppsInDisabledOrder();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) AND system = 0 ORDER BY disabled DESC, appName ASC")
    List<AppInfo> getAllUserAppsInDisabledOrder(String str);

    @Query("SELECT * FROM AppInfo WHERE system = 0 ORDER BY mobileRestricted DESC, appName ASC")
    List<AppInfo> getAllUserAppsInMobileRestrictedOrder();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) AND system = 0 ORDER BY mobileRestricted DESC, appName ASC")
    List<AppInfo> getAllUserAppsInMobileRestrictedOrder(String str);

    @Query("SELECT * FROM AppInfo WHERE system = 0 ORDER BY wifiRestricted DESC, appName ASC")
    List<AppInfo> getAllUserAppsInWifiRestrictedOrder();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) AND system = 0 ORDER BY wifiRestricted DESC, appName ASC")
    List<AppInfo> getAllUserAppsInWifiRestrictedOrder(String str);

    @Query("SELECT * FROM AppInfo WHERE system = 0 ORDER BY adhellWhitelisted DESC, appName ASC")
    List<AppInfo> getAllUserAppsInWhitelistedOrder();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) AND system = 0 ORDER BY adhellWhitelisted DESC, appName ASC")
    List<AppInfo> getAllUserAppsInWhitelistedOrder(String str);

    @Query("SELECT * FROM AppInfo WHERE system = 0 ORDER BY hasCustomDns DESC, appName ASC")
    List<AppInfo> getAllUserAppsInDnsOrder();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) AND system = 0 ORDER BY hasCustomDns DESC, appName ASC")
    List<AppInfo> getAllUserAppsInDnsOrder(String str);


    // All system apps
    @Query("SELECT * FROM AppInfo WHERE system = 1 ORDER BY disabled DESC, appName ASC")
    List<AppInfo> getAllSystemAppsInDisabledOrder();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) AND system = 1 ORDER BY disabled DESC, appName ASC")
    List<AppInfo> getAllSystemAppsInDisabledOrder(String str);
    // All system apps
    @Query("SELECT * FROM AppInfo WHERE system = 1 ORDER BY mobileRestricted DESC, appName ASC")
    List<AppInfo> getAllSystemAppsInMobileRestrictedOrder();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) AND system = 1 ORDER BY mobileRestricted DESC, appName ASC")
    List<AppInfo> getAllSystemAppsInMobileRestrictedOrder(String str);
    // All system apps
    @Query("SELECT * FROM AppInfo WHERE system = 1 ORDER BY wifiRestricted DESC, appName ASC")
    List<AppInfo> getAllSystemAppsInWifiRestrictedOrder();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) AND system = 1 ORDER BY wifiRestricted DESC, appName ASC")
    List<AppInfo> getAllSystemAppsInWifiRestrictedOrder(String str);
    // All system apps
    @Query("SELECT * FROM AppInfo WHERE system = 1 ORDER BY adhellWhitelisted DESC, appName ASC")
    List<AppInfo> getAllSystemAppsInWhitelistedOrder();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) AND system = 1 ORDER BY adhellWhitelisted DESC, appName ASC")
    List<AppInfo> getAllSystemAppsInWhitelistedOrder(String str);
    // All system apps
    @Query("SELECT * FROM AppInfo WHERE system = 1 ORDER BY hasCustomDns DESC, appName ASC")
    List<AppInfo> getAllSystemAppsInDnsOrder();

    @Query("SELECT * FROM AppInfo WHERE (appName LIKE :str OR packageName LIKE :str) AND system = 1 ORDER BY hasCustomDns DESC, appName ASC")
    List<AppInfo> getAllSystemAppsInDnsOrder(String str);
}