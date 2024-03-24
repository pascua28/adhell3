package com.fusionjack.adhell3.db.repository;

import androidx.lifecycle.LiveData;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.StaticProxy;
import com.fusionjack.adhell3.utils.AdhellFactory;

import java.util.List;

import io.reactivex.rxjava3.core.Single;

public class StaticProxyRepository {

    private final AppDatabase appDatabase;

    public StaticProxyRepository() {
        this.appDatabase = AdhellFactory.getInstance().getAppDatabase();
    }


    public LiveData<List<StaticProxy>> getItems() {
        return appDatabase.staticProxyDao().getAll();
    }

    public Single<StaticProxy> addItem(StaticProxy item) {
        if (item == null) {
            return Single.error(new IllegalArgumentException("Item cannot be null or empty"));
        }

        return Single.create(emitter -> {
            addItemToDatabase(item);
            emitter.onSuccess(item);
        });
    }
    public void addItemToDatabase(StaticProxy item) {
        appDatabase.staticProxyDao().insert(item);
    }

    public Single<StaticProxy> updateItem(String originName, StaticProxy item) {
        if (item == null) {
            return Single.error(new IllegalArgumentException("Item cannot be null or empty"));
        }

        return Single.create(emitter -> {
            updateItemFromDatabase(originName, item);
            emitter.onSuccess(item);
        });
    }

    public void updateItemFromDatabase(String originName, StaticProxy item) {
        appDatabase.staticProxyDao().updateByName(originName, item.name, item.hostname, item.port, item.exclusionList, item.user, item.password);
    }

    public LiveData<StaticProxy> getByProperties(String hostname, int port, String exclusionList, String user, String password) {
        return appDatabase.staticProxyDao().getByProperties(hostname, port, exclusionList, user, password);
    }
}