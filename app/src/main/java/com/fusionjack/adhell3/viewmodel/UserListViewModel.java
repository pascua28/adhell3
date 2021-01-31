package com.fusionjack.adhell3.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.annotation.NonNull;

import com.fusionjack.adhell3.db.repository.BlackListRepository;
import com.fusionjack.adhell3.db.repository.UserListRepository;
import com.fusionjack.adhell3.db.repository.WhiteListRepository;

import java.util.List;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class UserListViewModel extends ViewModel {
    private final UserListRepository repository;

    public UserListViewModel(UserListRepository repository) {
        this.repository = repository;
    }

    public Single<LiveData<List<String>>> getItems() {
        return Single.fromCallable(repository::getItems);
    }

    public void addItem(String item, SingleObserver<String> observer) {
        repository.addItem(item)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
    }

    public void removeItem(String item, SingleObserver<String> observer) {
        repository.removeItem(item)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
    }

    public static class BlackListFactory extends ViewModelProvider.NewInstanceFactory {
        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new UserListViewModel(new BlackListRepository());
        }
    }

    public static class WhiteListFactory extends ViewModelProvider.NewInstanceFactory {
        @NonNull
        @Override
        @SuppressWarnings("unchecked")
        public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
            return (T) new UserListViewModel(new WhiteListRepository());
        }
    }
}
