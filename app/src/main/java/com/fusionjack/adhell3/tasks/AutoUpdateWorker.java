package com.fusionjack.adhell3.tasks;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.BackoffPolicy;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.blocker.ContentBlocker56;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.dialogfragment.AutoUpdateDialogFragment;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.BlockUrlUtils;
import com.fusionjack.adhell3.utils.FirewallUtils;
import com.fusionjack.adhell3.utils.LogUtils;
import com.samsung.android.knox.net.firewall.Firewall;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class AutoUpdateWorker extends Worker {
    private static AppDatabase appDatabase;
    private static Firewall firewall;
    private int retryCount;
    private Handler handler = null;

    private class ExceededLimitException extends Exception{}

    public AutoUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
        AutoUpdateWorker.appDatabase = AppDatabase.getAppDatabase(context);
        AutoUpdateWorker.firewall = AdhellFactory.getInstance().getFirewall();

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
    }

    @NonNull
    @Override
    public Result doWork() {
        retryCount = this.getRunAttemptCount();
        if (retryCount > 5) {
            return Result.failure();
        }
        LogUtils.info("------Start Auto update------", handler);
        try {
            autoUpdateRules();
        } catch (ExceededLimitException e) {
            LogUtils.info("------Failed Auto update------", handler);
            return Result.failure();
        } catch (Exception e) {
            LogUtils.error("Failed auto update! Will be retried.", e, handler);
            LogUtils.info("------Failed Auto update------", handler);
            return Result.retry();
        }
        LogUtils.info("------Successful Auto update------", handler);
        LogUtils.info(String.format("Readjusting the start time of the next job to %s...", getNexStartDateTime()), handler);
        try{
            readjustPeriodicWork();
            LogUtils.info("  Done.", handler);
        } catch (Exception e) {
            LogUtils.error("Failure to readjust the start time of the next job.", e, handler);
        }
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

            LogUtils.info("\nAuto update completed.", handler);
        } else {
            LogUtils.info("Update not possible, the limit of the number of domains is exceeded!", handler);
            throw new ExceededLimitException();
        }
    }

    private void readjustPeriodicWork() {
        WorkManager workManager = WorkManager.getInstance(App.getAppContext());
        int repeatInterval = AppPreferences.getInstance().getAutoUpdateInterval();
        PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(AutoUpdateWorker.class, repeatInterval, TimeUnit.HOURS)
                .setConstraints(AutoUpdateDialogFragment.getAutoUpdateConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .setInitialDelay(
                        AutoUpdateDialogFragment.getInitialDelayForScheduleWork(
                                AppPreferences.getInstance().getStartHourAutoUpdate(),
                                AppPreferences.getInstance().getStartMinuteAutoUpdate()
                        ), TimeUnit.MILLISECONDS
                )
                .build();

        // Cancel previous job
        workManager.cancelUniqueWork(AutoUpdateDialogFragment.AUTO_UPDATE_WORK_TAG);

        // Enqueue new job with readjusting initial delay
        workManager.enqueueUniquePeriodicWork(AutoUpdateDialogFragment.AUTO_UPDATE_WORK_TAG, ExistingPeriodicWorkPolicy.REPLACE , workRequest);
    }

    private String getNexStartDateTime() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        int repeatInterval = AutoUpdateDialogFragment.intervalArray[AppPreferences.getInstance().getAutoUpdateInterval()];

        calendar.set(Calendar.HOUR_OF_DAY, AppPreferences.getInstance().getStartHourAutoUpdate());
        calendar.set(Calendar.MINUTE, AppPreferences.getInstance().getStartMinuteAutoUpdate());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        calendar.add(Calendar.HOUR_OF_DAY, repeatInterval);

        return dateFormat.format(calendar.getTime());
    }
}