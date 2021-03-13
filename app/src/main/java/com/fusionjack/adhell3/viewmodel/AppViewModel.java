package com.fusionjack.adhell3.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.DatabaseFactory;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.samsung.android.knox.application.ApplicationPolicy;

import java.util.List;

import io.reactivex.Single;

public class AppViewModel extends ViewModel {

    private final AppRepository repository;
    private final AppDatabase appDatabase;

    public AppViewModel() {
        this.repository = new AppRepository();
        this.appDatabase = AdhellFactory.getInstance().getAppDatabase();
    }

    public Single<LiveData<List<AppInfo>>> loadAppList(AppRepository.Type type) {
        return repository.loadAppList(type);
    }

    public Single<Boolean> stopApp(AppInfo appInfo) {
        ApplicationPolicy appPolicy = AdhellFactory.getInstance().getAppPolicy();
        return Single.fromCallable(() -> appPolicy.stopApp(appInfo.packageName));
    }

    public Single<Boolean> wipeAppData(AppInfo appInfo) {
        ApplicationPolicy appPolicy = AdhellFactory.getInstance().getAppPolicy();
        return Single.fromCallable(() -> appPolicy.wipeApplicationData(appInfo.packageName));
    }

    public void enableAllDisablerApps() {
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
    }

    public void enableAllMobileApps() {
        List<AppInfo> mobileAppList = appDatabase.applicationInfoDao().getMobileRestrictedApps();
        for (AppInfo app : mobileAppList) {
            app.mobileRestricted = false;
            appDatabase.applicationInfoDao().update(app);
        }
        appDatabase.restrictedPackageDao().deleteByType(DatabaseFactory.MOBILE_RESTRICTED_TYPE);
    }

    public void enableAllWifiApps() {
        List<AppInfo> wifiAppList = appDatabase.applicationInfoDao().getWifiRestrictedApps();
        for (AppInfo app : wifiAppList) {
            app.wifiRestricted = false;
            appDatabase.applicationInfoDao().update(app);
        }
        appDatabase.restrictedPackageDao().deleteByType(DatabaseFactory.WIFI_RESTRICTED_TYPE);
    }

    public void enableAllWhitelistApps() {
        List<AppInfo> whitelistedAppList = appDatabase.applicationInfoDao().getWhitelistedApps();
        for (AppInfo app : whitelistedAppList) {
            app.adhellWhitelisted = false;
            appDatabase.applicationInfoDao().update(app);
        }
        appDatabase.firewallWhitelistedPackageDao().deleteAll();
    }

}
