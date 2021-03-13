package com.fusionjack.adhell3.fragments;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.utils.SharedPreferenceBooleanLiveData;
import com.fusionjack.adhell3.utils.UiUtils;
import com.fusionjack.adhell3.utils.dialog.QuestionDialogBuilder;
import com.fusionjack.adhell3.utils.rx.RxCompletableIoBuilder;
import com.fusionjack.adhell3.utils.rx.RxSingleComputationBuilder;
import com.fusionjack.adhell3.utils.rx.RxSingleIoBuilder;
import com.fusionjack.adhell3.viewmodel.AppViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.functions.Action;

public abstract class AppFragment extends Fragment {

    private static final int STOP_APP_CONTEXT_MENU = 1;
    private static final int WIPE_APP_DATA_CONTEXT_MENU = 2;
    private static final int COPY_PACKAGE_NAME_CONTEXT_MENU = 3;

    protected Context context;
    private String searchText;
    private SearchView searchView;

    private List<AppInfo> restoredAppList;
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
        this.restoredAppList = Collections.emptyList();
        this.adapterAppList = new ArrayList<>();

        AppRepository.Type type = getType();
        this.adapter = new AppInfoAdapter(adapterAppList, type, context);

        initAppList(type);
        initHideSystemApps();
    }

    protected abstract AppRepository.Type getType();

    protected View inflateFragment(int fragmentViewId, LayoutInflater inflater, ViewGroup container, AppFlag flag) {
        View view = inflater.inflate(fragmentViewId, container, false);

        ListView listView = view.findViewById(flag.getLayout());
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view2, position, id) -> listOnItemClickListener(parent, view2, position, id, flag));

        registerForContextMenu(listView);

        return view;
    }

    protected abstract void listOnItemClickListener(AdapterView<?> adView, View view2, int position, long id, AppFlag flag);

    private void initAppList(AppRepository.Type type) {
        AppViewModel viewModel = new ViewModelProvider(this).get(AppViewModel.class);
        Consumer<LiveData<List<AppInfo>>> callback = liveData -> {
            safeGuardLiveData(() -> {
                liveData.observe(getViewLifecycleOwner(), appList -> {
                    this.initialAppList = appList;
                    this.restoredAppList = appList;
                    showOrHideSystemApps();
                });
            });
        };
        new RxSingleComputationBuilder().async(viewModel.loadAppList(type), callback);
    }

    private void initHideSystemApps() {
        Consumer<SharedPreferenceBooleanLiveData> callback = liveData -> {
            safeGuardLiveData(() -> {
                liveData.observe(getViewLifecycleOwner(), hideSystem -> {
                    showOrHideSystemApps();
                });
            });
        };
        Single<SharedPreferenceBooleanLiveData> liveData = AppPreferences.getInstance().getHideSystemAppsLiveData();
        new RxSingleIoBuilder().async(liveData, callback);
    }

    private void showOrHideSystemApps() {
        boolean hideSystem = AppPreferences.getInstance().isHideSystemApps();
        if (hideSystem) {
            this.currentAppList = filterList(restoredAppList);
        } else {
            this.currentAppList = restoredAppList;
        }
        if (searchText.isEmpty()) {
            updateAppList(currentAppList);
        } else {
            searchView.setQuery(searchText, true);
        }
    }

    private void safeGuardLiveData(Runnable action) {
        if (getView() == null) {
            LogUtils.error("View is null");
            return;
        }
        action.run();
    }

    @Override
    public void onCreateContextMenu(@NonNull ContextMenu menu, @NonNull View v, @Nullable ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        AppInfo appInfo = adapter.getItem(info.position);
        if (menu.size() == 0) {
            if (!appInfo.system) {
                menu.add(0, STOP_APP_CONTEXT_MENU, Menu.NONE, R.string.menu_stop_app);
            }
            menu.add(0, WIPE_APP_DATA_CONTEXT_MENU, Menu.NONE, R.string.menu_wipe_app_data);
            menu.add(0, COPY_PACKAGE_NAME_CONTEXT_MENU, Menu.NONE, R.string.menu_copy_package_name_to_clipboard);
        }
    }

    @Override
    public boolean onContextItemSelected(@NonNull MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        AppInfo appInfo = adapter.getItem(info.position);
        int id = item.getItemId();
        if (id == STOP_APP_CONTEXT_MENU) {
            stopApp(appInfo);
        } else if (id == WIPE_APP_DATA_CONTEXT_MENU) {
            wipeAppData(appInfo);
        } else if (id == COPY_PACKAGE_NAME_CONTEXT_MENU) {
            copyToClipboard(appInfo.packageName);
        }
        return true;
    }

    private void stopApp(AppInfo appInfo) {
        Consumer<Boolean> callback = success -> {
            if (success) {
                Toast.makeText(getContext(), "App has been stopped", Toast.LENGTH_LONG).show();
                adapter.notifyDataSetChanged();
            }
        };
        AppViewModel viewModel = new ViewModelProvider(this).get(AppViewModel.class);
        new RxSingleIoBuilder().async(viewModel.stopApp(appInfo), callback);
    }

    private void wipeAppData(AppInfo appInfo) {
        Runnable onPositiveButton = () -> {
            Consumer<Boolean> callback = success -> {
                if (success) {
                    Toast.makeText(getContext(), "App data has been successfully wiped", Toast.LENGTH_LONG).show();
                    adapter.notifyDataSetChanged();
                }
            };
            AppViewModel viewModel = new ViewModelProvider(this).get(AppViewModel.class);
            new RxSingleIoBuilder().async(viewModel.wipeAppData(appInfo), callback);
        };
        new QuestionDialogBuilder(getView())
                .setTitle(R.string.wipe_app_data_title)
                .setQuestion(R.string.wipe_app_data_info)
                .show(onPositiveButton);
    }

    private void copyToClipboard(String packageName) {
        Runnable callback = () -> Toast.makeText(getContext(), "Package name is copied to clipboard", Toast.LENGTH_LONG).show();
        Action action = () -> {
            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("adhell3_app_package_name", packageName);
            clipboard.setPrimaryClip(clip);
        };
        new RxCompletableIoBuilder().async(Completable.fromAction(action), callback);
    }

    @Override
    public void onCreateOptionsMenu(@androidx.annotation.NonNull Menu menu, @androidx.annotation.NonNull MenuInflater inflater) {
        if (menu.size() == 0) {
            inflater.inflate(R.menu.app_menu, menu);
            UiUtils.setMenuIconColor(menu, getContext());
            Optional.ofNullable(menu.findItem(R.id.action_hide_system)).ifPresent(this::initHideSystemMenu);
            initSearchView(menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_hide_system) {
            toggleHideSystem(item);
        }
        return super.onOptionsItemSelected(item);
    }

    private void initHideSystemMenu(MenuItem item) {
        boolean hideSystem = AppPreferences.getInstance().isHideSystemApps();
        if (hideSystem) {
            item.setTitle(R.string.menu_show_system);
            item.setIcon(R.drawable.ic_show_system);
        } else {
            item.setTitle(R.string.menu_hide_system);
            item.setIcon(R.drawable.ic_hide_system);
        }
        UiUtils.tintMenuIcon(item, getContext());
    }

    private void toggleHideSystem(MenuItem item) {
        boolean hideSystem = AppPreferences.getInstance().isHideSystemApps();
        AppPreferences.getInstance().setHideSystemApps(!hideSystem);
        initHideSystemMenu(item);
    }

    private List<AppInfo> filterList(List<AppInfo> list) {
        boolean hideSystem = AppPreferences.getInstance().isHideSystemApps();
        if (hideSystem) {
            return list.stream()
                    .filter(appInfo -> !appInfo.system)
                    .collect(Collectors.toList());
        }
        return list;
    }

    protected void setCurrentAppList(List<AppInfo> newList) {
        this.restoredAppList = newList;
        this.currentAppList = filterList(newList);
        updateAppList(currentAppList);
    }

    protected void restoreAppList() {
        this.restoredAppList = initialAppList;
        this.currentAppList = filterList(initialAppList);
        updateAppList(currentAppList);
    }

    private void updateAppList(List<AppInfo> list) {
        adapterAppList.clear();
        adapterAppList.addAll(list);
        adapter.notifyDataSetChanged();
    }

    private void initSearchView(Menu menu) {
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
                    List<AppInfo> filteredList = currentAppList.stream()
                            .filter(appInfo -> {
                                String appName = appInfo.appName.toLowerCase();
                                String packageName = appInfo.packageName.toLowerCase();
                                String _text = text.toLowerCase();
                                return appName.contains(_text) || packageName.contains(_text);
                            })
                            .collect(Collectors.toList());
                    updateAppList(filteredList);
                }
                return false;
            }
        });
        UiUtils.setSearchIconColor(searchView, getContext());
    }
}
