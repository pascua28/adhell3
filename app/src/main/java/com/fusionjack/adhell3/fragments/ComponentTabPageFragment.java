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
import android.view.animation.AlphaAnimation;
import android.widget.AdapterView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.fragment.app.Fragment;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.ActivityInfoAdapter;
import com.fusionjack.adhell3.adapter.ComponentAdapter;
import com.fusionjack.adhell3.adapter.ProviderInfoAdapter;
import com.fusionjack.adhell3.adapter.PermissionInfoAdapter;
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

import java.lang.ref.WeakReference;
import java.util.List;

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
                new CreateComponentAsyncTask(page, packageName, context, searchText).execute();
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
                if (toggleEnabled) {
                    fragmentAppPermissionBinding.permissionInfoListView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                        PermissionInfoAdapter adapter = (PermissionInfoAdapter) adView.getAdapter();
                        new SetComponentAsyncTask(page, packageName, adapter.getItem(position), context).execute();
                    });
                }
                new CreateComponentAsyncTask(page, packageName, context, searchText).execute();
                break;

            case SERVICES_PAGE:
                FragmentAppServiceBinding fragmentAppServiceBinding = FragmentAppServiceBinding.inflate(inflater);
                view = fragmentAppServiceBinding.getRoot();
                if (toggleEnabled) {
                    fragmentAppServiceBinding.serviceInfoListView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                        ServiceInfoAdapter adapter = (ServiceInfoAdapter) adView.getAdapter();
                        new SetComponentAsyncTask(page, packageName, adapter.getItem(position), context).execute();
                    });
                }
                new CreateComponentAsyncTask(page, packageName, context, searchText).execute();
                break;

            case RECEIVERS_PAGE:
                FragmentAppReceiverBinding fragmentAppReceiverBinding = FragmentAppReceiverBinding.inflate(inflater);
                view = fragmentAppReceiverBinding.getRoot();
                if (toggleEnabled) {
                    fragmentAppReceiverBinding.receiverInfoListView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                        ReceiverInfoAdapter adapter = (ReceiverInfoAdapter) adView.getAdapter();
                        new SetComponentAsyncTask(page, packageName, adapter.getItem(position), context).execute();
                    });
                }
                new CreateComponentAsyncTask(page, packageName, context, searchText).execute();
                break;

            case ACTIVITIES_PAGE:
                FragmentAppActivityBinding fragmentAppActivityBinding = FragmentAppActivityBinding.inflate(inflater);
                view = fragmentAppActivityBinding.getRoot();
                if (toggleEnabled) {
                    fragmentAppActivityBinding.activityInfoListView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                        ActivityInfoAdapter adapter = (ActivityInfoAdapter) adView.getAdapter();
                        new SetComponentAsyncTask(page, packageName, adapter.getItem(position), context).execute();
                    });
                }
                new CreateComponentAsyncTask(page, packageName, context, searchText).execute();
                break;

            case PROVIDER_PAGE:
                FragmentAppContentProviderBinding fragmentAppContentProviderBinding = FragmentAppContentProviderBinding.inflate(inflater);
                view = fragmentAppContentProviderBinding.getRoot();
                if (toggleEnabled) {
                    fragmentAppContentProviderBinding.providerInfoListView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                        ProviderInfoAdapter adapter = (ProviderInfoAdapter) adView.getAdapter();
                        new SetComponentAsyncTask(page, packageName, adapter.getItem(position), context).execute();
                    });
                }
                new CreateComponentAsyncTask(page, packageName, context, searchText).execute();
                break;

        }

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        // Close keyboard
        ViewCompat.getWindowInsetsController(view).hide(WindowInsetsCompat.Type.ime());
    }

    @Override
    public void onDestroyView() {
        view = null;
        super.onDestroyView();
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

    private static class CreateComponentAsyncTask extends AsyncTask<Void, Void, List<IComponentInfo>> {
        private final int page;
        private final String packageName;
        private final WeakReference<Context> contextReference;
        private final String searchText;

        CreateComponentAsyncTask(int page, String packageName, Context context, String searchText) {
            this.page = page;
            this.packageName = packageName;
            this.contextReference = new WeakReference<>(context);
            this.searchText = searchText;
        }

        @Override
        protected List<IComponentInfo> doInBackground(Void... voids) {
            switch (page) {
                case PERMISSIONS_PAGE:
                    return AppComponent.getPermissions(packageName, searchText);
                case SERVICES_PAGE:
                    return AppComponent.getServices(packageName, searchText);
                case RECEIVERS_PAGE:
                    return AppComponent.getReceivers(packageName, searchText);
                case ACTIVITIES_PAGE:
                    return AppComponent.getActivities(packageName, searchText);
                case PROVIDER_PAGE:
                    return AppComponent.getProviders(packageName, searchText);
            }
            return null;
        }

        @Override
        protected void onPostExecute(List<IComponentInfo> componentInfos) {
            Context context = contextReference.get();
            if (context != null) {
                ComponentAdapter adapter = null;
                int listViewId = -1;
                switch (page) {
                    case PERMISSIONS_PAGE:
                        listViewId = R.id.permissionInfoListView;
                        adapter = new PermissionInfoAdapter(context, componentInfos);
                        break;
                    case SERVICES_PAGE:
                        listViewId = R.id.serviceInfoListView;
                        adapter = new ServiceInfoAdapter(context, componentInfos);
                        break;
                    case RECEIVERS_PAGE:
                        listViewId = R.id.receiverInfoListView;
                        adapter = new ReceiverInfoAdapter(context, componentInfos);
                        break;
                    case ACTIVITIES_PAGE:
                        listViewId = R.id.activityInfoListView;
                        adapter = new ActivityInfoAdapter(context, componentInfos);
                        break;
                    case PROVIDER_PAGE:
                        listViewId = R.id.providerInfoListView;
                        adapter = new ProviderInfoAdapter(context, componentInfos);
                        break;
                }

                ListView listView = ((Activity) context).findViewById(listViewId);
                if (listView != null && adapter != null) {
                    listView.setAdapter(adapter);
                    if (listView.getVisibility() == View.GONE) {
                        AlphaAnimation animation = new AlphaAnimation(0f, 1f);
                        animation.setDuration(500);
                        animation.setStartOffset(50);
                        animation.setFillAfter(true);

                        listView.setVisibility(View.VISIBLE);
                        listView.startAnimation(animation);
                    }
                }
            }
        }
    }
}
