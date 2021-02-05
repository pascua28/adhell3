package com.fusionjack.adhell3.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppPermission;
import com.fusionjack.adhell3.utils.AdhellFactory;

import java.util.List;

import io.reactivex.Single;

public class AppComponentViewModel extends ViewModel {

    private final AppDatabase appDatabase;

    public AppComponentViewModel() {
        this.appDatabase = AdhellFactory.getInstance().getAppDatabase();
    }

    public Single<LiveData<List<AppPermission>>> getPermissions(String packageName) {
        return Single.fromCallable(() -> appDatabase.appPermissionDao().getPermissionsAsLiveData(packageName));
    }

    public Single<LiveData<List<AppPermission>>> getActivities(String packageName) {
        return Single.fromCallable(() -> appDatabase.appPermissionDao().getActivitiesAsLiveData(packageName));
    }

    public Single<LiveData<List<AppPermission>>> getServices(String packageName) {
        return Single.fromCallable(() -> appDatabase.appPermissionDao().getServicesAsLiveData(packageName));
    }

    public Single<LiveData<List<AppPermission>>> getReceivers(String packageName) {
        return Single.fromCallable(() -> appDatabase.appPermissionDao().getReceiversAsLiveData(packageName));
    }

}
