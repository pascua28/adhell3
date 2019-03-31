package com.fusionjack.adhell3.viewmodel;

import android.arch.lifecycle.ViewModel;
import android.widget.ProgressBar;

import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.fragments.FilterAppInfo;

import java.util.List;

import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class AppViewModel extends ViewModel {

    private final AppRepository repository;

    public AppViewModel() {
        this.repository = new AppRepository();
    }

    public void loadAppList(AppRepository.Type type, SingleObserver<List<AppInfo>> observer, FilterAppInfo filterAppInfo, ProgressBar progressBar) {
        repository.loadAppList("", type, filterAppInfo, progressBar)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
    }

    public void loadAppList(String text, AppRepository.Type type, SingleObserver<List<AppInfo>> observer, FilterAppInfo filterAppInfo, ProgressBar progressBar) {
        repository.loadAppList(text, type, filterAppInfo, progressBar)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
    }

}
