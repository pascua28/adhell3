package com.fusionjack.adhell3.tasks;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class RulesUpdateWorker extends Worker {
    private final AppDatabase appDatabase;
    private final Firewall firewall;
    private int retryCount;
    private Handler handler = null;

    public RulesUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
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
        firewall = AdhellFactory.getInstance().getFirewall();
    }

    @NonNull
    @Override
    public Result doWork() {
        retryCount = this.getRunAttemptCount();
        if (retryCount > AutoUpdateDialogFragment.MAX_RETRY) {
            AutoUpdateDialogFragment.enqueueNextAutoUpdateWork();
            return Result.failure();
        }

        LogUtils.info("------Start Rules auto update------", handler);
        try {
            autoUpdateRules();
        } catch (Exception e) {
            LogUtils.error("Failed Rules auto update! Will be retried.", e, handler);
            LogUtils.info("------Failed Rules auto update------", handler);
            return Result.retry();
        }
        LogUtils.info("------Successful Rules auto update------", handler);

        return Result.success();
    }

    private void autoUpdateRules() throws Exception {
        ContentBlocker56 contentBlocker = ContentBlocker56.getInstance();
        contentBlocker.setHandler(handler);
        if (firewall == null) {
            throw new Exception();
        }
        AdhellFactory.getInstance().updateAllProviders();
        String domainRulesText = "Updating domain rules...";
        String firewallRulesText = "Updating firewall rules...";
        boolean domainRulesNeedUpdate = !contentBlocker.isDomainRuleEmpty();
        boolean firewallRulesNeedUpdate = !contentBlocker.isFirewallRuleEmpty();
        if (FirewallUtils.getInstance().isCurrentDomainLimitAboveDefault()) {
            if (domainRulesNeedUpdate) {
                contentBlocker.disableDomainRules();
                AppPreferences.getInstance().setDomainRulesToggle(true);
                domainRulesText = "Enabling domain rules...";
            }
            if (firewallRulesNeedUpdate) {
                contentBlocker.disableFirewallRules();
                AppPreferences.getInstance().setFirewallRulesToggle(true);
                firewallRulesText = "Enabling firewall rules...";
            }
        }
        if (domainRulesNeedUpdate) {
            LogUtils.info(domainRulesText, handler);
            contentBlocker.setAllActiveRules();
            contentBlocker.processWhitelistedApps(handler);
            contentBlocker.processWhitelistedDomains(handler);
            contentBlocker.processBlockedDomains(handler);
            contentBlocker.applyDns(handler);
        }
        if (firewallRulesNeedUpdate) {
            LogUtils.info(firewallRulesText, handler);
            contentBlocker.processCustomRules(handler);
            contentBlocker.processMobileRestrictedApps(handler);
            contentBlocker.processWifiRestrictedApps(handler);
        }

        if (domainRulesNeedUpdate || firewallRulesNeedUpdate) {
            List<String> denyList = BlockUrlUtils.getAllBlockedUrls(appDatabase);
            List<String> userList = new ArrayList<>(BlockUrlUtils.getUserBlockedUrls(appDatabase, false, null));
            denyList.addAll(userList);
            AppPreferences.getInstance().setBlockedDomainsCount(denyList.size());

            if (!firewall.isFirewallEnabled()) {
                LogUtils.info("\nEnabling Knox firewall...", handler);
                firewall.enableFirewall(true);
                LogUtils.info("Knox firewall is enabled.", handler);
            }
            if (!firewall.isDomainFilterReportEnabled()) {
                LogUtils.info("\nEnabling firewall report...", handler);
                firewall.enableDomainFilterReport(true);
                LogUtils.info("Firewall report is enabled.", handler);
            }
            LogUtils.info("\nRules auto update completed.", handler);
        } else {
            LogUtils.info("Domain and firewall rules are disabled. Nothing to update.", handler);
        }
    }
}