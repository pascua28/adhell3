package com.fusionjack.adhell3.tasks;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.StaticProxy;
import com.fusionjack.adhell3.utils.AdhellFactory;

import io.reactivex.rxjava3.core.Completable;

public class StaticProxyRxTaskFactory {

    public static Completable deleteProxy(StaticProxy staticProxy) {
        return Completable.fromAction(() -> {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            appDatabase.staticProxyDao().delete(staticProxy);
        });
    }
}
