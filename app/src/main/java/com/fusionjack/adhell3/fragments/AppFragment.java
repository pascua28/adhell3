package com.fusionjack.adhell3.fragments;

import android.content.Context;
import android.view.Menu;
import android.view.MenuInflater;
import android.widget.Toast;

import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.viewmodel.AppViewModel;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class AppFragment extends Fragment {

    protected Context context;
    protected AppRepository.Type type;
    private AppViewModel viewModel;
    private String searchText;
    private SearchView searchView;
    private SingleObserver<List<AppInfo>> observer;

    private List<AppInfo> initAppList;
    private List<AppInfo> adapterAppList;
    protected AppInfoAdapter adapter;

    protected void initAppModel(AppRepository.Type type) {
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
            }

            @Override
            public void onSuccess(@NonNull List<AppInfo> list) {
                updateAppList(list);
            }

            @Override
            public void onError(@NonNull Throwable e) {
                LogUtils.error(e.getMessage(), e);
                Toast.makeText(context, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        };

        loadAppList(type);
    }

    private void loadAppList(AppRepository.Type type) {
        viewModel.loadAppList(type)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<LiveData<List<AppInfo>>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull LiveData<List<AppInfo>> liveData) {
                        liveData.observe(AppFragment.this, appList -> {
                            initAppList = appList;
                            if (searchText.isEmpty()) {
                                updateAppList(appList);
                            } else {
                                searchView.setQuery(searchText, true);
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
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
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
}
