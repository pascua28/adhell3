package com.fusionjack.adhell3.tasks;

import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.BackoffPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.blocker.ContentBlocker56;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.AppPermission;
import com.fusionjack.adhell3.db.entity.DisabledPackage;
import com.fusionjack.adhell3.db.entity.DnsPackage;
import com.fusionjack.adhell3.db.entity.FirewallWhitelistedPackage;
import com.fusionjack.adhell3.db.entity.RestrictedPackage;
import com.fusionjack.adhell3.dialogfragment.AutoUpdateDialogFragment;
import com.fusionjack.adhell3.model.AppComponent;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppCache;
import com.fusionjack.adhell3.utils.AppComponentFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.BlockUrlUtils;
import com.fusionjack.adhell3.utils.FileUtils;
import com.fusionjack.adhell3.utils.FirewallUtils;
import com.fusionjack.adhell3.utils.LogUtils;
import com.samsung.android.knox.application.ApplicationPolicy;
import com.samsung.android.knox.net.firewall.Firewall;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class AutoUpdateWorker extends Worker {
    private static AppDatabase appDatabase;
    private static Firewall firewall;
    private static AppCache appCache;
    private int retryCount;
    private Handler handler = null;

    private static class ExceededLimitException extends Exception{}

    public AutoUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        if (AppPreferences.getInstance().getCreateLogOnAutoUpdate()) {
            DocumentFile logFile = LogUtils.getAutoUpdateLogFile();
            this.handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    String nowDate = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new java.util.Date());
                    LogUtils.appendLogFile(String.format(Locale.getDefault(), "%s [retry%d]: %s", nowDate, retryCount, msg.obj.toString().trim()), logFile);
                }
            };
        }

        AutoUpdateWorker.appDatabase = AppDatabase.getAppDatabase(context);
        AutoUpdateWorker.firewall = AdhellFactory.getInstance().getFirewall();
        AutoUpdateWorker.appCache = AppCache.getInstance(context, this.handler);
    }

    @NonNull
    @Override
    public Result doWork() {
        retryCount = this.getRunAttemptCount();
        if (retryCount > 5) {
            return Result.failure();
        }

        boolean retryForFailing = false;

        LogUtils.info("------Start Rules auto update------", handler);
        try {
            autoUpdateRules();
        } catch (ExceededLimitException e) {
            LogUtils.info("------Failed Rules auto update. Domains limit exceeded------", handler);
        } catch (Exception e) {
            LogUtils.error("Failed Rules auto update! Will be retried.", e, handler);
            LogUtils.info("------Failed Rules auto update------", handler);
            retryForFailing = true;
        }
        LogUtils.info("------Successful Rules auto update------", handler);

        if (AppPreferences.getInstance().getAppComponentsAutoUpdate()) {
            LogUtils.info("------Start App components auto update------", handler);
            try {
                processAppComponentsInAutoUpdate();
            } catch (Exception e) {
                LogUtils.error("Failed App components auto update! Will be retried.", e, handler);
                LogUtils.info("------Failed App components auto update------", handler);
                retryForFailing = true;
            }
            LogUtils.info("------Successful App components auto update------", handler);
        }

        if (AppPreferences.getInstance().getCleanDatabaseAutoUpdate()) {
            LogUtils.info("------Start auto clean database------", handler);
            try {
                autoCleanDatabase();
            } catch (Exception e) {
                LogUtils.error("Failed auto clean database! Will be retried.", e, handler);
                LogUtils.info("------Failed auto clean database------", handler);
                retryForFailing = true;
            }
            LogUtils.info("------Successful auto clean database------", handler);
        }

        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        LogUtils.info(String.format(Locale.getDefault(), "Readjusting the start time of the next job to %s...", dateFormat.format(getNexStartDateTime().getTime())), handler);
        try{
            readjustPeriodicWork();
            LogUtils.info("  Done.", handler);
        } catch (Exception e) {
            LogUtils.error("Failure to readjust the start time of the next job. Will be retried.", e, handler);
            retryForFailing = true;
        }

        if (retryForFailing)
            return Result.retry();
        else
            return Result.success();
    }

    private void autoUpdateRules() throws Exception {
        ContentBlocker56 contentBlocker = ContentBlocker56.getInstance();
        if (firewall == null) {
            throw new Exception();
        }
        if (!FirewallUtils.getInstance().isCurrentDomainLimitAboveDefault()) {
            AdhellFactory.getInstance().updateAllProviders();
            if (!contentBlocker.isDomainRuleEmpty()) {
                LogUtils.info("Updating domain rules...", handler);
                contentBlocker.processWhitelistedApps(handler);
                contentBlocker.processWhitelistedDomains(handler);
                contentBlocker.processBlockedDomains(handler);
                AdhellFactory.getInstance().applyDns(handler);
            }
            if (!contentBlocker.isFirewallRuleEmpty()) {
                LogUtils.info("Updating firewall rules...", handler);
                contentBlocker.processCustomRules(handler);
                contentBlocker.processMobileRestrictedApps(handler);
                contentBlocker.processWifiRestrictedApps(handler);
            }
            List<String> denyList = BlockUrlUtils.getAllBlockedUrls(appDatabase);
            List<String> userList = new ArrayList<>(BlockUrlUtils.getUserBlockedUrls(appDatabase, false, null));
            denyList.addAll(userList);
            AppPreferences.getInstance().setBlockedDomainsCount(denyList.size());

            LogUtils.info("\nRules auto update completed.", handler);
        } else {
            LogUtils.info("Update not possible, the limit of the number of domains is exceeded!", handler);
            throw new ExceededLimitException();
        }
    }

    private void processAppComponentsInAutoUpdate() throws Exception {
        LogUtils.info(String.format(Locale.getDefault(), "Getting file '%s'...", AppComponentFactory.COMPONENTS_FILENAME), handler);
        DocumentFile componentsFile = FileUtils.getDocumentFile(AppComponentFactory.STORAGE_FOLDERS, AppComponentFactory.COMPONENTS_FILENAME, FileUtils.FileCreationType.IF_NOT_EXIST);

        LogUtils.info("Listing services, receivers and activities to be disabled...", handler);
        Set<String> compNames = AppComponentFactory.getInstance().getFileContent(componentsFile);

        if (compNames.size() > 0) {
            LogUtils.info("Updating disabled app components...", handler);
            int count = 0;
            List<AppInfo> apps = appDatabase.applicationInfoDao().getUserAndDisabledApps();
            for (AppInfo app : apps) {
                String packageName = app.packageName;
                Set<String> availableServiceNames = AppComponent.getServiceNames(packageName);
                Set<String> availableReceiverNames = AppComponent.getReceiverNames(packageName);
                Set<String> availableActivityNames = AppComponent.getActivityNames(packageName);
                for (String compName : compNames) {
                    boolean disable = false;
                    int permissionStatus = 0;

                    if (availableServiceNames.contains(compName)) {
                        disable = true;
                        permissionStatus = AppPermission.STATUS_SERVICE;
                        LogUtils.info(String.format(Locale.getDefault(), "Disabling service '%s' for package '%s'", compName, packageName), handler);
                    } else if (availableReceiverNames.contains(compName)) {
                        disable = true;
                        permissionStatus = AppPermission.STATUS_RECEIVER;
                        LogUtils.info(String.format(Locale.getDefault(), "Disabling receiver '%s' for package '%s'", compName, packageName), handler);
                    } else if (availableActivityNames.contains(compName)) {
                        disable = true;
                        permissionStatus = AppPermission.STATUS_ACTIVITY;
                        LogUtils.info(String.format(Locale.getDefault(), "Disabling activity '%s' for package '%s'", compName, packageName), handler);
                    }

                    if (disable) {
                        try {
                            count++;
                            boolean compState = AdhellFactory.getInstance().getComponentState(packageName, compName);
                            if (compState) {
                                ComponentName componentName = new ComponentName(packageName, compName);
                                ApplicationPolicy appPolicy = AdhellFactory.getInstance().getAppPolicy();
                                if (appPolicy != null) {
                                    boolean success = appPolicy.setApplicationComponentState(componentName, false);
                                    if (success) {
                                        AppPermission appService = new AppPermission();
                                        appService.packageName = packageName;
                                        appService.permissionName = compName;
                                        appService.permissionStatus = permissionStatus;
                                        appService.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                                        appDatabase.appPermissionDao().insert(appService);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            LogUtils.error("Unable to disable app components!", e, handler);
                        }
                    }
                }
            }
            if (count <= 0) {
                LogUtils.info("Nothing new to disable", handler);
            } else {
                LogUtils.info("Update for disabled app components completed.", handler);
            }
        } else {
            LogUtils.info("File is empty. Nothing to do.", handler);
        }
    }

    private void autoCleanDatabase() {
        int count = 0;

        // Disabled packages rules
        LogUtils.info("Cleaning disabled packages rules...", handler);
        List<DisabledPackage> disabledPackages = appDatabase.disabledPackageDao().getAll();
        for (DisabledPackage disabledPackage: disabledPackages) {
            if (!appCache.getNames().containsKey(disabledPackage.packageName)) {
                try {
                    LogUtils.info(String.format(Locale.getDefault(), "    Deleting rule for package: %s.", disabledPackage.packageName), handler);
                    appDatabase.disabledPackageDao().deleteByPackageName(disabledPackage.packageName);
                    LogUtils.info("    Done.", handler);
                } catch (Exception e) {
                    LogUtils.error("    Error deleting rule.", e, handler);
                }
                count++;
            }
        }
        if (count > 0)
            LogUtils.info("  Done.", handler);
        else
            LogUtils.info("  Nothing to clean up.", handler);

        // Restricted packages rules
        count = 0;
        LogUtils.info("Cleaning restricted packages rules...", handler);
        List<RestrictedPackage> restrictedPackages = appDatabase.restrictedPackageDao().getAll();
        for (RestrictedPackage restrictedPackage: restrictedPackages) {
            if (!appCache.getNames().containsKey(restrictedPackage.packageName)) {
                try {
                    LogUtils.info(String.format(Locale.getDefault(), "    Deleting rule for package: %s.", restrictedPackage.packageName), handler);
                    appDatabase.restrictedPackageDao().deleteByPackageName(restrictedPackage.packageName, restrictedPackage.type);
                    LogUtils.info("    Done.", handler);
                } catch (Exception e) {
                    LogUtils.error("    Error deleting rule.", e, handler);
                }
                count++;
            }
        }
        if (count > 0)
            LogUtils.info("  Done.", handler);
        else
            LogUtils.info("  Nothing to clean up.", handler);

        // Firewall whitelisted packages rules
        count = 0;
        LogUtils.info("Cleaning firewall whitelisted packages rules...", handler);
        List<FirewallWhitelistedPackage> whitelistedPackages = appDatabase.firewallWhitelistedPackageDao().getAll();
        for (FirewallWhitelistedPackage whitelistedPackage: whitelistedPackages) {
            if (!appCache.getNames().containsKey(whitelistedPackage.packageName)) {
                try {
                    LogUtils.info(String.format(Locale.getDefault(), "    Deleting rule for package: %s.", whitelistedPackage.packageName), handler);
                    appDatabase.firewallWhitelistedPackageDao().deleteByPackageName(whitelistedPackage.packageName);
                    LogUtils.info("    Done.", handler);
                } catch (Exception e) {
                    LogUtils.error("    Error deleting rule.", e, handler);
                }
                count++;
            }
        }
        if (count > 0)
            LogUtils.info("  Done.", handler);
        else
            LogUtils.info("  Nothing to clean up.", handler);

        // DNS packages rules
        count = 0;
        LogUtils.info("Cleaning DNS packages rules...", handler);
        List<DnsPackage> dnsPackages = appDatabase.dnsPackageDao().getAll();
        for (DnsPackage dnsPackage: dnsPackages) {
            if (!appCache.getNames().containsKey(dnsPackage.packageName)) {
                try {
                    LogUtils.info(String.format(Locale.getDefault(), "    Deleting rule for package: %s.", dnsPackage.packageName), handler);
                    appDatabase.dnsPackageDao().deleteByPackageName(dnsPackage.packageName);
                    LogUtils.info("    Done.", handler);
                } catch (Exception e) {
                    LogUtils.error("    Error deleting rule.", e, handler);
                }
                count++;
            }
        }
        if (count > 0)
            LogUtils.info("  Done.", handler);
        else
            LogUtils.info("  Nothing to clean up.", handler);

        // App component restriction packages rules
        count = 0;
        LogUtils.info("Cleaning app component restriction packages rules...", handler);
        List<AppPermission> appPermissionsPackages = appDatabase.appPermissionDao().getAll();
        for (AppPermission appPermissionsPackage : appPermissionsPackages) {
            if (!appCache.getNames().containsKey(appPermissionsPackage.packageName)) {
                try {
                    LogUtils.info(String.format(Locale.getDefault(), "    Deleting rules for package: %s.", appPermissionsPackage.packageName), handler);
                    appDatabase.appPermissionDao().deletePermissions(appPermissionsPackage.packageName);
                    appDatabase.appPermissionDao().deleteServices(appPermissionsPackage.packageName);
                    appDatabase.appPermissionDao().deleteReceivers(appPermissionsPackage.packageName);
                    appDatabase.appPermissionDao().deleteActivities(appPermissionsPackage.packageName);
                    LogUtils.info("    Done.", handler);
                } catch (Exception e) {
                    LogUtils.error("    Error deleting rule.", e, handler);
                }
                count++;
            }
        }
        if (count > 0)
            LogUtils.info("  Done.", handler);
        else
            LogUtils.info("  Nothing to clean up.", handler);

    }

    private void readjustPeriodicWork() {
        WorkManager workManager = WorkManager.getInstance(App.getAppContext());
        long nextJobMillis = getNexStartDateTime().getTimeInMillis();
        long nowMillis = Calendar.getInstance().getTimeInMillis();
        long initialDelay = nextJobMillis - nowMillis;

        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(AutoUpdateWorker.class)
                .setConstraints(AutoUpdateDialogFragment.getAutoUpdateConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build();

        // Cancel previous job
        workManager.cancelUniqueWork(AutoUpdateDialogFragment.AUTO_UPDATE_WORK_TAG);

        // Enqueue new job with readjusting initial delay
        workManager.enqueueUniqueWork(AutoUpdateDialogFragment.AUTO_UPDATE_WORK_TAG, ExistingWorkPolicy.REPLACE , workRequest);
    }

    private Calendar getNexStartDateTime() {
        Calendar calendar = Calendar.getInstance();
        int repeatInterval = AutoUpdateDialogFragment.intervalArray[AppPreferences.getInstance().getAutoUpdateInterval()];

        calendar.set(Calendar.HOUR_OF_DAY, AppPreferences.getInstance().getStartHourAutoUpdate());
        calendar.set(Calendar.MINUTE, AppPreferences.getInstance().getStartMinuteAutoUpdate());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        calendar.add(Calendar.DAY_OF_MONTH, repeatInterval);

        return calendar;
    }
}