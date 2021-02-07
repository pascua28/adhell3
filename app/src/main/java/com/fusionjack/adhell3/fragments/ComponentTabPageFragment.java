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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.IdRes;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.ActivityInfoAdapter;
import com.fusionjack.adhell3.adapter.ComponentAdapter;
import com.fusionjack.adhell3.adapter.ProviderInfoAdapter;
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
import com.fusionjack.adhell3.utils.rx.RxSingleIoBuilder;
import com.fusionjack.adhell3.viewmodel.AppComponentViewModel;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.functions.Action;

public class ComponentTabPageFragment extends Fragment {

    private static final String ARG_PAGE = "page";
    private static final String ARG_PACKAGE_NAME = "packageName";

    private static final int UNKNOWN_PAGE = -1;
    private static final int PERMISSIONS_PAGE = 0;
    private static final int ACTIVITIES_PAGE = 1;
    private static final int SERVICES_PAGE = 2;
    private static final int RECEIVERS_PAGE = 3;
    private static final int CONTENT_PROVIDERS_PAGE = 4;

    private int pageId;
    private String packageName;

    private List<IComponentInfo> adapterAppComponentList;
    private List<IComponentInfo> initialAppComponentList;
    private ComponentAdapter adapter;

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
        this.adapterAppComponentList = new ArrayList<>();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        return AppComponentPage.toAppComponentPage(pageId).map(page -> {
            View view = inflater.inflate(page.layoutId, container, false);

            Optional<ListView> listViewOpt = Optional.ofNullable(view.findViewById(page.listViewId));
            listViewOpt.ifPresent(listView -> {
                page.getAdapter(getContext(), adapterAppComponentList).ifPresent(adapter -> {
                    this.adapter = adapter;
                    listView.setAdapter(adapter);

                    boolean toggleEnabled = AppPreferences.getInstance().isAppComponentToggleEnabled();
                    if (toggleEnabled) {
                        listView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                            Action action = () -> page.toggleAppComponent(packageName, adapter.getItem(position));
                            new RxCompletableIoBuilder().async(Completable.fromAction(action));
                        });
                        listView.setOnItemLongClickListener((parent, view1, position, id) -> {
                            if (page.pageId == PERMISSIONS_PAGE) {
                                return true;
                            }
                            String componentName = adapter.getItem(position).getName();
                            Action action = () -> page.appendComponentNameToFile(componentName);
                            String message = "'" + adapter.getNamePart(componentName) + "' is inserted to file";
                            Runnable callback = () -> Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                            new RxCompletableIoBuilder()
                                    .showErrorAlert(getContext())
                                    .async(Completable.fromAction(action), callback);
                            return true;
                        });
                    }

                    Consumer<LiveData<List<AppPermission>>> callback = liveData -> {
                        safeGuardLiveData(() -> {
                            liveData.observe(getViewLifecycleOwner(), dbAppComponentList -> {
                                if (initialAppComponentList == null) {
                                    initialAppComponentList = page.combineAppComponentList(packageName, dbAppComponentList);
                                    adapterAppComponentList.addAll(initialAppComponentList);
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

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.appcomponent_menu, menu);
        initSearchView(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_enable_all) {
            enableAllComponents();
        }
        return super.onOptionsItemSelected(item);
    }

    private void enableAllComponents() {
        AppComponentPage.toAppComponentPage(pageId).ifPresent(page -> {
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_question, (ViewGroup) getView(), false);

            TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
            String titlePlaceholder = getResources().getString(R.string.enable_apps_component_dialog_title);
            titleTextView.setText(String.format(titlePlaceholder, page.getName()));

            TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
            String questionPlaceholder = getResources().getString(R.string.enable_apps_component_dialog_text);
            questionTextView.setText(String.format(questionPlaceholder, page.getName()));

            new AlertDialog.Builder(getContext())
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        Action action = () -> page.enableAppComponents(packageName);
                        new RxCompletableIoBuilder().async(Completable.fromAction(action));
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        });
    }

    private void initSearchView(Menu menu) {
        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setMaxWidth(Integer.MAX_VALUE);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String text) {
                if (text.isEmpty()) {
                    updateAppComponentList(initialAppComponentList);
                } else {
                    SingleOnSubscribe<List<IComponentInfo>> source = emitter -> {
                        List<IComponentInfo> filteredList = initialAppComponentList.stream()
                                .filter(componentInfo -> {
                                    String componentName = componentInfo.getName().toLowerCase();
                                    return componentName.contains(text.toLowerCase());
                                })
                                .collect(Collectors.toList());
                        emitter.onSuccess(filteredList);
                    };
                    new RxSingleIoBuilder().async(Single.create(source), list -> updateAppComponentList(list));
                }
                return false;
            }
        });
    }

    private void updateAppComponentList(List<IComponentInfo> list) {
        if (adapter != null) {
            adapterAppComponentList.clear();
            adapterAppComponentList.addAll(list);
            adapter.notifyDataSetChanged();
        }
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
                case ACTIVITIES_PAGE:
                    page = createActivityPage(pageId);
                    break;
                case SERVICES_PAGE:
                    page = createServicePage(pageId);
                    break;
                case RECEIVERS_PAGE:
                    page = createReceiverPage(pageId);
                    break;
                case CONTENT_PROVIDERS_PAGE:
                    page = createProviderPage(pageId);
            }
            return Optional.ofNullable(page);
        }

        private static AppComponentPage createPermissionPage(int pageId) {
            AppComponentPage page = new AppComponentPage(pageId);
            page.layoutId = R.layout.fragment_app_permission;
            page.listViewId = R.id.permissionInfoListView;
            return page;
        }

        private static AppComponentPage createActivityPage(int pageId) {
            AppComponentPage page = new AppComponentPage(pageId);
            page.layoutId = R.layout.fragment_app_activity;
            page.listViewId = R.id.activityInfoListView;
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

        private static AppComponentPage createProviderPage(int pageId) {
            AppComponentPage page = new AppComponentPage(pageId);
            page.layoutId = R.layout.fragment_app_provider;
            page.listViewId = R.id.providerInfoListView;
            return page;
        }

        String getName() {
            switch (pageId) {
                case PERMISSIONS_PAGE:
                    return "permissions";
                case ACTIVITIES_PAGE:
                    return "activities";
                case SERVICES_PAGE:
                    return "services";
                case RECEIVERS_PAGE:
                    return "receivers";
                case CONTENT_PROVIDERS_PAGE:
                    return "content providers";
            }
            return "";
        }

        Optional<ComponentAdapter> getAdapter(Context context, List<IComponentInfo> list) {
            ComponentAdapter adapter = null;
            switch (pageId) {
                case PERMISSIONS_PAGE:
                    adapter = new PermissionInfoAdapter(context, list);
                    break;
                case ACTIVITIES_PAGE:
                    adapter = new ActivityInfoAdapter(context, list);
                    break;
                case SERVICES_PAGE:
                    adapter = new ServiceInfoAdapter(context, list);
                    break;
                case RECEIVERS_PAGE:
                    adapter = new ReceiverInfoAdapter(context, list);
                    break;
                case CONTENT_PROVIDERS_PAGE:
                    adapter = new ProviderInfoAdapter(context, list);
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
                case ACTIVITIES_PAGE:
                    observable = viewModel.getActivities(packageName);
                    break;
                case SERVICES_PAGE:
                    observable = viewModel.getServices(packageName);
                    break;
                case RECEIVERS_PAGE:
                    observable = viewModel.getReceivers(packageName);
                    break;
                case CONTENT_PROVIDERS_PAGE:
                    observable = viewModel.getProviders(packageName);
            }
            return Optional.ofNullable(observable);
        }

        void appendComponentNameToFile(String componentName) throws IOException {
            switch (pageId) {
                case ACTIVITIES_PAGE:
                    AppComponentFactory.getInstance().appendActivityNameToFile(componentName);
                    break;
                case SERVICES_PAGE:
                    AppComponentFactory.getInstance().appendServiceNameToFile(componentName);
                    break;
                case RECEIVERS_PAGE:
                    AppComponentFactory.getInstance().appendReceiverNameToFile(componentName);
                    break;
                case CONTENT_PROVIDERS_PAGE:
                    AppComponentFactory.getInstance().appendProviderNameToFile(componentName);
                    break;
            }
        }

        void toggleAppComponent(String packageName, IComponentInfo info) {
            switch (pageId) {
                case PERMISSIONS_PAGE:
                    AppComponentFactory.getInstance().togglePermissionState(packageName, info.getName());
                    break;
                case ACTIVITIES_PAGE:
                    AppComponentFactory.getInstance().toggleActivityState(packageName, info.getName());
                    break;
                case SERVICES_PAGE:
                    AppComponentFactory.getInstance().toggleServiceState(packageName, info.getName());
                    break;
                case RECEIVERS_PAGE:
                    ReceiverInfo receiverInfo = (ReceiverInfo) info;
                    AppComponentFactory.getInstance().toggleReceiverState(packageName, receiverInfo.getName(), receiverInfo.getPermission());
                    break;
                case CONTENT_PROVIDERS_PAGE:
                    AppComponentFactory.getInstance().toggleProviderState(packageName, info.getName());
                    break;
            }
        }

        void enableAppComponents(String packageName) {
            switch (pageId) {
                case PERMISSIONS_PAGE:
                    AppComponentFactory.getInstance().enablePermissions(packageName);
                    break;
                case ACTIVITIES_PAGE:
                    AppComponentFactory.getInstance().enableActivities(packageName);
                    break;
                case SERVICES_PAGE:
                    AppComponentFactory.getInstance().enableServices(packageName);
                    break;
                case RECEIVERS_PAGE:
                    AppComponentFactory.getInstance().enableReceivers(packageName);
                    break;
                case CONTENT_PROVIDERS_PAGE:
                    AppComponentFactory.getInstance().enableProviders(packageName);
            }
        }

        List<IComponentInfo> combineAppComponentList(String packageName, List<AppPermission> appComponentList) {
            switch (pageId) {
                case PERMISSIONS_PAGE:
                    return AppComponent.combinePermissionsList(packageName, appComponentList);
                case ACTIVITIES_PAGE:
                    return AppComponent.combineActivitiesList(packageName, appComponentList);
                case SERVICES_PAGE:
                    return AppComponent.combineServicesList(packageName, appComponentList);
                case RECEIVERS_PAGE:
                    return AppComponent.combineReceiversList(packageName, appComponentList);
                case CONTENT_PROVIDERS_PAGE:
                    return AppComponent.combineProvidersList(packageName, appComponentList);
                default:
                    return Collections.emptyList();
            }
        }
    }

}
