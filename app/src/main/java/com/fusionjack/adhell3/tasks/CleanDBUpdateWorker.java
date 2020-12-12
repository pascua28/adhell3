package com.fusionjack.adhell3.tasks;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppPermission;
import com.fusionjack.adhell3.db.entity.DisabledPackage;
import com.fusionjack.adhell3.db.entity.DnsPackage;
import com.fusionjack.adhell3.db.entity.FirewallWhitelistedPackage;
import com.fusionjack.adhell3.db.entity.RestrictedPackage;
import com.fusionjack.adhell3.dialogfragment.AutoUpdateDialogFragment;
import com.fusionjack.adhell3.utils.AppCache;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.LogUtils;

import java.text.DateFormat;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

public class CleanDBUpdateWorker extends Worker {
    private final AppDatabase appDatabase;
    private int retryCount;
    private Handler handler = null;

    public CleanDBUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        if (AppPreferences.getInstance().getCreateLogOnAutoUpdate()) {
            DocumentFile logFile = LogUtils.getAutoUpdateLogFile();
            this.handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    String nowDate = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new java.util.Date());
                    if (msg.obj != null) {
                        LogUtils.appendLogFile(String.format(Locale.getDefault(), "%s [retry%d]: %s", nowDate, retryCount, msg.obj.toString().trim()), logFile);
                    }
                }
            };
        }
        appDatabase = AppDatabase.getAppDatabase(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        retryCount = this.getRunAttemptCount();
        if (retryCount > AutoUpdateDialogFragment.MAX_RETRY) {
            AutoUpdateDialogFragment.enqueueNextAutoUpdateWork();
            return Result.failure();
        }

        if (AppPreferences.getInstance().getCleanDBOnAutoUpdate()) {
            LogUtils.info("------Start auto clean database------", handler);
            try {
                LogUtils.info("Getting app cache instance...", handler);
                AppCache appCache = AppCache.getInstanceSync(handler);
                cleanDatabase(appDatabase, appCache, handler);
            } catch (Exception e) {
                LogUtils.error("Failed auto clean database! Will be retried.", e, handler);
                LogUtils.info("------Failed auto clean database------", handler);
                return Result.retry();
            }
            LogUtils.info("------Successful auto clean database------", handler);
        } else {
            LogUtils.info("------Auto clean database is disable------", handler);
        }

        return Result.success();
    }

    public static void cleanDatabase(AppDatabase appDatabase, AppCache appCache, Handler handler) {
        int count = 0;

        // Disabled packages rules
        LogUtils.info("Cleaning disabled packages rules...", handler);
        List<DisabledPackage> disabledPackages = appDatabase.disabledPackageDao().getAll();
        for (DisabledPackage disabledPackage: disabledPackages) {
            if (!appCache.getNames().containsKey(disabledPackage.packageName)) {
                try {
                    LogUtils.info(String.format("    Deleting rule for package: %s.", disabledPackage.packageName), handler);
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
                    LogUtils.info(String.format("    Deleting rule for package: %s.", restrictedPackage.packageName), handler);
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
                    LogUtils.info(String.format("    Deleting rule for package: %s.", whitelistedPackage.packageName), handler);
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

        // Domain whitelist packages rules
        count = 0;
        LogUtils.info("Cleaning Domain whitelist packages rules...", handler);
        List<String> whiteUrls = appDatabase.whiteUrlDao().getAll3();
        for (String whiteUrl : whiteUrls) {
            if (whiteUrl.indexOf('|') != -1) {
                StringTokenizer tokens = new StringTokenizer(whiteUrl, "|");
                if (tokens.countTokens() == 2) {
                    final String packageName = tokens.nextToken();
                    if (!appCache.getNames().containsKey(packageName)) {
                        try {
                            LogUtils.info(String.format("    Deleting rule for package: %s.", packageName), handler);
                            appDatabase.whiteUrlDao().deleteByUrl(whiteUrl);
                        } catch (Exception e) {
                            LogUtils.error("    Error deleting rule.", e, handler);
                        }
                        count++;
                    }
                }
            }
        }
        if (count > 0)
            LogUtils.info("  Done.", handler);
        else
            LogUtils.info("  Nothing to clean up.", handler);

        // Domain blacklist packages rules
        count = 0;
        LogUtils.info("Cleaning Domain blacklist packages rules...", handler);
        List<String> blockUrls = appDatabase.userBlockUrlDao().getAll3();
        for (String blockUrl : blockUrls) {
            if (blockUrl.indexOf('|') != -1) {
                StringTokenizer tokens = new StringTokenizer(blockUrl, "|");
                if (tokens.countTokens() == 2) {
                    final String packageName = tokens.nextToken();
                    if (!appCache.getNames().containsKey(packageName)) {
                        try {
                            LogUtils.info(String.format("    Deleting rule for package: %s.", packageName), handler);
                            appDatabase.userBlockUrlDao().deleteByUrl(blockUrl);
                        } catch (Exception e) {
                            LogUtils.error("    Error deleting rule.", e, handler);
                        }
                        count++;
                    }
                }
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
                    LogUtils.info(String.format("    Deleting rule for package: %s.", dnsPackage.packageName), handler);
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
                    LogUtils.info(String.format("    Deleting rules for package: %s.", appPermissionsPackage.packageName), handler);
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
}
