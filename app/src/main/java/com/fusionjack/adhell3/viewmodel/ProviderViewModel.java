package com.fusionjack.adhell3.viewmodel;

import androidx.lifecycle.ViewModel;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.utils.AdhellFactory;

import java.util.List;

import io.reactivex.rxjava3.core.Single;

public class ProviderViewModel extends ViewModel {

    public static final int NO_PROVIDER = -1;
    public static final int ALL_PROVIDERS = 0;

    private final AppDatabase appDatabase;

    public ProviderViewModel() {
        this.appDatabase = AdhellFactory.getInstance().getAppDatabase();
    }

    public Single<List<String>> getUrls(long providerId) {
        return Single.fromCallable(() -> providerId == ALL_PROVIDERS ?
                appDatabase.blockUrlProviderDao().getUniqueBlockedUrls() :
                appDatabase.blockUrlDao().getUrlsByProviderId(providerId));
    }

}
