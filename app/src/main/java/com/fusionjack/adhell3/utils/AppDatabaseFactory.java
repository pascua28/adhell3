package com.fusionjack.adhell3.utils;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.blocker.ContentBlocker;
import com.fusionjack.adhell3.blocker.ContentBlocker56;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.DatabaseFactory;
import com.fusionjack.adhell3.db.entity.AppInfo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.ObservableEmitter;
import io.reactivex.rxjava3.core.Single;

public final class AppDatabaseFactory {

    private AppDatabaseFactory() {
    }

    public static void restoreDatabase(ObservableEmitter<String> emitter, boolean hasInternetAccess) throws Exception {
        emitter.onNext("Restore database: Disabling firewall and domain blocker ...");
        ContentBlocker contentBlocker = ContentBlocker56.getInstance();
        contentBlocker.disableDomainRules();
        contentBlocker.disableFirewallRules();

        emitter.onNext("Restore database: Enabling disabled apps ...");
        AdhellFactory.getInstance().setAppDisablerToggle(false);

        emitter.onNext("Restore database: Enabling app's components ...");
        AdhellFactory.getInstance().setAppComponentToggle(false);

        emitter.onNext("Restore database: Resetting database ...");
        resetAppDatabase();

        emitter.onNext("Restore database: Importing entries to database ...");
        DatabaseFactory.getInstance().restoreDatabase();

        emitter.onNext("Restore database: Updating host providers ...");
        if (hasInternetAccess) {
            AdhellFactory.getInstance().updateAllProviders();
        }
    }

    // Find new/deleted apps and process them if they exist
    public static Single<AppDiff> detectNewOrDeletedApps() {
        return Single.fromCallable(() -> {
            AppDiff diff = findAppDiff();
            if (!diff.isEmpty()) {
                processAppDiff(diff);
            }
            return diff;
        });
    }

    private static AppDiff findAppDiff() {
        LogUtils.info("Finding apps diff ...");

        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        List<String> currentApps = appDatabase.applicationInfoDao().getAllPackageNames();
        PackageManager packageManager = AdhellFactory.getInstance().getPackageManager();
        List<ApplicationInfo> installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        return findDiff(installedApps, currentApps);
    }

    private static AppDiff findDiff(List<ApplicationInfo> installedApps, List<String> currentApps) {
        AppDiff appDiff = new AppDiff();
        try {
            String ownPackageName = App.get().getApplicationContext().getPackageName();

            // Find new apps
            Set<String> currentAppSet = new HashSet<>(currentApps); // To improve contains() performance
            List<ApplicationInfo> newApps = installedApps.stream()
                    .filter(app -> !app.packageName.equalsIgnoreCase(ownPackageName) && !currentAppSet.contains(app.packageName))
                    .collect(Collectors.toList());
            appDiff.putNewApps(newApps);

            // Find deleted apps
            Set<String> installedAppSet = installedApps.stream() // To improve contains() performance
                    .map(app -> app.packageName)
                    .collect(Collectors.toSet());

            List<String> deletedApps = currentApps.stream()
                    .filter(packageName -> !installedAppSet.contains(packageName))
                    .collect(Collectors.toList());
            appDiff.putDeletedApps(deletedApps);
        } finally {
            if (appDiff.isEmpty()) {
                LogUtils.info("No app diff detected.");
            } else {
                LogUtils.info("New apps: " + appDiff.getNewApps().stream().map(app -> app.packageName).collect(Collectors.toList()).toString());
                LogUtils.info("Deleted apps: " + appDiff.getDeletedApps().toString());
            }
        }
        return appDiff;
    }

    private static void processAppDiff(AppDiff diff) {
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();

        List<ApplicationInfo> newApps = diff.getNewApps();
        newApps.forEach(app -> {
            // Add app's icon, name and version name to AppCache
            AppCache.inject(app);

            // Add new app to app database
            long lastId = appDatabase.applicationInfoDao().getLastAppId();
            appDatabase.applicationInfoDao().insert(toAppInfo(app, ++lastId));

            // Disable app's services and receivers based on adhell3_services.txt and adhell3_receivers.txt files
            // Also check disabled app's component for consistency:
            // If some app's component are disabled and then the app is uninstalled, they cannot be re-enabled and
            // android system still marks them as disabled even the app is reinstalled.
            // If the app is reinstalled, their database entries are missing. checkAppComponentConsistency() will re-add them.
            boolean enabled = AppPreferences.getInstance().isAppComponentToggleEnabled();
            if (enabled) {
                String packageName = app.packageName;
                AppComponentFactory.getInstance().processAppComponentInBatchForApp(packageName, false);
                AppComponentFactory.getInstance().checkAppComponentConsistency(packageName);
            }
        });

        List<String> deletedApps = diff.getDeletedApps();
        deletedApps.forEach(packageName -> {
            appDatabase.applicationInfoDao().deleteByPackageName(packageName);
            appDatabase.disabledPackageDao().deleteByPackageName(packageName);
            appDatabase.restrictedPackageDao().deleteByPackageName(packageName);
            appDatabase.firewallWhitelistedPackageDao().deleteByPackageName(packageName);
            appDatabase.dnsPackageDao().deleteByPackageName(packageName);
            appDatabase.reportBlockedUrlDao().deleteByPackageName(packageName);

            // Knox cannot re-enable app's services and receivers because the app has been uninstalled
            // However, the app's permission can be re-enabled. For the sake of consistency, we leave them disabled
            // If the app is reinstalled, their disabled permissions, services and receivers will be applied automatically
            appDatabase.appPermissionDao().deleteByPackageName(packageName);
        });
    }

    private static AppInfo toAppInfo(ApplicationInfo app, long appId) {
        PackageManager packageManager = AdhellFactory.getInstance().getPackageManager();
        String appName = packageManager.getApplicationLabel(app).toString();
        String packageName = app.packageName;
        int mask = ApplicationInfo.FLAG_SYSTEM | ApplicationInfo.FLAG_UPDATED_SYSTEM_APP;

        AppInfo appInfo = new AppInfo();
        appInfo.id = appId;
        appInfo.appName = appName;
        appInfo.packageName = packageName;
        appInfo.system = (app.flags & mask) != 0;
        try {
            PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
            appInfo.installTime = packageInfo.firstInstallTime;
        } catch (PackageManager.NameNotFoundException e) {
            appInfo.installTime = 0;
        }
        return appInfo;
    }

    public static Single<AppInfoResult> getInstalledApps() {
        return Single.fromCallable(AppDatabaseFactory::fetchInstalledApps);
    }

    private static AppInfoResult fetchInstalledApps() throws Exception {
        AppInfoResult result = new AppInfoResult();
        processAppsInParallel(result, false);

        return result;
    }

    // The app related tables are deleted and then the installed apps will be inserted into database
    public static Completable resetInstalledApps() {
        return Completable.fromAction(AppDatabaseFactory::resetAppDatabase);
    }

    private static void resetAppDatabase() throws Exception {
        resetAppRelatedTables();
        processAppsInParallel(null, true);
    }

    private static void resetAppRelatedTables() {
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        appDatabase.applicationInfoDao().deleteAll();
        appDatabase.disabledPackageDao().deleteAll();
        appDatabase.restrictedPackageDao().deleteAll();
        appDatabase.firewallWhitelistedPackageDao().deleteAll();
        appDatabase.dnsPackageDao().deleteAll();
        appDatabase.appPermissionDao().deleteAll();
    }

    private static void processAppsInParallel(AppInfoResult result, boolean insertToDatabase) throws Exception {
        PackageManager packageManager = AdhellFactory.getInstance().getPackageManager();
        List<ApplicationInfo> installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        int cpuCount = Runtime.getRuntime().availableProcessors();
        ExecutorService executorService = Executors.newFixedThreadPool(cpuCount);

        List<AppExecutor> appExecutors = chunkInstalledApps(installedApps, cpuCount, insertToDatabase);
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

    private static List<AppExecutor> chunkInstalledApps(List<ApplicationInfo> apps, int cpuCount, boolean insertToDatabase) {
        List<AppExecutor> appExecutors = new ArrayList<>();
        int appCount = apps.size();
        int distributedAppCount = (int) Math.ceil(appCount / (double) cpuCount);
        final AtomicInteger counter = new AtomicInteger();
        final Collection<List<ApplicationInfo>> chunks = apps.stream()
                .collect(Collectors.groupingBy(it -> counter.getAndIncrement() / distributedAppCount))
                .values();
        //List<List<ApplicationInfo>> chunks = Lists.partition(apps, distributedAppCount);
        for (List<ApplicationInfo> chunk : chunks) {
            long id = distributedAppCount * appExecutors.size();
            AppExecutor appExecutor = new AppExecutor(chunk, id, insertToDatabase);
            appExecutors.add(appExecutor);
        }
        return appExecutors;
    }

    private static class AppExecutor implements Callable<AppInfoResult> {
        private final List<ApplicationInfo> apps;
        private final boolean insertToDatabase;
        private long appId;

        AppExecutor(List<ApplicationInfo> apps, long appId, boolean insertToDatabase) {
            this.apps = apps;
            this.appId = appId;
            this.insertToDatabase = insertToDatabase;
        }

        @Override
        public AppInfoResult call() {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            String ownPackageName = App.get().getApplicationContext().getPackageName();
            PackageManager packageManager = AdhellFactory.getInstance().getPackageManager();

            AppInfoResult appInfoResult = new AppInfoResult();
            for (ApplicationInfo app : apps) {
                String packageName = app.packageName;
                if (packageName.equals(ownPackageName)) {
                    continue;
                }

                Drawable icon;
                try {
                    icon = packageManager.getApplicationIcon(packageName);
                } catch (PackageManager.NameNotFoundException e) {
                    icon = null;
                }
                appInfoResult.addAppIcon(packageName, icon);

                String appName = packageManager.getApplicationLabel(app).toString();
                appInfoResult.addAppName(packageName, appName);

                try {
                    PackageInfo packageInfo = packageManager.getPackageInfo(packageName, 0);
                    appInfoResult.addVersionName(packageName, packageInfo.versionName);
                } catch (PackageManager.NameNotFoundException ignore) {
                }
            }

            if (insertToDatabase) {
                List<AppInfo> appsInfo = apps.stream()
                        .filter(app -> !app.packageName.equalsIgnoreCase(ownPackageName))
                        .map(app -> toAppInfo(app, appId++))
                        .collect(Collectors.toList());
                appDatabase.applicationInfoDao().insertAll(appsInfo);
            }

            return appInfoResult;
        }
    }
}
