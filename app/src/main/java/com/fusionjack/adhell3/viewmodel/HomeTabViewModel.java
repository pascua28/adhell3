package com.fusionjack.adhell3.viewmodel;

import android.content.Context;
import android.os.AsyncTask;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppPermission;
import com.fusionjack.adhell3.db.entity.ReportBlockedUrl;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.FirewallUtils;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

    public LiveData<String> getDomainInfo(Context context) {
        if (_domainInfo == null) {
            domainInfoBase = context.getResources().getString(R.string.domain_rules_info);
            _domainInfo = new MutableLiveData<>();
            // Set loading string as initial value
            _domainInfo.setValue(loadingString);
        }
        return _domainInfo;
    }

    private void updateDomainInfo(int blacklistDomain, int whitelistDomain, int whitelistApp) {
        _domainInfo.setValue(
                String.format(domainInfoBase, blacklistDomain, whitelistDomain, whitelistApp)
        );
    }

    public LiveData<String> getFirewallInfo(Context context) {
        if (_firewallInfo == null) {
            firewallInfoBase = context.getResources().getString(R.string.firewall_rules_info);
            _firewallInfo = new MutableLiveData<>();
            // Set loading string as initial value
            _firewallInfo.setValue(loadingString);
        }
        return _firewallInfo;
    }

    private void updateFirewallInfo(int mobileData, int wifiData, int custom) {
        _firewallInfo.setValue(
                String.format(firewallInfoBase, mobileData, wifiData, custom)
        );
    }

    public LiveData<String> getDisablerInfo(Context context) {
        if (_disablerInfo == null) {
            disablerInfoBase = context.getResources().getString(R.string.app_disabler_info);
            _disablerInfo = new MutableLiveData<>();
            // Set loading string as initial value
            _disablerInfo.setValue(loadingString);
        }
        return _disablerInfo;
    }

    private void updateDisablerInfo(int disabledApp) {
        _disablerInfo.setValue(
                String.format(disablerInfoBase, disabledApp)
        );
    }

    public LiveData<String> getAppComponentInfo(Context context) {
        if (_appComponentInfo == null) {
            appComponentInfoBase = context.getResources().getString(R.string.app_component_toggle_info);
            _appComponentInfo = new MutableLiveData<>();
            // Set loading string as initial value
            _appComponentInfo.setValue(loadingString);
        }
        return _appComponentInfo;
    }

    private void updateAppComponentInfo(int permission,int service, int receiver, int activity) {
        _appComponentInfo.setValue(
                String.format(appComponentInfoBase, permission, service, receiver, activity)
        );
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

    private void updateBlockedDomainInfo() {
        if (_reportBlockedUrls != null) {
            HashMap<String, List<ReportBlockedUrl>> list = _reportBlockedUrls.getValue();
            if (list != null && list.size() > 0 && reportBlockedUrlsDb.getValue() != null) {
                _blockedDomainInfo.setValue(
                        String.format(Locale.getDefault(), "%s%d", blockedDomainInfoBase, reportBlockedUrlsDb.getValue().size())
                );
            } else {
                _blockedDomainInfo.setValue("");
            }
        }
    }

    public void setInfo() {
        new SetInfoAsyncTask(this).execute();
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
        _loadingVisibility.setValue(isVisible);
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
        HashMap<String, List<ReportBlockedUrl>> returnHashMap = new HashMap<>();
        for (ReportBlockedUrl reportBlockedUrl : reportBlockedUrls) {
            List<ReportBlockedUrl> newList = returnHashMap.get(reportBlockedUrl.packageName);
            if (newList == null) {
                newList = new ArrayList<>();
                newList.add(reportBlockedUrl);
                returnHashMap.put(reportBlockedUrl.packageName, newList);
            } else {
                newList.add(reportBlockedUrl);
            }
        }
        // Sort HashMap by 'blockDate'
        LinkedList<HashMap.Entry<String, List<ReportBlockedUrl>>> linkedList = new LinkedList<>(returnHashMap.entrySet());
        LinkedHashMap<String, List<ReportBlockedUrl>> sortedHashMap = new LinkedHashMap<>();
        linkedList.sort((list1, list2) ->
                ((Comparable<Long>) list2.getValue().get(0).blockDate).compareTo(list1.getValue().get(0).blockDate));
        for (Map.Entry<String, List<ReportBlockedUrl>> entry : linkedList) {
            sortedHashMap.put(entry.getKey(), entry.getValue());
        }

        return sortedHashMap;
    }

    public void setReportBlockedUrls() {
        if (reportBlockedUrlsDb.getValue() != null) {
            _reportBlockedUrls.setValue(convertBlockedUrls(reportBlockedUrlsDb.getValue()));
        }
    }

    public void refreshBlockedUrls() {
        updateLoadingBarVisibility(true);
        new RefreshBlockedUrlAsyncTask(this).execute();
    }

    private static class SetInfoAsyncTask extends AsyncTask<Void, Void, Void> {
        private int mobileSize;
        private int wifiSize;
        private int customSize;
        private int blackListSize;
        private int whiteListSize;
        private int whitelistAppSize;
        private int disablerSize;
        private int permissionSize;
        private int serviceSize;
        private int receiverSize;
        private int activitySize;
        private final boolean appDisablerEnabled = AppPreferences.getInstance().isAppDisablerToggleEnabled();
        private final boolean appComponentEnabled = AppPreferences.getInstance().isAppComponentToggleEnabled();
        private final WeakReference<HomeTabViewModel> homeTabViewModelWeakReference;

        SetInfoAsyncTask(HomeTabViewModel homeTabViewModel) {
            this.homeTabViewModelWeakReference = new WeakReference<>(homeTabViewModel);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (appDisablerEnabled) {
                AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
                disablerSize = appDatabase.disabledPackageDao().getAll().size();
            }

            if (appComponentEnabled) {
                AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
                List<AppPermission> appPermissions = appDatabase.appPermissionDao().getAll();
                for (AppPermission appPermission : appPermissions) {
                    switch (appPermission.permissionStatus) {
                        case AppPermission.STATUS_PERMISSION:
                            permissionSize++;
                            break;
                        case AppPermission.STATUS_SERVICE:
                            serviceSize++;
                            break;
                        case AppPermission.STATUS_RECEIVER:
                            receiverSize++;
                            break;
                        case AppPermission.STATUS_ACTIVITY:
                            activitySize++;
                            break;
                    }
                }
            }

            FirewallUtils.DomainStat domainStat = FirewallUtils.getInstance().getDomainStatFromKnox();
            blackListSize = domainStat.blackListSize;
            whiteListSize = domainStat.whiteListSize;
            whitelistAppSize = FirewallUtils.getInstance().getWhitelistAppCountFromKnox();

            // Dirty solution: Every deny firewall is created for IPv4 and IPv6.
            FirewallUtils.FirewallStat stat = FirewallUtils.getInstance().getFirewallStatFromKnox();
            customSize = stat.allNetworkSize / 2;
            mobileSize = stat.mobileDataSize / 2;
            wifiSize = stat.wifiDataSize / 2;

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            HomeTabViewModel homeTabViewModel = homeTabViewModelWeakReference.get();
            if (homeTabViewModel != null) {
                homeTabViewModel.updateDomainInfo(blackListSize, whiteListSize, whitelistAppSize);

                homeTabViewModel.updateFirewallInfo(mobileSize, wifiSize, customSize);

                if (appDisablerEnabled) {
                    homeTabViewModel.updateDisablerInfo(disablerSize);
                }

                if (appComponentEnabled) {
                    homeTabViewModel.updateAppComponentInfo(permissionSize, serviceSize, receiverSize, activitySize);
                }
            }
        }
    }

    private static class RefreshBlockedUrlAsyncTask extends AsyncTask<Void, Void, Void> {
        private final WeakReference<HomeTabViewModel> homeTabViewModelWeakReference;

        RefreshBlockedUrlAsyncTask(HomeTabViewModel homeTabViewModel) {
            this.homeTabViewModelWeakReference = new WeakReference<>(homeTabViewModel);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            FirewallUtils.getInstance().updateReportBlockedUrl();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            HomeTabViewModel homeTabViewModel = homeTabViewModelWeakReference.get();
            if (homeTabViewModel != null) {
                homeTabViewModel.setReportBlockedUrls();
                homeTabViewModel.updateBlockedDomainInfo();
                homeTabViewModel.updateLoadingBarVisibility(false);
            }
        }
    }
}
