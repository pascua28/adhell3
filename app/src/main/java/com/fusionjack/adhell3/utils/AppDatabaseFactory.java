package com.fusionjack.adhell3.utils;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.DisabledPackage;
import com.fusionjack.adhell3.db.entity.DnsPackage;
import com.fusionjack.adhell3.db.entity.FirewallWhitelistedPackage;
import com.fusionjack.adhell3.db.entity.RestrictedPackage;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

import static com.fusionjack.adhell3.db.DatabaseFactory.MOBILE_RESTRICTED_TYPE;
import static com.fusionjack.adhell3.db.DatabaseFactory.WIFI_RESTRICTED_TYPE;

public final class AppDatabaseFactory {

    private AppDatabaseFactory() {
    }

    // The app related tables are deleted and then will be filled with installed apps
    public static Completable resetInstalledApps() {
        return Completable.fromCallable(() -> getInstalledApps(false));
    }

    // The app related tables are deleted and they will be filled with installed apps and then the modified apps are restored
    public static Single<AppInfoResult> refreshInstalledApps() {
        return Single.fromCallable(() -> getInstalledApps(true));
    }

    private static AppInfoResult getInstalledApps(boolean handleModifiedApp) throws Exception {
        AppInfoResult result = handleModifiedApp ? new AppInfoResult() : null;
        List<AppInfo> modifiedApps = null;

        try {
            // Get installed apps
            PackageManager packageManager = AdhellFactory.getInstance().getPackageManager();
            List<ApplicationInfo> installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

            // Backup modified apps temporary
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            modifiedApps = appDatabase.applicationInfoDao().getModifiedApps();
            resetAppRelatedTables();

            // Cache the installed apps and insert them into db in parallel
            processAppsInParallel(installedApps, result);
        } finally {
            // Put back the modified apps into db
            if (handleModifiedApp) {
                handleModifiedApps(modifiedApps);
            }
        }

        return result;
    }

    private static void resetAppRelatedTables() {
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        appDatabase.applicationInfoDao().deleteAll();
        appDatabase.firewallWhitelistedPackageDao().deleteAll();
        appDatabase.disabledPackageDao().deleteAll();
        appDatabase.restrictedPackageDao().deleteAll();
        appDatabase.dnsPackageDao().deleteAll();
    }

    private static void processAppsInParallel(List<ApplicationInfo> apps, AppInfoResult result) throws Exception {
        int cpuCount = Runtime.getRuntime().availableProcessors() / 2;
        ExecutorService executorService = Executors.newFixedThreadPool(cpuCount);

        List<AppExecutor> appExecutors = chunkInstalledApps(apps);
        List<Future<AppInfoResult>> futures = executorService.invokeAll(appExecutors);
        for (Future<AppInfoResult> future : futures) {
            AppInfoResult chunkResult = future.get(60, TimeUnit.SECONDS);
            if (result != null) {
                result.addAppIcons(chunkResult.getAppsIcons());
                result.addAppNames(chunkResult.getAppsNames());
                result.addVersionNames(chunkResult.getVersionNames());
            }
        }

        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(120, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }

    private static List<AppExecutor> chunkInstalledApps(List<ApplicationInfo> apps) {
        List<AppExecutor> appExecutors = new ArrayList<>();
        int appCount = apps.size();
        int cpuCount = Runtime.getRuntime().availableProcessors() / 2;
        int distributedAppCount = (int) Math.ceil(appCount / (double) cpuCount);
        final AtomicInteger counter = new AtomicInteger();
        final Collection<List<ApplicationInfo>> chunks = apps.stream()
                .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / distributedAppCount))
                .values();
        //List<List<ApplicationInfo>> chunks = Lists.partition(apps, distributedAppCount);
        for (List<ApplicationInfo> chunk : chunks) {
            long id = distributedAppCount * appExecutors.size();
            AppExecutor appExecutor = new AppExecutor(chunk, id);
            appExecutors.add(appExecutor);
        }
        return appExecutors;
    }

    private static void handleModifiedApps(List<AppInfo> modifiedApps) {
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        if (modifiedApps == null || appDatabase.applicationInfoDao().getAppSize() == 0) {
            return;
        }
        for (AppInfo modifiedApp : modifiedApps) {
            AppInfo appInfo = appDatabase.applicationInfoDao().getAppByPackageName(modifiedApp.packageName);
            if (appInfo != null) {
                if (modifiedApp.adhellWhitelisted) {
                    appInfo.adhellWhitelisted = true;
                    FirewallWhitelistedPackage whitelistedPackage = new FirewallWhitelistedPackage();
                    whitelistedPackage.packageName = modifiedApp.packageName;
                    whitelistedPackage.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                    appDatabase.firewallWhitelistedPackageDao().insert(whitelistedPackage);
                }
                if (modifiedApp.disabled) {
                    appInfo.disabled = true;
                    DisabledPackage disabledPackage = new DisabledPackage();
                    disabledPackage.packageName = modifiedApp.packageName;
                    disabledPackage.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                    appDatabase.disabledPackageDao().insert(disabledPackage);
                }
                if (modifiedApp.mobileRestricted) {
                    appInfo.mobileRestricted = true;
                    RestrictedPackage restrictedPackage = new RestrictedPackage();
                    restrictedPackage.packageName = modifiedApp.packageName;
                    restrictedPackage.type = MOBILE_RESTRICTED_TYPE;
                    restrictedPackage.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                    appDatabase.restrictedPackageDao().insert(restrictedPackage);
                }
                if (modifiedApp.wifiRestricted) {
                    appInfo.wifiRestricted = true;
                    RestrictedPackage restrictedPackage = new RestrictedPackage();
                    restrictedPackage.packageName = modifiedApp.packageName;
                    restrictedPackage.type = WIFI_RESTRICTED_TYPE;
                    restrictedPackage.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                    appDatabase.restrictedPackageDao().insert(restrictedPackage);
                }
                if (modifiedApp.hasCustomDns) {
                    appInfo.hasCustomDns = true;
                    DnsPackage dnsPackage = new DnsPackage();
                    dnsPackage.packageName = modifiedApp.packageName;
                    dnsPackage.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                    appDatabase.dnsPackageDao().insert(dnsPackage);
                }
                appDatabase.applicationInfoDao().update(appInfo);
            }
        }
    }

    private static class AppExecutor implements Callable<AppInfoResult> {
        private final List<ApplicationInfo> apps;
        private long appId;

        AppExecutor(List<ApplicationInfo> apps, long appId) {
            this.apps = apps;
            this.appId = appId;
        }

        @Override
        public AppInfoResult call() {
            String ownPackageName = App.get().getApplicationContext().getPackageName();
            PackageManager packageManager = AdhellFactory.getInstance().getPackageManager();
            AppInfoResult appInfoResult = new AppInfoResult();

            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            List<AppInfo> appsInfo = new ArrayList<>();
            int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;

            for (ApplicationInfo app : apps) {
                if (app.packageName.equals(ownPackageName)) {
                    continue;
                }

                Drawable icon;
                try {
                    icon = packageManager.getApplicationIcon(app.packageName);
                } catch (PackageManager.NameNotFoundException e) {
                    icon = null;
                }
                appInfoResult.addAppIcon(app.packageName, icon);

                String appName = packageManager.getApplicationLabel(app).toString();
                appInfoResult.addAppName(app.packageName, appName);

                AppInfo appInfo = new AppInfo();
                appInfo.id = appId++;
                appInfo.appName = appName;
                appInfo.packageName = app.packageName;
                appInfo.system = (app.flags & mask) != 0;
                try {
                    PackageInfo packageInfo = packageManager.getPackageInfo(app.packageName, 0);
                    appInfo.installTime = packageInfo.firstInstallTime;
                    appInfoResult.addVersionName(app.packageName, packageInfo.versionName);
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                    appInfo.installTime = 0;
                }
                appsInfo.add(appInfo);
            }
            appDatabase.applicationInfoDao().insertAll(appsInfo);

            return appInfoResult;
        }
    }
}
