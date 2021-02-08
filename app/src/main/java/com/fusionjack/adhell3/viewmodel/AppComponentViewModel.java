package com.fusionjack.adhell3.viewmodel;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.AppPermission;
import com.fusionjack.adhell3.utils.AdhellFactory;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import io.reactivex.Single;

public class AppComponentViewModel extends ViewModel {

    private static final Comparator<AppInfo> DISABLED_COMPONENTS_COMPARATOR = Comparator.comparing(info -> info.appName);
    private static final int APP_MASK = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;

    private final AppDatabase appDatabase;

    public AppComponentViewModel() {
        this.appDatabase = AdhellFactory.getInstance().getAppDatabase();
    }

    public Single<LiveData<List<AppPermission>>> getPermissions(String packageName) {
        return Single.fromCallable(() -> appDatabase.appPermissionDao().getPermissionsAsLiveData(packageName));
    }

    public Single<LiveData<List<AppPermission>>> getActivities(String packageName) {
        return Single.fromCallable(() -> appDatabase.appPermissionDao().getActivitiesAsLiveData(packageName));
    }

    public Single<LiveData<List<AppPermission>>> getServices(String packageName) {
        return Single.fromCallable(() -> appDatabase.appPermissionDao().getServicesAsLiveData(packageName));
    }

    public Single<LiveData<List<AppPermission>>> getReceivers(String packageName) {
        return Single.fromCallable(() -> appDatabase.appPermissionDao().getReceiversAsLiveData(packageName));
    }

    public Single<LiveData<List<AppPermission>>> getProviders(String packageName) {
        return Single.fromCallable(() -> appDatabase.appPermissionDao().getProvidersAsLiveData(packageName));
    }

    public Single<List<AppInfo>> getDisabledComponentApps() {
        return Single.fromCallable(() ->
                appDatabase.appPermissionDao().getApps().stream()
                        .map(this::toAppInfo)
                        .sorted(DISABLED_COMPONENTS_COMPARATOR)
                        .collect(Collectors.toList())
        );
    }

    private AppInfo toAppInfo(String packageName) {
        AppInfo appInfo = new AppInfo();
        PackageManager packageManager = AdhellFactory.getInstance().getPackageManager();
        try {
            ApplicationInfo info = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);
            appInfo.appName = packageManager.getApplicationLabel(info).toString();
            appInfo.packageName = packageName;
            appInfo.system = (info.flags & APP_MASK) != 0;
        } catch (PackageManager.NameNotFoundException ignored) {
        }
        return appInfo;
    }

}
