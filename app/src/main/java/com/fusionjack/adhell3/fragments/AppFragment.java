package com.fusionjack.adhell3.fragments;

import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v7.widget.SearchView;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.utils.AppCache;
import com.fusionjack.adhell3.viewmodel.AppViewModel;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.SingleObserver;
import io.reactivex.disposables.Disposable;

public class AppFragment extends Fragment {

    protected Context context;
    protected AppRepository.Type type;
    private AppViewModel viewModel;
    private String searchText;
    private SearchView searchView;
    private SingleObserver<List<AppInfo>> observer;

    private List<AppInfo> appList;
    protected AppInfoAdapter adapter;
    protected FilterAppInfo filterAppInfo;

    protected void initAppModel(AppRepository.Type type) {
        this.filterAppInfo = new FilterAppInfo();
        this.context = getContext();
        this.type = type;
        this.searchText = "";

        appList = new ArrayList<>();
        adapter = new AppInfoAdapter(appList, type, false, context);
        viewModel = ViewModelProviders.of(this).get(AppViewModel.class);

        observer = new SingleObserver<List<AppInfo>>() {
            @Override
            public void onSubscribe(Disposable d) {
            }

            @Override
            public void onSuccess(List<AppInfo> list) {
                appList.clear();
                appList.addAll(list);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onError(Throwable e) {
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        AppCache.getInstance(context, null);
    }

    protected void loadAppList(AppRepository.Type type, ProgressBar progressBar) {
        viewModel.loadAppList(type, observer, filterAppInfo, progressBar);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.app_menu, menu);

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
                viewModel.loadAppList(text, type, observer, filterAppInfo, null);
                return false;
            }
        });
    }

    protected void resetSearchView() {
        if (searchView != null) {
            searchText = "";
            searchView.setQuery(searchText, false);
            searchView.setIconified(true);
        }
    }

}
