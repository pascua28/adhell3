package com.fusionjack.adhell3.blocker;

import android.os.Handler;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.fragments.HomeTabFragment;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.BlockUrlUtils;
import com.fusionjack.adhell3.utils.FirewallUtils;
import com.fusionjack.adhell3.utils.LogUtils;
import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.samsung.android.knox.AppIdentity;
import com.samsung.android.knox.application.ApplicationPolicy;
import com.samsung.android.knox.net.firewall.DomainFilterRule;
import com.samsung.android.knox.net.firewall.Firewall;
import com.samsung.android.knox.net.firewall.FirewallResponse;
import com.samsung.android.knox.net.firewall.FirewallRule;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ContentBlocker56 implements ContentBlocker {
    private static ContentBlocker56 mInstance = null;
    private final Firewall firewall;
    private final AppDatabase appDatabase;
    private final ApplicationPolicy appPolicy;
    private final FirewallUtils firewallUtils;
    private final List<String> whiteListedAppRules = new ArrayList<>();
    private List<DomainFilterRule> allActiveRules;
    private Handler handler;

    private ContentBlocker56() {
        this.appDatabase = AdhellFactory.getInstance().getAppDatabase();
        this.firewall = AdhellFactory.getInstance().getFirewall();
        this.firewallUtils = FirewallUtils.getInstance();
        this.appPolicy = AdhellFactory.getInstance().getAppPolicy();
    }

    public static ContentBlocker56 getInstance() {
        if (mInstance == null) {
            mInstance = getSync();
        }
        return mInstance;
    }

    private static synchronized ContentBlocker56 getSync() {
        if (mInstance == null) {
            mInstance = new ContentBlocker56();
        }
        return mInstance;
    }

    @Override
    public void setHandler(Handler handler) {
        this.handler = handler;
    }

    @Override
    public void enableFirewallRules() {
        if (firewall == null) {
            return;
        }

        LogUtils.info("Enabling firewall rules...", handler);

        try {
            processCustomRules(handler);
            processMobileRestrictedApps(handler);
            processWifiRestrictedApps(handler);

            LogUtils.info("\nFirewall rules are enabled.", handler);

            if (!firewall.isFirewallEnabled()) {
                LogUtils.info("\nEnabling Knox firewall...", handler);
                firewall.enableFirewall(true);
                LogUtils.info("Knox firewall is enabled.", handler);
            }
        } catch (Exception e) {
            disableFirewallRules();
            e.printStackTrace();
        }

        AppPreferences.getInstance().setFirewallRulesToggle(true);
    }

    @Override
    public void disableFirewallRules() {
        if (firewall == null) {
            return;
        }

        LogUtils.info("Disabling firewall rules...", handler);

        // Clear firewall rules
        LogUtils.info("\nClearing firewall rules...", handler);
        FirewallResponse[] response = firewall.clearRules(Firewall.FIREWALL_ALL_RULES);
        LogUtils.info(response == null ? "No response" : response[0].getMessage(), handler);

        LogUtils.info("\nFirewall rules are disabled.", handler);

        if (firewall.isFirewallEnabled() && isDomainRuleEmpty()) {
            LogUtils.info("\nDisabling Knox firewall...", handler);
            firewall.enableFirewall(false);
            LogUtils.info("\nKnox firewall is disabled.", handler);
        }

        AppPreferences.getInstance().setFirewallRulesToggle(false);
    }

    @Override
    public void enableDomainRules(boolean updateProviders) {
        if (firewall == null) {
            return;
        }

        LogUtils.info("Enabling domain rules...", handler);

        if (updateProviders) {
            LogUtils.info("Updating providers...\n", handler);
            AdhellFactory.getInstance().updateAllProviders();
        }

        try {
            setAllActiveRules();
            processWhitelistedApps(handler);
            processWhitelistedDomains(handler);
            processBlockedDomains(handler);
            applyDns(handler);

            List<String> denyList = BlockUrlUtils.getAllBlockedUrls(appDatabase);
            List<String> userList = new ArrayList<>(BlockUrlUtils.getUserBlockedUrls(appDatabase, false, null));
            denyList.addAll(userList);
            AppPreferences.getInstance().setBlockedDomainsCount(denyList.size());

            LogUtils.info("\nDomain rules are enabled.", handler);

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
            AppPreferences.getInstance().setDomainRulesToggle(true);
        } catch (Exception e) {
            disableDomainRules();
            e.printStackTrace();
        }
    }

    @Override
    public void updateAllRules(boolean updateProviders, HomeTabFragment parentFragment) {
        if (firewall == null) {
            return;
        }
         try {
             if (updateProviders) {
                 LogUtils.info("Updating providers...\n", handler);
                 AdhellFactory.getInstance().updateAllProviders();
             }

             if (firewallUtils.isCurrentDomainLimitAboveDefault()) {
                 if (parentFragment.getDomainSwitchState()) {
                     disableDomainRules();
                     AppPreferences.getInstance().setDomainRulesToggle(true);
                 }
                 if (parentFragment.getFirewallSwitchState()) {
                     disableFirewallRules();
                     AppPreferences.getInstance().setFirewallRulesToggle(true);
                 }
                 LogUtils.info("Enabling domain/firewall rules...", handler);
             } else {
                 LogUtils.info("Updating domain/firewall rules...", handler);
             }

             if (parentFragment.getDomainSwitchState()) {
                 setAllActiveRules();
                 processWhitelistedApps(handler);
                 processWhitelistedDomains(handler);
                 processBlockedDomains(handler);
                 applyDns(handler);
             }
             if (parentFragment.getFirewallSwitchState()) {
                 processCustomRules(handler);
                 processMobileRestrictedApps(handler);
                 processWifiRestrictedApps(handler);
             }

             List<String> denyList = BlockUrlUtils.getAllBlockedUrls(appDatabase);
             List<String> userList = new ArrayList<>(BlockUrlUtils.getUserBlockedUrls(appDatabase, false, null));
             denyList.addAll(userList);
             AppPreferences.getInstance().setBlockedDomainsCount(denyList.size());

             LogUtils.info("\nDomain/firewall rules are Updating.", handler);

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
         } catch (Exception e) {
             LogUtils.error("Error during update! Disabling domain/firewall rules...", e, handler);
             disableDomainRules();
             disableFirewallRules();
             e.printStackTrace();
         }
    }

    @Override
    public void disableDomainRules() {
        if (firewall == null) {
            return;
        }

        LogUtils.info("Disabling domain rules...", handler);

        // Clear domain filter rules
        LogUtils.info("\nClearing domain rules...", handler);
        FirewallResponse[] response = firewall.removeDomainFilterRules(DomainFilterRule.CLEAR_ALL);
        LogUtils.info(response == null ? "No response" : response[0].getMessage(), handler);

        LogUtils.info("\nDomain rules are disabled.", handler);

        if (firewall.isFirewallEnabled() && isFirewallRuleEmpty()) {
            LogUtils.info("\nDisabling Knox firewall...", handler);
            firewall.enableFirewall(false);
            LogUtils.info("Knox firewall is disabled.", handler);
        }
        if (firewall.isDomainFilterReportEnabled()) {
            firewall.enableDomainFilterReport(false);
        }

        AppPreferences.getInstance().resetBlockedDomainsCount();
        AppPreferences.getInstance().resetWhitelistedDomainsCount();
        AppPreferences.getInstance().setDomainRulesToggle(false);
    }

    public void setAllActiveRules() {
        LogUtils.info("\nGetting all active rules...", handler);
        allActiveRules = firewallUtils.getDomainFilterRuleForAllAppsFromKnox();
    }

    public void processCustomRules(Handler handler) throws Exception {
        LogUtils.info("\nProcessing custom rules...", handler);

        FirewallRule[] enabledRules = firewall.getRules(Firewall.FIREWALL_DENY_RULE, FirewallRule.Status.ENABLED);
        int count = 0;
        List<String> urls = appDatabase.userBlockUrlDao().getAll3();
        for (String url : urls) {
            if (url.indexOf('|') != -1) {
                List<String> splittedUrl = Splitter.on('|').omitEmptyStrings().trimResults().splitToList(url);
                if (splittedUrl.size() == 3) {
                    String packageName = splittedUrl.get(0);
                    if (appPolicy != null && appPolicy.getApplicationName(packageName) != null) {
                        String ip = splittedUrl.get(1);
                        String port = splittedUrl.get(2);

                        boolean add = true;
                        for (FirewallRule enabledRule : enabledRules) {
                            String packageName1 = enabledRule.getApplication().getPackageName();
                            String ip1 = enabledRule.getIpAddress();
                            String port1 = enabledRule.getPortNumber();
                            Firewall.NetworkInterface networkInterface = enabledRule.getNetworkInterface();
                            if (packageName1.equalsIgnoreCase(packageName)
                                    && ip1.equalsIgnoreCase(ip)
                                    && port1.equalsIgnoreCase(port)
                                    && networkInterface != Firewall.NetworkInterface.MOBILE_DATA_ONLY
                                    && networkInterface != Firewall.NetworkInterface.WIFI_DATA_ONLY) {
                                add = false;
                                break;
                            }
                        }

                        LogUtils.info("\nRule: " + packageName + "|" + ip + "|" + port, handler);
                        if (add) {
                            FirewallRule[] firewallRules;
                            if (ip.equalsIgnoreCase("*")) {
                                firewallRules = new FirewallRule[2];
                                firewallRules[0] = new FirewallRule(FirewallRule.RuleType.DENY, Firewall.AddressType.IPV4);
                                firewallRules[0].setIpAddress(ip);
                                firewallRules[0].setPortNumber(port);
                                firewallRules[0].setApplication(new AppIdentity(packageName, null));
                                firewallRules[0].setNetworkInterface(Firewall.NetworkInterface.ALL_NETWORKS);

                                firewallRules[1] = new FirewallRule(FirewallRule.RuleType.DENY, Firewall.AddressType.IPV6);
                                firewallRules[1].setIpAddress(ip);
                                firewallRules[1].setPortNumber(port);
                                firewallRules[1].setApplication(new AppIdentity(packageName, null));
                            } else {
                                Firewall.AddressType type;
                                InetAddress address = InetAddress.getByName(ip);
                                if (address instanceof Inet6Address) {
                                    type = Firewall.AddressType.IPV6;
                                } else if (address instanceof Inet4Address) {
                                    type = Firewall.AddressType.IPV4;
                                } else {
                                    throw new Exception("Unknown ip address type");
                                }

                                firewallRules = new FirewallRule[1];
                                firewallRules[0] = new FirewallRule(FirewallRule.RuleType.DENY, type);
                                firewallRules[0].setIpAddress(ip);
                                firewallRules[0].setPortNumber(port);
                                firewallRules[0].setApplication(new AppIdentity(packageName, null));
                            }
                            firewallRules[0].setNetworkInterface(Firewall.NetworkInterface.ALL_NETWORKS);

                            firewallUtils.addFirewallRules(firewallRules, handler);
                        } else {
                            LogUtils.info("The firewall rule is already been enabled", handler);
                        }
                        ++count;
                    }
                }
            }
        }

        LogUtils.info("Custom rule size: " + count, handler);

        for (FirewallRule enabledRule : enabledRules) {
            String packageName = enabledRule.getApplication().getPackageName();
            String ip = enabledRule.getIpAddress();
            String port = enabledRule.getPortNumber();
            Firewall.NetworkInterface networkInterface = enabledRule.getNetworkInterface();
            if (networkInterface != Firewall.NetworkInterface.MOBILE_DATA_ONLY
                    && networkInterface != Firewall.NetworkInterface.WIFI_DATA_ONLY) {
                boolean remove = true;
                for (String url : urls) {
                    if (url.indexOf('|') != -1) {
                        List<String> splittedUrl = Splitter.on('|').omitEmptyStrings().trimResults().splitToList(url);
                        String packageName1 = splittedUrl.get(0);
                        String ip1 = splittedUrl.get(1);
                        String port1 = splittedUrl.get(2);

                        if (packageName1.equalsIgnoreCase(packageName) && ip1.equalsIgnoreCase(ip) && port1.equalsIgnoreCase(port)) {
                            remove = false;
                            break;
                        }
                    }
                }

                if (remove) {
                    String addressType = (enabledRule.getAddressType() == Firewall.AddressType.IPV4) ? "IPv4" : "IPv6";
                    LogUtils.info("\nRemoving " + addressType + " rule: " + packageName + "|" + ip + "|" + port, handler);
                    FirewallRule[] removeRule = new FirewallRule[1];
                    removeRule[0] = enabledRule;
                    firewallUtils.removeFirewallRules(removeRule, handler);
                }
            }
        }
    }

    public void processMobileRestrictedApps(Handler handler) throws Exception {
        LogUtils.info("\nProcessing mobile restricted apps...", handler);

        List<AppInfo> restrictedApps = appDatabase.applicationInfoDao().getMobileRestrictedApps();
        int size = restrictedApps.size();
        LogUtils.info("Size: " + size, handler);

        FirewallRule[] enabledRules = firewall.getRules(Firewall.FIREWALL_DENY_RULE, FirewallRule.Status.ENABLED);
        for (AppInfo app : restrictedApps) {
            String packageName = app.packageName;

            if (appPolicy != null && appPolicy.getApplicationName(packageName) != null) {
                boolean add = true;
                for (FirewallRule enabledRule : enabledRules) {
                    String packageName1 = enabledRule.getApplication().getPackageName();
                    Firewall.NetworkInterface networkInterface = enabledRule.getNetworkInterface();
                    if (packageName1.equalsIgnoreCase(packageName) && networkInterface == Firewall.NetworkInterface.MOBILE_DATA_ONLY) {
                        add = false;
                        break;
                    }
                }

                LogUtils.info("Package name: " + packageName, handler);
                if (add) {
                    FirewallRule[] mobileRules = firewallUtils.createFirewallRules(packageName,
                            Firewall.NetworkInterface.MOBILE_DATA_ONLY);
                    firewallUtils.addFirewallRules(mobileRules, handler);
                } else {
                    LogUtils.info("The firewall rule is already been enabled", handler);
                }
            }
        }

        for (FirewallRule enabledRule : enabledRules) {
            String packageName = enabledRule.getApplication().getPackageName();
            Firewall.NetworkInterface networkInterface = enabledRule.getNetworkInterface();
            if (networkInterface == Firewall.NetworkInterface.MOBILE_DATA_ONLY) {
                boolean remove = true;
                for (AppInfo app : restrictedApps) {
                    String packageName1 = app.packageName;
                    if (packageName1.equalsIgnoreCase(packageName)) {
                        remove = false;
                        break;
                    }
                }
                if (remove) {
                    LogUtils.info("Removing package name: " + packageName, handler);
                    FirewallRule[] removeRule = new FirewallRule[1];
                    removeRule[0] = enabledRule;
                    firewallUtils.removeFirewallRules(removeRule, handler);
                }
            }
        }
    }

    public void processWifiRestrictedApps(Handler handler) throws Exception {
        LogUtils.info("\nProcessing wifi restricted apps...", handler);

        List<AppInfo> restrictedApps = appDatabase.applicationInfoDao().getWifiRestrictedApps();
        int size = restrictedApps.size();
        LogUtils.info("Size: " + size, handler);
        FirewallRule[] enabledRules = firewall.getRules(Firewall.FIREWALL_DENY_RULE, FirewallRule.Status.ENABLED);
        for (AppInfo app : restrictedApps) {
            String packageName = app.packageName;

            if (appPolicy != null && appPolicy.getApplicationName(packageName) != null) {
                boolean add = true;
                for (FirewallRule enabledRule : enabledRules) {
                    String packageName1 = enabledRule.getApplication().getPackageName();
                    Firewall.NetworkInterface networkInterface = enabledRule.getNetworkInterface();
                    if (packageName1.equalsIgnoreCase(packageName) && networkInterface == Firewall.NetworkInterface.WIFI_DATA_ONLY) {
                        add = false;
                        break;
                    }
                }

                LogUtils.info("Package name: " + packageName, handler);
                if (add) {
                    FirewallRule[] wifiRules = firewallUtils.createFirewallRules(packageName,
                            Firewall.NetworkInterface.WIFI_DATA_ONLY);
                    firewallUtils.addFirewallRules(wifiRules, handler);
                } else {
                    LogUtils.info("The firewall rule is already been enabled", handler);
                }
            }
        }

        for (FirewallRule enabledRule : enabledRules) {
            String packageName = enabledRule.getApplication().getPackageName();
            Firewall.NetworkInterface networkInterface = enabledRule.getNetworkInterface();
            if (networkInterface == Firewall.NetworkInterface.WIFI_DATA_ONLY) {
                boolean remove = true;
                for (AppInfo app : restrictedApps) {
                    String packageName1 = app.packageName;
                    if (packageName1.equalsIgnoreCase(packageName)) {
                        remove = false;
                        break;
                    }
                }
                if (remove) {
                    LogUtils.info("Removing package name: " + packageName, handler);
                    FirewallRule[] removeRule = new FirewallRule[1];
                    removeRule[0] = enabledRule;
                    firewallUtils.removeFirewallRules(removeRule, handler);
                }
            }
        }
    }

    public void processWhitelistedApps(Handler handler) throws Exception {
        LogUtils.info("\nProcessing white-listed apps...", handler);

        List<String> superAllow = new ArrayList<>();
        superAllow.add("*");

        // Create domain filter rule for white listed apps
        List<AppInfo> whitelistedApps = appDatabase.applicationInfoDao().getWhitelistedApps();
        LogUtils.info("Size: " + whitelistedApps.size(), handler);

        whiteListedAppRules.clear();

        List<DomainFilterRule> currentWhitelistedApps = firewallUtils.getWhitelistedAppsFromKnox(allActiveRules);

        if (whitelistedApps.size() <= 0 && currentWhitelistedApps.size() <= 0)
            return;

        List<DomainFilterRule> addRules = new ArrayList<>();
        List<DomainFilterRule> removeRules = new ArrayList<>(currentWhitelistedApps);

        for (AppInfo appInfo : whitelistedApps) {
            if (appPolicy != null && appPolicy.getApplicationName(appInfo.packageName) != null) {
                boolean add = true;
                whiteListedAppRules.add(appInfo.packageName);
                for (DomainFilterRule whitelistRule : currentWhitelistedApps) {
                    if (appInfo.packageName.equals(whitelistRule.getApplication().getPackageName())) {
                        removeRules.remove(whitelistRule);
                        add = false;
                    }
                }
                if (add)
                    addRules.add(new DomainFilterRule(new AppIdentity(appInfo.packageName, null), new ArrayList<>(), superAllow));
            }
        }

        if (addRules.size() > 0 || removeRules.size() > 0 || currentWhitelistedApps.size() > 0) {
            LogUtils.info("     Active apps count: " + currentWhitelistedApps.size(), handler);
            LogUtils.info("     Apps to add: " + addRules.size(), handler);
            LogUtils.info("     Apps to remove: " + removeRules.size(), handler);
        }

        if (addRules.size() > 0) firewallUtils.addDomainFilterRules(addRules, handler);
        if (removeRules.size() > 0) firewallUtils.removeDomainFilterRules(removeRules, handler);
    }

    private boolean isNotWhitelistedApp(String packageName) {
        boolean isWhitelisted = false;
        for (String whiteListedAppRule : whiteListedAppRules) {
            if (whiteListedAppRule.equals(packageName)) {
                isWhitelisted = true;
                break;
            }
        }

        return !isWhitelisted;
    }


    public void processWhitelistedDomains(Handler handler) throws Exception {
        LogUtils.info("\nProcessing whitelist domain...", handler);
        int whitelistDomainsCount = 0;

        // Process user-defined white list
        // 1. URL for individual package: packageName|url
        // 2. URL for all packages: url
        List<String> whiteUrls = appDatabase.whiteUrlDao().getAll3();
        LogUtils.info("Size: " + whiteUrls.size(), handler);

        List<DomainFilterRule> currentWhiteUrlIndividualAppsList = firewallUtils.getWhitelistUrlAppsFromKnox(allActiveRules);
        List<String> currentWhiteUrlListAllApps = firewallUtils.getWhitelistUrlAllAppsFromKnox();

        if (whiteUrls.size() <= 0 && currentWhiteUrlListAllApps.size() <= 0 && currentWhiteUrlIndividualAppsList.size() <= 0)
            return;

        List<String> denyList = BlockUrlUtils.getAllBlockedUrls(appDatabase);
        denyList.addAll(BlockUrlUtils.getUserBlockedUrls(appDatabase, false, null));

        LogUtils.info("\n   Processing whitelist domain for specific package", handler);

        Map<String, List<String>> urlsIndividualApp = whiteUrls.stream()
                .filter(whiteUrl -> whiteUrl.indexOf('|') != -1)
                .map(whiteUrl -> Splitter.on('|').omitEmptyStrings().trimResults().splitToList(whiteUrl))
                .filter(whiteUrl -> whiteUrl.size() == 2 && isNotWhitelistedApp(whiteUrl.get(0)))
                .map(whiteUrl -> Maps.immutableEntry(whiteUrl.get(0), whiteUrl.get(1)))
                .collect(Collectors.groupingBy(Map.Entry::getKey, LinkedHashMap::new, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));

        Map<String, List<String>> appsToAdd = new HashMap<>(urlsIndividualApp);
        List<DomainFilterRule> appsToUpdate = new ArrayList<>();
        List<DomainFilterRule> appsToRemove = new ArrayList<>(currentWhiteUrlIndividualAppsList);

        whitelistDomainsCount += urlsIndividualApp.entrySet().stream()
                .mapToInt(whiteUrl -> {
                    currentWhiteUrlIndividualAppsList.forEach(whitePackage -> {
                        if (whitePackage.getApplication().getPackageName().equals(whiteUrl.getKey())) {
                            appsToAdd.remove(whiteUrl.getKey());
                            appsToUpdate.add(whitePackage);
                            appsToRemove.remove(whitePackage);
                        }
                    });
                    return whiteUrl.getValue().size();
                })
                .max().orElse(0);

        if (appsToAdd.size() > 0) {
            LogUtils.info("     Adding rules for specific package:", handler);
            for (Map.Entry<String, List<String>> appToAdd : appsToAdd.entrySet()) {
                if (appPolicy != null && appPolicy.getApplicationName(appToAdd.getKey()) != null) {
                    LogUtils.info("        PackageName: " + appToAdd.getKey() + "\n        Domain: " + appToAdd.getValue(), handler);
                    final AppIdentity appIdentity = new AppIdentity(appToAdd.getKey(), null);
                    List<String> allowList = new ArrayList<>(appToAdd.getValue());
                    processDomains(appIdentity, denyList, allowList);
                }
            }
        }

        if (appsToUpdate.size() > 0) {
            LogUtils.info("     Updating rules for specific package:", handler);
            for (DomainFilterRule appToUpdate : appsToUpdate) {
                if (appPolicy != null && appPolicy.getApplicationName(appToUpdate.getApplication().getPackageName()) != null) {
                    LogUtils.info("        PackageName: " + appToUpdate.getApplication().getPackageName(), handler);

                    final AppIdentity appIdentity = new AppIdentity(appToUpdate.getApplication().getPackageName(), null);

                    List<String> allowList = new ArrayList<>(Objects.requireNonNull(urlsIndividualApp.get(appToUpdate.getApplication().getPackageName())));
                    List<String> allowDomainsToAdd = new ArrayList<>(allowList);
                    List<String> allowDomainsToRemove = new ArrayList<>(appToUpdate.getAllowDomains());
                    allowDomainsToAdd.removeAll(allowDomainsToRemove);
                    allowDomainsToRemove.removeAll(allowList);

                    List<String> denyDomainsToAdd = new ArrayList<>(denyList);
                    List<String> denyDomainsToRemove = new ArrayList<>(appToUpdate.getDenyDomains());
                    denyDomainsToAdd.removeAll(appToUpdate.getDenyDomains());
                    denyDomainsToRemove.removeAll(denyList);

                    LogUtils.info("           Total unique domains to block: " + denyList.size(), handler);
                    LogUtils.info("           Active block domains count: " + appToUpdate.getDenyDomains().size(), handler);
                    LogUtils.info("           Active white domains count: " + appToUpdate.getAllowDomains().size(), handler);
                    LogUtils.info("           Domains to add: " + (denyDomainsToAdd.size() + allowDomainsToAdd.size()), handler);
                    LogUtils.info("           Domains to remove: " + (denyDomainsToRemove.size() + allowDomainsToRemove.size()), handler);

                    if (denyDomainsToAdd.size() > 0 || allowDomainsToAdd.size() > 0)
                        processDomains(appIdentity, denyDomainsToAdd, allowDomainsToAdd);
                    if (denyDomainsToRemove.size() > 0 || allowDomainsToRemove.size() > 0)
                        processRemoveDomains(appIdentity, denyDomainsToRemove, allowDomainsToRemove);
                }
            }
        }

        if (appsToRemove.size() > 0) {
            LogUtils.info("     Removing rules for specific package:", handler);
            for (DomainFilterRule appToRemove : appsToRemove) {
                LogUtils.info("        PackageName: " + appToRemove.getApplication().getPackageName(), handler);
            }
            firewallUtils.removeDomainFilterRules(appsToRemove, handler);
        }


        // Whitelist URL for all apps
        LogUtils.info("\n   Processing whitelist domain for all packages", handler);
        Set<String> allowList = new HashSet<>();
        for (String whiteUrl : whiteUrls) {
            if (whiteUrl.indexOf('|') == -1) {
                allowList.add(whiteUrl);
                LogUtils.info("     Domain: " + whiteUrl, handler);
            }
        }
        whitelistDomainsCount += allowList.size();

        final AppIdentity appIdentity = new AppIdentity("*", null);
        List<String> domainsToAdd = new ArrayList<>(allowList);
        List<String> domainsToRemove = new ArrayList<>(currentWhiteUrlListAllApps);
        domainsToAdd.removeAll(currentWhiteUrlListAllApps);
        domainsToRemove.removeAll(allowList);

        if (currentWhiteUrlListAllApps.size() > 0 || domainsToAdd.size() > 0 || domainsToRemove.size() > 0) {
            LogUtils.info("        Active domains count: " + currentWhiteUrlListAllApps.size(), handler);
            LogUtils.info("        Domains to add: " + domainsToAdd.size(), handler);
            LogUtils.info("        Domains to remove: " + domainsToRemove.size(), handler);
        }

        if (domainsToAdd.size() > 0) {
            List<DomainFilterRule> rulesAdd = new ArrayList<>();
            rulesAdd.add(new DomainFilterRule(appIdentity, new ArrayList<>(), new ArrayList<>(domainsToAdd)));
            firewallUtils.addDomainFilterRules(rulesAdd, handler);
        }
        if (domainsToRemove.size() > 0) {
            List<DomainFilterRule> rulesRemove = new ArrayList<>();
            rulesRemove.add(new DomainFilterRule(appIdentity, new ArrayList<>(), new ArrayList<>(domainsToRemove)));
            firewallUtils.removeDomainFilterRules(rulesRemove, handler);
        }
        AppPreferences.getInstance().setWhitelistedDomainsCount(whitelistDomainsCount);
    }

    public void applyDns(Handler handler) {
        if (AppPreferences.getInstance().isDnsNotEmpty()) {
            FirewallUtils firewallUtils = FirewallUtils.getInstance();
            List<DomainFilterRule> rulesToAdd = new ArrayList<>();
            String dns1 = AppPreferences.getInstance().getDns1();
            String dns2 = AppPreferences.getInstance().getDns2();
            List<AppInfo> dnsPackages = appDatabase.applicationInfoDao().getDnsApps();

            LogUtils.info("\nProcessing DNS...", handler);
            LogUtils.info("DNS 1: " + dns1, handler);
            LogUtils.info("DNS 2: " + dns2, handler);
            LogUtils.info("Size: " + dnsPackages.size(), handler);

            List<DomainFilterRule> customDnsRules = firewallUtils.getCustomDnsAppsFromKnox(allActiveRules);

            List<AppInfo> appsToAdd = new ArrayList<>(dnsPackages);
            List<DomainFilterRule> appsToUpdate = new ArrayList<>();
            List<DomainFilterRule> appsToRemove = new ArrayList<>(customDnsRules);

            dnsPackages.forEach(app -> customDnsRules.forEach(customDnsRule -> {
                if (customDnsRule.getApplication().getPackageName().equals(app.packageName)) {
                    appsToAdd.remove(app);
                    appsToUpdate.add(customDnsRule);
                }
            }));

            if (appsToUpdate.size() > 0) {
                for (DomainFilterRule appToUpdate : appsToUpdate) {
                    if (!appToUpdate.getDns1().equals(dns1) && !appToUpdate.getDns2().equals(dns2)) {
                        LogUtils.info("   Updating DNS settings for package: " + appToUpdate.getApplication().getPackageName(), handler);
                        DomainFilterRule rule = new DomainFilterRule(new AppIdentity(appToUpdate.getApplication().getPackageName(), null));
                        rulesToAdd.add(rule);
                    } else {
                        LogUtils.info("   DNS settings already set for package: " + appToUpdate.getApplication().getPackageName(), handler);
                        appsToRemove.remove(appToUpdate);
                    }

                }
            }

            if (appsToRemove.size() > 0) {
                List<DomainFilterRule> rulesToRemove = new ArrayList<>();
                List<DomainFilterRule> rulesToUpdate = new ArrayList<>();
                for (DomainFilterRule appToRemove : appsToRemove) {
                    LogUtils.info("   Removing DNS settings for package: " + appToRemove.getApplication().getPackageName(), handler);
                    List<DomainFilterRule> currentDomainList = firewallUtils.getCustomDnsAppsFromKnox(appToRemove.getApplication().getPackageName());

                    for (DomainFilterRule currentDomain : currentDomainList) {
                        rulesToRemove.add(currentDomain);
                        if (currentDomain.getAllowDomains().size() > 0 ||
                                currentDomain.getDenyDomains().size() > 0 ||
                                currentDomain.getDns1() == null ||
                                currentDomain.getDns2() == null
                        ) {
                            rulesToUpdate.add(
                                    new DomainFilterRule(currentDomain.getApplication(), currentDomain.getDenyDomains(), currentDomain.getAllowDomains(), null, null)
                            );
                        }
                    }
                }
                try {
                    if (rulesToRemove.size() > 0) {
                        for (DomainFilterRule ruleToRemove : rulesToRemove) {
                            List<DomainFilterRule> ruleAsList = new ArrayList<>();
                            ruleAsList.add(ruleToRemove);
                            firewallUtils.removeDomainFilterRules(ruleAsList, handler);
                        }
                    }
                    if (rulesToUpdate.size() > 0) {
                        for (DomainFilterRule ruleToUpdate : rulesToUpdate) {
                            processDomains(ruleToUpdate.getApplication(), ruleToUpdate.getDenyDomains(), ruleToUpdate.getAllowDomains());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (appsToAdd.size() > 0) {
                for (AppInfo app : appsToAdd) {
                    LogUtils.info("   Adding DNS settings for package: " + app.packageName, handler);
                    DomainFilterRule rule = new DomainFilterRule(new AppIdentity(app.packageName, null));
                    rulesToAdd.add(rule);
                }
            }

            if (rulesToAdd.size() > 0) {
                try {
                    for (DomainFilterRule ruleToAdd : rulesToAdd) {
                        processDomains(ruleToAdd.getApplication(), ruleToAdd.getDenyDomains(), ruleToAdd.getAllowDomains(), dns1, dns2);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            LogUtils.info("Done.", handler);
        } else {
            // Clean all existent rules if in update mode and dns is empty
            FirewallUtils firewallUtils = FirewallUtils.getInstance();
            LogUtils.info("\nChecking if all DNS rules are disabled...", handler);
            List<DomainFilterRule> customDnsRules = firewallUtils.getCustomDnsAppsFromKnox(allActiveRules);
            if (customDnsRules.size() > 0) {
                List<DomainFilterRule> rulesToRemove = new ArrayList<>();
                List<DomainFilterRule> rulesToUpdate = new ArrayList<>();
                for (DomainFilterRule appToRemove : customDnsRules) {
                    LogUtils.info("   Removing previous DNS rules for package: " + appToRemove.getApplication().getPackageName(), handler);
                    List<DomainFilterRule> currentDomainList = firewallUtils.getCustomDnsAppsFromKnox(appToRemove.getApplication().getPackageName());

                    for (DomainFilterRule currentDomain : currentDomainList) {
                        rulesToRemove.add(currentDomain);
                        if (currentDomain.getAllowDomains().size() > 0 ||
                                currentDomain.getDenyDomains().size() > 0 ||
                                currentDomain.getDns1() == null ||
                                currentDomain.getDns2() == null
                        ) {
                            rulesToUpdate.add(
                                    new DomainFilterRule(currentDomain.getApplication(), currentDomain.getDenyDomains(), currentDomain.getAllowDomains(), null, null)
                            );
                        }
                    }
                }
                try {
                    if (rulesToRemove.size() > 0) {
                        for (DomainFilterRule ruleToRemove : rulesToRemove) {
                            List<DomainFilterRule> ruleAsList = new ArrayList<>();
                            ruleAsList.add(ruleToRemove);
                            firewallUtils.removeDomainFilterRules(ruleAsList, handler);
                        }
                    }
                    if (rulesToUpdate.size() > 0) {
                        for (DomainFilterRule ruleToUpdate : rulesToUpdate) {
                            processDomains(ruleToUpdate.getApplication(), ruleToUpdate.getDenyDomains(), ruleToUpdate.getAllowDomains());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            LogUtils.info("Done.", handler);
        }
    }

    public void processBlockedDomains(Handler handler) throws Exception {
        LogUtils.info("\nProcessing blocked domains...", handler);

        List<String> denyList = BlockUrlUtils.getAllBlockedUrls(appDatabase);
        List<String> userList = new ArrayList<>(BlockUrlUtils.getUserBlockedUrls(appDatabase, false, null));
        denyList.addAll(userList);

        for (String denyUrl : userList) {
            LogUtils.info("     Manual blacklist: " + denyUrl, handler);
        }

        LogUtils.info("     Total unique domains to block: " + denyList.size(), handler);

        final AppIdentity appIdentity = new AppIdentity("*", null);
        try {
            List<String> activeKnowDenyList = firewallUtils.getActiveDenyDomainsFromKnox();
            List<String> domainsToAdd = new ArrayList<>(denyList);
            List<String> domainsToRemove = new ArrayList<>(activeKnowDenyList);
            domainsToAdd.removeAll(activeKnowDenyList);
            domainsToRemove.removeAll(denyList);

            LogUtils.info("     Active domains count: " + activeKnowDenyList.size(), handler);
            LogUtils.info("     Domains to add: " + domainsToAdd.size(), handler);
            LogUtils.info("     Domains to remove: " + domainsToRemove.size(), handler);

            if (domainsToAdd.size() > 0) processDomains(appIdentity, domainsToAdd, new ArrayList<>());
            if (domainsToRemove.size() > 0) processRemoveDomains(appIdentity, domainsToRemove, new ArrayList<>());
        } catch (Exception e) {
            processDomains(appIdentity, denyList, new ArrayList<>());
            e.printStackTrace();
        }
    }

    private void processDomains(AppIdentity appIdentity, List<String> denyList, List<String> allowList) throws Exception {
        processDomains(appIdentity, denyList, allowList, null, null);
    }

    private void processDomains(AppIdentity appIdentity, List<String> denyList, List<String> allowList, String dns1, String dns2) throws Exception {
        int start = 0;
        int partitionSize = 5000;
        if (denyList.size() > 0) {
            List<List<String>> chunks = Lists.partition(denyList, partitionSize);
            for (List<String> chunk : chunks) {
                LogUtils.info("\n     Processing " + start + " to " + (start + chunk.size()) + " domains...", handler);
                start += chunk.size();

                List<DomainFilterRule> rules = new ArrayList<>();
                rules.add(new DomainFilterRule(appIdentity, chunk, allowList, dns1, dns2));
                firewallUtils.addDomainFilterRules(rules, handler);
            }
        } else if (allowList.size() > 0) {
            LogUtils.info("\n     Processing " + start + " to " + (start + allowList.size()) + " domains...", handler);

            List<DomainFilterRule> rules = new ArrayList<>();
            rules.add(new DomainFilterRule(appIdentity, denyList, allowList, dns1, dns2));
            firewallUtils.addDomainFilterRules(rules, handler);
        } else if (dns1 != null && dns2 != null) {
            List<DomainFilterRule> rules = new ArrayList<>();
            rules.add(new DomainFilterRule(appIdentity, denyList, allowList, dns1, dns2));
            firewallUtils.addDomainFilterRules(rules, handler);
        }
    }

    private void processRemoveDomains(AppIdentity appIdentity, List<String> denyList, List<String> allowList) throws Exception {
        int start = 0;
        int partitionSize = 5000;
        if (denyList.size() > 0) {
            List<List<String>> chunks = Lists.partition(denyList, partitionSize);
            for (List<String> chunk : chunks) {
                LogUtils.info("\n     Processing " + start + " to " + (start + chunk.size()) + " domains...", handler);
                start += chunk.size();

                List<DomainFilterRule> rules = new ArrayList<>();
                rules.add(new DomainFilterRule(appIdentity, chunk, allowList));
                firewallUtils.removeDomainFilterRules(rules, handler);
            }
        } else if (allowList.size() > 0) {
            LogUtils.info("\n     Processing " + start + " to " + (start + allowList.size()) + " domains...", handler);

            List<DomainFilterRule> rules = new ArrayList<>();
            rules.add(new DomainFilterRule(appIdentity, denyList, allowList));
            firewallUtils.removeDomainFilterRules(rules, handler);
        }
    }
    @Override
    public boolean isEnabled() {
        return firewall != null && firewall.isFirewallEnabled();
    }

    @Override
    public boolean isDomainRuleEmpty() {
        if (isEnabled()) {
            if (BlockUrlUtils.isDomainLimitAboveDefault()) {
                // If the domain count more than 15k, calling firewall.getDomainFilterRules() might crash the firewall
                int domainCount = AppPreferences.getInstance().getBlockedDomainsCount() + AppPreferences.getInstance().getWhitelistedDomainsCount();
                return domainCount == 0;
            }

            List<String> packageNameList = Collections.singletonList(Firewall.FIREWALL_ALL_PACKAGES);
            List<DomainFilterRule> rules = firewall.getDomainFilterRules(packageNameList);
            return rules != null && rules.size() == 0;
        }
        return true;
    }

    @Override
    public boolean isFirewallRuleEmpty() {
        if (isEnabled()) {
            FirewallRule[] rules = firewall.getRules(Firewall.FIREWALL_DENY_RULE, null);
            return rules == null || rules.length == 0;
        }
        return true;
    }
}