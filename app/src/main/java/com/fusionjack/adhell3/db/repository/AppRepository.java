package com.fusionjack.adhell3.db.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.fragments.FilterAppInfo;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.samsung.android.knox.application.ApplicationPolicy;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.core.Single;

public class AppRepository {
    private final AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
    private final MutableLiveData<List<AppInfo>> resultList = new MutableLiveData<>();

    public Single<LiveData<List<AppInfo>>> loadAppList(Type type, FilterAppInfo filterAppInfo) {
        return Single.create(emitter -> {
            LiveData<List<AppInfo>> list = null;
            MutableLiveData<List<AppInfo>> tempListLiveData = new MutableLiveData<>();
            MediatorLiveData<List<AppInfo>> mediatorList = new MediatorLiveData<>();
            switch (type) {
                case DISABLER:
                    boolean appComponentsEnabled = AppPreferences.getInstance().isAppDisablerToggleEnabled();
                    if (filterAppInfo.getSystemAppsFilter() && filterAppInfo.getUserAppsFilter()) {
                        if (appComponentsEnabled) {
                            list = appDatabase.applicationInfoDao().getAppsInDisabledOrder();
                        } else {
                            list = appDatabase.applicationInfoDao().getEnabledApps();
                        }
                    } else if (filterAppInfo.getSystemAppsFilter()) {
                        if (appComponentsEnabled) {
                            list = appDatabase.applicationInfoDao().getAllSystemAppsInDisabledOrder();
                        } else {
                            list = appDatabase.applicationInfoDao().getSystemApps();
                        }
                    } else if (filterAppInfo.getUserAppsFilter()) {
                        if (appComponentsEnabled) {
                            list = appDatabase.applicationInfoDao().getAllUserAppsInDisabledOrder();
                        } else {
                            list = appDatabase.applicationInfoDao().getUserApps();
                        }
                    }
                    break;
                case MOBILE_RESTRICTED:
                    if (filterAppInfo.getSystemAppsFilter() && filterAppInfo.getUserAppsFilter()) {
                        list = appDatabase.applicationInfoDao().getAppsInMobileRestrictedOrder();
                    } else if (filterAppInfo.getSystemAppsFilter()) {
                        list = appDatabase.applicationInfoDao().getAllSystemAppsInMobileRestrictedOrder();
                    } else if (filterAppInfo.getUserAppsFilter()) {
                        list = appDatabase.applicationInfoDao().getAllUserAppsInMobileRestrictedOrder();
                    }
                    break;
                case WIFI_RESTRICTED:
                    if (filterAppInfo.getSystemAppsFilter() && filterAppInfo.getUserAppsFilter()) {
                        list = appDatabase.applicationInfoDao().getAppsInWifiRestrictedOrder();
                    } else if (filterAppInfo.getSystemAppsFilter()) {
                        list = appDatabase.applicationInfoDao().getAllSystemAppsInWifiRestrictedOrder();
                    } else if (filterAppInfo.getUserAppsFilter()) {
                        list = appDatabase.applicationInfoDao().getAllUserAppsInWifiRestrictedOrder();
                    }
                    break;
                case WHITELISTED:
                    if (filterAppInfo.getSystemAppsFilter() && filterAppInfo.getUserAppsFilter()) {
                        list = appDatabase.applicationInfoDao().getAppsInWhitelistedOrder();
                    } else if (filterAppInfo.getSystemAppsFilter()) {
                        list = appDatabase.applicationInfoDao().getAllSystemAppsInWhitelistedOrder();
                    } else if (filterAppInfo.getUserAppsFilter()) {
                        list = appDatabase.applicationInfoDao().getAllUserAppsInWhitelistedOrder();
                    }
                    break;
                case COMPONENT:
                    boolean showSystemApps = BuildConfig.SHOW_SYSTEM_APP_COMPONENT;
                    if ((filterAppInfo.getSystemAppsFilter() && showSystemApps) && filterAppInfo.getUserAppsFilter()) {
                        list = appDatabase.applicationInfoDao().getEnabledApps();
                    } else if ((filterAppInfo.getSystemAppsFilter() && showSystemApps)) {
                        list = appDatabase.applicationInfoDao().getSystemApps();
                    } else if (filterAppInfo.getUserAppsFilter()) {
                        list = appDatabase.applicationInfoDao().getUserApps();
                    }
                    break;
                case DNS:
                    if (filterAppInfo.getSystemAppsFilter() && filterAppInfo.getUserAppsFilter()) {
                        list = appDatabase.applicationInfoDao().getAppsInDnsOrder();
                    } else if (filterAppInfo.getSystemAppsFilter()) {
                        list = appDatabase.applicationInfoDao().getAllSystemAppsInDnsOrder();
                    } else if (filterAppInfo.getUserAppsFilter()) {
                        list = appDatabase.applicationInfoDao().getAllUserAppsInDnsOrder();
                    }
                    break;
            }

            if (list != null) {
                mediatorList.addSource(list, appInfos -> {
                    if (appInfos.size() > 0) {
                        mediatorList.setValue(appInfos);
                    }
                } );
            }
            mediatorList.addSource(tempListLiveData, appInfos -> {
                if (appInfos.size() > 0) {
                    mediatorList.postValue(appInfos);
                }
            });

            if ((filterAppInfo.getHighlightRunningApps() || !filterAppInfo.getRunningAppsFilter() || !filterAppInfo.getStoppedAppsFilter()) && list != null && list.getValue() != null) {
                List<AppInfo> tempList = new ArrayList<>();
                ApplicationPolicy appPolicy = AdhellFactory.getInstance().getAppPolicy();
                for (AppInfo item : list.getValue()) {
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
                tempListLiveData.postValue(tempList);
                tempList.clear();
            }

            emitter.onSuccess(mediatorList);
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
