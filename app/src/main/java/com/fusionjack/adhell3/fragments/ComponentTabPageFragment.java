package com.fusionjack.adhell3.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.ComponentAdapter;
import com.fusionjack.adhell3.adapter.PermissionInfoAdapter;
import com.fusionjack.adhell3.adapter.ReceiverInfoAdapter;
import com.fusionjack.adhell3.adapter.ServiceInfoAdapter;
import com.fusionjack.adhell3.db.entity.AppPermission;
import com.fusionjack.adhell3.model.AppComponent;
import com.fusionjack.adhell3.model.IComponentInfo;
import com.fusionjack.adhell3.model.ReceiverInfo;
import com.fusionjack.adhell3.utils.AppComponentFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.utils.rx.RxCompletableIoBuilder;
import com.fusionjack.adhell3.utils.rx.RxSingleComputationBuilder;
import com.fusionjack.adhell3.viewmodel.AppComponentViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.functions.Action;

public class ComponentTabPageFragment extends Fragment {

    private static final String ARG_PAGE = "page";
    private static final String ARG_PACKAGE_NAME = "packageName";
    private int pageId;
    private String packageName;

    private static final int UNKNOWN_PAGE = -1;
    private static final int PERMISSIONS_PAGE = 0;
    private static final int SERVICES_PAGE = 1;
    private static final int RECEIVERS_PAGE = 2;

    public static ComponentTabPageFragment newInstance(int page, String packageName) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        args.putString(ARG_PACKAGE_NAME, packageName);
        ComponentTabPageFragment fragment = new ComponentTabPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.pageId = Optional.ofNullable(getArguments()).map(bundle -> bundle.getInt(ARG_PAGE)).orElse(UNKNOWN_PAGE);
        this.packageName = getArguments().getString(ARG_PACKAGE_NAME);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.appcomponent_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_enable_all) {
            enableComponent();
        }
        return super.onOptionsItemSelected(item);
    }

    private void enableComponent() {
        Action action = () -> AppComponentPage.toAppComponentPage(pageId).ifPresent(page -> page.enableAppComponents(packageName));
        new RxCompletableIoBuilder().async(Completable.fromAction(action));
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        return AppComponentPage.toAppComponentPage(pageId).map(page -> {
            View view = inflater.inflate(page.layoutId, container, false);

            Optional<ListView> listViewOpt = Optional.ofNullable(view.findViewById(page.listViewId));
            listViewOpt.ifPresent(listView -> {
                List<IComponentInfo> list = new ArrayList<>();
                page.getAdapter(getContext(), list).ifPresent(adapter -> {
                    listView.setAdapter(adapter);

                    boolean toggleEnabled = AppPreferences.getInstance().isAppComponentToggleEnabled();
                    if (toggleEnabled) {
                        listView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                            Action action = () -> page.toggleAppComponent(packageName, adapter.getItem(position));
                            new RxCompletableIoBuilder().async(Completable.fromAction(action));
                        });
                    }

                    Consumer<LiveData<List<AppPermission>>> callback = liveData -> {
                        safeGuardLiveData(() -> {
                            liveData.observe(getViewLifecycleOwner(), appComponentList -> {
                                if (list.isEmpty()) {
                                    list.addAll(page.combineAppComponentList(packageName, appComponentList));
                                }
                                adapter.notifyDataSetChanged();
                            });
                        });
                    };

                    AppComponentViewModel viewModel = new ViewModelProvider(this).get(AppComponentViewModel.class);
                    page.getAppComponentList(viewModel, packageName).ifPresent(action ->
                            new RxSingleComputationBuilder().async(action, callback)
                    );
                });
            });

            return view;
        }).orElse(null);
    }

    private void safeGuardLiveData(Runnable action) {
        if (getView() == null) {
            LogUtils.error("View is null");
            return;
        }
        action.run();
    }

    private static class AppComponentPage {
        @LayoutRes private int layoutId;
        @IdRes private int listViewId;
        private final int pageId;

        AppComponentPage(int pageId) {
            this.pageId = pageId;
        }

        static Optional<AppComponentPage> toAppComponentPage(int pageId) {
            AppComponentPage page = null;
            switch (pageId) {
                case PERMISSIONS_PAGE:
                    page = createPermissionPage(pageId);
                    break;
                case SERVICES_PAGE:
                    page = createServicePage(pageId);
                    break;
                case RECEIVERS_PAGE:
                    page = createReceiverPage(pageId);
                    break;
            }
            return Optional.ofNullable(page);
        }

        private static AppComponentPage createPermissionPage(int pageId) {
            AppComponentPage page = new AppComponentPage(pageId);
            page.layoutId = R.layout.fragment_app_permission;
            page.listViewId = R.id.permissionInfoListView;
            return page;
        }

        private static AppComponentPage createServicePage(int pageId) {
            AppComponentPage page = new AppComponentPage(pageId);
            page.layoutId = R.layout.fragment_app_service;
            page.listViewId = R.id.serviceInfoListView;
            return page;
        }

        private static AppComponentPage createReceiverPage(int pageId) {
            AppComponentPage page = new AppComponentPage(pageId);
            page.layoutId = R.layout.fragment_app_receiver;
            page.listViewId = R.id.receiverInfoListView;
            return page;
        }

        Optional<ComponentAdapter> getAdapter(Context context, List<IComponentInfo> list) {
            ComponentAdapter adapter = null;
            switch (pageId) {
                case PERMISSIONS_PAGE:
                    adapter = new PermissionInfoAdapter(context, list);
                    break;
                case SERVICES_PAGE:
                    adapter = new ServiceInfoAdapter(context, list);
                    break;
                case RECEIVERS_PAGE:
                    adapter = new ReceiverInfoAdapter(context, list);
                    break;
            }
            return Optional.ofNullable(adapter);
        }

        Optional<Single<LiveData<List<AppPermission>>>> getAppComponentList(AppComponentViewModel viewModel, String packageName) {
            Single<LiveData<List<AppPermission>>> observable = null;
            switch (pageId) {
                case PERMISSIONS_PAGE:
                    observable = viewModel.getPermissions(packageName);
                    break;
                case SERVICES_PAGE:
                    observable = viewModel.getServices(packageName);
                    break;
                case RECEIVERS_PAGE:
                    observable = viewModel.getReceivers(packageName);
                    break;
            }
            return Optional.ofNullable(observable);
        }

        void toggleAppComponent(String packageName, IComponentInfo info) {
            switch (pageId) {
                case PERMISSIONS_PAGE:
                    AppComponentFactory.getInstance().togglePermissionState(packageName, info.getName());
                    break;
                case SERVICES_PAGE:
                    AppComponentFactory.getInstance().toggleServiceState(packageName, info.getName());
                    break;
                case RECEIVERS_PAGE:
                    ReceiverInfo receiverInfo = (ReceiverInfo) info;
                    AppComponentFactory.getInstance().toggleReceiverState(packageName, receiverInfo.getName(), receiverInfo.getPermission());
                    break;
            }
        }

        void enableAppComponents(String packageName) {
            switch (pageId) {
                case PERMISSIONS_PAGE:
                    AppComponentFactory.getInstance().enablePermissions(packageName);
                    break;
                case SERVICES_PAGE:
                    AppComponentFactory.getInstance().enableServices(packageName);
                    break;
                case RECEIVERS_PAGE:
                    AppComponentFactory.getInstance().enableReceivers(packageName);
                    break;
            }
        }

        List<IComponentInfo> combineAppComponentList(String packageName, List<AppPermission> appComponentList) {
            switch (pageId) {
                case PERMISSIONS_PAGE:
                    return AppComponent.combinePermissionsList(packageName, appComponentList);
                case SERVICES_PAGE:
                    return AppComponent.combineServicesList(packageName, appComponentList);
                case RECEIVERS_PAGE:
                    return AppComponent.combineReceiversList(packageName, appComponentList);
                default:
                    return Collections.emptyList();
            }
        }
    }

}
