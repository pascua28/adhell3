package com.fusionjack.adhell3.fragments;

import android.content.ComponentName;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.ActivityDisabledInfoAdapter;
import com.fusionjack.adhell3.adapter.ComponentDisabledAdapter;
import com.fusionjack.adhell3.adapter.ContentProviderDisabledInfoAdapter;
import com.fusionjack.adhell3.adapter.PermissionDisabledInfoAdapter;
import com.fusionjack.adhell3.adapter.ReceiverDisabledInfoAdapter;
import com.fusionjack.adhell3.adapter.ServiceDisabledInfoAdapter;
import com.fusionjack.adhell3.databinding.DialogQuestionBinding;
import com.fusionjack.adhell3.databinding.FragmentAppActivityDisabledBinding;
import com.fusionjack.adhell3.databinding.FragmentAppContentProviderDisabledBinding;
import com.fusionjack.adhell3.databinding.FragmentAppPermissionDisabledBinding;
import com.fusionjack.adhell3.databinding.FragmentAppReceiverDisabledBinding;
import com.fusionjack.adhell3.databinding.FragmentAppServiceDisabledBinding;
import com.fusionjack.adhell3.model.ActivityInfo;
import com.fusionjack.adhell3.model.AppComponentDisabled;
import com.fusionjack.adhell3.model.IComponentInfo;
import com.fusionjack.adhell3.model.PermissionInfo;
import com.fusionjack.adhell3.model.ProviderInfo;
import com.fusionjack.adhell3.model.ReceiverInfo;
import com.fusionjack.adhell3.model.ServiceInfo;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppCache;
import com.fusionjack.adhell3.utils.LogUtils;
import com.samsung.android.knox.application.ApplicationPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.core.SingleOnSubscribe;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ComponentDisabledTabPageFragment extends Fragment {

    private static final int PERMISSIONS_PAGE = 0;
    private static final int SERVICES_PAGE = 1;
    private static final int RECEIVERS_PAGE = 2;
    private static final int ACTIVITIES_PAGE = 3;
    private static final int PROVIDER_PAGE = 4;
    private static final String ARG_PAGE = "page";
    private int page;
    private String searchText;
    private Map<String, Drawable> appIcons;
    private Map<String, String> appNames;
    private Parcelable state;
    private View view;
    private SearchView searchView;
    private int listViewID;

    private MutableLiveData<Boolean> _loadingVisibility;

    public static ComponentDisabledTabPageFragment newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        ComponentDisabledTabPageFragment fragment = new ComponentDisabledTabPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.page = getArguments().getInt(ARG_PAGE);
        }
        if (this.searchText == null) this.searchText = "";

        AppCache appCache = AppCache.getInstance(null);
        appIcons = appCache.getIcons();
        appNames = appCache.getNames();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.search_menu, menu);

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
                if (searchText.length() > 0) state = null;
                CreateDisabledComponentRxTask();

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
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        view = null;
        switch (page) {
            case PERMISSIONS_PAGE:
                FragmentAppPermissionDisabledBinding fragmentAppPermissionDisabledBinding = FragmentAppPermissionDisabledBinding.inflate(inflater);
                view = fragmentAppPermissionDisabledBinding.getRoot();
                listViewID = fragmentAppPermissionDisabledBinding.permissionExpandableListView.getId();
                break;

            case SERVICES_PAGE:
                FragmentAppServiceDisabledBinding fragmentAppServiceDisabledBinding = FragmentAppServiceDisabledBinding.inflate(inflater);
                view = fragmentAppServiceDisabledBinding.getRoot();
                listViewID = fragmentAppServiceDisabledBinding.serviceExpandableListView.getId();
                break;

            case RECEIVERS_PAGE:
                FragmentAppReceiverDisabledBinding fragmentAppReceiverDisabledBinding = FragmentAppReceiverDisabledBinding.inflate(inflater);
                view = fragmentAppReceiverDisabledBinding.getRoot();
                listViewID = fragmentAppReceiverDisabledBinding.receiverExpandableListView.getId();
                break;

            case ACTIVITIES_PAGE:
                FragmentAppActivityDisabledBinding fragmentAppActivityDisabledBinding = FragmentAppActivityDisabledBinding.inflate(inflater);
                view = fragmentAppActivityDisabledBinding.getRoot();
                listViewID = fragmentAppActivityDisabledBinding.activityExpandableListView.getId();
                break;

            case PROVIDER_PAGE:
                FragmentAppContentProviderDisabledBinding fragmentAppContentProviderDisabledBinding = FragmentAppContentProviderDisabledBinding.inflate(inflater);
                view = fragmentAppContentProviderDisabledBinding.getRoot();
                listViewID = fragmentAppContentProviderDisabledBinding.providerExpandableListView.getId();
                break;
        }
        CreateDisabledComponentRxTask();

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getLoadingBarVisibility().observe(
                getViewLifecycleOwner(),
                isVisible -> {
                    ProgressBar loadingBar = view.findViewById(R.id.loadingBar);
                    ExpandableListView listView = view.findViewById(listViewID);

                    if (isVisible) {
                        loadingBar.setVisibility(View.VISIBLE);
                        listView.setVisibility(View.GONE);
                    } else {
                        loadingBar.setVisibility(View.GONE);
                        listView.setVisibility(View.VISIBLE);
                    }
                }
        );
    }

    @Override
    public void onPause() {
        super.onPause();
        ExpandableListView listView = view.findViewById(listViewID);
        state = listView.onSaveInstanceState();
        // Close keyboard
        WindowInsetsControllerCompat windowInsetsControllerCompat = ViewCompat.getWindowInsetsController(view);
        if (windowInsetsControllerCompat != null) {
            windowInsetsControllerCompat.hide(WindowInsetsCompat.Type.ime());
        }
    }

    @Override
    public void onDestroyView() {
        view = null;
        super.onDestroyView();
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

    private void CreateDisabledComponentRxTask() {
        Single.create((SingleOnSubscribe<Map<String, List<IComponentInfo>>>) emitter -> {
            List<IComponentInfo> resultList = new ArrayList<>();
            switch (page) {
                case PERMISSIONS_PAGE:
                    resultList = AppComponentDisabled.getDisabledPermissions(searchText);
                    break;
                case SERVICES_PAGE:
                    resultList = AppComponentDisabled.getDisabledServices(searchText);
                    break;
                case RECEIVERS_PAGE:
                    resultList = AppComponentDisabled.getDisabledReceivers(searchText);
                    break;
                case ACTIVITIES_PAGE:
                    resultList = AppComponentDisabled.getDisabledActivities(searchText);
                    break;
                case PROVIDER_PAGE:
                    resultList = AppComponentDisabled.getDisabledProviders(searchText);
                    break;
            }
            Map<String, List<IComponentInfo>> componentMap = new TreeMap<>(String::compareToIgnoreCase);
            for (IComponentInfo componentInfo : resultList) {
                String packageName = componentInfo.getPackageName();
                String appName = appNames.get(packageName);
                if (appName == null || appName.length() <= 0) appName = packageName;
                List<IComponentInfo> tempList;
                if (componentMap.get(appName) != null && componentMap.size() > 0)
                    tempList = componentMap.get(appName);
                else
                    tempList = new ArrayList<>();
                if (tempList != null) {
                    tempList.add(componentInfo);
                }
                componentMap.put(appName, tempList);
            }
            emitter.onSuccess(componentMap);
        })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Map<String, List<IComponentInfo>>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        updateLoadingBarVisibility(true);
                    }

                    @Override
                    public void onSuccess(@NonNull Map<String, List<IComponentInfo>> componentInfos) {
                        Context context = getContext();
                        if (context != null) {
                            ComponentDisabledAdapter adapter = null;
                            int listViewId = -1;
                            switch (page) {
                                case PERMISSIONS_PAGE:
                                    listViewId = R.id.permissionExpandableListView;
                                    adapter = new PermissionDisabledInfoAdapter(context, componentInfos, appIcons);
                                    break;
                                case SERVICES_PAGE:
                                    listViewId = R.id.serviceExpandableListView;
                                    adapter = new ServiceDisabledInfoAdapter(context, componentInfos, appIcons);
                                    break;
                                case RECEIVERS_PAGE:
                                    listViewId = R.id.receiverExpandableListView;
                                    adapter = new ReceiverDisabledInfoAdapter(context, componentInfos, appIcons);
                                    break;
                                case ACTIVITIES_PAGE:
                                    listViewId = R.id.activityExpandableListView;
                                    adapter = new ActivityDisabledInfoAdapter(context, componentInfos, appIcons);
                                    break;
                                case PROVIDER_PAGE:
                                    listViewId = R.id.providerExpandableListView;
                                    adapter = new ContentProviderDisabledInfoAdapter(context, componentInfos, appIcons);
                                    break;
                            }

                            ExpandableListView listView = view.findViewById(listViewId);
                            if (listView != null && adapter != null) {
                                listView.setAdapter(adapter);
                                listView.setOnChildClickListener((ExpandableListView parent, View view, int groupPosition, int childPosition, long id) -> {
                                    DialogQuestionBinding dialogQuestionBinding = DialogQuestionBinding.inflate(LayoutInflater.from(context));
                                    dialogQuestionBinding.titleTextView.setText(R.string.enable_app_component_dialog_title);
                                    dialogQuestionBinding.questionTextView.setText(R.string.enable_app_component_dialog_text);
                                    List<String> groupList = new ArrayList<>(componentInfos.keySet());

                                    final String packageName;
                                    String packageNameTmp = "";
                                    final String compName;
                                    String compNameTmp = "";
                                    IComponentInfo component = null;

                                    List<IComponentInfo> compList = componentInfos.get(groupList.get(groupPosition));
                                    if (compList != null) {
                                        packageNameTmp = compList.get(childPosition).getPackageName();
                                        component = compList.get(childPosition);
                                    }

                                    packageName = packageNameTmp;

                                    if (component instanceof PermissionInfo) {
                                        PermissionInfo permissionInfo = (PermissionInfo) component;
                                        compNameTmp = permissionInfo.getName();
                                    }
                                    if (component instanceof ServiceInfo) {
                                        ServiceInfo serviceInfo = (ServiceInfo) component;
                                        compNameTmp = serviceInfo.getName();
                                    }
                                    if (component instanceof ReceiverInfo) {
                                        ReceiverInfo receiverInfo = (ReceiverInfo) component;
                                        compNameTmp = receiverInfo.getName();
                                    }
                                    if (component instanceof ActivityInfo) {
                                        ActivityInfo activityInfo = (ActivityInfo) component;
                                        compNameTmp = activityInfo.getName();
                                    }
                                    if (component instanceof ProviderInfo) {
                                        ProviderInfo providerInfo = (ProviderInfo) component;
                                        compNameTmp = providerInfo.getName();
                                    }
                                    compName = compNameTmp;

                                    AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                                            .setView(dialogQuestionBinding.getRoot())
                                            .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                                                ApplicationPolicy appPolicy = AdhellFactory.getInstance().getAppPolicy();
                                                if (appPolicy != null) {
                                                    EnableAppComponentRxTask(packageName, compName);
                                                }
                                            })
                                            .setNegativeButton(android.R.string.no, null)
                                            .create();

                                    alertDialog.show();

                                    return false;
                                });

                                if (state != null) {
                                    listView.onRestoreInstanceState(state);
                                    adapter.notifyDataSetChanged();
                                }
                            }
                        }
                        updateLoadingBarVisibility(false);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        LogUtils.error(e.getMessage(), e);
                        updateLoadingBarVisibility(false);
                    }
                });
    }

    private void EnableAppComponentRxTask(String packageName, String compName) {
        Single.create((SingleOnSubscribe<Boolean>)emitter -> {
            ApplicationPolicy appPolicy = AdhellFactory.getInstance().getAppPolicy();
            ComponentName componentName = new ComponentName(packageName, compName);
            boolean result = false;
            if (appPolicy != null) {
                result = appPolicy.setApplicationComponentState(componentName, true);
                if (result) {
                    AdhellFactory.getInstance().getAppDatabase().appPermissionDao().delete(packageName, compName);
                }
            }
            emitter.onSuccess(result);
        })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Boolean>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull Boolean result) {
                        if (result) {
                            CreateDisabledComponentRxTask();
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        e.printStackTrace();
                    }
                });
    }
}
