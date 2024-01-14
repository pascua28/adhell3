package com.fusionjack.adhell3.utils;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Handler;
import android.util.Patterns;

import androidx.annotation.Nullable;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.blocker.ContentBlocker;
import com.fusionjack.adhell3.blocker.ContentBlocker56;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.AppPermission;
import com.fusionjack.adhell3.db.entity.BlockUrl;
import com.fusionjack.adhell3.db.entity.BlockUrlProvider;
import com.fusionjack.adhell3.db.entity.DisabledPackage;
import com.fusionjack.adhell3.receiver.CustomDeviceAdminReceiver;
import com.fusionjack.adhell3.utils.dialog.DialogBuilder;
import com.samsung.android.knox.AppIdentity;
import com.samsung.android.knox.EnterpriseDeviceManager;
import com.samsung.android.knox.application.ApplicationPolicy;
import com.samsung.android.knox.license.KnoxEnterpriseLicenseManager;
import com.samsung.android.knox.net.firewall.DomainFilterRule;
import com.samsung.android.knox.net.firewall.Firewall;
import com.samsung.android.knox.restriction.RestrictionPolicy;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import javax.inject.Inject;

import io.reactivex.rxjava3.core.ObservableEmitter;

import static com.samsung.android.knox.application.ApplicationPolicy.ERROR_UNKNOWN;
import static com.samsung.android.knox.application.ApplicationPolicy.PERMISSION_POLICY_STATE_DEFAULT;
import static com.samsung.android.knox.application.ApplicationPolicy.PERMISSION_POLICY_STATE_DENY;

public final class AdhellFactory {
    private static AdhellFactory instance;

    @Nullable
    @Inject
    ApplicationPolicy appPolicy;

    @Nullable
    @Inject
    RestrictionPolicy restrictionPolicy;

    @Nullable
    @Inject
    Firewall firewall;

    @Inject
    AppDatabase appDatabase;

    @Inject
    PackageManager packageManager;

    @Inject
    SharedPreferences sharedPreferences;

    @Nullable
    @Inject
    KnoxEnterpriseLicenseManager knoxEnterpriseLicenseManager;

    private AdhellFactory() {
        App.get().getAppComponent().inject(this);
    }

    public static AdhellFactory getInstance() {
        if (instance == null) {
            instance = new AdhellFactory();
        }
        return instance;
    }

    @Nullable
    public ApplicationPolicy getAppPolicy() {
        return appPolicy;
    }

    @Nullable
    public RestrictionPolicy getRestrictionPolicy() {
        return restrictionPolicy;
    }

    @Nullable
    public Firewall getFirewall() {
        return firewall;
    }

    public AppDatabase getAppDatabase() {
        return appDatabase;
    }

    public PackageManager getPackageManager() {
        return packageManager;
    }

    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public void createNotSupportedDialog(Context context) {
        String knoxIsSupported = "Knox Enterprise License Manager is " + (knoxEnterpriseLicenseManager == null ? "not available" : "available");
        String knoxApiLevel = "Knox API Level: " + EnterpriseDeviceManager.getAPILevel();
        DialogBuilder.showDialog(R.string.not_supported_dialog_title, knoxIsSupported + "\n" + knoxApiLevel, context);
    }

    public void createNoInternetConnectionDialog(Context context) {
        DialogBuilder.showDialog(R.string.no_internet_connection_dialog_title, R.string.no_internet_connection, context);
    }

    public void setAppComponentToggle(boolean enabled) {
        // If the toggle is enabled, then we need to disable the app component
        AppPreferences.getInstance().setAppComponentToggle(enabled);
        boolean state = !enabled;
        setAppComponentState(state);
    }

    public void setAppComponentState(boolean state) {
        if (appPolicy == null) {
            return;
        }

        List<AppPermission> appPermissions = appDatabase.appPermissionDao().getAll();
        for (AppPermission appPermission : appPermissions) {
            String packageName = appPermission.packageName;
            String permissionName = appPermission.permissionName;
            switch (appPermission.permissionStatus) {
                case AppPermission.STATUS_PERMISSION:
                    List<String> permissions = new ArrayList<>();
                    permissions.add(permissionName);
                    setAppPermission(packageName, permissions, state);
                    break;
                case AppPermission.STATUS_ACTIVITY:
                case AppPermission.STATUS_SERVICE:
                case AppPermission.STATUS_PROVIDER:
                    ComponentName componentName = new ComponentName(packageName, permissionName);
                    appPolicy.setApplicationComponentState(componentName, state);
                    break;
                case AppPermission.STATUS_RECEIVER:
                    StringTokenizer tokenizer = new StringTokenizer(permissionName, "|");
                    componentName = new ComponentName(packageName, tokenizer.nextToken());
                    appPolicy.setApplicationComponentState(componentName, state);
                    break;
            }
        }
    }

    public int setAppPermission(String packageName, List<String> permissions, boolean state) {
        if (appPolicy == null) {
            return ERROR_UNKNOWN;
        }

        if (state) {
            return appPolicy.applyRuntimePermissions(new AppIdentity(packageName, null), permissions, PERMISSION_POLICY_STATE_DEFAULT);
        }
        return appPolicy.applyRuntimePermissions(new AppIdentity(packageName, null), permissions, PERMISSION_POLICY_STATE_DENY);
    }

    public void setDns(String primaryDns, String secondaryDns, ObservableEmitter<Integer> emitter) {
        if (primaryDns.isEmpty() && secondaryDns.isEmpty()) {
            AppPreferences.getInstance().removeDns();
            if (emitter != null) {
                emitter.onNext(R.string.restored_dns);
            }
        } else {
            try {
                InetAddress primaryDnsAddress = InetAddress.getByName(primaryDns);
                InetAddress secondaryDnsAddress = InetAddress.getByName(secondaryDns);

                if ((primaryDnsAddress instanceof Inet4Address || primaryDnsAddress instanceof Inet4Address) || (primaryDnsAddress instanceof Inet6Address && secondaryDnsAddress instanceof Inet6Address)) {
                    AppPreferences.getInstance().setDns(primaryDns, secondaryDns);
                    if (emitter != null) {
                        emitter.onNext(R.string.changed_dns);
                    }
                } else {
                    if (emitter != null) {
                        emitter.onNext(R.string.mixed_dns);
                    }
                }
            } catch (UnknownHostException e) {
                if (emitter != null) {
                    emitter.onNext(R.string.check_input_dns);
                }
            }
        }
    }

    public void applyDns(Handler handler) {
        if (AppPreferences.getInstance().isDnsNotEmpty()) {
            String dns1 = AppPreferences.getInstance().getDns1();
            String dns2 = AppPreferences.getInstance().getDns2();
            try {
                InetAddress dns1Address = InetAddress.getByName(dns1);
                InetAddress dns2Address = InetAddress.getByName(dns2);

                if ((dns1Address instanceof Inet4Address && dns2Address instanceof Inet4Address) || (dns1Address instanceof Inet6Address && dns2Address instanceof Inet6Address)) {
                    LogUtils.info("\nProcessing DNS...", handler);

                    LogUtils.info("DNS 1: " + dns1, handler);
                    LogUtils.info("DNS 2: " + dns2, handler);
                    List<AppInfo> dnsPackages = appDatabase.applicationInfoDao().getDnsApps();
                    if (dnsPackages.size() == 0) {
                        LogUtils.info("No app is selected", handler);
                    } else {
                        LogUtils.info("Size: " + dnsPackages.size(), handler);
                        List<DomainFilterRule> rules = new ArrayList<>();
                        for (AppInfo app : dnsPackages) {
                            DomainFilterRule rule = new DomainFilterRule(new AppIdentity(app.packageName, null));
                            rule.setDns1(dns1);
                            rule.setDns2(dns2);
                            rules.add(rule);
                        }

                        try {
                            FirewallUtils.getInstance().addDomainFilterRules(rules, handler);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }
    }

    public void setAppDisablerToggle(boolean state) {
        if (appPolicy == null) {
            return;
        }

        AppPreferences.getInstance().setAppDisablerToggle(state);
        List<DisabledPackage> disabledPackages = appDatabase.disabledPackageDao().getAll();
        for (DisabledPackage disabledPackage : disabledPackages) {
            if (state) {
                appPolicy.setDisableApplication(disabledPackage.packageName);
            } else {
                appPolicy.setEnableApplication(disabledPackage.packageName);
            }
        }
    }

    public void updateAllProviders() {
        List<BlockUrlProvider> providers = appDatabase.blockUrlProviderDao().getBlockUrlProviderBySelectedFlag(1);
        appDatabase.blockUrlDao().deleteBlockUrlsBySelectedProvider();
        for (BlockUrlProvider provider : providers) {
            try {
                List<BlockUrl> blockUrls = BlockUrlUtils.loadBlockUrls(provider);
                provider.count = blockUrls.size();
                provider.lastUpdated = new Date();
                appDatabase.blockUrlProviderDao().updateBlockUrlProviders(provider);
                appDatabase.blockUrlDao().insertAll(blockUrls);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public boolean hasInternetAccess(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
            LogUtils.info( "Is internet connection exists: " + isConnected);
            return isConnected;
        }
        return false;
    }

    public static void uninstall(Activity activity) {
        if (DeviceAdminInteractor.getInstance().isKnoxEnabled(activity)) {
            ContentBlocker contentBlocker = ContentBlocker56.getInstance();
            contentBlocker.disableDomainRules();
            contentBlocker.disableFirewallRules();
        }
        ComponentName devAdminReceiver = new ComponentName(activity, CustomDeviceAdminReceiver.class);
        DevicePolicyManager dpm = (DevicePolicyManager) activity.getSystemService(Context.DEVICE_POLICY_SERVICE);
        dpm.removeActiveAdmin(devAdminReceiver);
        Intent intent = new Intent(Intent.ACTION_DELETE);
        String packageName = "package:" + BuildConfig.APPLICATION_ID;
        intent.setData(Uri.parse(packageName));
        activity.startActivity(intent);
    }

    public boolean getComponentState(String packageName, String name) {
        if (appPolicy == null) {
            return false;
        }

        ComponentName componentName = new ComponentName(packageName, name);
        return appPolicy.getApplicationComponentState(componentName);
    }
}