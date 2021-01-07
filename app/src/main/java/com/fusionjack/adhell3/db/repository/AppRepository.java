package com.fusionjack.adhell3.db.repository;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.utils.AdhellFactory;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;

public class AppRepository {
    private final AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();

    public enum Type {
        DISABLER,
        MOBILE_RESTRICTED,
        WIFI_RESTRICTED,
        WHITELISTED,
        COMPONENT,
        DNS
    }

    public Single<List<AppInfo>> loadAppList(Type type) {
        return Single.create(emitter -> {
            List<AppInfo> list = new ArrayList<>();
            switch (type) {
                case DISABLER:
                    list = appDatabase.applicationInfoDao().getAppsInDisabledOrder();
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
}
