package com.fusionjack.adhell3.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.fusionjack.adhell3.db.entity.AppPermission;

import java.util.List;

@Dao
public interface AppPermissionDao {

    @Query("SELECT * FROM AppPermission")
    LiveData<List<AppPermission>> getAllAsLiveData();

    @Query("SELECT * FROM AppPermission")
    List<AppPermission> getAll();

    @Query("SELECT * FROM AppPermission WHERE packageName = :packageName AND permissionStatus = -1")
    LiveData<List<AppPermission>> getPermissionsAsLiveData(String packageName);

    @Query("SELECT * FROM AppPermission WHERE packageName = :packageName AND permissionName = :permissionName AND permissionStatus = -1")
    AppPermission getPermission(String packageName, String permissionName);

    @Query("SELECT * FROM AppPermission WHERE packageName = :packageName AND permissionStatus = 1")
    LiveData<List<AppPermission>> getActivitiesAsLiveData(String packageName);

    @Query("SELECT * FROM AppPermission WHERE packageName = :packageName AND permissionStatus = 1")
    List<AppPermission> getActivities(String packageName);

    @Query("SELECT * FROM AppPermission WHERE packageName = :packageName AND permissionName = :activityName AND permissionStatus = 1")
    AppPermission getActivity(String packageName, String activityName);

    @Query("SELECT * FROM AppPermission WHERE packageName = :packageName AND permissionStatus = 2")
    LiveData<List<AppPermission>> getServicesAsLiveData(String packageName);

    @Query("SELECT * FROM AppPermission WHERE packageName = :packageName AND permissionStatus = 2")
    List<AppPermission> getServices(String packageName);

    @Query("SELECT * FROM AppPermission WHERE packageName = :packageName AND permissionName = :serviceName AND permissionStatus = 2")
    AppPermission getService(String packageName, String serviceName);

    @Query("SELECT * FROM AppPermission WHERE packageName = :packageName AND permissionStatus = 5")
    LiveData<List<AppPermission>> getReceiversAsLiveData(String packageName);

    @Query("SELECT * FROM AppPermission WHERE packageName = :packageName AND permissionStatus = 5")
    List<AppPermission> getReceivers(String packageName);

    @Query("SELECT * FROM AppPermission WHERE packageName = :packageName AND permissionName = :receiverPairName AND permissionStatus = 5")
    AppPermission getReceiver(String packageName, String receiverPairName);

    @Query("SELECT * FROM AppPermission WHERE packageName = :packageName AND permissionStatus = 3")
    LiveData<List<AppPermission>> getProvidersAsLiveData(String packageName);

    @Query("SELECT * FROM AppPermission WHERE packageName = :packageName AND permissionStatus = 3")
    List<AppPermission> getProviders(String packageName);

    @Query("SELECT * FROM AppPermission WHERE packageName = :packageName AND permissionName = :providerName AND permissionStatus = 3")
    AppPermission getProvider(String packageName, String providerName);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AppPermission appPermission);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<AppPermission> appPermissions);

    @Query("DELETE FROM AppPermission WHERE packageName = :packageName AND permissionName = :permissionName")
    void delete(String packageName, String permissionName);

    @Query("DELETE FROM AppPermission WHERE packageName = :packageName")
    void deleteByPackageName(String packageName);

    @Query("DELETE FROM AppPermission WHERE permissionStatus = -1 AND packageName = :packageName")
    void deletePermissions(String packageName);

    @Query("DELETE FROM AppPermission")
    void deleteAll();
}
