package com.fusionjack.adhell3.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.model.AppFlag;
import com.fusionjack.adhell3.utils.AppCacheChangeListener;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.viewmodel.AppViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    private AppViewModel viewModel;
    private String searchText;
    private SearchView searchView;
    private SingleObserver<List<AppInfo>> observer;

    private List<AppInfo> initAppList;
    private List<AppInfo> adapterAppList;
    protected AppInfoAdapter adapter;

    static FilterAppInfo filterAppInfo;

    public void initAppModel(AppRepository.Type type) {
        filterAppInfo = MainActivity.getFilterAppInfo();
        this.context = getContext();
        this.type = type;
        this.searchText = "";

        this.initAppList = new ArrayList<>();
        this.adapterAppList = new ArrayList<>();
        this.adapter = new AppInfoAdapter(adapterAppList, type, context);
        this.viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        this.observer = new SingleObserver<List<AppInfo>>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                viewModel.updateLoadingBarVisibility(true);
            }

            @Override
            public void onSuccess(@NonNull List<AppInfo> list) {
                updateAppList(list);
            }

            @Override
            public void onError(@NonNull Throwable e) {
                if (e.getMessage() != null) {
                    if (getActivity() instanceof MainActivity) {
                        MainActivity mainActivity = (MainActivity) getActivity();
                        mainActivity.makeSnackbar(e.getMessage(), Snackbar.LENGTH_SHORT)
                                .show();
                    }
                }
                viewModel.updateLoadingBarVisibility(false);
            }
        };

        loadAppList(type);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewModel.getLoadingBarVisibility().observe(
                getViewLifecycleOwner(),
                isVisible -> {
                    if (rootView != null && appFlag != null) {
                        ProgressBar loadingBar = rootView.findViewById(R.id.loadingBar);
                        ListView listView = rootView.findViewById(appFlag.getLayout());

                        if (isVisible) {
                            loadingBar.setVisibility(View.VISIBLE);

                            if (listView.getVisibility() == View.VISIBLE) {
                                listView.setVisibility(View.GONE);
                            }
                        } else {
                            loadingBar.setVisibility(View.GONE);

                            if (listView.getVisibility() == View.GONE) {
                                listView.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                }
        );
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        resetSearchView();
        // Close keyboard
        Objects.requireNonNull(ViewCompat.getWindowInsetsController(rootView)).hide(WindowInsetsCompat.Type.ime());
    }

    private void loadAppList(AppRepository.Type type) {
        viewModel.loadAppList(type, filterAppInfo)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<LiveData<List<AppInfo>>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull LiveData<List<AppInfo>> liveData) {
                        liveData.observe(AppFragment.this, appList -> {
                            if (appList != null) {
                                initAppList = appList;
                                if (searchText.isEmpty()) {
                                    updateAppList(appList);
                                } else {
                                    searchView.setQuery(searchText, true);
                                }
                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        LogUtils.error(e.getMessage(), e);
                    }
                });
    }

    private void updateAppList(List<AppInfo> list) {
        adapterAppList.clear();
        adapterAppList.addAll(list);
        adapter.notifyDataSetChanged();
        viewModel.updateLoadingBarVisibility(false);
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
                    updateAppList(initAppList);
                } else {
                    Single.create((SingleOnSubscribe<List<AppInfo>>) emitter -> {
                        List<AppInfo> filteredList = initAppList.stream()
                                .filter(appInfo -> appInfo.appName.contains(text) || appInfo.packageName.contains(text))
                                .collect(Collectors.toList());
                        emitter.onSuccess(filteredList);
                    }).subscribe(observer);
                }
                return false;
            }
        });
    }

    @Override
    public void onDestroyOptionsMenu()
    {
        searchView.setOnQueryTextListener(null);
        searchView = null;
    }

    @Override
    public void onAppCacheChange() {
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    private void resetSearchView() {
        if (searchView != null) {
            searchView.setQuery("", false);
            searchView.setIconified(true);
        }
    }
}
