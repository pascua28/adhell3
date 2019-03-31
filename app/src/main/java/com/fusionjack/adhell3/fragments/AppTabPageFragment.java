package com.fusionjack.adhell3.fragments;

import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.view.MenuCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.DatabaseFactory;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.model.AppFlag;
import com.fusionjack.adhell3.tasks.SetAppAsyncTask;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.samsung.android.knox.application.ApplicationPolicy;

import java.util.List;


public class AppTabPageFragment extends AppFragment {
    public static final int PACKAGE_DISABLER_PAGE = 0;
    private static final int MOBILE_RESTRICTER_PAGE = 1;
    private static final int WIFI_RESTRICTER_PAGE = 2;
    private static final int WHITELIST_PAGE = 3;
    private static final String ARG_PAGE = "page";
    private int page;
    private AppFlag appFlag;

    public static AppTabPageFragment newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        AppTabPageFragment fragment = new AppTabPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.page = getArguments().getInt(ARG_PAGE);

        AppRepository.Type type;
        switch (page) {
            case PACKAGE_DISABLER_PAGE:
                type = AppRepository.Type.DISABLER;
                break;

            case MOBILE_RESTRICTER_PAGE:
                type = AppRepository.Type.MOBILE_RESTRICTED;
                break;

            case WIFI_RESTRICTER_PAGE:
                type = AppRepository.Type.WIFI_RESTRICTED;
                break;

            case WHITELIST_PAGE:
                type = AppRepository.Type.WHITELISTED;
                break;

            default:
                type = AppRepository.Type.DISABLER;
                break;
        }
        initAppModel(type);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View view = null;
        ProgressBar loadingBar = null;
        switch (page) {
            case PACKAGE_DISABLER_PAGE:
                view = inflater.inflate(R.layout.fragment_package_disabler, container, false);
                loadingBar = view.findViewById(R.id.progressBarAppDisabler);
                appFlag = AppFlag.createDisablerFlag();
                break;

            case MOBILE_RESTRICTER_PAGE:
                view = inflater.inflate(R.layout.fragment_mobile_restricter, container, false);
                loadingBar = view.findViewById(R.id.progressBarAppMobile);
                appFlag = AppFlag.createMobileRestrictedFlag();
                break;

            case WIFI_RESTRICTER_PAGE:
                view = inflater.inflate(R.layout.fragment_wifi_restricter, container, false);
                loadingBar = view.findViewById(R.id.progressBarAppWifi);
                appFlag = AppFlag.createWifiRestrictedFlag();
                break;

            case WHITELIST_PAGE:
                view = inflater.inflate(R.layout.fragment_whitelisted_app, container, false);
                loadingBar = view.findViewById(R.id.progressBarAppWhitelist);
                appFlag = AppFlag.createWhitelistedFlag();
                break;
        }

        if (view != null) {
            ProgressBar finalLoadingBar = loadingBar;
            ListView listView = view.findViewById(appFlag.getLoadLayout());
            listView.setAdapter(adapter);
            if (page != PACKAGE_DISABLER_PAGE || AppPreferences.getInstance().isAppDisablerToggleEnabled()) {
                listView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                    AppInfoAdapter adapter = (AppInfoAdapter) adView.getAdapter();
                    new SetAppAsyncTask(adapter.getItem(position), appFlag, context).execute();
                });
            }

            if (page == PACKAGE_DISABLER_PAGE) {
                int themeColor = context.getResources().getColor(R.color.colorBottomNavUnselected, context.getTheme());

                ImageView filterButton = view.findViewById(R.id.filterButton);
                filterButton.setColorFilter(themeColor, PorterDuff.Mode.SRC_IN);
                filterButton.setOnClickListener(v -> {
                    PopupMenu popup = new PopupMenu(context, filterButton);
                    popup.getMenuInflater().inflate(R.menu.filter_appinfo_menu, popup.getMenu());
                    popup.getMenu().findItem(R.id.filterSystemApps).setChecked(filterAppInfo.getSystemAppsFilter());
                    popup.getMenu().findItem(R.id.filterUserApps).setChecked(filterAppInfo.getUserAppsFilter());
                    popup.getMenu().findItem(R.id.filterRunningApps).setChecked(filterAppInfo.getRunningAppsFilter());
                    popup.getMenu().findItem(R.id.filterStoppedApps).setChecked(filterAppInfo.getStoppedAppsFilter());
                    MenuCompat.setGroupDividerEnabled(popup.getMenu(), true);
                    popup.setOnMenuItemClickListener(item -> {
                        item.setChecked(!item.isChecked());
                        switch (item.getItemId()) {
                            case R.id.filterSystemApps:
                                filterAppInfo.setSystemAppsFilter(item.isChecked());
                                break;
                            case R.id.filterUserApps:
                                filterAppInfo.setUserAppsFilter(item.isChecked());
                                break;
                            case R.id.filterRunningApps:
                                filterAppInfo.setRunningAppsFilter(item.isChecked());
                                break;
                            case R.id.filterStoppedApps:
                                filterAppInfo.setStoppedAppsFilter(item.isChecked());
                                break;
                        }
                        resetSearchView();
                        loadAppList(type, finalLoadingBar);
                        return false;
                    });
                    popup.show();
                });
            }

            SwipeRefreshLayout swipeContainer = view.findViewById(appFlag.getRefreshLayout());
            swipeContainer.setOnRefreshListener(() -> {
                loadAppList(type, finalLoadingBar);
                swipeContainer.setRefreshing(false);
                resetSearchView();
            });

            loadAppList(type, loadingBar);
        }
        return view;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_enable_all:
                enableAllPackages();
        }
        return super.onOptionsItemSelected(item);
    }

    private void enableAllPackages() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_question, (ViewGroup) getView(), false);
        View parentView = LayoutInflater.from(getContext()).inflate(R.layout.fragment_dns, (ViewGroup) getView(), false);
        final ProgressBar[] loadingBar = {null};
        TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
        titleTextView.setText(R.string.enable_apps_dialog_title);
        TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
        questionTextView.setText(R.string.enable_apps_dialog_text);

        new AlertDialog.Builder(context)
                .setView(dialogView)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                    Toast.makeText(getContext(), getString(R.string.enabled_all_apps), Toast.LENGTH_SHORT).show();
                    AsyncTask.execute(() -> {
                        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
                        switch (page) {
                            case PACKAGE_DISABLER_PAGE:
                                loadingBar[0] = parentView.findViewById(R.id.progressBarAppDisabler);
                                ApplicationPolicy appPolicy = AdhellFactory.getInstance().getAppPolicy();
                                List<AppInfo> disabledAppList = appDatabase.applicationInfoDao().getDisabledApps();
                                for (AppInfo app : disabledAppList) {
                                    app.disabled = false;
                                    if (appPolicy != null) {
                                        appPolicy.setEnableApplication(app.packageName);
                                    }
                                    appDatabase.applicationInfoDao().update(app);
                                }
                                appDatabase.disabledPackageDao().deleteAll();
                                break;

                            case MOBILE_RESTRICTER_PAGE:
                                loadingBar[0] = parentView.findViewById(R.id.progressBarAppMobile);
                                List<AppInfo> mobileAppList = appDatabase.applicationInfoDao().getMobileRestrictedApps();
                                for (AppInfo app : mobileAppList) {
                                    app.mobileRestricted = false;
                                    appDatabase.applicationInfoDao().update(app);
                                }
                                appDatabase.restrictedPackageDao().deleteByType(DatabaseFactory.MOBILE_RESTRICTED_TYPE);
                                break;

                            case WIFI_RESTRICTER_PAGE:
                                loadingBar[0] = parentView.findViewById(R.id.progressBarAppWifi);
                                List<AppInfo> wifiAppList = appDatabase.applicationInfoDao().getWifiRestrictedApps();
                                for (AppInfo app : wifiAppList) {
                                    app.wifiRestricted = false;
                                    appDatabase.applicationInfoDao().update(app);
                                }
                                appDatabase.restrictedPackageDao().deleteByType(DatabaseFactory.WIFI_RESTRICTED_TYPE);
                                break;

                            case WHITELIST_PAGE:
                                loadingBar[0] = parentView.findViewById(R.id.progressBarAppWhitelist);
                                List<AppInfo> whitelistedAppList = appDatabase.applicationInfoDao().getWhitelistedApps();
                                for (AppInfo app : whitelistedAppList) {
                                    app.adhellWhitelisted = false;
                                    appDatabase.applicationInfoDao().update(app);
                                }
                                appDatabase.firewallWhitelistedPackageDao().deleteAll();
                                break;
                        }
                        loadAppList(type, loadingBar[0]);
                    });
                })
                .setNegativeButton(android.R.string.no, null).show();
    }
}

