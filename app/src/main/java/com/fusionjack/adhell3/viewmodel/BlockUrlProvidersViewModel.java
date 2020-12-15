package com.fusionjack.adhell3.viewmodel;

import android.content.Context;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.BlockUrl;
import com.fusionjack.adhell3.db.entity.BlockUrlProvider;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.BlockUrlUtils;
import com.google.android.material.snackbar.Snackbar;

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
            updateDomainCountInfo(-1);
        }
        return _domainCountInfo;
    }

    public void updateDomainCountInfo(int domainCount) {
        String result = "";
        if (domainCount > 0) {
            result = String.format(domainCountInfoBase, domainCount);
        }
        _domainCountInfo.setValue(result);
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
        _loadingVisibility.setValue(isVisible);
    }

    public void addProvider(String strProvider, Context context) {
        new AddProviderAsyncTask(strProvider, context).execute();
    }

    public void setDomainCount() {
        updateLoadingBarVisibility(true);
        new SetDomainCountAsyncTask(this).execute();
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
                providersViewModel.setDomainCount();
            }
        }
    }

    private static class SetDomainCountAsyncTask extends AsyncTask<Void, Integer, Integer> {
        private final WeakReference<BlockUrlProvidersViewModel> providersViewModelWeakReference;

        SetDomainCountAsyncTask(BlockUrlProvidersViewModel providersViewModel) {
            this.providersViewModelWeakReference = new WeakReference<>(providersViewModel);
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
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

    private static class AddProviderAsyncTask extends AsyncTask<Void, Void, String> {
        private final WeakReference<Context> contextReference;
        private final String provider;

        AddProviderAsyncTask(String provider, Context context) {
            this.provider = provider;
            this.contextReference = new WeakReference<>(context);
        }

        @Override
        protected String doInBackground(Void... voids) {
            String result = "";
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
                result = e.getMessage();
            }
            return result;
        }

        @Override
        protected void onPostExecute(String result) {
            if (!result.isEmpty()) {
                Context context = contextReference.get();
                if (context instanceof MainActivity) {
                    String message = "Error! " + result;
                    MainActivity mainActivity = (MainActivity) context;
                    mainActivity.makeSnackbar(message, Snackbar.LENGTH_LONG)
                            .show();
                }
            }
        }
    }
}
