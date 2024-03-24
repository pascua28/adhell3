package com.fusionjack.adhell3.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.fusionjack.adhell3.db.entity.StaticProxy;
import com.fusionjack.adhell3.db.repository.StaticProxyRepository;

import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class StaticProxyViewModel extends ViewModel {
    private final StaticProxyRepository repository;

    public StaticProxyViewModel() {
        this.repository = new StaticProxyRepository();
    }

    public Single<LiveData<List<StaticProxy>>> getItems() {
        return Single.fromCallable(repository::getItems);
    }

    public void addItem(StaticProxy item, SingleObserver<StaticProxy> observer) {
        repository.addItem(item)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
    }

    public void updateItem(String originName, StaticProxy item, SingleObserver<StaticProxy> observer) {
        repository.updateItem(originName, item)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
    }
}
