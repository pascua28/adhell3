package com.fusionjack.adhell3.db.repository;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.fragments.FilterAppInfo;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.samsung.android.knox.application.ApplicationPolicy;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.core.Single;

public class AppRepository {

    public Single<List<AppInfo>> loadAppList(String text, Type type, FilterAppInfo filterAppInfo) {
        return Single.create(emitter -> {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            String filterText = '%' + text + '%';
            List<AppInfo> list = new ArrayList<>();
            switch (type) {
                case DISABLER:
                    if (filterAppInfo.getSystemAppsFilter() && filterAppInfo.getUserAppsFilter()) {
                        if (text.length() == 0) {
                            list = appDatabase.applicationInfoDao().getAppsInDisabledOrder();
                        } else {
                            list = appDatabase.applicationInfoDao().getAppsInDisabledOrder(filterText);
                        }
                    } else if (filterAppInfo.getSystemAppsFilter()) {
                        if (text.length() == 0) {
                            list = appDatabase.applicationInfoDao().getAllSystemAppsInDisabledOrder();
                        } else {
                            list = appDatabase.applicationInfoDao().getAllSystemAppsInDisabledOrder(filterText);
                        }
                    } else if (filterAppInfo.getUserAppsFilter()) {
                        if (text.length() == 0) {
                            list = appDatabase.applicationInfoDao().getAllUserAppsInDisabledOrder();
                        } else {
                            list = appDatabase.applicationInfoDao().getAllUserAppsInDisabledOrder(filterText);
                        }
                    }
                    break;
                case MOBILE_RESTRICTED:
                    if (filterAppInfo.getSystemAppsFilter() && filterAppInfo.getUserAppsFilter()) {
                        if (text.length() == 0) {
                            list = appDatabase.applicationInfoDao().getAppsInMobileRestrictedOrder();
                        } else {
                            list = appDatabase.applicationInfoDao().getAppsInMobileRestrictedOrder(filterText);
                        }
                    } else if (filterAppInfo.getSystemAppsFilter()) {
                        if (text.length() == 0) {
                            list = appDatabase.applicationInfoDao().getAllSystemAppsInMobileRestrictedOrder();
                        } else {
                            list = appDatabase.applicationInfoDao().getAllSystemAppsInMobileRestrictedOrder(filterText);
                        }
                    } else if (filterAppInfo.getUserAppsFilter()) {
                        if (text.length() == 0) {
                            list = appDatabase.applicationInfoDao().getAllUserAppsInMobileRestrictedOrder();
                        } else {
                            list = appDatabase.applicationInfoDao().getAllUserAppsInMobileRestrictedOrder(filterText);
                        }
                    }
                    break;
                case WIFI_RESTRICTED:
                    if (filterAppInfo.getSystemAppsFilter() && filterAppInfo.getUserAppsFilter()) {
                        if (text.length() == 0) {
                            list = appDatabase.applicationInfoDao().getAppsInWifiRestrictedOrder();
                        } else {
                            list = appDatabase.applicationInfoDao().getAppsInWifiRestrictedOrder(filterText);
                        }
                    } else if (filterAppInfo.getSystemAppsFilter()) {
                        if (text.length() == 0) {
                            list = appDatabase.applicationInfoDao().getAllSystemAppsInWifiRestrictedOrder();
                        } else {
                            list = appDatabase.applicationInfoDao().getAllSystemAppsInWifiRestrictedOrder(filterText);
                        }
                    } else if (filterAppInfo.getUserAppsFilter()) {
                        if (text.length() == 0) {
                            list = appDatabase.applicationInfoDao().getAllUserAppsInWifiRestrictedOrder();
                        } else {
                            list = appDatabase.applicationInfoDao().getAllUserAppsInWifiRestrictedOrder(filterText);
                        }
                    }
                    break;
                case WHITELISTED:
                    if (filterAppInfo.getSystemAppsFilter() && filterAppInfo.getUserAppsFilter()) {
                        if (text.length() == 0) {
                            list = appDatabase.applicationInfoDao().getAppsInWhitelistedOrder();
                        } else {
                            list = appDatabase.applicationInfoDao().getAppsInWhitelistedOrder(filterText);
                        }
                    } else if (filterAppInfo.getSystemAppsFilter()) {
                        if (text.length() == 0) {
                            list = appDatabase.applicationInfoDao().getAllSystemAppsInWhitelistedOrder();
                        } else {
                            list = appDatabase.applicationInfoDao().getAllSystemAppsInWhitelistedOrder(filterText);
                        }
                    } else if (filterAppInfo.getUserAppsFilter()) {
                        if (text.length() == 0) {
                            list = appDatabase.applicationInfoDao().getAllUserAppsInWhitelistedOrder();
                        } else {
                            list = appDatabase.applicationInfoDao().getAllUserAppsInWhitelistedOrder(filterText);
                        }
                    }
                    break;
                case COMPONENT:
                    boolean showSystemApps = BuildConfig.SHOW_SYSTEM_APP_COMPONENT;
                    if (text.length() == 0) {
                        list = showSystemApps ?
                                appDatabase.applicationInfoDao().getEnabledAppsAlphabetically() :
                                appDatabase.applicationInfoDao().getUserApps();
                    } else {
                        list = showSystemApps ?
                                appDatabase.applicationInfoDao().getEnabledAppsAlphabetically(filterText) :
                                appDatabase.applicationInfoDao().getUserApps(filterText);
                    }
                    break;
                case DNS:
                    if (filterAppInfo.getSystemAppsFilter() && filterAppInfo.getUserAppsFilter()) {
                        if (text.length() == 0) {
                            list = appDatabase.applicationInfoDao().getAppsInDnsOrder();
                        } else {
                            list = appDatabase.applicationInfoDao().getAppsInDnsOrder(filterText);
                        }
                    } else if (filterAppInfo.getSystemAppsFilter()) {
                        if (text.length() == 0) {
                            list = appDatabase.applicationInfoDao().getAllSystemAppsInDnsOrder();
                        } else {
                            list = appDatabase.applicationInfoDao().getAllSystemAppsInDnsOrder(filterText);
                        }
                    } else if (filterAppInfo.getUserAppsFilter()) {
                        if (text.length() == 0) {
                            list = appDatabase.applicationInfoDao().getAllUserAppsInDnsOrder();
                        } else {
                            list = appDatabase.applicationInfoDao().getAllUserAppsInDnsOrder(filterText);
                        }
                    }
                    break;
            }

            if (type != Type.COMPONENT && (filterAppInfo.getHighlightRunningApps() || !filterAppInfo.getRunningAppsFilter() || !filterAppInfo.getStoppedAppsFilter())) {
                List<AppInfo> tempList = new ArrayList<>();
                ApplicationPolicy appPolicy = AdhellFactory.getInstance().getAppPolicy();
                for (AppInfo item : list) {
                    boolean isRunning = false;
                    try {
                        if (appPolicy != null) {
                            isRunning = appPolicy.isApplicationRunning(item.packageName);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    if (filterAppInfo.getRunningAppsFilter() && isRunning) {
                        if (filterAppInfo.getHighlightRunningApps()) {
                            item.appName = item.appName + AppInfoAdapter.RUNNING_TAG;
                        }
                        tempList.add(item);
                    } else if (filterAppInfo.getStoppedAppsFilter() && !isRunning)
                        tempList.add(item);
                }
                list.clear();
                list.addAll(tempList);
                tempList.clear();
            }
            emitter.onSuccess(list);
        });
    }

    public enum Type {
        DISABLER,
        MOBILE_RESTRICTED,
        WIFI_RESTRICTED,
        WHITELISTED,
        COMPONENT,
        DNS
    }
}
