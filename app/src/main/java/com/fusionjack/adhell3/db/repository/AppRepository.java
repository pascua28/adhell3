package com.fusionjack.adhell3.db.repository;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPreferences;

import java.util.List;

import io.reactivex.rxjava3.core.Single;

public class AppRepository {

    public Single<LiveData<List<AppInfo>>> loadAppList(Type type) {
        return Single.create(emitter -> {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            LiveData<List<AppInfo>> list = new MutableLiveData<>();
            switch (type) {
                case DISABLER:
                    if (AppPreferences.getInstance().isAppDisablerToggleEnabled()) {
                        list = appDatabase.applicationInfoDao().getAppsInDisabledOrder();
                    } else {
                        list = appDatabase.applicationInfoDao().getEnabledApps();
                    }
                    break;
                case MOBILE_RESTRICTED:
                    list = appDatabase.applicationInfoDao().getAppsInMobileRestrictedOrder();
                    break;
                case WIFI_RESTRICTED:
                    list = appDatabase.applicationInfoDao().getAppsInWifiRestrictedOrder();
                    break;
                case WHITELISTED:
                    list = appDatabase.applicationInfoDao().getAppsInWhitelistedOrder();
                    break;
                case COMPONENT:
                    boolean showSystemApps = BuildConfig.SHOW_SYSTEM_APP_COMPONENT;
                    list = showSystemApps ?
                            appDatabase.applicationInfoDao().getEnabledApps() :
                            appDatabase.applicationInfoDao().getUserApps();
                    break;
                case DNS:
                    list = appDatabase.applicationInfoDao().getAppsInDnsOrder();
                    break;
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
