package com.fusionjack.adhell3.utils;

import android.os.Handler;

import androidx.annotation.NonNull;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.ReportBlockedUrl;
import com.samsung.android.knox.AppIdentity;
import com.samsung.android.knox.net.firewall.DomainFilterReport;
import com.samsung.android.knox.net.firewall.DomainFilterRule;
import com.samsung.android.knox.net.firewall.Firewall;
import com.samsung.android.knox.net.firewall.FirewallResponse;
import com.samsung.android.knox.net.firewall.FirewallRule;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public final class FirewallUtils {
    private static FirewallUtils instance;

    private final Firewall firewall;
    private final AppDatabase appDatabase;

    private FirewallUtils() {
        firewall = AdhellFactory.getInstance().getFirewall();
        appDatabase = AdhellFactory.getInstance().getAppDatabase();
    }

    public static FirewallUtils getInstance() {
        if (instance == null) {
            instance = new FirewallUtils();
        }
        return instance;
    }

    public void addDomainFilterRules(List<DomainFilterRule> domainRules, Handler handler) throws Exception {
        if (firewall == null) {
            throw new Exception("Knox Firewall is not initialized");
        }

        try {
            FirewallResponse[] response = firewall.addDomainFilterRules(domainRules);
            handleResponse(response, handler);
        } catch (SecurityException ex) {
            // Missing required MDM permission
            LogUtils.error("Failed to add domain filter rule to Knox Firewall", ex, handler);
        }
    }

    public void addFirewallRules(FirewallRule[] firewallRules, Handler handler) throws Exception {
        if (firewall == null) {
            throw new Exception("Knox Firewall is not initialized");
        }

        try {
            FirewallResponse[] response = firewall.addRules(firewallRules);
            handleResponse(response, handler);
        } catch (SecurityException ex) {
            // Missing required MDM permission
            LogUtils.error("Failed to add firewall rules to Knox Firewall", ex, handler);
        }
    }

    public FirewallRule[] createFirewallRules(String packageName, Firewall.NetworkInterface networkInterface) {
        FirewallRule[] rules = new FirewallRule[2];

        rules[0] = new FirewallRule(FirewallRule.RuleType.DENY, Firewall.AddressType.IPV4);
        rules[0].setNetworkInterface(networkInterface);
        rules[0].setApplication(new AppIdentity(packageName, null));

        rules[1] = new FirewallRule(FirewallRule.RuleType.DENY, Firewall.AddressType.IPV6);
        rules[1].setNetworkInterface(networkInterface);
        rules[1].setApplication(new AppIdentity(packageName, null));

        return rules;
    }

    public DomainStat getDomainStatFromKnox() {
        DomainStat stat = new DomainStat();
        if (firewall == null) {
            return stat;
        }

        if (BlockUrlUtils.isDomainLimitAboveDefault()) {
            // If the domain count more than 15k, calling firewall.getDomainFilterRules() might crash the firewall
            stat.blackListSize = AppPreferences.getInstance().getBlockedDomainsCount();
            stat.whiteListSize = appDatabase.whiteUrlDao().getAll3().size();
        } else {
            List<String> packageNameList = new ArrayList<>();
            packageNameList.add(Firewall.FIREWALL_ALL_PACKAGES);
            List<DomainFilterRule> domainRules = firewall.getDomainFilterRules(packageNameList);
            if (domainRules != null && domainRules.size() > 0) {
                stat.blackListSize = domainRules.get(0).getDenyDomains().size();
                stat.whiteListSize = domainRules.get(0).getAllowDomains().size();
            }
        }
        stat.whiteAppsSize = appDatabase.applicationInfoDao().getWhitelistedApps().size();

        return stat;
    }

    public int getWhitelistAppCountFromKnox() {
        if (firewall == null) {
            return 0;
        }

        int whitelistedSize = 0;
        List<String> packageNameList = new ArrayList<>();
        List<AppInfo> appInfos = appDatabase.applicationInfoDao().getWhitelistedApps();
        for (AppInfo appInfo : appInfos) {
            packageNameList.clear();
            packageNameList.add(appInfo.packageName);
            List<DomainFilterRule> domainRules = firewall.getDomainFilterRules(packageNameList);
            if (domainRules != null && domainRules.size() > 0) {
                whitelistedSize += domainRules.get(0).getAllowDomains().size();
            }
        }
        return whitelistedSize;
    }

    public FirewallStat getFirewallStatFromKnox() {
        FirewallStat stat = new FirewallStat();
        if (firewall == null) {
            return stat;
        }

        FirewallRule[] firewallRules = firewall.getRules(Firewall.FIREWALL_DENY_RULE, null);
        if (firewallRules != null) {
            for (FirewallRule firewallRule : firewallRules) {
                Firewall.NetworkInterface networkInterfaces = firewallRule.getNetworkInterface();
                switch (networkInterfaces) {
                    case ALL_NETWORKS:
                        stat.allNetworkSize++;
                        break;
                    case MOBILE_DATA_ONLY:
                        stat.mobileDataSize++;
                        break;
                    case WIFI_DATA_ONLY:
                        stat.wifiDataSize++;
                        break;
                }
            }
        }
        return stat.dividedByTwo();
    }

    public List<ReportBlockedUrl> getReportBlockedUrls() {
        return appDatabase.reportBlockedUrlDao().getAll();
    }

    public void fetchReportBlockedUrlLastXHours() {
        if (firewall == null) {
            return;
        }

        List<String> packageNames = appDatabase.applicationInfoDao().getAllPackageNames();
        List<DomainFilterReport> reports = firewall.getDomainFilterReport(packageNames);
        if (reports == null) {
            return;
        }

        appDatabase.reportBlockedUrlDao().deleteBefore(last_x_hours_db());

        ReportBlockedUrl lastBlockedUrl = appDatabase.reportBlockedUrlDao().getLastBlockedDomain();
        long lastBlockedTimestamp = 0;
        if (lastBlockedUrl != null) {
            lastBlockedTimestamp = lastBlockedUrl.blockDate / 1000;
        }

        List<ReportBlockedUrl> reportBlockedUrls = new ArrayList<>();
        for (DomainFilterReport b : reports) {
            if (b.getTimeStamp() > lastBlockedTimestamp) {
                ReportBlockedUrl reportBlockedUrl =
                        new ReportBlockedUrl(b.getDomainUrl(), b.getPackageName(), b.getTimeStamp() * 1000);
                reportBlockedUrls.add(reportBlockedUrl);
            }
        }
        appDatabase.reportBlockedUrlDao().insertAll(reportBlockedUrls);
    }

    public List<ReportBlockedUrl> getReportBlockedUrlLastXHours() {
        fetchReportBlockedUrlLastXHours();
        return appDatabase.reportBlockedUrlDao().getReportBlockUrlAfter(last_x_hours_ui());
    }

    public long last_x_hours_ui() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, -BuildConfig.BLOCKED_DOMAIN_DURATION_UI);
        return cal.getTimeInMillis();
    }

    private long last_x_hours_db() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.HOUR_OF_DAY, -BuildConfig.BLOCKED_DOMAIN_DURATION_DB);
        return cal.getTimeInMillis();
    }

    private void handleResponse(FirewallResponse[] response, Handler handler) throws Exception {
        if (response == null) {
            Exception ex = new Exception("There was no response from Knox Firewall");
            LogUtils.error("There was no response from Knox Firewall", ex, handler);
            throw ex;
        } else {
            if (FirewallResponse.Result.SUCCESS == response[0].getResult()) {
                LogUtils.info("Result: Success", handler);
            } else {
                LogUtils.info("Result: Failed", handler);
                Exception ex = new Exception(response[0].getMessage());
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                ex.printStackTrace(pw);
                LogUtils.error(sw.toString(), ex, handler);
                throw ex;
            }
        }
    }

    public static class FirewallStat {
        public int allNetworkSize;
        public int mobileDataSize;
        public int wifiDataSize;

        private static final String DELIMITER = "~";

        public FirewallStat() {
        }

        public FirewallStat(int allNetworkSize, int mobileDataSize, int wifiDataSize) {
            this.allNetworkSize = allNetworkSize;
            this.mobileDataSize = mobileDataSize;
            this.wifiDataSize = wifiDataSize;
        }

        public static FirewallStat toStat(String statStr) {
            FirewallStat stat = new FirewallStat();
            if (statStr != null) {
                String[] array = statStr.split(DELIMITER);
                if (array.length == 3) {
                    stat.allNetworkSize = Integer.parseInt(array[0]);
                    stat.mobileDataSize = Integer.parseInt(array[1]);
                    stat.wifiDataSize = Integer.parseInt(array[2]);
                }
            }
            return stat;
        }

        @NonNull
        @Override
        public String toString() {
            return allNetworkSize + DELIMITER + mobileDataSize + DELIMITER + wifiDataSize;
        }

        // Knox Firewall stores for both IPv4 and IPv6
        public FirewallStat dividedByTwo() {
            if (allNetworkSize > 0) allNetworkSize = allNetworkSize / 2;
            if (mobileDataSize > 0) mobileDataSize = mobileDataSize / 2;
            if (wifiDataSize > 0) wifiDataSize = wifiDataSize / 2;
            return this;
        }
    }

    public static class DomainStat {
        public int blackListSize;
        public int whiteListSize;
        public int whiteAppsSize;

        private static final String DELIMITER = "~";

        public DomainStat() {
        }

        public DomainStat(int blackListSize, int whiteListSize, int whiteAppsSize) {
            this.blackListSize = blackListSize;
            this.whiteListSize = whiteListSize;
            this.whiteAppsSize = whiteAppsSize;
        }

        public static DomainStat toStat(String statStr) {
            DomainStat stat = new DomainStat();
            if (statStr != null) {
                String[] array = statStr.split(DELIMITER);
                if (array.length == 3) {
                    stat.blackListSize = Integer.parseInt(array[0]);
                    stat.whiteListSize = Integer.parseInt(array[1]);
                    stat.whiteAppsSize = Integer.parseInt(array[2]);
                }
            }
            return stat;
        }

        @NonNull
        @Override
        public String toString() {
            return blackListSize + DELIMITER + whiteListSize + DELIMITER + whiteAppsSize;
        }
    }
}
