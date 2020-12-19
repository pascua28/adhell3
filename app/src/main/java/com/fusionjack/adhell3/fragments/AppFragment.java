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
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.model.AppFlag;
import com.fusionjack.adhell3.viewmodel.AppViewModel;
import com.google.android.material.snackbar.Snackbar;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;

public class AppFragment extends Fragment {
    Context context;
    AppRepository.Type type;
    AppInfoAdapter adapter;
    AppFlag appFlag;
    View rootView;
    private AppViewModel viewModel;
    private String searchText;
    private SearchView searchView;
    private SingleObserver<List<AppInfo>> observer;
    private List<AppInfo> appList;

    static FilterAppInfo filterAppInfo;


    public void initAppModel(AppRepository.Type type) {
        filterAppInfo = MainActivity.getFilterAppInfo();
        this.context = getContext();
        this.type = type;
        this.searchText = "";

        appList = new ArrayList<>();
        adapter = new AppInfoAdapter(appList, type, false, context);
        viewModel = new ViewModelProvider(this).get(AppViewModel.class);

        observer = new SingleObserver<List<AppInfo>>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                viewModel.updateLoadingBarVisibility(true);
            }

            @Override
            public void onSuccess(@NonNull List<AppInfo> list) {
                appList.clear();
                appList.addAll(list);
                adapter.notifyDataSetChanged();
                viewModel.updateLoadingBarVisibility(false);
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
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        viewModel.getLoadingBarVisibility().observe(
                getViewLifecycleOwner(),
                isVisible -> {
                    if (rootView != null && appFlag != null) {
                        ProgressBar loadingBar = rootView.findViewById(R.id.loadingBar);
                        ListView listView = rootView.findViewById(appFlag.getLoadLayout());
                        SwipeRefreshLayout swipeContainer = rootView.findViewById(appFlag.getRefreshLayout());

                        if (isVisible) {
                            if (!swipeContainer.isRefreshing()) {
                                loadingBar.setVisibility(View.VISIBLE);
                            }

                            if (listView.getVisibility() == View.VISIBLE) {
                                listView.setVisibility(View.GONE);
                            }
                        } else {
                            loadingBar.setVisibility(View.GONE);
                            swipeContainer.setRefreshing(false);

                            if (listView.getVisibility() == View.GONE) {
                                listView.setVisibility(View.VISIBLE);
                            }
                        }
                    }
                }
        );
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    public void loadAppList(AppRepository.Type type) {
        if (searchText.isEmpty()) {
            viewModel.loadAppList(type, observer, filterAppInfo);
        } else {
            viewModel.loadAppList(searchText, type, observer, filterAppInfo);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (!MainActivity.finishActivity.compareAndSet(true, false)) {
            viewModel.updateLoadingBarVisibility(true);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.app_menu, menu);

        initSearchView(menu);
    }

    public void initSearchView(Menu menu) {
        searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setMaxWidth(Integer.MAX_VALUE);
        if (!searchText.isEmpty()) {
            searchView.setQuery(searchText, false);
            searchView.setIconified(false);
            searchView.requestFocus();
        }

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String text) {
                searchText = text;
                viewModel.loadAppList(text, type, observer, filterAppInfo);
                return false;
            }
        });
    }

    public void resetSearchView() {
        if (searchView != null) {
            searchText = "";
            searchView.setQuery(searchText, false);
            searchView.setIconified(true);
        }
    }
}
