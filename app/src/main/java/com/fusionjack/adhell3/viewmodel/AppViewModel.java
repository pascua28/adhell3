package com.fusionjack.adhell3.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.model.FilterAppInfo;

import java.util.List;

import io.reactivex.rxjava3.core.Single;

public class AppViewModel extends ViewModel {
    private MutableLiveData<FilterAppInfo> _filterAppInfoLive;
    protected LiveData<FilterAppInfo> filterAppInfoLive = null;
    private final AppRepository repository;

    public AppViewModel() {
        this.repository = new AppRepository();
    }

    public Single<LiveData<List<AppInfo>>> loadAppList(AppRepository.Type type) {
        return repository.loadAppList(type);
    }

    public LiveData<FilterAppInfo> getFilterAppInfo() {
        if (_filterAppInfoLive == null) {
            _filterAppInfoLive = new MutableLiveData<>();
            filterAppInfoLive = _filterAppInfoLive;
            // Set initial value as true
            updateFilterAppInfo(new FilterAppInfo());
        }
        return filterAppInfoLive;
    }

    public void updateFilterAppInfo(FilterAppInfo filterAppInfo) {
        if (_filterAppInfoLive != null) {
            _filterAppInfoLive.setValue(filterAppInfo);
        }
    }
}
