package com.fusionjack.adhell3.fragments;

import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.view.MenuCompat;

import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.databinding.DialogQuestionBinding;
import com.fusionjack.adhell3.databinding.FragmentMobileRestricterBinding;
import com.fusionjack.adhell3.databinding.FragmentPackageDisablerBinding;
import com.fusionjack.adhell3.databinding.FragmentWhitelistedAppBinding;
import com.fusionjack.adhell3.databinding.FragmentWifiRestricterBinding;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.DatabaseFactory;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.model.AppFlag;
import com.fusionjack.adhell3.tasks.SetAppAsyncTask;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.google.android.material.snackbar.Snackbar;
import com.samsung.android.knox.application.ApplicationPolicy;

import java.util.List;


public class AppTabPageFragment extends AppFragment {
    public static final int PACKAGE_DISABLER_PAGE = 0;
    private static final int MOBILE_RESTRICTER_PAGE = 1;
    private static final int WIFI_RESTRICTER_PAGE = 2;
    private static final int WHITELIST_PAGE = 3;
    private static final String ARG_PAGE = "page";
    private int page;

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
        if (getArguments() != null) {
            this.page = getArguments().getInt(ARG_PAGE);
        }
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
                type = null;
                break;
        }
        initAppModel(type);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        setHasOptionsMenu(true);
        View view = null;
        switch (page) {
            case PACKAGE_DISABLER_PAGE:
                FragmentPackageDisablerBinding fragmentPackageDisablerBinding = FragmentPackageDisablerBinding.inflate(inflater);
                view = fragmentPackageDisablerBinding.getRoot();
                appFlag = AppFlag.createDisablerFlag();
                break;

            case MOBILE_RESTRICTER_PAGE:
                FragmentMobileRestricterBinding fragmentMobileRestricterBinding = FragmentMobileRestricterBinding.inflate(inflater);
                view = fragmentMobileRestricterBinding.getRoot();
                appFlag = AppFlag.createMobileRestrictedFlag();
                break;

            case WIFI_RESTRICTER_PAGE:
                FragmentWifiRestricterBinding fragmentWifiRestricterBinding = FragmentWifiRestricterBinding.inflate(inflater);
                view = fragmentWifiRestricterBinding.getRoot();
                appFlag = AppFlag.createWifiRestrictedFlag();
                break;

            case WHITELIST_PAGE:
                FragmentWhitelistedAppBinding fragmentWhitelistedAppBinding = FragmentWhitelistedAppBinding.inflate(inflater);
                view = fragmentWhitelistedAppBinding.getRoot();
                appFlag = AppFlag.createWhitelistedFlag();
                break;
        }

        if (view != null) {
            ListView listView = view.findViewById(appFlag.getLayout());
            listView.setAdapter(adapter);
            if (page != PACKAGE_DISABLER_PAGE || AppPreferences.getInstance().isAppDisablerToggleEnabled()) {
                listView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                    AppInfoAdapter adapter = (AppInfoAdapter) adView.getAdapter();
                    new SetAppAsyncTask(adapter.getItem(position), appFlag, context).execute();
                });
            }

            int themeColor = context.getResources().getColor(R.color.colorBottomNavUnselected, context.getTheme());
            ImageView filterButton = view.findViewById(R.id.filterButton);
            filterButton.setColorFilter(themeColor, PorterDuff.Mode.SRC_IN);
            filterButton.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(context, filterButton);
                popup.getMenuInflater().inflate(R.menu.filter_appinfo_menu, popup.getMenu());
                popup.getMenu().findItem(R.id.highlightRunningApps).setChecked(filterAppInfo.getHighlightRunningApps());
                popup.getMenu().findItem(R.id.filterSystemApps).setChecked(filterAppInfo.getSystemAppsFilter());
                popup.getMenu().findItem(R.id.filterUserApps).setChecked(filterAppInfo.getUserAppsFilter());
                popup.getMenu().findItem(R.id.filterRunningApps).setChecked(filterAppInfo.getRunningAppsFilter());
                popup.getMenu().findItem(R.id.filterStoppedApps).setChecked(filterAppInfo.getStoppedAppsFilter());
                MenuCompat.setGroupDividerEnabled(popup.getMenu(), true);
                popup.setOnMenuItemClickListener(item -> {
                    item.setChecked(!item.isChecked());
                    int id = item.getItemId();
                    if (id == R.id.highlightRunningApps) {
                        filterAppInfo.setHighlightRunningApps(item.isChecked());
                    } else if (id == R.id.filterSystemApps) {
                        filterAppInfo.setSystemAppsFilter(item.isChecked());
                        if (!item.isChecked()) {
                            if (!popup.getMenu().findItem(R.id.filterUserApps).isChecked()) {
                                popup.getMenu().findItem(R.id.filterUserApps).setChecked(true);
                                filterAppInfo.setUserAppsFilter(true);
                            }
                        }
                    } else if (id == R.id.filterUserApps) {
                        filterAppInfo.setUserAppsFilter(item.isChecked());
                        if (!item.isChecked()) {
                            if (!popup.getMenu().findItem(R.id.filterSystemApps).isChecked()) {
                                popup.getMenu().findItem(R.id.filterSystemApps).setChecked(true);
                                filterAppInfo.setSystemAppsFilter(true);
                            }
                        }
                    } else if (id == R.id.filterRunningApps) {
                        filterAppInfo.setRunningAppsFilter(item.isChecked());
                        if (!item.isChecked()) {
                            if (!popup.getMenu().findItem(R.id.filterStoppedApps).isChecked()) {
                                popup.getMenu().findItem(R.id.filterStoppedApps).setChecked(true);
                                filterAppInfo.setStoppedAppsFilter(true);
                            }
                        }
                    } else if (id == R.id.filterStoppedApps) {
                        filterAppInfo.setStoppedAppsFilter(item.isChecked());
                        if (!item.isChecked()) {
                            if (!popup.getMenu().findItem(R.id.filterRunningApps).isChecked()) {
                                popup.getMenu().findItem(R.id.filterRunningApps).setChecked(true);
                                filterAppInfo.setRunningAppsFilter(true);
                            }
                        }
                    }
                    if (!filterAppInfo.getHighlightRunningApps() &&
                            filterAppInfo.getSystemAppsFilter() &&
                            filterAppInfo.getUserAppsFilter() &&
                            filterAppInfo.getRunningAppsFilter() &&
                            filterAppInfo.getStoppedAppsFilter()
                    ) {
                        filterButton.setColorFilter(themeColor, PorterDuff.Mode.SRC_IN);
                    } else {
                        int accentColor = context.getResources().getColor(R.color.colorAccent, context.getTheme());
                        filterButton.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN);
                    }

                    MainActivity.setFilterAppInfo(filterAppInfo);
                    return false;
                });
                popup.show();
            });
        }
        rootView = view;
        super.onCreateView(inflater, container, savedInstanceState);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Set filter button color
        int themeColor = context.getResources().getColor(R.color.colorBottomNavUnselected, context.getTheme());
        ImageView filterButton = requireView().findViewById(R.id.filterButton);
        if (!filterAppInfo.getHighlightRunningApps() &&
                filterAppInfo.getSystemAppsFilter() &&
                filterAppInfo.getUserAppsFilter() &&
                filterAppInfo.getRunningAppsFilter() &&
                filterAppInfo.getStoppedAppsFilter()
        ) {
            filterButton.setColorFilter(themeColor, PorterDuff.Mode.SRC_IN);
        } else {
            int accentColor = context.getResources().getColor(R.color.colorAccent, context.getTheme());
            filterButton.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_enable_all) {
            enableAllPackages();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        rootView = null;
    }

    private void enableAllPackages() {
        DialogQuestionBinding dialogQuestionBinding = DialogQuestionBinding.inflate(LayoutInflater.from(getContext()));
        dialogQuestionBinding.titleTextView.setText(R.string.enable_apps_dialog_title);
        dialogQuestionBinding.questionTextView.setText(R.string.enable_apps_dialog_text);

        AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                .setView(dialogQuestionBinding.getRoot())
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                    if (getActivity() instanceof MainActivity) {
                        MainActivity mainActivity = (MainActivity) getActivity();
                        mainActivity.makeSnackbar(getString(R.string.enabled_all_apps), Snackbar.LENGTH_SHORT)
                                .show();
                    }
                    AsyncTask.execute(() -> {
                        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
                        switch (page) {
                            case PACKAGE_DISABLER_PAGE:
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
                                List<AppInfo> mobileAppList = appDatabase.applicationInfoDao().getMobileRestrictedApps();
                                for (AppInfo app : mobileAppList) {
                                    app.mobileRestricted = false;
                                    appDatabase.applicationInfoDao().update(app);
                                }
                                appDatabase.restrictedPackageDao().deleteByType(DatabaseFactory.MOBILE_RESTRICTED_TYPE);
                                break;

                            case WIFI_RESTRICTER_PAGE:
                                List<AppInfo> wifiAppList = appDatabase.applicationInfoDao().getWifiRestrictedApps();
                                for (AppInfo app : wifiAppList) {
                                    app.wifiRestricted = false;
                                    appDatabase.applicationInfoDao().update(app);
                                }
                                appDatabase.restrictedPackageDao().deleteByType(DatabaseFactory.WIFI_RESTRICTED_TYPE);
                                break;

                            case WHITELIST_PAGE:
                                List<AppInfo> whitelistedAppList = appDatabase.applicationInfoDao().getWhitelistedApps();
                                for (AppInfo app : whitelistedAppList) {
                                    app.adhellWhitelisted = false;
                                    appDatabase.applicationInfoDao().update(app);
                                }
                                appDatabase.firewallWhitelistedPackageDao().deleteAll();
                                break;
                        }
                    });
                })
                .setNegativeButton(android.R.string.no, null)
                .create();

        alertDialog.show();
    }
}

