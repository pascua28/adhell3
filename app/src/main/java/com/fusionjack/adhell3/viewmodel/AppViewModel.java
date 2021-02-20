package com.fusionjack.adhell3.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.samsung.android.knox.application.ApplicationPolicy;

import java.util.List;

import io.reactivex.Single;

public class AppViewModel extends ViewModel {

    private final AppRepository repository;

    public AppViewModel() {
        this.repository = new AppRepository();
    }

    public Single<LiveData<List<AppInfo>>> loadAppList(AppRepository.Type type) {
        return repository.loadAppList(type);
    }

    public Single<Boolean> stopApp(AppInfo appInfo) {
        ApplicationPolicy appPolicy = AdhellFactory.getInstance().getAppPolicy();
        return Single.fromCallable(() -> appPolicy.stopApp(appInfo.packageName));
    }

}
