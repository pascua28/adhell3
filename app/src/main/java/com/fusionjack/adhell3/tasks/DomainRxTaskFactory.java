package com.fusionjack.adhell3.tasks;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.BlockUrl;
import com.fusionjack.adhell3.db.entity.BlockUrlProvider;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.BlockUrlUtils;

import java.util.Date;
import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Single;

public class DomainRxTaskFactory {

    public static Completable deleteProvider(BlockUrlProvider provider) {
        return Completable.fromAction(() -> {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            appDatabase.blockUrlProviderDao().delete(provider);
        });
    }

    public static Single<Boolean> selectProvider(boolean isChecked, BlockUrlProvider provider) {
        return Single.fromCallable(() -> {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();

            if (provider.lastUpdated == null) {
                List<BlockUrl> blockUrls = BlockUrlUtils.loadBlockUrls(provider);
                provider.count = blockUrls.size();
                provider.lastUpdated = new Date();
                appDatabase.blockUrlDao().insertAll(blockUrls);
            }

            // BlockUrlUtils.getAllBlockedUrlsCount works based on 'provider.selected', so it needs to be updated first
            provider.selected = isChecked;
            appDatabase.blockUrlProviderDao().updateBlockUrlProviders(provider);

            if (isChecked) {
                int totalDomainCount = BlockUrlUtils.getTotalDomainCount(appDatabase);
                if (totalDomainCount > AdhellAppIntegrity.BLOCK_URL_LIMIT) {
                    provider.selected = false;
                    appDatabase.blockUrlProviderDao().updateBlockUrlProviders(provider);
                }
            }

            return !isChecked || provider.selected; // Definition of isValid
        });
    }

    public static Completable loadProvider(BlockUrlProvider provider) {
        return Completable.fromAction(() -> {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            List<BlockUrl> blockUrls = BlockUrlUtils.loadBlockUrls(provider);
            appDatabase.blockUrlDao().insertAll(blockUrls);
            provider.count = blockUrls.size();
            provider.lastUpdated = new Date();
            appDatabase.blockUrlProviderDao().updateBlockUrlProviders(provider);
        });
    }

    public static Single<BlockUrlProvider> addProvider(String providerUrl) {
        return Single.fromCallable(() -> {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            BlockUrlProvider provider = new BlockUrlProvider();
            provider.url = providerUrl;
            provider.count = 0;
            provider.deletable = true;
            provider.lastUpdated = null;
            provider.selected = false;
            provider.id = appDatabase.blockUrlProviderDao().insertAll(provider)[0];
            provider.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
            appDatabase.blockUrlProviderDao().updateBlockUrlProviders(provider);
            return provider;
        });
    }

    public static Completable updateAllProviders() {
        return Completable.fromAction(() -> AdhellFactory.getInstance().updateAllProviders());
    }
}
