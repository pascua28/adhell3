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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class FirewallUtils {
    private static FirewallUtils instance;
    private final Firewall firewall;
    private final AppDatabase appDatabase;

    private final static String allPackageName = "*";

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
        if (firewall == null) {
            return new ArrayList<>();
        }
        return firewall.getDomainFilterRules(Collections.singletonList(allPackageName)).stream()
                .filter(domainFilterRule -> domainFilterRule.getApplication().getPackageName().equals(allPackageName))
                .flatMap(domainFilterRule -> domainFilterRule.getDenyDomains().stream())
                .collect(Collectors.toList());
    }

    public List<DomainFilterRule> getDomainFilterRuleForAllAppsFromKnox() {
        if (firewall == null) {
            return new ArrayList<>();
        }
        return appDatabase.applicationInfoDao().getAllApps().stream()
                .flatMap(appInfo -> {
                    List<DomainFilterRule> domainFilterRuleList = firewall.getDomainFilterRules(Collections.singletonList(appInfo.packageName));
                    return domainFilterRuleList == null
                            ? Stream.empty()
                            : domainFilterRuleList.stream();
                })
                .collect(Collectors.toList());
    }

    public List<DomainFilterRule> getWhitelistedAppsFromKnox(List<DomainFilterRule> allDomainFilterRules) {
        if (allDomainFilterRules != null) {
            return allDomainFilterRules.stream()
                    .filter(domainFilterRule -> domainFilterRule.getAllowDomains().contains(allPackageName) &&
                            (
                                    domainFilterRule.getAllowDomains().size() > 0 ||
                                    domainFilterRule.getDenyDomains().size() > 0 ||
                                    domainFilterRule.getDns1() == null ||
                                    domainFilterRule.getDns2() == null
                            )
                    )
                    .collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    public List<DomainFilterRule> getWhitelistUrlAppsFromKnox(List<DomainFilterRule> allDomainFilterRules) {
        if (allDomainFilterRules != null) {
            return allDomainFilterRules.stream()
                    .filter(domainFilterRule -> !domainFilterRule.getAllowDomains().contains(allPackageName) &&
                            (
                                    domainFilterRule.getAllowDomains().size() > 0 ||
                                    domainFilterRule.getDenyDomains().size() > 0 ||
                                    domainFilterRule.getDns1() == null ||
                                    domainFilterRule.getDns2() == null
                            )
                    )
                    .collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    public List<DomainFilterRule> getCustomDnsAppsFromKnox(List<DomainFilterRule> allDomainFilterRules) {
        if (allDomainFilterRules != null) {
            return allDomainFilterRules.stream()
                    .filter(domainFilterRule -> domainFilterRule.getDns1() != null && domainFilterRule.getDns2() != null)
                    .collect(Collectors.toList());
        } else {
            return new ArrayList<>();
        }
    }

    public List<DomainFilterRule> getCustomDnsAppsFromKnox(String packageName) {
        if (firewall == null) {
            return new ArrayList<>();
        }
        return firewall.getDomainFilterRules(Collections.singletonList(packageName)).stream()
                .filter(domainFilterRule -> domainFilterRule.getDns1() != null && domainFilterRule.getDns2() != null)
                .collect(Collectors.toList());
    }

    public List<String> getWhitelistUrlAllAppsFromKnox() {
        if (firewall == null) {
            return new ArrayList<>();
        }
        return firewall.getDomainFilterRules(Collections.singletonList(allPackageName)).stream()
                .filter(domainFilterRule -> !domainFilterRule.getAllowDomains().contains(allPackageName))
                .flatMap(domainFilterRule -> domainFilterRule.getAllowDomains().stream())
                .collect(Collectors.toList());
    }

    public int getWhitelistAppCountFromKnox() {
        if (firewall == null) {
            return 0;
        }

        List<AppInfo> appInfos = appDatabase.applicationInfoDao().getWhitelistedApps();
        return appInfos.stream()
                .map(appInfo -> firewall.getDomainFilterRules(Collections.singletonList(appInfo.packageName)))
                .filter(domainFilterRules -> domainFilterRules != null && domainFilterRules.size() > 0)
                .flatMap(Collection::stream)
                .mapToInt(domainFilterRule -> domainFilterRule.getAllowDomains().size())
                .sum();
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
        if (firewall == null) {
            return;
        }

        List<String> packageNames = appDatabase.applicationInfoDao().getAllPackageNames();
        List<DomainFilterReport> reports = firewall.getDomainFilterReport(packageNames);
        if (reports == null) {
            return;
        }

        long time = tree_days();
        appDatabase.reportBlockedUrlDao().deleteBefore(time);

        ReportBlockedUrl lastBlockedUrl = appDatabase.reportBlockedUrlDao().getLastBlockedDomain();
        long lastBlockedTimestamp = 0;
        if (lastBlockedUrl != null) {
            lastBlockedTimestamp = lastBlockedUrl.blockDate / 1000;
        }
        long finalLastBlockedTimestamp = lastBlockedTimestamp;

        List<ReportBlockedUrl> reportBlockedUrls = reports.stream()
                .filter(domainFilterReport -> domainFilterReport.getTimeStamp() > finalLastBlockedTimestamp)
                .map(domainFilterReport -> new ReportBlockedUrl(
                        domainFilterReport.getDomainUrl(),
                        domainFilterReport.getPackageName(),
                        domainFilterReport.getTimeStamp() * 1000)
                )
                .collect(Collectors.toList());

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
