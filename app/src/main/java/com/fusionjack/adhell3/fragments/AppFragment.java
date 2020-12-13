package com.fusionjack.adhell3.fragments;

import android.content.Context;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.ListView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.repository.AppRepository;
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
    static FilterAppInfo filterAppInfo;
    private AppViewModel viewModel;
    private String searchText;
    private SearchView searchView;
    private SingleObserver<List<AppInfo>> observer;
    private List<AppInfo> appList;

    void initAppModel(AppRepository.Type type) {
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
            }

            @Override
            public void onSuccess(@NonNull List<AppInfo> list) {
                appList.clear();
                appList.addAll(list);
                adapter.notifyDataSetChanged();
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
            }
        };
    }

    void loadAppList(AppRepository.Type type, ProgressBar loadingBar, ListView listView) {
        if (loadingBar != null) {
            loadingBar.setVisibility(View.VISIBLE);
        }
        if (searchText.isEmpty()) {
            viewModel.loadAppList(type, observer, filterAppInfo, loadingBar, listView);
        } else {
            viewModel.loadAppList(searchText, type, observer, filterAppInfo, loadingBar, listView);
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.app_menu, menu);

        initSearchView(menu);
    }

    void initSearchView(Menu menu) {
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
                viewModel.loadAppList(text, type, observer, filterAppInfo, null, null);
                return false;
            }
        });
    }

    void resetSearchView() {
        if (searchView != null) {
            searchText = "";
            searchView.setQuery(searchText, false);
            searchView.setIconified(true);
        }
    }
}
