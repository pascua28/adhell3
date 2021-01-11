package com.fusionjack.adhell3.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.DatabaseFactory;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.model.AppFlag;
import com.fusionjack.adhell3.tasks.ToggleAppInfoRxTask;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.samsung.android.knox.application.ApplicationPolicy;

import java.util.List;

public class AppTabPageFragment extends AppFragment {
    private static final String ARG_PAGE = "page";
    private int page;

    public static final int PACKAGE_DISABLER_PAGE = 0;
    public static final int MOBILE_RESTRICTER_PAGE = 1;
    public static final int WIFI_RESTRICTER_PAGE = 2;
    public static final int WHITELIST_PAGE = 3;

    public static AppTabPageFragment newInstance(int page) {
        Bundle args = new Bundle();
        args.putInt(ARG_PAGE, page);
        AppTabPageFragment fragment = new AppTabPageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

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

        View view = null;
        switch (page) {
            case PACKAGE_DISABLER_PAGE:
                view = inflateFragment(R.layout.fragment_package_disabler, inflater, container, type, AppFlag.createDisablerFlag());
                break;

            case MOBILE_RESTRICTER_PAGE:
                view = inflateFragment(R.layout.fragment_mobile_restricter, inflater, container, type, AppFlag.createMobileRestrictedFlag());
                break;

            case WIFI_RESTRICTER_PAGE:
                view = inflateFragment(R.layout.fragment_wifi_restricter, inflater, container, type, AppFlag.createWifiRestrictedFlag());
                break;

            case WHITELIST_PAGE:
                view = inflateFragment(R.layout.fragment_whitelisted_app, inflater, container, type, AppFlag.createWhitelistedFlag());
                break;
        }
        return view;
    }

    @Override
    protected void listOnItemClickListener(AdapterView<?> adView, View view2, int position, long id, AppFlag appFlag) {
        if (page != PACKAGE_DISABLER_PAGE || AppPreferences.getInstance().isAppDisablerToggleEnabled()) {
            AppInfoAdapter adapter = (AppInfoAdapter) adView.getAdapter();
            ToggleAppInfoRxTask.run(adapter.getItem(position), appFlag, adapter);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_enable_all) {
            enableAllPackages();
        }
        return super.onOptionsItemSelected(item);
    }

    private void enableAllPackages() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_question, (ViewGroup) getView(), false);
        TextView titlTextView = dialogView.findViewById(R.id.titleTextView);
        titlTextView.setText(R.string.enable_apps_dialog_title);
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
            .setNegativeButton(android.R.string.no, null).show();
    }
}
