package com.fusionjack.adhell3.fragments;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.ComponentAdapter;
import com.fusionjack.adhell3.adapter.PermissionInfoAdapter;
import com.fusionjack.adhell3.adapter.ReceiverInfoAdapter;
import com.fusionjack.adhell3.adapter.ServiceInfoAdapter;
import com.fusionjack.adhell3.model.AppComponent;
import com.fusionjack.adhell3.model.IComponentInfo;
import com.fusionjack.adhell3.model.PermissionInfo;
import com.fusionjack.adhell3.model.ReceiverInfo;
import com.fusionjack.adhell3.model.ServiceInfo;
import com.fusionjack.adhell3.utils.AppComponentFactory;
import com.fusionjack.adhell3.utils.AppPreferences;

import java.lang.ref.WeakReference;
import java.util.List;

public class ComponentTabPageFragment extends Fragment {

    private static final String ARG_PAGE = "page";
    private static final String ARG_PACKAGENAME = "packageName";
    private int page;
    private String packageName;
    private Context context;

    public static final int PERMISSIONS_PAGE = 0;
    public static final int SERVICES_PAGE = 1;
    public static final int RECEIVERS_PAGE = 2;

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
        this.page = getArguments().getInt(ARG_PAGE);
        this.packageName = getArguments().getString(ARG_PACKAGENAME);
        this.context = getContext();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.appcomponent_menu, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_enable_all:
                enableComponent();
        }
        return super.onOptionsItemSelected(item);
    }

    private void enableComponent() {
        new EnableComponentAsyncTask(page, packageName, context).execute();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        View view = null;
        boolean toggleEnabled = AppPreferences.getInstance().isAppComponentToggleEnabled();
        switch (page) {
            case PERMISSIONS_PAGE:
                view = inflater.inflate(R.layout.fragment_app_permission, container, false);
                ListView listView = view.findViewById(R.id.permissionInfoListView);
                if (listView != null && toggleEnabled) {
                    listView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                        PermissionInfoAdapter adapter = (PermissionInfoAdapter) adView.getAdapter();
                        new SetComponentAsyncTask(PERMISSIONS_PAGE, packageName, adapter.getItem(position), context).execute();
                    });
                }
                new CreateComponentAsyncTask(PERMISSIONS_PAGE, packageName, context).execute();
                break;

            case SERVICES_PAGE:
                view = inflater.inflate(R.layout.fragment_app_service, container, false);
                listView = view.findViewById(R.id.serviceInfoListView);
                if (listView != null && toggleEnabled) {
                    listView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                        ServiceInfoAdapter adapter = (ServiceInfoAdapter) adView.getAdapter();
                        new SetComponentAsyncTask(SERVICES_PAGE, packageName, adapter.getItem(position), context).execute();
                    });
                }
                new CreateComponentAsyncTask(SERVICES_PAGE, packageName, context).execute();
                break;

            case RECEIVERS_PAGE:
                view = inflater.inflate(R.layout.fragment_app_receiver, container, false);
                listView = view.findViewById(R.id.receiverInfoListView);
                if (listView != null && toggleEnabled) {
                    listView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                        ReceiverInfoAdapter adapter = (ReceiverInfoAdapter) adView.getAdapter();
                        new SetComponentAsyncTask(RECEIVERS_PAGE, packageName, adapter.getItem(position), context).execute();
                    });
                }
                new CreateComponentAsyncTask(RECEIVERS_PAGE, packageName, context).execute();
                break;
        }

        return view;
    }

    private static class EnableComponentAsyncTask extends AsyncTask<Void, Void, Void> {
        private int page;
        private String packageName;
        private WeakReference<Context> contextWeakReference;

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
        private int page;
        private String packageName;
        private IComponentInfo componentInfo;
        private WeakReference<Context> contextWeakReference;

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
        private int page;
        private String packageName;
        private WeakReference<Context> contextReference;

        CreateComponentAsyncTask(int page, String packageName, Context context) {
            this.page = page;
            this.packageName = packageName;
            this.contextReference = new WeakReference<>(context);
        }

        @Override
        protected List<IComponentInfo> doInBackground(Void... voids) {
            switch (page) {
                case PERMISSIONS_PAGE:
                    return AppComponent.getPermissions(packageName);
                case SERVICES_PAGE:
                    return AppComponent.getServices(packageName);
                case RECEIVERS_PAGE:
                    return AppComponent.getReceivers(packageName);
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
                }

                ListView listView = ((Activity) context).findViewById(listViewId);
                if (listView != null && adapter != null) {
                    listView.setAdapter(adapter);
                }
            }
        }
    }
}
