package com.fusionjack.adhell3.utils;

import android.content.SharedPreferences;

import com.fusionjack.adhell3.blocker.ContentBlocker;
import com.fusionjack.adhell3.viewmodel.ProviderViewModel;

import io.reactivex.rxjava3.core.Single;

public final class AppPreferences {
    private static AppPreferences instance;
    private final SharedPreferences sharedPreferences;

    private static final String ADHELL3_FOLDER = "adhell3Folder";

    private static final String DOMAIN_RULE_TOGGLE = "domainRuleToggle";
    private static final String FIREWALL_RULE_TOGGLE = "firewallRuleToggle";
    private static final String DISABLER_TOGGLE = "disablerToggle";
    private static final String APP_COMPONENT_TOGGLE = "appComponentToggle";

    private static final String DOMAIN_STAT = "domainStat";
    private static final String FIREWALL_STAT = "firewallStat";
    private static final String BLOCKED_DOMAINS_COUNT = "blockedDomainsCount";
    private final static String DNS_ALL_APPS_ENABLED = "dnsAllAppsEnabled";
    private static final String DNS1 = "dns1";
    private static final String DNS2 = "dns2";
    private static final String PASSWORD = "password";
    private static final String CURRENT_PROVIDER_ID = "currentProviderId";
    private static final String HIDE_SYSTEM_APPS = "hideSystemApps";

    private static final String HIDE_STOPPED_APPS = "hideStoppedApps";

    private AppPreferences() {
        sharedPreferences = AdhellFactory.getInstance().getSharedPreferences();
    }

    public static AppPreferences getInstance() {
        if (instance == null) {
            instance = new AppPreferences();
        }
        return instance;
    }

    public String getAdhell3FolderUri() {
        return sharedPreferences.getString(ADHELL3_FOLDER, null);
    }

    public void setAdhell3FolderUri(String uri) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(ADHELL3_FOLDER, uri);
        editor.apply();
    }

    public Single<SharedPreferenceBooleanLiveData> getHideSystemAppsLiveData() {
        return Single.fromCallable(() ->
                new SharedPreferenceBooleanLiveData(sharedPreferences, HIDE_SYSTEM_APPS, false));
    }

    public boolean isHideSystemApps() {
        return sharedPreferences.getBoolean(HIDE_SYSTEM_APPS, false);
    }

    public void setHideSystemApps(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(HIDE_SYSTEM_APPS, enabled);
        editor.apply();
    }

    public Single<SharedPreferenceBooleanLiveData> getHideStoppedAppsLiveData() {
        return Single.fromCallable(() ->
                new SharedPreferenceBooleanLiveData(sharedPreferences, HIDE_STOPPED_APPS, false));
    }

    public boolean isHideStoppedApps() {
        return sharedPreferences.getBoolean(HIDE_STOPPED_APPS, false);
    }

    public void setHideStoppedApps(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(HIDE_STOPPED_APPS, enabled);
        editor.apply();
    }

    public long getCurrentProviderId() {
        return sharedPreferences.getLong(CURRENT_PROVIDER_ID, ProviderViewModel.ALL_PROVIDERS);
    }

    public void resetCurrentProviderId() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(CURRENT_PROVIDER_ID);
        editor.apply();
    }

    public void setCurrentProviderId(long providerId) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(CURRENT_PROVIDER_ID, providerId);
        editor.apply();
    }

    public Single<SharedPreferenceBooleanLiveData> getDomainRuleLiveData(ContentBlocker contentBlocker) {
        return Single.fromCallable(() ->
                new SharedPreferenceBooleanLiveData(sharedPreferences, DOMAIN_RULE_TOGGLE, !contentBlocker.isDomainRuleEmpty()));
    }

    public boolean isDomainRuleToggleEnabled(ContentBlocker contentBlocker) {
        return sharedPreferences.getBoolean(DOMAIN_RULE_TOGGLE, !contentBlocker.isDomainRuleEmpty());
    }

    public void setDomainRuleToggle(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(DOMAIN_RULE_TOGGLE, enabled);
        editor.apply();
    }

    public Single<SharedPreferenceBooleanLiveData> getFirewallRuleLiveData(ContentBlocker contentBlocker) {
        return Single.fromCallable(() ->
                new SharedPreferenceBooleanLiveData(sharedPreferences, FIREWALL_RULE_TOGGLE, !contentBlocker.isFirewallRuleEmpty()));
    }

    public boolean isFirewallRuleToggleEnabled(ContentBlocker contentBlocker) {
        return sharedPreferences.getBoolean(FIREWALL_RULE_TOGGLE, !contentBlocker.isFirewallRuleEmpty());
    }

    public void setFirewallRuleToggle(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(FIREWALL_RULE_TOGGLE, enabled);
        editor.apply();
    }

    public Single<SharedPreferenceBooleanLiveData> getAppDisablerLiveData() {
        return Single.fromCallable(() ->
                new SharedPreferenceBooleanLiveData(sharedPreferences, DISABLER_TOGGLE, true));
    }

    public boolean isAppDisablerToggleEnabled() {
        return sharedPreferences.getBoolean(DISABLER_TOGGLE, true);
    }

    public void setAppDisablerToggle(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(DISABLER_TOGGLE, enabled);
        editor.apply();
    }

    public Single<SharedPreferenceBooleanLiveData> getAppComponentLiveData() {
        return Single.fromCallable(() ->
                new SharedPreferenceBooleanLiveData(sharedPreferences, APP_COMPONENT_TOGGLE, true));
    }

    public boolean isAppComponentToggleEnabled() {
        return sharedPreferences.getBoolean(APP_COMPONENT_TOGGLE, true);
    }

    public void setAppComponentToggle(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(APP_COMPONENT_TOGGLE, enabled);
        editor.apply();
    }

    public boolean isDnsAllAppsEnabled() {
        return sharedPreferences.getBoolean(DNS_ALL_APPS_ENABLED, false);
    }

    public void setDnsAllApps(boolean enabled) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean(DNS_ALL_APPS_ENABLED, enabled);
        editor.apply();
    }

    public String getDns1() {
        return sharedPreferences.getString(DNS1, "0.0.0.0");
    }

    public String getDns2() {
        return sharedPreferences.getString(DNS2, "0.0.0.0");
    }

    public boolean isDnsNotEmpty() {
        return sharedPreferences.contains(DNS1) && sharedPreferences.contains(DNS2);
    }

    public void setDns(String dns1, String dns2) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(DNS1, dns1);
        editor.putString(DNS2, dns2);
        editor.apply();
    }

    public void removeDns() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(DNS1);
        editor.remove(DNS2);
        editor.apply();
    }

    public Single<SharedPreferenceStringLiveData> getDomainCountLiveData() {
        return Single.fromCallable(() -> {
            FirewallUtils.DomainStat stat = FirewallUtils.getInstance().getDomainStatFromKnox();
            LogUtils.info("Domain stat from Knox: " + stat.toString());
            return new SharedPreferenceStringLiveData(sharedPreferences, DOMAIN_STAT, stat.toString());
        });
    }

    public void setDomainStatStr(String statStr) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(DOMAIN_STAT, statStr);
        editor.apply();
    }

    public Single<SharedPreferenceStringLiveData> getFirewallStatLiveData() {
        return Single.fromCallable(() -> {
            FirewallUtils.FirewallStat stat = FirewallUtils.getInstance().getFirewallStatFromKnox();
            LogUtils.info("Firewall stat from Knox: " + stat.toString());
            return new SharedPreferenceStringLiveData(sharedPreferences, FIREWALL_STAT, stat.toString());
        });
    }

    public void setFirewallStatStr(String statStr) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(FIREWALL_STAT, statStr);
        editor.apply();
    }

    public void setBlockedDomainsCount(int count) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(BLOCKED_DOMAINS_COUNT, count);
        editor.apply();
    }

    public int getBlockedDomainsCount() {
        return sharedPreferences.getInt(BLOCKED_DOMAINS_COUNT, 0);
    }

    public void resetBlockedDomainsCount() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.remove(BLOCKED_DOMAINS_COUNT);
        editor.apply();
    }

    public void resetPassword() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PASSWORD, "");
        editor.apply();
    }

    public void setPassword(String password) throws PasswordStorage.CannotPerformOperationException {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(PASSWORD, PasswordStorage.createHash(password));
        editor.apply();
    }

    public String getPasswordHash() {
        return sharedPreferences.getString(PASSWORD, "");
    }
}
