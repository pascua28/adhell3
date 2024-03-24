package com.fusionjack.adhell3.db.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import com.fusionjack.adhell3.db.entity.StaticProxy;

import java.util.List;

@Dao
public interface StaticProxyDao {

    @Query("SELECT * FROM StaticProxy")
    LiveData<List<StaticProxy>> getAll();

    @Query("SELECT * FROM StaticProxy")
    List<StaticProxy> getAll2();

    @Insert()
    void insert(StaticProxy staticProxy);

    @Insert
    void insertAll(List<StaticProxy> staticProxies);

    @Query("UPDATE StaticProxy SET name = :name, hostname = :hostname, port = :port, exclusionList = :exclusionList, user = :user, password = :password WHERE name = :originName")
    void updateByName(String originName, String name, String hostname, int port, String exclusionList, String user, String password);

    @Delete
    void delete(StaticProxy staticProxy);

    @Query("DELETE FROM StaticProxy WHERE name = :name")
    void deleteByName(String name);

    @Query("DELETE FROM StaticProxy")
    void deleteAll();

    @Query("SELECT * FROM StaticProxy WHERE hostname = :hostname AND port = :port AND exclusionList = :exclusionList AND user = :user AND password = :password")
    LiveData<StaticProxy> getByProperties(String hostname, int port, String exclusionList, String user, String password);
}