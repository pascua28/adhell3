package com.fusionjack.adhell3.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.ActivityInfoAdapter;
import com.fusionjack.adhell3.adapter.ComponentAdapter;
import com.fusionjack.adhell3.adapter.PermissionInfoAdapter;
import com.fusionjack.adhell3.adapter.ProviderInfoAdapter;
import com.fusionjack.adhell3.adapter.ReceiverInfoAdapter;
import com.fusionjack.adhell3.adapter.ServiceInfoAdapter;
import com.fusionjack.adhell3.databinding.FragmentAppActivityBinding;
import com.fusionjack.adhell3.databinding.FragmentAppContentProviderBinding;
import com.fusionjack.adhell3.databinding.FragmentAppPermissionBinding;
import com.fusionjack.adhell3.databinding.FragmentAppReceiverBinding;
import com.fusionjack.adhell3.databinding.FragmentAppServiceBinding;
import com.fusionjack.adhell3.model.ActivityInfo;
import com.fusionjack.adhell3.model.AppComponent;
import com.fusionjack.adhell3.model.IComponentInfo;
import com.fusionjack.adhell3.model.PermissionInfo;
import com.fusionjack.adhell3.model.ProviderInfo;
import com.fusionjack.adhell3.model.ReceiverInfo;
import com.fusionjack.adhell3.model.ServiceInfo;
import com.fusionjack.adhell3.utils.AppComponentFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.LogUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.core.SingleOnSubscribe;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ComponentTabPageFragment extends Fragment {
    private static final int PERMISSIONS_PAGE = 0;
    private static final int SERVICES_PAGE = 1;
    private static final int RECEIVERS_PAGE = 2;
    private static final int ACTIVITIES_PAGE = 3;
    private static final int PROVIDER_PAGE = 4;
    private static final String ARG_PAGE = "page";
    private static final String ARG_PACKAGENAME = "packageName";
    private int page;
    private String packageName;
    private Context context;
    private SearchView searchView;
    private String searchText;
    private View view;
    private int listViewID;

    private MutableLiveData<Boolean> _loadingVisibility;

    public static ComponentTabPageFragment newInstance(int page, String packageName) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        args.putString(ARG_PACKAGENAME, packageName);
        ComponentTabPageFragment fragment = new ComponentTabPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            this.page = getArguments().getInt(ARG_PAGE);
            this.packageName = getArguments().getString(ARG_PACKAGENAME);
        }
        this.context = getContext();
        if (this.searchText == null) this.searchText = "";
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.appcomponent_menu, menu);

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
                CreateComponentRxTask();
                return false;
            }
        });
    }

    @Override
    public void onDestroyOptionsMenu() {
        searchView.setOnQueryTextListener(null);
        searchView = null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_enable_all) {
            enableComponent();
        }
        return super.onOptionsItemSelected(item);
    }

    private void enableComponent() {
        new EnableComponentAsyncTask(page, packageName, context).execute();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        view = null;
        boolean toggleEnabled = AppPreferences.getInstance().isAppComponentToggleEnabled();
        switch (page) {
            case PERMISSIONS_PAGE:
                FragmentAppPermissionBinding fragmentAppPermissionBinding = FragmentAppPermissionBinding.inflate(inflater);
                view = fragmentAppPermissionBinding.getRoot();
                listViewID = fragmentAppPermissionBinding.permissionInfoListView.getId();
                if (toggleEnabled) {
                    fragmentAppPermissionBinding.permissionInfoListView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                        PermissionInfoAdapter adapter = (PermissionInfoAdapter) adView.getAdapter();
                        new SetComponentAsyncTask(page, packageName, adapter.getItem(position), context).execute();
                    });
                }
                CreateComponentRxTask();
                break;

            case SERVICES_PAGE:
                FragmentAppServiceBinding fragmentAppServiceBinding = FragmentAppServiceBinding.inflate(inflater);
                view = fragmentAppServiceBinding.getRoot();
                listViewID = fragmentAppServiceBinding.serviceInfoListView.getId();
                if (toggleEnabled) {
                    fragmentAppServiceBinding.serviceInfoListView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                        ServiceInfoAdapter adapter = (ServiceInfoAdapter) adView.getAdapter();
                        new SetComponentAsyncTask(page, packageName, adapter.getItem(position), context).execute();
                    });
                }
                CreateComponentRxTask();
                break;

            case RECEIVERS_PAGE:
                FragmentAppReceiverBinding fragmentAppReceiverBinding = FragmentAppReceiverBinding.inflate(inflater);
                view = fragmentAppReceiverBinding.getRoot();
                listViewID = fragmentAppReceiverBinding.receiverInfoListView.getId();
                if (toggleEnabled) {
                    fragmentAppReceiverBinding.receiverInfoListView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                        ReceiverInfoAdapter adapter = (ReceiverInfoAdapter) adView.getAdapter();
                        new SetComponentAsyncTask(page, packageName, adapter.getItem(position), context).execute();
                    });
                }
                CreateComponentRxTask();
                break;

            case ACTIVITIES_PAGE:
                FragmentAppActivityBinding fragmentAppActivityBinding = FragmentAppActivityBinding.inflate(inflater);
                view = fragmentAppActivityBinding.getRoot();
                listViewID = fragmentAppActivityBinding.activityInfoListView.getId();
                if (toggleEnabled) {
                    fragmentAppActivityBinding.activityInfoListView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                        ActivityInfoAdapter adapter = (ActivityInfoAdapter) adView.getAdapter();
                        new SetComponentAsyncTask(page, packageName, adapter.getItem(position), context).execute();
                    });
                }
                CreateComponentRxTask();
                break;

            case PROVIDER_PAGE:
                FragmentAppContentProviderBinding fragmentAppContentProviderBinding = FragmentAppContentProviderBinding.inflate(inflater);
                view = fragmentAppContentProviderBinding.getRoot();
                listViewID = fragmentAppContentProviderBinding.providerInfoListView.getId();
                if (toggleEnabled) {
                    fragmentAppContentProviderBinding.providerInfoListView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                        ProviderInfoAdapter adapter = (ProviderInfoAdapter) adView.getAdapter();
                        new SetComponentAsyncTask(page, packageName, adapter.getItem(position), context).execute();
                    });
                }
                CreateComponentRxTask();
                break;
        }
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getLoadingBarVisibility().observe(
                getViewLifecycleOwner(),
                isVisible -> {
                    ProgressBar loadingBar = view.findViewById(R.id.loadingBar);
                    ListView listView = view.findViewById(listViewID);

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

    private void CreateComponentRxTask() {
        Single.create((SingleOnSubscribe<List<IComponentInfo>>) emitter -> {
            List<IComponentInfo> resultList = new ArrayList<>();
            switch (page) {
                case PERMISSIONS_PAGE:
                    resultList = AppComponent.getPermissions(packageName, searchText);
                    break;
                case SERVICES_PAGE:
                    resultList = AppComponent.getServices(packageName, searchText);
                    break;
                case RECEIVERS_PAGE:
                    resultList = AppComponent.getReceivers(packageName, searchText);
                    break;
                case ACTIVITIES_PAGE:
                    resultList = AppComponent.getActivities(packageName, searchText);
                    break;
                case PROVIDER_PAGE:
                    resultList = AppComponent.getProviders(packageName, searchText);
                    break;
            }
            emitter.onSuccess(resultList);
        })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<IComponentInfo>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        updateLoadingBarVisibility(true);
                    }

                    @Override
                    public void onSuccess(@NonNull List<IComponentInfo> componentInfos) {
                        if (context != null) {
                            ComponentAdapter adapter = null;
                            switch (page) {
                                case PERMISSIONS_PAGE:
                                    adapter = new PermissionInfoAdapter(context, componentInfos);
                                    break;
                                case SERVICES_PAGE:
                                    adapter = new ServiceInfoAdapter(context, componentInfos);
                                    break;
                                case RECEIVERS_PAGE:
                                    adapter = new ReceiverInfoAdapter(context, componentInfos);
                                    break;
                                case ACTIVITIES_PAGE:
                                    adapter = new ActivityInfoAdapter(context, componentInfos);
                                    break;
                                case PROVIDER_PAGE:
                                    adapter = new ProviderInfoAdapter(context, componentInfos);
                                    break;
                            }
                            ListView listView = view.findViewById(listViewID);
                            if (listView != null && adapter != null) {
                                listView.setAdapter(adapter);
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

    private static class EnableComponentAsyncTask extends AsyncTask<Void, Void, Void> {
        private final int page;
        private final String packageName;
        private final WeakReference<Context> contextWeakReference;

        EnableComponentAsyncTask(int page, String packageName, Context context) {
            this.page = page;
            this.packageName = packageName;
            this.contextWeakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            switch (page) {
                case PERMISSIONS_PAGE:
                    AppComponentFactory.getInstance().enablePermissions(packageName);
                    break;
                case SERVICES_PAGE:
                    AppComponentFactory.getInstance().enableServices(packageName);
                    break;
                case RECEIVERS_PAGE:
                    AppComponentFactory.getInstance().enableReceivers(packageName);
                    break;
                case ACTIVITIES_PAGE:
                    AppComponentFactory.getInstance().enableActivities(packageName);
                    break;
                case PROVIDER_PAGE:
                    AppComponentFactory.getInstance().enableProviders(packageName);
                    break;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Context context = contextWeakReference.get();
            if (context != null) {
                int listViewId = -1;
                switch (page) {
                    case PERMISSIONS_PAGE:
                        listViewId = R.id.permissionInfoListView;
                        break;
                    case SERVICES_PAGE:
                        listViewId = R.id.serviceInfoListView;
                        break;
                    case RECEIVERS_PAGE:
                        listViewId = R.id.receiverInfoListView;
                        break;
                    case ACTIVITIES_PAGE:
                        listViewId = R.id.activityInfoListView;
                        break;
                    case PROVIDER_PAGE:
                        listViewId = R.id.providerInfoListView;
                        break;
                }

                ListView listView = ((Activity) context).findViewById(listViewId);
                if (listView != null) {
                    if (listView.getAdapter() instanceof ComponentAdapter) {
                        ((ComponentAdapter) listView.getAdapter()).notifyDataSetChanged();
                    }
                }
            }
        }
    }

    private static class SetComponentAsyncTask extends AsyncTask<Void, Void, Void> {
        private final int page;
        private final String packageName;
        private final IComponentInfo componentInfo;
        private final WeakReference<Context> contextWeakReference;

        SetComponentAsyncTask(int page, String packageName, IComponentInfo componentInfo, Context context) {
            this.page = page;
            this.packageName = packageName;
            this.componentInfo = componentInfo;
            this.contextWeakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            switch (page) {
                case PERMISSIONS_PAGE:
                    PermissionInfo permissionInfo = (PermissionInfo) componentInfo;
                    String permissionName = permissionInfo.getName();
                    AppComponentFactory.getInstance().togglePermissionState(packageName, permissionName);
                    break;

                case SERVICES_PAGE:
                    ServiceInfo serviceInfo = (ServiceInfo) componentInfo;
                    String serviceName = serviceInfo.getName();
                    AppComponentFactory.getInstance().toggleServiceState(packageName, serviceName);
                    break;

                case RECEIVERS_PAGE:
                    ReceiverInfo receiverInfo = (ReceiverInfo) componentInfo;
                    String receiverName = receiverInfo.getName();
                    String receiverPermission = receiverInfo.getPermission();
                    AppComponentFactory.getInstance().toggleReceiverState(packageName, receiverName, receiverPermission);
                    break;

                case ACTIVITIES_PAGE:
                    ActivityInfo activityInfo = (ActivityInfo) componentInfo;
                    String activityName = activityInfo.getName();
                    AppComponentFactory.getInstance().toggleActivityState(packageName, activityName);
                    break;

                case PROVIDER_PAGE:
                    ProviderInfo providerInfo = (ProviderInfo) componentInfo;
                    String providerName = providerInfo.getName();
                    AppComponentFactory.getInstance().toggleProviderState(packageName, providerName);
                    break;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Context context = contextWeakReference.get();
            if (context != null) {
                int listViewId = -1;
                switch (page) {
                    case PERMISSIONS_PAGE:
                        listViewId = R.id.permissionInfoListView;
                        break;
                    case SERVICES_PAGE:
                        listViewId = R.id.serviceInfoListView;
                        break;
                    case RECEIVERS_PAGE:
                        listViewId = R.id.receiverInfoListView;
                        break;
                    case ACTIVITIES_PAGE:
                        listViewId = R.id.activityInfoListView;
                        break;
                    case PROVIDER_PAGE:
                        listViewId = R.id.providerInfoListView;
                        break;
                }

                ListView listView = ((Activity) context).findViewById(listViewId);
                if (listView != null) {
                    if (listView.getAdapter() instanceof ComponentAdapter) {
                        ((ComponentAdapter) listView.getAdapter()).notifyDataSetChanged();
                    }
                }
            }
        }
    }
}
