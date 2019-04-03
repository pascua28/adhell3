package com.fusionjack.adhell3.db.repository;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.ListView;
import android.widget.ProgressBar;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.fragments.FilterAppInfo;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.samsung.android.knox.application.ApplicationPolicy;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import io.reactivex.Single;

public class AppRepository {

    ProgressBar loadingBar;
    ListView listView;


    public Single<List<AppInfo>> loadAppList(String text, Type type, FilterAppInfo filterAppInfo, ProgressBar loadingBar, ListView listView) {
        return Single.create(emitter -> {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            String filterText = '%' + text + '%';
            List<AppInfo> list = new ArrayList<>();
            if (loadingBar != null) this.loadingBar = new WeakReference<>(loadingBar).get();
            if (listView != null) this.listView = new WeakReference<>(listView).get();
            //showProgressBar();
            switch (type) {
                case DISABLER:
                    ApplicationPolicy appPolicy = AdhellFactory.getInstance().getAppPolicy();
                    List<AppInfo> tempList = list;
                    if (filterAppInfo.getSystemAppsFilter() && filterAppInfo.getUserAppsFilter()) {
                        if (text.length() == 0) {
                            tempList = appDatabase.applicationInfoDao().getAppsInDisabledOrder();
                        }
                        tempList = appDatabase.applicationInfoDao().getAppsInDisabledOrder(filterText);
                        list.addAll(tempList);
                        tempList.clear();
                    } else if (filterAppInfo.getSystemAppsFilter()) {
                        if (text.length() == 0) {
                            tempList = appDatabase.applicationInfoDao().getAllSystemApps();
                        }
                        tempList = appDatabase.applicationInfoDao().getAllSystemApps(filterText);
                        list.addAll(tempList);
                        tempList.clear();
                    } else if (filterAppInfo.getUserAppsFilter()) {
                        if (text.length() == 0) {
                            tempList = appDatabase.applicationInfoDao().getAllUserApps();
                        }
                        tempList = appDatabase.applicationInfoDao().getAllUserApps(filterText);
                        list.addAll(tempList);
                        tempList.clear();
                    }

                    for (AppInfo item : list) {
                        boolean isRunning = false;
                        try {
                            if (appPolicy != null) {
                                isRunning = appPolicy.isApplicationRunning(item.packageName);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if (filterAppInfo.getRunningAppsFilter() && isRunning) {
                            item.appName = item.appName + AppInfoAdapter.RUNNING_TAG;
                            tempList.add(item);
                        } else if (filterAppInfo.getStoppedAppsFilter() && !isRunning)
                            tempList.add(item);
                    }
                    list.clear();
                    list.addAll(tempList);
                    tempList.clear();
                    break;
                case MOBILE_RESTRICTED:
                    if (text.length() == 0) {
                        list = appDatabase.applicationInfoDao().getAppsInMobileRestrictedOrder();
                    }
                    list = appDatabase.applicationInfoDao().getAppsInMobileRestrictedOrder(filterText);
                    break;
                case WIFI_RESTRICTED:
                    if (text.length() == 0) {
                        list = appDatabase.applicationInfoDao().getAppsInWifiRestrictedOrder();
                    }
                    list = appDatabase.applicationInfoDao().getAppsInWifiRestrictedOrder(filterText);
                    break;
                case WHITELISTED:
                    if (text.length() == 0) {
                        list = appDatabase.applicationInfoDao().getAppsInWhitelistedOrder();
                    }
                    list = appDatabase.applicationInfoDao().getAppsInWhitelistedOrder(filterText);
                    break;
                case COMPONENT:
                    boolean showSystemApps = BuildConfig.SHOW_SYSTEM_APP_COMPONENT;
                    if (text.length() == 0) {
                        list = showSystemApps ?
                                appDatabase.applicationInfoDao().getEnabledAppsAlphabetically() :
                                appDatabase.applicationInfoDao().getUserApps();
                    }
                    list = showSystemApps ?
                            appDatabase.applicationInfoDao().getEnabledAppsAlphabetically(filterText) :
                            appDatabase.applicationInfoDao().getUserApps(filterText);
                    break;
                case DNS:
                    if (text.length() == 0) {
                        list = appDatabase.applicationInfoDao().getAppsInDnsOrder();
                    }
                    list = appDatabase.applicationInfoDao().getAppsInDnsOrder(filterText);
                    break;
            }
            hideProgressBar();
            emitter.onSuccess(list);
        });
    }

    private void hideProgressBar() {
        if (loadingBar != null && listView != null) {
            new Handler(Looper.getMainLooper()).post(() -> {
                loadingBar.setVisibility(View.GONE);
                if (listView.getVisibility() == View.GONE) {
                    AlphaAnimation animation = new AlphaAnimation(0f, 1f);
                    animation.setDuration(500);
                    animation.setStartOffset(50);
                    animation.setFillAfter(true);

                    listView.setVisibility(View.VISIBLE);
                    listView.startAnimation(animation);
                }
            });
        }
    }

    private void showProgressBar() {
        if (loadingBar != null) {
            new Handler(Looper.getMainLooper()).post(() ->
                loadingBar.setVisibility(View.VISIBLE));
        }
    }

    public enum Type {
        DISABLER,
        MOBILE_RESTRICTED,
        WIFI_RESTRICTED,
        WHITELISTED,
        COMPONENT,
        DNS
    }
}
