package com.fusionjack.adhell3.viewmodel;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.fusionjack.adhell3.db.repository.BlackListRepository;
import com.fusionjack.adhell3.db.repository.UserListRepository;
import com.fusionjack.adhell3.db.repository.WhiteListRepository;
import com.fusionjack.adhell3.utils.BlockUrlPatternsMatch;

import java.util.List;
import java.util.StringTokenizer;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class UserListViewModel extends ViewModel {
    private final UserListRepository repository;

    public UserListViewModel(UserListRepository repository) {
        this.repository = repository;
    }

    public Single<LiveData<List<String>>> getItems() {
        return Single.fromCallable(repository::getItems);
    }

    public Single<String> addItemObservable(String item) {
        return repository.addItem(item);
    }

    public void addItem(String item, SingleObserver<String> observer) {
        repository.addItem(item)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(observer);
    }

    public void validateDomain(String domainStr) {
        String domainToAdd = domainStr.trim().toLowerCase();
        if (domainToAdd.indexOf('|') == -1) {
            if (!BlockUrlPatternsMatch.isUrlValid(domainToAdd)) {
                throw new IllegalArgumentException("Url not valid. Please check");
            }
        } else {
            // packageName|url
            StringTokenizer tokens = new StringTokenizer(domainToAdd, "|");
            if (tokens.countTokens() != 2 && tokens.countTokens() != 3) {
                throw new IllegalArgumentException("Rule not valid. Please check");
            }
        }
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
