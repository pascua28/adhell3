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

    @Query("SELECT COUNT(*) FROM AppPermission WHERE permissionStatus = -1")
    LiveData<Integer> getPermissionSize();

    @Query("SELECT COUNT(*) FROM AppPermission WHERE permissionStatus = 2")
    LiveData<Integer> getServiceSize();

    @Query("SELECT COUNT(*) FROM AppPermission WHERE permissionStatus = 5")
    LiveData<Integer> getReceiverSize();

    @Query("SELECT * FROM AppPermission")
    LiveData<List<AppPermission>> getAllAsLiveData();

    @Query("SELECT * FROM AppPermission")
    List<AppPermission> getAll();

    @Query("SELECT * FROM AppPermission WHERE packageName = :packageName AND permissionName = :permissionName AND permissionStatus = -1")
    AppPermission getPermission(String packageName, String permissionName);

    @Query("SELECT * FROM AppPermission WHERE packageName = :packageName AND permissionStatus = 2")
    List<AppPermission> getServices(String packageName);

    @Query("SELECT * FROM AppPermission WHERE packageName = :packageName AND permissionName = :serviceName AND permissionStatus = 2")
    AppPermission getService(String packageName, String serviceName);

    @Query("SELECT * FROM AppPermission WHERE packageName = :packageName AND permissionStatus = 5")
    List<AppPermission> getReceivers(String packageName);

    @Query("SELECT * FROM AppPermission WHERE packageName = :packageName AND permissionName = :receiverPairName AND permissionStatus = 5")
    AppPermission getReceiver(String packageName, String receiverPairName);

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

    @Query("DELETE FROM AppPermission WHERE permissionStatus = 2 AND packageName = :packageName")
    void deleteServices(String packageName);

    @Query("DELETE FROM AppPermission WHERE permissionStatus = 5 AND packageName = :packageName")
    void deleteReceivers(String packageName);

    @Query("DELETE FROM AppPermission")
    void deleteAll();
}
