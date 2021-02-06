package com.fusionjack.adhell3.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppPermission;
import com.fusionjack.adhell3.db.entity.ReportBlockedUrl;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.FirewallUtils;

import java.util.List;

import io.reactivex.Single;

public class HomeViewModel extends ViewModel {

    private final AppDatabase appDatabase;

    public HomeViewModel() {
        this.appDatabase = AdhellFactory.getInstance().getAppDatabase();
    }

    public Single<LiveData<Integer>> getDisablerInfo() {
        return Single.fromCallable(() -> appDatabase.disabledPackageDao().getSize());
    }

    public Single<LiveData<List<AppPermission>>> getAppComponentInfo() {
        return Single.fromCallable(() -> appDatabase.appPermissionDao().getAllAsLiveData());
    }

    public Single<LiveData<List<ReportBlockedUrl>>> getReportedBlockedDomains() {
        return Single.fromCallable(() ->
                appDatabase.reportBlockedUrlDao().getReportBlockUrlAfterAsLiveData(FirewallUtils.getInstance().last_x_hours_ui()));
    }

}
