package com.fusionjack.adhell3.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.fragments.FilterAppInfo;

import java.util.List;

import io.reactivex.rxjava3.core.Single;

public class AppViewModel extends ViewModel {
    private MutableLiveData<Boolean> _loadingVisibility;
    private final AppRepository repository;

    public AppViewModel() {
        this.repository = new AppRepository();
    }

    public Single<List<AppInfo>> loadAppList(AppRepository.Type type, FilterAppInfo filterAppInfo) {
        return repository.loadAppList("", type, filterAppInfo);
    }

    public LiveData<Boolean> getLoadingBarVisibility() {
        if (_loadingVisibility == null) {
            _loadingVisibility = new MutableLiveData<>();
            // Set initial value as true
            updateLoadingBarVisibility(true);
        }
        return _loadingVisibility;
    }

    public void updateLoadingBarVisibility(boolean isVisible) {
        _loadingVisibility.postValue(isVisible);
    }
}
