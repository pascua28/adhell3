package com.fusionjack.adhell3.viewmodel;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.ReportBlockedUrl;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.FirewallUtils;
import com.fusionjack.adhell3.utils.LogUtils;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.core.SingleOnSubscribe;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class HomeTabViewModel extends ViewModel {
    private LiveData<List<ReportBlockedUrl>> reportBlockedUrlsDb;
    private MediatorLiveData<HashMap<String, List<ReportBlockedUrl>>> _reportBlockedUrls;
    private MutableLiveData<String> _domainInfo;
    private String domainInfoBase;
    private MutableLiveData<String> _firewallInfo;
    private String firewallInfoBase;
    private MutableLiveData<String> _disablerInfo;
    private String disablerInfoBase;
    private MutableLiveData<String> _appComponentInfo;
    private String appComponentInfoBase;
    private MutableLiveData<String> _blockedDomainInfo;
    private String blockedDomainInfoBase;
    private MutableLiveData<Boolean> _loadingVisibility;

    private final String loadingString = "Loading information...";

    public HomeTabViewModel() {
    }

    public LiveData<String> getDomainInfo(String strBase) {
        if (_domainInfo == null) {
            domainInfoBase = strBase;
            _domainInfo = new MutableLiveData<>();
            // Set loading string as initial value
            _domainInfo.setValue(loadingString);
        }
        return _domainInfo;
    }

    public void updateDomainInfo(int blacklistDomain, int whitelistDomain, int whitelistApp) {
        _domainInfo.setValue(
                String.format(domainInfoBase, blacklistDomain, whitelistDomain, whitelistApp)
        );
    }

    public void resetDomainInfo() {
        _domainInfo.setValue(loadingString);
    }

    public LiveData<String> getFirewallInfo(String strBase) {
        if (_firewallInfo == null) {
            firewallInfoBase = strBase;
            _firewallInfo = new MutableLiveData<>();
            // Set loading string as initial value
            _firewallInfo.setValue(loadingString);
        }
        return _firewallInfo;
    }

    public void updateFirewallInfo(int mobileData, int wifiData, int custom) {
        _firewallInfo.setValue(
                String.format(firewallInfoBase, mobileData, wifiData, custom)
        );
    }

    public void resetFirewallInfo() {
        _firewallInfo.setValue(loadingString);
    }

    public LiveData<String> getDisablerInfo(String strBase) {
        if (_disablerInfo == null) {
            disablerInfoBase = strBase;
            _disablerInfo = new MutableLiveData<>();
            // Set loading string as initial value
            _disablerInfo.setValue(loadingString);
        }
        return _disablerInfo;
    }

    public void updateDisablerInfo(int disabledApp) {
        _disablerInfo.setValue(
                String.format(disablerInfoBase, disabledApp)
        );
    }

    public void resetDisablerInfo() {
        _disablerInfo.setValue(loadingString);
    }

    public LiveData<String> getAppComponentInfo(String strBase) {
        if (_appComponentInfo == null) {
            appComponentInfoBase = strBase;
            _appComponentInfo = new MutableLiveData<>();
            // Set loading string as initial value
            _appComponentInfo.setValue(loadingString);
        }
        return _appComponentInfo;
    }

    public void updateAppComponentInfo(int permission, int service, int receiver, int activity, int provider) {
        _appComponentInfo.setValue(
                String.format(appComponentInfoBase, permission, service, receiver, activity, provider)
        );
    }

    public void resetAppComponentInfo() {
        _appComponentInfo.setValue(loadingString);
    }

    public LiveData<String> getBlockedDomainInfo(Context context) {
        if (_blockedDomainInfo == null) {
            blockedDomainInfoBase = context.getResources().getString(R.string.last_day_blocked);
            _blockedDomainInfo = new MutableLiveData<>();
            // Set empty string as initial value
            updateBlockedDomainInfo();
        }
        return _blockedDomainInfo;
    }

    public void updateBlockedDomainInfo() {
        if (_reportBlockedUrls != null && _reportBlockedUrls.getValue() != null) {
            int total = 0;
            for (List<ReportBlockedUrl> list : _reportBlockedUrls.getValue().values()) {
                total += list.size();
            }
            if (total > 0 || AppPreferences.getInstance().isDomainRulesToggleEnabled()) {
                _blockedDomainInfo.setValue(
                        String.format(Locale.getDefault(), "%s%d", blockedDomainInfoBase, total)
                );
                return;
            }
        }
        _blockedDomainInfo.setValue("");
    }

    public LiveData<Boolean> getLoadingBarVisibility() {
        if (_loadingVisibility == null) {
            _loadingVisibility = new MutableLiveData<>();
            // Set initial value as true
            updateLoadingBarVisibility(true);
        }
        return _loadingVisibility;
    }

    public void updateLoadingBarVisibility(boolean isVisible) {
        if (_loadingVisibility != null) {
            if ( _loadingVisibility.getValue() != null) {
                boolean currentState = _loadingVisibility.getValue();
                if (currentState != isVisible) {
                    _loadingVisibility.setValue(isVisible);
                }
            } else {
                _loadingVisibility.setValue(isVisible);
            }
        }
    }

    public LiveData<HashMap<String, List<ReportBlockedUrl>>> getReportBlockedUrls() {
        if (_reportBlockedUrls == null) {
            _reportBlockedUrls = new MediatorLiveData<>();
            reportBlockedUrlsDb = AdhellFactory.getInstance().getAppDatabase().reportBlockedUrlDao().getLiveReportBlockUrl();
            _reportBlockedUrls.addSource(
                    reportBlockedUrlsDb,
                    reportBlockedUrls -> _reportBlockedUrls.setValue(convertBlockedUrls(reportBlockedUrls))
            );
        }
        return _reportBlockedUrls;
    }

    public HashMap<String, List<ReportBlockedUrl>> convertBlockedUrls(List<ReportBlockedUrl> reportBlockedUrls) {
        return reportBlockedUrls.stream()
                .sorted((o1, o2) -> ((Comparable<Long>) o2.blockDate).compareTo(o1.blockDate))
                .collect(
                        Collectors.groupingBy(
                                ReportBlockedUrl::getPackageName,
                                LinkedHashMap::new,
                                Collectors.mapping(reportBlockedUrl -> reportBlockedUrl, Collectors.toList())
                        )
                );
    }

    public void setReportBlockedUrls() {
        if (reportBlockedUrlsDb.getValue() != null) {
            _reportBlockedUrls.setValue(convertBlockedUrls(reportBlockedUrlsDb.getValue()));
        }
    }

    public void refreshBlockedUrls() {
        updateLoadingBarVisibility(true);
        loadBlockedUrls();
    }

    private void loadBlockedUrls() {
        Single.create((SingleOnSubscribe<List<ReportBlockedUrl>>) emitter -> {
            List<ReportBlockedUrl> blockedUrls = FirewallUtils.getInstance().getReportBlockedUrl();
            emitter.onSuccess(blockedUrls);
        })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<ReportBlockedUrl>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull List<ReportBlockedUrl> blockedUrls) {
                        setReportBlockedUrls();
                        updateLoadingBarVisibility(false);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        LogUtils.error(e.getMessage(), e);
                    }
                });
    }

    public void setInfoCount() {
        Single.create((SingleOnSubscribe<InfoCount>) emitter -> {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();

            int disablerSize = appDatabase.disabledPackageDao().getSize();
            int permissionSize = appDatabase.appPermissionDao().getPermissionSize();
            int serviceSize = appDatabase.appPermissionDao().getServiceSize();
            int receiverSize = appDatabase.appPermissionDao().getReceiverSize();
            int activitySize = appDatabase.appPermissionDao().getActivitySize();
            int providerSize = appDatabase.appPermissionDao().getProviderSize();

            FirewallUtils.DomainStat domainStat = FirewallUtils.getInstance().getDomainStatFromKnox();
            int blackListSize = domainStat.blackListSize;
            int whiteListSize = domainStat.whiteListSize;

            int whitelistAppSize = FirewallUtils.getInstance().getWhitelistAppCountFromKnox();

            // Dirty solution: Every deny firewall is created for IPv4 and IPv6.
            FirewallUtils.FirewallStat stat = FirewallUtils.getInstance().getFirewallStatFromKnox();
            int customSize = stat.allNetworkSize / 2;
            int mobileSize = stat.mobileDataSize / 2;
            int wifiSize = stat.wifiDataSize / 2;

            emitter.onSuccess(new InfoCount(mobileSize, wifiSize, customSize, blackListSize,
                    whiteListSize, whitelistAppSize, disablerSize, permissionSize, serviceSize, receiverSize, activitySize, providerSize));
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<InfoCount>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull InfoCount infoCount) {
                        int mobileSize = infoCount.getMobileSize();
                        int wifiSize = infoCount.getWifiSize();
                        int customSize = infoCount.getCustomSize();
                        int blackListSize = infoCount.getBlackListSize();
                        int whiteListSize = infoCount.getWhiteListSize();
                        int whitelistAppSize = infoCount.getWhitelistAppSize();
                        int disablerSize = infoCount.getDisablerSize();
                        int permissionSize = infoCount.getPermissionSize();
                        int serviceSize = infoCount.getServiceSize();
                        int receiverSize = infoCount.getReceiverSize();
                        int activitySize = infoCount.getActivitySize();
                        int providerSize = infoCount.getProviderSize();

                        boolean domainRulesEnabled = AppPreferences.getInstance().isDomainRulesToggleEnabled();
                        boolean firewallRulesEnabled = AppPreferences.getInstance().isFirewallRulesToggleEnabled();
                        boolean appDisablerEnabled = AppPreferences.getInstance().isAppDisablerToggleEnabled();
                        boolean appComponentEnabled = AppPreferences.getInstance().isAppComponentToggleEnabled();

                        if (domainRulesEnabled) {
                            updateDomainInfo(blackListSize, whiteListSize, whitelistAppSize);
                        } else {
                            resetDomainInfo();
                        }

                        if (firewallRulesEnabled) {
                            updateFirewallInfo(mobileSize, wifiSize, customSize);
                        } else {
                            resetFirewallInfo();
                        }

                        if (appDisablerEnabled) {
                            updateDisablerInfo(disablerSize);
                        } else {
                            resetDisablerInfo();
                        }

                        if (appComponentEnabled) {
                            updateAppComponentInfo(permissionSize, serviceSize, receiverSize, activitySize, providerSize);
                        } else {
                            resetAppComponentInfo();
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        LogUtils.error(e.getMessage(), e);
                    }
                });
    }

    private static class InfoCount {
        private final int mobileSize;
        private final int wifiSize;
        private final int customSize;
        private final int blackListSize;
        private final int whiteListSize;
        private final int whitelistAppSize;
        private final int disablerSize;
        private final int permissionSize;
        private final int serviceSize;
        private final int receiverSize;
        private final int activitySize;
        private final int providerSize;

        public InfoCount(int mobileSize, int wifiSize, int customSize, int blackListSize,
                         int whiteListSize, int whitelistAppSize, int disablerSize,
                         int permissionSize, int serviceSize, int receiverSize, int activitySize, int providerSize) {
            this.mobileSize = mobileSize;
            this.wifiSize = wifiSize;
            this.customSize = customSize;
            this.blackListSize = blackListSize;
            this.whiteListSize = whiteListSize;
            this.whitelistAppSize = whitelistAppSize;
            this.disablerSize = disablerSize;
            this.permissionSize = permissionSize;
            this.serviceSize = serviceSize;
            this.receiverSize = receiverSize;
            this.activitySize = activitySize;
            this.providerSize = providerSize;
        }

        public int getMobileSize() {
            return mobileSize;
        }

        public int getWifiSize() {
            return wifiSize;
        }

        public int getCustomSize() {
            return customSize;
        }

        public int getBlackListSize() {
            return blackListSize;
        }

        public int getWhiteListSize() {
            return whiteListSize;
        }

        public int getWhitelistAppSize() {
            return whitelistAppSize;
        }

        public int getDisablerSize() {
            return disablerSize;
        }

        public int getPermissionSize() {
            return permissionSize;
        }

        public int getServiceSize() {
            return serviceSize;
        }

        public int getActivitySize() {
            return activitySize;
        }

        public int getProviderSize() {
            return providerSize;
        }

        public int getReceiverSize() {
            return receiverSize;
        }
    }
}
