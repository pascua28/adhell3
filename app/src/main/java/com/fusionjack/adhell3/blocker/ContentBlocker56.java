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
import com.samsung.android.knox.AppIdentity;
import com.samsung.android.knox.net.firewall.DomainFilterRule;
import com.samsung.android.knox.net.firewall.Firewall;
import com.samsung.android.knox.net.firewall.FirewallResponse;
import com.samsung.android.knox.net.firewall.FirewallRule;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;

public class ContentBlocker56 implements ContentBlocker {
    private static ContentBlocker56 mInstance = null;
    private final Firewall firewall;
    private final AppDatabase appDatabase;
    private final FirewallUtils firewallUtils;
    private final List<String> whiteListedAppRules = new ArrayList<>();
    private Handler handler;

    private ContentBlocker56() {
        this.appDatabase = AdhellFactory.getInstance().getAppDatabase();
        this.firewall = AdhellFactory.getInstance().getFirewall();
        this.firewallUtils = FirewallUtils.getInstance();
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
    }

    @Override
    public void enableDomainRules(boolean updateProviders) {
        if (firewall == null) {
            return;
        }

        LogUtils.info("Enabling domain rules...", handler);

        if (updateProviders) {
            LogUtils.info("Updating providers...", handler);
            AdhellFactory.getInstance().updateAllProviders();
        }

        try {
            processWhitelistedApps(handler);
            processWhitelistedDomains(handler);
            processBlockedDomains(handler);
            AdhellFactory.getInstance().applyDns(handler);

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

         if ((parentFragment.getDomainSwitchState() || parentFragment.getFirewallSwitchState()) && !firewallUtils.isCurrentDomainLimitAboveDefault()) {
             LogUtils.info("Updating domain rules...", handler);

             if (updateProviders) {
                 LogUtils.info("Updating providers...", handler);
                 AdhellFactory.getInstance().updateAllProviders();
             }

             try {
                 if (parentFragment.getDomainSwitchState()) {
                     processWhitelistedApps(handler);
                     processWhitelistedDomains(handler);
                     processBlockedDomains(handler);
                     AdhellFactory.getInstance().applyDns(handler);
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

                 LogUtils.info("\nDomain rules are Updating.", handler);

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
                 disableDomainRules();
                 disableFirewallRules();
                 e.printStackTrace();
             }
         } else {
             LogUtils.info("Update not possible.", handler);
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
    }

    public void processCustomRules(Handler handler) throws Exception {
        LogUtils.info("\nProcessing custom rules...", handler);

        FirewallRule[] enabledRules = firewall.getRules(Firewall.FIREWALL_DENY_RULE, FirewallRule.Status.ENABLED);
        int count = 0;
        List<String> urls = appDatabase.userBlockUrlDao().getAll3();
        for (String url : urls) {
            if (url.indexOf('|') != -1) {
                StringTokenizer tokens = new StringTokenizer(url, "|");
                if (tokens.countTokens() == 3) {
                    String packageName = tokens.nextToken().trim();
                    String ip = tokens.nextToken().trim();
                    String port = tokens.nextToken().trim();

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
                                && networkInterface != Firewall.NetworkInterface.WIFI_DATA_ONLY)
                        {
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
                        StringTokenizer tokens = new StringTokenizer(url, "|");
                        String packageName1 = tokens.nextToken().trim();
                        String ip1 = tokens.nextToken().trim();
                        String port1 = tokens.nextToken().trim();

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
        boolean isCurrentDomainLimitAboveDefault = firewallUtils.isCurrentDomainLimitAboveDefault();

        List<String> superAllow = new ArrayList<>();
        superAllow.add("*");

        // Create domain filter rule for white listed apps
        List<AppInfo> whitelistedApps = appDatabase.applicationInfoDao().getWhitelistedApps();
        LogUtils.info("Size: " + whitelistedApps.size(), handler);

        whiteListedAppRules.clear();

        if (!isCurrentDomainLimitAboveDefault) {
            List<DomainFilterRule> currentWhitelistedApps = firewallUtils.getWhitelistedAppsFromKnox();

            if (whitelistedApps.size() <= 0 && currentWhitelistedApps.size() <= 0)
                return;

            List<DomainFilterRule> addRules = new ArrayList<>();
            List<DomainFilterRule> removeRules = new ArrayList<>(currentWhitelistedApps);

            for (AppInfo appInfo : whitelistedApps) {
                boolean add = true;
                whiteListedAppRules.add(appInfo.packageName);
                for (DomainFilterRule whitelistRule : currentWhitelistedApps) {
                    if (appInfo.packageName.equals(whitelistRule.getApplication().getPackageName())) {
                        removeRules.remove(whitelistRule);
                        add = false;
                    }
                }
                if (add) addRules.add(new DomainFilterRule(new AppIdentity(appInfo.packageName, null), new ArrayList<>(), superAllow));
            }

            if (addRules.size() > 0 || removeRules.size() > 0 || currentWhitelistedApps.size() > 0) {
                LogUtils.info("     Active apps count: " + currentWhitelistedApps.size(), handler);
                LogUtils.info("     Apps to add: " + addRules.size(), handler);
                LogUtils.info("     Apps to remove: " + removeRules.size(), handler);
            }

            if (addRules.size() > 0) firewallUtils.addDomainFilterRules(addRules, handler);
            if (removeRules.size() > 0) firewallUtils.removeDomainFilterRules(removeRules, handler);

        } else {
            if (whitelistedApps.size() == 0) {
                return;
            }

            List<DomainFilterRule> rules = new ArrayList<>();

            for (AppInfo app : whitelistedApps) {
                LogUtils.info("Package name: " + app.packageName, handler);
                whiteListedAppRules.add(app.packageName);
                rules.add(new DomainFilterRule(new AppIdentity(app.packageName, null), new ArrayList<>(), superAllow));
            }

            firewallUtils.addDomainFilterRules(rules, handler);
        }
    }

    public void processWhitelistedDomains(Handler handler) throws Exception {
        LogUtils.info("\nProcessing whitelist domain...", handler);
        boolean isCurrentDomainLimitAboveDefault = firewallUtils.isCurrentDomainLimitAboveDefault();

        // Process user-defined white list
        // 1. URL for individual package: packageName|url
        // 2. URL for all packages: url
        List<String> whiteUrls = appDatabase.whiteUrlDao().getAll3();
        LogUtils.info("Size: " + whiteUrls.size(), handler);

        if (!isCurrentDomainLimitAboveDefault) {
            List<String> currentWhiteUrlListAllApps = firewallUtils.getWhitelistUrlAllAppsFromKnox();
            List<DomainFilterRule> currentWhiteUrlIndividualAppsList = firewallUtils.getWhitelistUrlAppsFromKnox();

            if (whiteUrls.size() <= 0 && currentWhiteUrlListAllApps.size() <= 0 && currentWhiteUrlIndividualAppsList.size() <= 0)
                return;

            List<String> denyList = BlockUrlUtils.getAllBlockedUrls(appDatabase);
            List<String> userList = BlockUrlUtils.getUserBlockedUrls(appDatabase, false, null);
            denyList.addAll(userList);

            LogUtils.info("\n   Processing whitelist domain for specific package", handler);
            Map<String, List<String>> urlsIndividualApp = new HashMap<>();
            for (String whiteUrl : whiteUrls) {
                if (whiteUrl.indexOf('|') != -1) {
                    StringTokenizer tokens = new StringTokenizer(whiteUrl, "|");
                    if (tokens.countTokens() == 2) {
                        final String packageName = tokens.nextToken();
                        final String url = tokens.nextToken();
                        List<String> newUrl;
                        if (urlsIndividualApp.get(packageName) == null) {
                            newUrl = new ArrayList<>();
                        } else {
                            newUrl = new ArrayList<>(Objects.requireNonNull(urlsIndividualApp.get(packageName)));
                        }
                        newUrl.add(url);
                        if (isNotWhitelistedApp(packageName)) urlsIndividualApp.put(packageName, newUrl);
                    }
                }
            }

            Map<String, List<String>> appsToAdd = new HashMap<>(urlsIndividualApp);
            List<DomainFilterRule> appsToUpdate = new ArrayList<>();
            List<DomainFilterRule> appsToRemove = new ArrayList<>(currentWhiteUrlIndividualAppsList);

            for (Map.Entry<String, List<String>> whiteUrl : urlsIndividualApp.entrySet()) {
                for (DomainFilterRule whitePackage : currentWhiteUrlIndividualAppsList) {
                    if (whitePackage.getApplication().getPackageName().equals(whiteUrl.getKey())) {
                        appsToAdd.remove(whiteUrl.getKey());
                        appsToUpdate.add(whitePackage);
                        appsToRemove.remove(whitePackage);
                    }
                }
            }

            if (appsToAdd.size() > 0) {
                LogUtils.info("     Adding rules for specific package:", handler);
                for (Map.Entry<String, List<String>> appToAdd : appsToAdd.entrySet()) {
                    LogUtils.info("        PackageName: " + appToAdd.getKey() + "\n        Domain: " + appToAdd.getValue(), handler);
                    final AppIdentity appIdentity = new AppIdentity(appToAdd.getKey(), null);
                    List<String> allowList = new ArrayList<>(appToAdd.getValue());
                    processDomains(appIdentity, denyList, allowList);
                }
            }

            if (appsToUpdate.size() > 0) {
                LogUtils.info("     Updating rules for specific package:", handler);
                for (DomainFilterRule appToUpdate : appsToUpdate) {
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
        } else {
            if (whiteUrls.size() == 0) {
                return;
            }

            List<String> denyList = BlockUrlUtils.getAllBlockedUrls(appDatabase);
            List<String> userList = BlockUrlUtils.getUserBlockedUrls(appDatabase, false, null);
            denyList.addAll(userList);

            LogUtils.info("\n   Processing whitelist domain for specific package", handler);
            for (String whiteUrl : whiteUrls) {
                if (whiteUrl.indexOf('|') != -1) {
                    StringTokenizer tokens = new StringTokenizer(whiteUrl, "|");
                    if (tokens.countTokens() == 2) {
                        final String packageName = tokens.nextToken();
                        final String url = tokens.nextToken();
                        if (isNotWhitelistedApp(packageName)) {
                            LogUtils.info("PackageName: " + packageName + ", Domain: " + url, handler);
                            final AppIdentity appIdentity = new AppIdentity(packageName, null);
                            List<String> allowList = new ArrayList<>();
                            allowList.add(url);
                            processDomains(appIdentity, denyList, allowList);
                        }
                    }
                }
            }

            // Whitelist domain for all apps
            LogUtils.info("\n   Processing whitelist domain for all packages", handler);
            Set<String> allowList = new HashSet<>();
            for (String whiteUrl : whiteUrls) {
                if (whiteUrl.indexOf('|') == -1) {
                    allowList.add(whiteUrl);
                    LogUtils.info("Domain: " + whiteUrl, handler);
                }
            }
            if (allowList.size() > 0) {
                final AppIdentity appIdentity = new AppIdentity("*", null);
                List<DomainFilterRule> rules = new ArrayList<>();
                rules.add(new DomainFilterRule(appIdentity, new ArrayList<>(), new ArrayList<>(allowList)));
                try {
                    firewallUtils.addDomainFilterRules(rules, handler);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void processBlockedDomains(Handler handler) throws Exception {
        boolean isCurrentDomainLimitAboveDefault = firewallUtils.isCurrentDomainLimitAboveDefault();
        LogUtils.info("\nProcessing blocked domains...", handler);

        List<String> denyList = BlockUrlUtils.getAllBlockedUrls(appDatabase);
        List<String> userList = new ArrayList<>(BlockUrlUtils.getUserBlockedUrls(appDatabase, false, null));
        denyList.addAll(userList);

        for (String denyUrl : userList) {
            LogUtils.info("     Manual blacklist: " + denyUrl, handler);
        }

        LogUtils.info("     Total unique domains to block: " + denyList.size(), handler);

        final AppIdentity appIdentity = new AppIdentity("*", null);
        if (!isCurrentDomainLimitAboveDefault) {
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
        } else {
            processDomains(appIdentity, denyList, new ArrayList<>());
        }
    }

    private void processDomains(AppIdentity appIdentity, List<String> denyList, List<String> allowList) throws Exception {
        int start = 0;
        int partitionSize = 5000;
        if (denyList.size() > 0) {
            List<List<String>> chunks = new ArrayList<>();
            for (int i = 0; i < denyList.size(); i += partitionSize) {
                chunks.add(denyList.subList(i, Math.min(i + partitionSize, denyList.size())));
            }
            for (List<String> chunk : chunks) {
                LogUtils.info("\n     Processing " + start + " to " + (start + chunk.size()) + " domains...", handler);
                start += chunk.size();

                List<DomainFilterRule> rules = new ArrayList<>();
                rules.add(new DomainFilterRule(appIdentity, chunk, allowList));
                firewallUtils.addDomainFilterRules(rules, handler);
            }
        } else if (allowList.size() > 0) {
            LogUtils.info("\n     Processing " + start + " to " + (start + allowList.size()) + " domains...", handler);

            List<DomainFilterRule> rules = new ArrayList<>();
            rules.add(new DomainFilterRule(appIdentity, denyList, allowList));
            firewallUtils.addDomainFilterRules(rules, handler);
        }
    }


    private void processRemoveDomains(AppIdentity appIdentity, List<String> denyList, List<String> allowList) throws Exception {
        int start = 0;
        int partitionSize = 5000;
        if (denyList.size() > 0) {
            List<List<String>> chunks = new ArrayList<>();
            for (int i = 0; i < denyList.size(); i += partitionSize) {
                chunks.add(denyList.subList(i, Math.min(i + partitionSize, denyList.size())));
            }
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

    @Override
    public boolean isEnabled() {
        return firewall != null && firewall.isFirewallEnabled();
    }

    @Override
    public boolean isDomainRuleEmpty() {
        if (isEnabled()) {
            if (BlockUrlUtils.isDomainLimitAboveDefault()) {
                // If the domain count more than 15k, calling firewall.getDomainFilterRules() might crash the firewall
                int domainCount = AppPreferences.getInstance().getBlockedDomainsCount();
                return domainCount == 0;
            }

            List<String> packageNameList = new ArrayList<>();
            packageNameList.add(Firewall.FIREWALL_ALL_PACKAGES);
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