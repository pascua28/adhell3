package com.fusionjack.adhell3.viewmodel;

import androidx.lifecycle.ViewModel;

import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.repository.AppRepository;

import java.util.List;

import io.reactivex.Single;

public class AppViewModel extends ViewModel {

    private final AppRepository repository;

    public AppViewModel() {
        this.repository = new AppRepository();
    }

    public Single<List<AppInfo>> loadAppList(AppRepository.Type type) {
        return repository.loadAppList("", type);
    }

}
