package com.fusionjack.adhell3.fragments;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ExpandableListView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.ComponentDisabledAdapter;
import com.fusionjack.adhell3.adapter.PermissionDisabledInfoAdapter;
import com.fusionjack.adhell3.adapter.ReceiverDisabledInfoAdapter;
import com.fusionjack.adhell3.adapter.ServiceDisabledInfoAdapter;
import com.fusionjack.adhell3.model.AppComponentDisabled;
import com.fusionjack.adhell3.model.IComponentInfo;
import com.fusionjack.adhell3.utils.AppCache;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class ComponentDisabledTabPageFragment extends Fragment {

    private static final int PERMISSIONS_PAGE = 0;
    private static final int SERVICES_PAGE = 1;
    private static final int RECEIVERS_PAGE = 2;
    private static final String ARG_PAGE = "page";
    private int page;
    private Context context;
    private String searchText;
    private Map<String, Drawable> appIcons;
    private Map<String, String> appNames;
    private Parcelable state;
    private int listViewID;

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
        this.page = Objects.requireNonNull(getArguments()).getInt(ARG_PAGE);
        this.context = getContext();
        if (this.searchText == null) this.searchText = "";

        setRetainInstance(true);

        appIcons = AppCache.getInstance(context, null).getIcons();
        appNames = AppCache.reload(context, null).getNames();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.search_menu, menu);

        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
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
                new CreateComponentAsyncTask(page, context, searchText, appIcons, appNames, state).execute();

                return false;
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        View view = null;
        switch (page) {
            case PERMISSIONS_PAGE:
                view = inflater.inflate(R.layout.fragment_app_permission_disabled, container, false);
                listViewID = R.id.permissionExpandableListView;
                break;

            case SERVICES_PAGE:
                view = inflater.inflate(R.layout.fragment_app_service_disabled, container, false);
                listViewID = R.id.serviceExpandableListView;
                break;

            case RECEIVERS_PAGE:
                view = inflater.inflate(R.layout.fragment_app_receiver_disabled, container, false);
                listViewID = R.id.receiverExpandableListView;
                break;
        }
        new CreateComponentAsyncTask(page, context, searchText, appIcons, appNames, state).execute();

        return view;
    }

    @Override
    public void onPause() {
        ExpandableListView listView = ((Activity) context).findViewById(listViewID);
        state = listView.onSaveInstanceState();
        super.onPause();
    }

    private static class CreateComponentAsyncTask extends AsyncTask<Void, Void, Map<String, List<IComponentInfo>>> {
        private final int page;
        private final WeakReference<Context> contextReference;
        private final String searchText;
        private Map<String, Drawable> appIcons;
        private Map<String, String> appNames;
        private final WeakReference<Parcelable> stateReference;

        CreateComponentAsyncTask(int page, Context context,
                                 String searchText,
                                 Map<String, Drawable> appIcons,
                                 Map<String, String> appNames,
                                 Parcelable state)
        {
            this.page = page;
            this.contextReference = new WeakReference<>(context);
            this.searchText = searchText;
            this.appIcons = appIcons;
            this.appNames = appNames;
            this.stateReference = new WeakReference<>(state);
        }

        @Override
        protected Map<String, List<IComponentInfo>> doInBackground(Void... voids) {
            List<IComponentInfo> list = null;
            switch (page) {
                case PERMISSIONS_PAGE:
                    list = AppComponentDisabled.getDisabledPermissions(searchText);
                    break;
                case SERVICES_PAGE:
                    list = AppComponentDisabled.getDisabledServices(searchText);
                    break;
                case RECEIVERS_PAGE:
                    list = AppComponentDisabled.getDisabledReceivers(searchText);
                    break;
            }
            Map<String, List<IComponentInfo>> componentMap = new TreeMap<>(String::compareToIgnoreCase);
            if (list != null) {
                for (IComponentInfo componentInfo : list) {
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
            }

            return componentMap;
        }

        @Override
        protected void onPostExecute(Map<String, List<IComponentInfo>> componentInfos) {
            Context context = contextReference.get();
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
                }

                ExpandableListView listView = ((Activity) context).findViewById(listViewId);
                if (listView != null && adapter != null) {
                    listView.setAdapter(adapter);
                    Parcelable state = stateReference.get();
                    if (state != null) {
                        listView.onRestoreInstanceState(state);
                        adapter.notifyDataSetChanged();
                    }
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
