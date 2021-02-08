package com.fusionjack.adhell3.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.model.AppFlag;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.utils.rx.RxSingleComputationBuilder;
import com.fusionjack.adhell3.utils.rx.RxSingleIoBuilder;
import com.fusionjack.adhell3.viewmodel.AppViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;

public abstract class AppFragment extends Fragment {

    protected Context context;
    private String searchText;
    private SearchView searchView;

    private List<AppInfo> currentAppList;
    private List<AppInfo> initialAppList;
    private List<AppInfo> adapterAppList;
    private AppInfoAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = getContext();
        this.searchText = "";
        this.currentAppList = Collections.emptyList();
        this.initialAppList = Collections.emptyList();
        this.adapterAppList = new ArrayList<>();

        AppRepository.Type type = getType();
        this.adapter = new AppInfoAdapter(adapterAppList, type, context);

        initAppList(type);
    }

    protected abstract AppRepository.Type getType();

    protected View inflateFragment(int fragmentViewId, LayoutInflater inflater, ViewGroup container, AppFlag flag) {
        View view = inflater.inflate(fragmentViewId, container, false);

        ListView listView = view.findViewById(flag.getLayout());
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view2, position, id) -> listOnItemClickListener(parent, view2, position, id, flag));

        return view;
    }

    protected abstract void listOnItemClickListener(AdapterView<?> adView, View view2, int position, long id, AppFlag flag);

    private void initAppList(AppRepository.Type type) {
        AppViewModel viewModel = new ViewModelProvider(this).get(AppViewModel.class);
        Consumer<LiveData<List<AppInfo>>> callback = liveData -> {
            safeGuardLiveData(() -> {
                liveData.observe(getViewLifecycleOwner(), appList -> {
                    this.initialAppList = appList;
                    this.currentAppList = appList;
                    if (searchText.isEmpty()) {
                        updateAppList(currentAppList);
                    } else {
                        searchView.setQuery(searchText, false);
                    }
                });
            });
        };
        new RxSingleComputationBuilder().async(viewModel.loadAppList(type), callback);
    }

    private void safeGuardLiveData(Runnable action) {
        if (getView() == null) {
            LogUtils.error("View is null");
            return;
        }
        action.run();
    }

    protected void setCurrentAppList(List<AppInfo> newList) {
        this.currentAppList = newList;
        updateAppList(currentAppList);
    }

    protected void restoreAppList() {
        this.currentAppList = initialAppList;
        updateAppList(currentAppList);
    }

    private void updateAppList(List<AppInfo> list) {
        adapterAppList.clear();
        adapterAppList.addAll(list);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onCreateOptionsMenu(@androidx.annotation.NonNull Menu menu, @androidx.annotation.NonNull MenuInflater inflater) {
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
                    updateAppList(currentAppList);
                } else {
                    SingleOnSubscribe<List<AppInfo>> source = emitter -> {
                        List<AppInfo> filteredList = currentAppList.stream()
                                .filter(appInfo -> {
                                    String appName = appInfo.appName.toLowerCase();
                                    String packageName = appInfo.packageName.toLowerCase();
                                    String _text = text.toLowerCase();
                                    return appName.contains(_text) || packageName.contains(_text);
                                })
                                .collect(Collectors.toList());
                        emitter.onSuccess(filteredList);
                    };
                    new RxSingleIoBuilder().async(Single.create(source), list -> updateAppList(list));
                }
                return false;
            }
        });
    }
}
