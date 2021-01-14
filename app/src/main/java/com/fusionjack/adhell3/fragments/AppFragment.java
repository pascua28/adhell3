package com.fusionjack.adhell3.fragments;

import android.content.Context;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;

import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.model.AppFlag;
import com.fusionjack.adhell3.model.FilterAppInfo;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppCacheChangeListener;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.viewmodel.AppViewModel;
import com.google.android.material.snackbar.Snackbar;
import com.samsung.android.knox.application.ApplicationPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.core.SingleOnSubscribe;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AppFragment extends Fragment implements AppCacheChangeListener {
    Context context;
    AppRepository.Type type;
    AppFlag appFlag;
    View rootView;
    private String searchText;
    private SearchView searchView;
    private SingleObserver<List<AppInfo>> observer;
    private List<AppInfo> initAppList;
    private List<AppInfo> searchAppList;
    private List<AppInfo> adapterAppList;
    protected AppInfoAdapter adapter;
    protected AppViewModel viewModel;

    private MutableLiveData<Boolean> _loadingVisibility;

    public void setFilterAppHighlightState(boolean state) {
        FilterAppInfo newFilterAppInfo = viewModel.getFilterAppInfo().getValue();
        if (newFilterAppInfo != null) {
            newFilterAppInfo.setHighlightRunningApps(state);
            viewModel.updateFilterAppInfo(newFilterAppInfo);
        }
    }

    public void setFilterAppSystemState(boolean state) {
        FilterAppInfo newFilterAppInfo = viewModel.getFilterAppInfo().getValue();
        if (newFilterAppInfo != null) {
            newFilterAppInfo.setSystemAppsFilter(state);
            viewModel.updateFilterAppInfo(newFilterAppInfo);
        }
    }

    public void setFilterAppUserState(boolean state) {
        FilterAppInfo newFilterAppInfo = viewModel.getFilterAppInfo().getValue();
        if (newFilterAppInfo != null) {
            newFilterAppInfo.setUserAppsFilter(state);
            viewModel.updateFilterAppInfo(newFilterAppInfo);
        }
    }

    public void setFilterAppRunningState(boolean state) {
        FilterAppInfo newFilterAppInfo = viewModel.getFilterAppInfo().getValue();
        if (newFilterAppInfo != null) {
            newFilterAppInfo.setRunningAppsFilter(state);
            viewModel.updateFilterAppInfo(newFilterAppInfo);
        }
    }

    public void setFilterAppStoppedState(boolean state) {
        FilterAppInfo newFilterAppInfo = viewModel.getFilterAppInfo().getValue();
        if (newFilterAppInfo != null) {
            newFilterAppInfo.setStoppedAppsFilter(state);
            viewModel.updateFilterAppInfo(newFilterAppInfo);
        }
    }

    public void initAppModel(AppRepository.Type type) {
        this.context = getContext();
        this.type = type;
        this.searchText = "";

        this.initAppList = new ArrayList<>();
        this.adapterAppList = new ArrayList<>();
        this.adapter = new AppInfoAdapter(adapterAppList, type, context);
        this.viewModel = new ViewModelProvider(getActivity() != null ? getActivity() : this).get(AppViewModel.class);

        this.observer = new SingleObserver<List<AppInfo>>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                updateLoadingBarVisibility(true);
            }

            @Override
            public void onSuccess(@NonNull List<AppInfo> list) {
                filterAppList(list);
            }

            @Override
            public void onError(@NonNull Throwable e) {
                if (e.getMessage() != null) {
                    if (getActivity() instanceof MainActivity) {
                        MainActivity mainActivity = (MainActivity) getActivity();
                        mainActivity.makeSnackbar(e.getMessage(), Snackbar.LENGTH_SHORT)
                                .show();
                    }
                    updateLoadingBarVisibility(false);
                }
            }
        };
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getLoadingBarVisibility().observe(
                getViewLifecycleOwner(),
                isVisible -> {
                    if (rootView != null && appFlag != null) {
                        ProgressBar loadingBar = rootView.findViewById(R.id.loadingBar);
                        ListView listView = rootView.findViewById(appFlag.getLayout());

                        if (isVisible) {
                            loadingBar.setVisibility(View.VISIBLE);
                            listView.setVisibility(View.GONE);
                        } else {
                            loadingBar.setVisibility(View.GONE);
                            listView.setVisibility(View.VISIBLE);
                        }
                    }
                }
        );

        viewModel.getFilterAppInfo().observe(
                getViewLifecycleOwner(),
                filterAppInfo -> {
                    // Set icon color
                    ImageView filterButton = requireView().findViewById(R.id.filterButton);
                    if (filterAppInfo.equals(new FilterAppInfo())) {
                        int themeColor = context.getResources().getColor(R.color.colorBottomNavUnselected, context.getTheme());
                        filterButton.setColorFilter(themeColor, PorterDuff.Mode.SRC_IN);
                    } else {
                        int accentColor = context.getResources().getColor(R.color.colorAccent, context.getTheme());
                        filterButton.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN);
                    }

                    if (searchAppList == null) {
                        searchAppList = initAppList;
                    }
                    if (searchAppList != null && searchAppList.size() > 0) {
                        filterAppList(searchAppList);
                    }
                }
        );
    }

    protected void loadAppList(AppRepository.Type type) {
        viewModel.loadAppList(type)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<LiveData<List<AppInfo>>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        updateLoadingBarVisibility(true);
                    }

                    @Override
                    public void onSuccess(@NonNull LiveData<List<AppInfo>> liveData) {
                        observeSuccessLoadList(liveData);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        if (e.getMessage() != null) {
                            LogUtils.error(e.getMessage(), e);
                            updateLoadingBarVisibility(false);
                        }
                    }
                });
    }

    private void observeSuccessLoadList(LiveData<List<AppInfo>> liveData) {
        if (this.isVisible()) {
            liveData.observe(getViewLifecycleOwner(), appList -> {
                if (appList != null && !appList.equals(initAppList)) {
                    initAppList = appList;
                    if (searchText.isEmpty()) {
                        filterAppList(appList);
                    } else {
                        if (searchView != null) {
                            searchView.setQuery(searchText, true);
                        }
                    }
                }
            });
        }
    }

    private void filterAppList(List<AppInfo> list) {
        FilterAppInfo filterAppInfo = viewModel.getFilterAppInfo().getValue();
        if (filterAppInfo != null) {
            Single.create((SingleOnSubscribe<List<AppInfo>>) emitter -> {
                // Filter System and/or User apps
                List<AppInfo> filterList;
                if (!filterAppInfo.getSystemAppsFilter()) {
                    filterList = list.stream()
                            .peek(appInfo -> appInfo.appName = appInfo.appName.replace(AppInfoAdapter.RUNNING_TAG, ""))
                            .filter(appInfo -> !appInfo.system)
                            .collect(Collectors.toList());
                } else if (!filterAppInfo.getUserAppsFilter()) {
                    filterList = list.stream()
                            .peek(appInfo -> appInfo.appName = appInfo.appName.replace(AppInfoAdapter.RUNNING_TAG, ""))
                            .filter(appInfo -> appInfo.system)
                            .collect(Collectors.toList());
                } else {
                    filterList = list.stream()
                            .peek(appInfo -> appInfo.appName = appInfo.appName.replace(AppInfoAdapter.RUNNING_TAG, ""))
                            .collect(Collectors.toList());
                }

                // Filter Running and/or Stopped apps
                List<AppInfo> finalList = new ArrayList<>();
                if (filterAppInfo.getHighlightRunningApps() || !filterAppInfo.getRunningAppsFilter() || !filterAppInfo.getStoppedAppsFilter()) {
                    ApplicationPolicy appPolicy = AdhellFactory.getInstance().getAppPolicy();
                    for (AppInfo item : filterList) {
                        boolean isRunning = false;
                        try {
                            if (appPolicy != null) {
                                isRunning = appPolicy.isApplicationRunning(item.packageName);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (filterAppInfo.getRunningAppsFilter() && isRunning) {
                            if (filterAppInfo.getHighlightRunningApps()) {
                                item.appName = String.format("%s%s", item.appName, AppInfoAdapter.RUNNING_TAG);
                            }
                            finalList.add(item);
                        } else if (filterAppInfo.getStoppedAppsFilter() && !isRunning) {
                            finalList.add(item);
                        }
                    }
                } else {
                    finalList = filterList;
                }
                emitter.onSuccess(finalList);
            })
                    .subscribeOn(Schedulers.computation())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new SingleObserver<List<AppInfo>>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {
                            updateLoadingBarVisibility(true);
                            searchAppList = list;
                        }

                        @Override
                        public void onSuccess(@NonNull List<AppInfo> appInfos) {
                            updateAppList(appInfos);
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            if (e.getMessage() != null) {
                                if (getActivity() instanceof MainActivity) {
                                    MainActivity mainActivity = (MainActivity) getActivity();
                                    mainActivity.makeSnackbar(e.getMessage(), Snackbar.LENGTH_SHORT)
                                            .show();
                                }
                                updateLoadingBarVisibility(false);
                            }
                        }
                    });
        }
    }

    private void updateAppList(List<AppInfo> list) {
        adapterAppList.clear();
        adapterAppList.addAll(list);
        adapter.notifyDataSetChanged();
        updateLoadingBarVisibility(false);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.app_menu, menu);
        initSearchView(menu);
    }

    protected void initSearchView(Menu menu) {
        searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String text) {
                searchText = text;
                if (text.isEmpty()) {
                    filterAppList(initAppList);
                } else {
                    Single.create((SingleOnSubscribe<List<AppInfo>>) emitter -> {
                        List<AppInfo> filteredList = initAppList.stream()
                                .filter(appInfo -> appInfo.appName.toLowerCase().contains(text.toLowerCase()) || appInfo.packageName.toLowerCase().contains(text.toLowerCase()))
                                .collect(Collectors.toList());
                        emitter.onSuccess(filteredList);
                    })
                            .subscribeOn(Schedulers.computation())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(observer);
                }
                return false;
            }
        });
    }

    @Override
    public void onDestroyOptionsMenu() {
        searchView.setOnQueryTextListener(null);
        searchView = null;
    }

    @Override
    public void onAppCacheChange() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        resetSearchView();
    }

    private void resetSearchView() {
        if (searchView != null && !searchText.isEmpty()) {
            searchView.setQuery("", false);
            searchView.setIconified(true);
        }
    }

    private LiveData<Boolean> getLoadingBarVisibility() {
        if (_loadingVisibility == null) {
            _loadingVisibility = new MutableLiveData<>();
            // Set initial value as true
            updateLoadingBarVisibility(true);
        }
        return _loadingVisibility;
    }

    private void updateLoadingBarVisibility(boolean isVisible) {
        if (_loadingVisibility != null) {
            if ( _loadingVisibility.getValue() != null) {
                boolean currentState = _loadingVisibility.getValue();
                if (currentState != isVisible) {
                    _loadingVisibility.setValue(isVisible);
                }
            } else {
                _loadingVisibility.setValue(isVisible);
            }
        }
    }
}
