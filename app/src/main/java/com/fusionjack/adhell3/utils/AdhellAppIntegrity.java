package com.fusionjack.adhell3.utils;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.BlockUrl;
import com.fusionjack.adhell3.db.entity.BlockUrlProvider;
import com.fusionjack.adhell3.db.entity.PolicyPackage;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AdhellAppIntegrity {
    private static final String ADHELL_STANDARD_PACKAGE = BuildConfig.DEFAULT_HOST;
    public static final int BLOCK_URL_LIMIT = BuildConfig.DOMAIN_LIMIT;
    public final static String DEFAULT_POLICY_ID = "default-policy";

    private static AdhellAppIntegrity instance;
    private final AppDatabase appDatabase;

    private AdhellAppIntegrity() {
        this.appDatabase = AdhellFactory.getInstance().getAppDatabase();
    }

    public static AdhellAppIntegrity getInstance() {
        if (instance == null) {
            instance = new AdhellAppIntegrity();
        }
        return instance;
    }

    public void checkDefaultPolicyExists() {
        PolicyPackage policyPackage = appDatabase.policyPackageDao().getPolicyById(DEFAULT_POLICY_ID);
        if (policyPackage != null) {
            LogUtils.info("Default PolicyPackage exists");
            return;
        }
        LogUtils.info("Default PolicyPackage does not exist. Creating default policy.");
        policyPackage = new PolicyPackage();
        policyPackage.id = DEFAULT_POLICY_ID;
        policyPackage.name = "Default Policy";
        policyPackage.description = "Automatically generated policy from current Adhell app settings";
        policyPackage.active = true;
        policyPackage.createdAt = policyPackage.updatedAt = new Date();
        appDatabase.policyPackageDao().insert(policyPackage);
        LogUtils.info("Default PolicyPackage has been added");
    }

    public void checkAdhellStandardPackage() {
        BlockUrlProvider blockUrlProvider =
                appDatabase.blockUrlProviderDao().getByUrl(ADHELL_STANDARD_PACKAGE);
        if (blockUrlProvider != null) {
            return;
        }

        // Remove existing default
        if (appDatabase.blockUrlProviderDao().getDefault().size() > 0) {
            appDatabase.blockUrlProviderDao().deleteDefault();
        }

        // Add the default package
        blockUrlProvider = new BlockUrlProvider();
        blockUrlProvider.url = ADHELL_STANDARD_PACKAGE;
        blockUrlProvider.lastUpdated = new Date();
        blockUrlProvider.deletable = false;
        blockUrlProvider.selected = true;
        blockUrlProvider.policyPackageId = DEFAULT_POLICY_ID;
        long[] ids = appDatabase.blockUrlProviderDao().insertAll(blockUrlProvider);
        blockUrlProvider.id = ids[0];
        List<BlockUrl> blockUrls;
        try {
            blockUrls = BlockUrlUtils.loadBlockUrls(blockUrlProvider);
            blockUrlProvider.count = blockUrls.size();
            LogUtils.info("Number of urls to insert: " + blockUrlProvider.count);
            // Save url provider
            appDatabase.blockUrlProviderDao().updateBlockUrlProviders(blockUrlProvider);
            // Save urls from providers
            appDatabase.blockUrlDao().insertAll(blockUrls);
        } catch (Exception e) {
            LogUtils.error(e.getMessage(), e);
        }
    }

    public AppDiff findAppsDiff() {
        LogUtils.info("Checking package database ...");

        List<String> currentApps = appDatabase.applicationInfoDao().getAllPackageNames();
        PackageManager packageManager = AdhellFactory.getInstance().getPackageManager();
        List<ApplicationInfo> installedApps = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

        return findListsDiff(installedApps, currentApps);
    }

    private AppDiff findListsDiff(List<ApplicationInfo> installedApps, List<String> currentApps) {
        LogUtils.info("Comparing app lists ...");
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
            Set<String> installedAppSet = installedApps.stream()
                    .map(app -> app.packageName)
                    .collect(Collectors.toSet());

            List<String> deletedApps = currentApps.stream()
                    .filter(packageName -> !installedAppSet.contains(packageName))
                    .collect(Collectors.toList());
            appDiff.putDeletedApps(deletedApps);
        } finally {
            if (appDiff.isEmpty()) {
                LogUtils.info("No apps diff detected.");
            } else {
                LogUtils.info("New apps: " + appDiff.getNewApps().stream().map(app -> app.packageName).collect(Collectors.toList()).toString());
                LogUtils.info("Deleted apps: " + appDiff.getDeletedApps().toString());
            }
        }
        return appDiff;
    }
}
