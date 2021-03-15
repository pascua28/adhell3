package com.fusionjack.adhell3.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.BlockUrlProvider;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.BlockUrlUtils;

import java.util.List;

import io.reactivex.rxjava3.core.Single;

public class BlockUrlProvidersViewModel extends ViewModel {

    public BlockUrlProvidersViewModel() {
    }

    public Single<LiveData<Integer>> getDomainCount() {
        return Single.fromCallable(() -> {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            return BlockUrlUtils.getTotalDomainCountAsLiveData(appDatabase);
        });
    }

    public Single<LiveData<List<BlockUrlProvider>>> getBlockUrlProviders() {
        return Single.fromCallable(() -> {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            return appDatabase.blockUrlProviderDao().getAllAsLiveData();
        });
    }
}
