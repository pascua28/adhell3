package com.fusionjack.adhell3.utils;

import android.os.Handler;

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


    public void removeDomainFilterRules(List<DomainFilterRule> domainRules, Handler handler) throws Exception {
        if (firewall == null) {
            throw new Exception("Knox Firewall is not initialized");
        }

        try {
            FirewallResponse[] response = firewall.removeDomainFilterRules(domainRules);
            handleResponse(response, handler);
        } catch (SecurityException ex) {
            // Missing required MDM permission
            LogUtils.error("Failed to remove domain filter rule to Knox Firewall", ex, handler);
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

    public void removeFirewallRules(FirewallRule[] firewallRules, Handler handler) throws Exception {
        if (firewall == null) {
            throw new Exception("Knox Firewall is not initialized");
        }

        try {
            FirewallResponse[] response = firewall.removeRules(firewallRules);
            handleResponse(response, handler);
        } catch (SecurityException ex) {
            // Missing required MDM permission
            LogUtils.error("Failed to remove firewall rules to Knox Firewall", ex, handler);
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
            stat.whiteListSize = AppPreferences.getInstance().getWhitelistedDomainsCount();
        } else {
            List<String> packageNameList = new ArrayList<>();
            packageNameList.add(Firewall.FIREWALL_ALL_PACKAGES);
            List<DomainFilterRule> domainRules = firewall.getDomainFilterRules(packageNameList);
            if (domainRules != null && domainRules.size() > 0) {
                stat.blackListSize = domainRules.get(0).getDenyDomains().size();
                stat.whiteListSize = domainRules.get(0).getAllowDomains().size();
            } else {
                stat.blackListSize = AppPreferences.getInstance().getBlockedDomainsCount();
                stat.whiteListSize = AppPreferences.getInstance().getWhitelistedDomainsCount();
            }
        }
        return stat;
    }

    public boolean isCurrentDomainLimitAboveDefault() {
        int domainLimit = getDomainStatFromKnox().blackListSize + getDomainStatFromKnox().whiteListSize;
        return domainLimit > BlockUrlUtils.MAX_DOMAIN_KNOX_LIMIT;
    }

    public List<String> getActiveDenyDomainsFromKnox() {
        List<String> returnDomainRules = new ArrayList<>();
        if (firewall == null) {
            return returnDomainRules;
        }

        List<String> packageNameList = new ArrayList<>();
        packageNameList.add("*");
        List<DomainFilterRule> domainRules = firewall.getDomainFilterRules(packageNameList);
        for (DomainFilterRule denyDomain : domainRules) {
            if (denyDomain.getApplication().getPackageName().equals("*")) {
                for (String denyUrl : denyDomain.getDenyDomains()) {
                    if (!returnDomainRules.contains(denyUrl)) returnDomainRules.add(denyUrl);
                }
            }
        }
        return returnDomainRules;
    }

    public List<DomainFilterRule> getDomainFilterRuleForAllAppsFromKnox() {
        List<AppInfo> appList = appDatabase.applicationInfoDao().getAppsInDisabledOrder();
        List<DomainFilterRule> domainFilterRules = new ArrayList<>();
        if (firewall == null) {
            return domainFilterRules;
        }
        for (AppInfo app : appList) {
            List<String> packageNameList = new ArrayList<>();
            packageNameList.add(app.packageName);
            domainFilterRules.addAll(firewall.getDomainFilterRules(packageNameList));
        }
        return domainFilterRules;
    }

    public List<DomainFilterRule> getWhitelistedAppsFromKnox(List<DomainFilterRule> allDomainFilterRules) {
        List<DomainFilterRule> whitelistApps = new ArrayList<>();
        if (allDomainFilterRules != null) {
            for (DomainFilterRule domainRule : allDomainFilterRules) {
                if (domainRule.getAllowDomains().contains("*")) {
                    if (domainRule.getAllowDomains().size() > 0 ||
                            domainRule.getDenyDomains().size() > 0 ||
                            domainRule.getDns1() == null ||
                            domainRule.getDns2() == null
                    ) {
                        whitelistApps.add(domainRule);
                    }
                }
            }
        }
        return whitelistApps;
    }

    public List<DomainFilterRule> getWhitelistUrlAppsFromKnox(List<DomainFilterRule> allDomainFilterRules) {
        List<DomainFilterRule> whitelistUrl = new ArrayList<>();
        if (allDomainFilterRules != null) {
            for (DomainFilterRule domainRule : allDomainFilterRules) {
                if (!domainRule.getAllowDomains().contains("*")) {
                    if (domainRule.getAllowDomains().size() > 0 ||
                            domainRule.getDenyDomains().size() > 0 ||
                            domainRule.getDns1() == null ||
                            domainRule.getDns2() == null
                    ) {
                        whitelistUrl.add(domainRule);
                    }
                }
            }
        }
        return whitelistUrl;
    }

    public List<DomainFilterRule> getCustomDnsAppsFromKnox(List<DomainFilterRule> allDomainFilterRules) {
        List<DomainFilterRule> customDnsRules = new ArrayList<>();
        if (allDomainFilterRules != null) {
            for (DomainFilterRule domainRule : allDomainFilterRules) {
                if (domainRule.getDns1() != null && domainRule.getDns2() != null) {
                    customDnsRules.add(domainRule);
                }
            }
        }
        return customDnsRules;
    }

    public List<DomainFilterRule> getCustomDnsAppsFromKnox(String packageName) {
        List<AppInfo> appList = new ArrayList<>();
        AppInfo allAppInfo = new AppInfo();
        allAppInfo.packageName = packageName;
        appList.add(allAppInfo);
        List<DomainFilterRule> customDnsRules = new ArrayList<>();
        if (firewall == null) {
            return customDnsRules;
        }
        for (AppInfo app : appList) {
            List<String> packageNameList = new ArrayList<>();
            packageNameList.add(app.packageName);
            List<DomainFilterRule> domainRules = firewall.getDomainFilterRules(packageNameList);
            for (DomainFilterRule domainRule : domainRules) {
                if (domainRule.getDns1() != null && domainRule.getDns2() != null) {
                    customDnsRules.add(domainRule);
                }
            }
        }
        return customDnsRules;
    }

    public List<String> getWhitelistUrlAllAppsFromKnox() {
        List<AppInfo> appList = new ArrayList<>();
        AppInfo allAppInfo = new AppInfo();
        allAppInfo.packageName = "*";
        appList.add(allAppInfo);
        List<String> whitelistApps = new ArrayList<>();
        if (firewall == null) {
            return whitelistApps;
        }
        for (AppInfo app : appList) {
            List<String> packageNameList = new ArrayList<>();
            packageNameList.add(app.packageName);
            List<DomainFilterRule> domainRules = firewall.getDomainFilterRules(packageNameList);
            for (DomainFilterRule domainRule : domainRules) {
                if (!domainRule.getAllowDomains().contains("*")) {
                    whitelistApps.addAll(domainRule.getAllowDomains());
                }
            }
        }
        return whitelistApps;
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
        return stat;
    }

    public List<ReportBlockedUrl> getReportBlockedUrl() {
        updateReportBlockedUrl();
        return appDatabase.reportBlockedUrlDao().getReportBlockUrlBetween(tree_days(), System.currentTimeMillis());
    }

    public void updateReportBlockedUrl() {
        List<ReportBlockedUrl> reportBlockedUrls = new ArrayList<>();
        if (firewall == null) {
            return;
        }

        List<DomainFilterReport> reports = firewall.getDomainFilterReport(null);
        if (reports == null) {
            return;
        }

        long time = tree_days();
        AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
        appDatabase.reportBlockedUrlDao().deleteBefore(time);

        ReportBlockedUrl lastBlockedUrl = appDatabase.reportBlockedUrlDao().getLastBlockedDomain();
        long lastBlockedTimestamp = 0;
        if (lastBlockedUrl != null) {
            lastBlockedTimestamp = lastBlockedUrl.blockDate / 1000;
        }

        for (DomainFilterReport b : reports) {
            if (b.getTimeStamp() > lastBlockedTimestamp) {
                ReportBlockedUrl reportBlockedUrl =
                        new ReportBlockedUrl(b.getDomainUrl(), b.getPackageName(), b.getTimeStamp() * 1000);
                reportBlockedUrls.add(reportBlockedUrl);
            }
        }
        appDatabase.reportBlockedUrlDao().insertAll(reportBlockedUrls);
    }

    public static long tree_days() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -3);
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
        public int mobileDataSize;
        public int wifiDataSize;
        public int allNetworkSize;
    }

    public static class DomainStat {
        public int blackListSize;
        public int whiteListSize;
    }
}
