package com.fusionjack.adhell3.db.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity(
        tableName = "StaticProxy"
)
public class StaticProxy {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    public long id;

    @ColumnInfo(name = "name")
    public String name;
    
    @ColumnInfo(name = "hostname")
    public String hostname;

    @ColumnInfo(name = "port")
    public int port;

    @ColumnInfo(name = "exclusionList")
    public String exclusionList;

    @ColumnInfo(name = "user")
    public String user;

    @ColumnInfo(name = "password")
    public String password;

    public StaticProxy(String name, String hostname, int port, String exclusionList, String user, String password) throws IllegalArgumentException {
        if (name.trim().isEmpty()) {
            throw new IllegalArgumentException("Name cannot be empty");
        }
        if (!isValidPort(port)) {
            throw new IllegalArgumentException("Port is not valid TCP port");
        }

        if (hostname.isEmpty()) {
            throw new IllegalArgumentException("Invalid hostname");
        }

        this.name = name;
        this.hostname = hostname;
        this.port = port;
        this.exclusionList = exclusionList;
        this.user = user;
        this.password = password;
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }

    private static boolean isValidPort(int port) {
        return port >= 1 && port <= 65535;
    }
}
