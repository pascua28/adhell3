package com.fusionjack.adhell3.viewmodel;

import android.content.Context;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.BlockUrl;
import com.fusionjack.adhell3.db.entity.BlockUrlProvider;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.BlockUrlUtils;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;

public class BlockUrlProvidersViewModel extends ViewModel {
    private LiveData<List<BlockUrlProvider>> blockUrlProviders;
    private MutableLiveData<String> _domainCountInfo;
    private String domainCountInfoBase;
    private MutableLiveData<Boolean> _loadingVisibility;

    public BlockUrlProvidersViewModel() {
    }

    public LiveData<List<BlockUrlProvider>> getBlockUrlProviders() {
        if (blockUrlProviders == null) {
            blockUrlProviders = new MutableLiveData<>();
            loadBlockUrlProviders();
        }
        return blockUrlProviders;
    }

    private void loadBlockUrlProviders() {
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        blockUrlProviders = appDatabase.blockUrlProviderDao().getAll();
    }

    public LiveData<String> getDomainCountInfo(Context context) {
        if (_domainCountInfo == null) {
            domainCountInfoBase = context.getResources().getString(R.string.total_unique_domains);
            _domainCountInfo = new MutableLiveData<>();
            // Set initial value as 0
            updateDomainCountInfo(0);
        }
        return _domainCountInfo;
    }

    public void updateDomainCountInfo(int domainCount) {
        _domainCountInfo.setValue(
                String.format(domainCountInfoBase, domainCount)
        );
    }

    public LiveData<Boolean> getLoadingBarVisibility() {
        if (_loadingVisibility == null) {
            _loadingVisibility = new MutableLiveData<>();
            // Set initial value as false
            updateLoadingBarVisibility(false);
        }
        return _loadingVisibility;
    }

    public void updateLoadingBarVisibility(boolean isVisible) {
        _loadingVisibility.setValue(isVisible);
    }

    public void setProvider() {
        new SetProviderAsyncTask(this).execute();
    }

    public void addProvider(String strProvider) {
        new AddProviderAsyncTask(strProvider).execute();
    }

    public void setDomainCount(int delay) {
        new SetDomainCountAsyncTask(delay, this).execute();
    }

    public void updateProvider(Context context, boolean updateProviders) {
        updateLoadingBarVisibility(true);
        new UpdateProviderAsyncTask(context, updateProviders, this).execute();
    }

    private static class UpdateProviderAsyncTask extends AsyncTask<Void, Void, Void> {
        private final WeakReference<BlockUrlProvidersViewModel> providersViewModelWeakReference;
        private final WeakReference<Context> contextWeakReference;
        private final boolean updateProviders;

        UpdateProviderAsyncTask(Context context, boolean updateProviders, BlockUrlProvidersViewModel providersViewModel) {
            this.providersViewModelWeakReference = new WeakReference<>(providersViewModel);
            this.contextWeakReference = new WeakReference<>(context);
            this.updateProviders = updateProviders;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Context context = contextWeakReference.get();
            if (context != null) {
                if (AdhellFactory.getInstance().hasInternetAccess(context) && updateProviders) {
                    AdhellFactory.getInstance().updateAllProviders();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            BlockUrlProvidersViewModel providersViewModel = providersViewModelWeakReference.get();
            if (providersViewModel != null) {
                providersViewModel.updateLoadingBarVisibility(false);

                providersViewModel.setProvider();

                providersViewModel.setDomainCount(0);
            }
        }
    }

    private static class SetDomainCountAsyncTask extends AsyncTask<Void, Integer, Integer> {
        private final int delay;
        private final WeakReference<BlockUrlProvidersViewModel> providersViewModelWeakReference;

        SetDomainCountAsyncTask(int delay, BlockUrlProvidersViewModel providersViewModel) {
            this.delay = delay;
            this.providersViewModelWeakReference = new WeakReference<>(providersViewModel);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return BlockUrlUtils.getAllBlockedUrlsCount(appDatabase);
        }

        @Override
        protected void onPostExecute(Integer count) {
            BlockUrlProvidersViewModel providersViewModel = providersViewModelWeakReference.get();
            if (providersViewModel != null) {
                providersViewModel.updateDomainCountInfo(count);
                providersViewModel.updateLoadingBarVisibility(false);
            }
        }
    }

    private static class SetProviderAsyncTask extends AsyncTask<Void, Void, List<BlockUrlProvider>> {
        private final WeakReference<BlockUrlProvidersViewModel> providersViewModelWeakReference;

        SetProviderAsyncTask(BlockUrlProvidersViewModel providersViewModel) {
            this.providersViewModelWeakReference = new WeakReference<>(providersViewModel);
        }

        @Override
        protected List<BlockUrlProvider> doInBackground(Void... voids) {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            return appDatabase.blockUrlProviderDao().getAll2();
        }

        @Override
        protected void onPostExecute(List<BlockUrlProvider> providers) {
            BlockUrlProvidersViewModel providersViewModel = providersViewModelWeakReference.get();
            if (providersViewModel != null) {
                providersViewModel.updateLoadingBarVisibility(false);
            }
        }
    }

    private static class AddProviderAsyncTask extends AsyncTask<Void, Void, Void> {
        private final String provider;

        AddProviderAsyncTask(String provider) {
            this.provider = provider;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();

            BlockUrlProvider blockUrlProvider = new BlockUrlProvider();
            blockUrlProvider.url = provider;
            blockUrlProvider.count = 0;
            blockUrlProvider.deletable = true;
            blockUrlProvider.lastUpdated = new Date();
            blockUrlProvider.selected = false;
            blockUrlProvider.id = appDatabase.blockUrlProviderDao().insertAll(blockUrlProvider)[0];
            blockUrlProvider.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
            appDatabase.blockUrlProviderDao().updateBlockUrlProviders(blockUrlProvider);

            // Load providers
            try {
                List<BlockUrl> blockUrls = BlockUrlUtils.loadBlockUrls(blockUrlProvider);
                blockUrlProvider.count = blockUrls.size();
                blockUrlProvider.lastUpdated = new Date();
                appDatabase.blockUrlProviderDao().updateBlockUrlProviders(blockUrlProvider);
                appDatabase.blockUrlDao().insertAll(blockUrls);
            } catch (Exception e) {
                appDatabase.blockUrlProviderDao().delete(blockUrlProvider);
                e.printStackTrace();
            }
            return null;
        }
    }
}
