package com.fusionjack.adhell3.tasks;

import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.model.AppFlag;
import com.fusionjack.adhell3.utils.AppDatabaseFactory;
import com.fusionjack.adhell3.utils.rx.RxCompletableIoBuilder;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.functions.Action;

public class ToggleAppInfoRxTask {

    public static void run(AppInfo appInfo, AppFlag appFlag, AppInfoAdapter adapter) {
        Runnable onCompleteCallback = adapter::notifyDataSetChanged;
        Action action = () -> AppDatabaseFactory.toggleAppInfo(appInfo, appFlag);
        new RxCompletableIoBuilder().async(Completable.fromAction(action), onCompleteCallback);
    }

}
