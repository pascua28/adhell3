package com.fusionjack.adhell3.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.fragments.FilterAppInfo;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AppViewModel extends ViewModel {
    private final AppRepository repository;
    private MutableLiveData<Boolean> _loadingVisibility;

    public AppViewModel() {
        this.repository = new AppRepository();
    }

    public void loadAppList(AppRepository.Type type, SingleObserver<List<AppInfo>> observer, FilterAppInfo filterAppInfo) {
        repository.loadAppList("", type, filterAppInfo)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
    }

    public void loadAppList(String text, AppRepository.Type type, SingleObserver<List<AppInfo>> observer, FilterAppInfo filterAppInfo) {
        repository.loadAppList(text, type, filterAppInfo)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
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
