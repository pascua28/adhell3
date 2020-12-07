package com.fusionjack.adhell3.tasks;


import android.content.ComponentName;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.AppPermission;
import com.fusionjack.adhell3.dialogfragment.AutoUpdateDialogFragment;
import com.fusionjack.adhell3.model.AppComponent;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppComponentFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.FileUtils;
import com.fusionjack.adhell3.utils.LogUtils;
import com.samsung.android.knox.application.ApplicationPolicy;

import java.text.DateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class AppComponentsUpdateWorker extends Worker {
    private final AppDatabase appDatabase;
    private int retryCount;
    private Handler handler = null;

    public AppComponentsUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);

        if (AppPreferences.getInstance().getCreateLogOnAutoUpdate()) {
            DocumentFile logFile = LogUtils.getAutoUpdateLogFile();
            this.handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    String nowDate = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(new java.util.Date());
                    if (msg.obj != null) {
                        LogUtils.appendLogFile(String.format(Locale.getDefault(), "%s [retry%d]: %s", nowDate, retryCount, msg.obj.toString().trim()), logFile);
                    }
                }
            };
        }
        appDatabase = AppDatabase.getAppDatabase(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        retryCount = this.getRunAttemptCount();
        if (retryCount > AutoUpdateDialogFragment.MAX_RETRY) {
            AutoUpdateDialogFragment.enqueueNextAutoUpdateWork();
            return Result.failure();
        }

        if (AppPreferences.getInstance().getAppComponentsAutoUpdate()) {
            LogUtils.info("------Start App components auto update------", handler);
            try {
                processAppComponentsInAutoUpdate();
            } catch (Exception e) {
                LogUtils.error("Failed App components auto update! Will be retried.", e, handler);
                LogUtils.info("------Failed App components auto update------", handler);
                return Result.retry();
            }
            LogUtils.info("------Successful App components auto update------", handler);
        } else {
            LogUtils.info("------App components auto update is disable------", handler);
        }

        return Result.success();
    }

    private void processAppComponentsInAutoUpdate() throws Exception {
        LogUtils.info(String.format(Locale.getDefault(), "Getting file '%s'...", AppComponentFactory.COMPONENTS_FILENAME), handler);
        DocumentFile componentsFile = FileUtils.getDocumentFile(AppComponentFactory.STORAGE_FOLDERS, AppComponentFactory.COMPONENTS_FILENAME, FileUtils.FileCreationType.IF_NOT_EXIST);

        LogUtils.info("Listing services, receivers and activities to be disabled...", handler);
        Set<String> compNames = AppComponentFactory.getInstance().getFileContent(componentsFile);

        if (compNames.size() > 0) {
            LogUtils.info("Updating disabled app components...", handler);
            int count = 0;
            List<AppInfo> apps = appDatabase.applicationInfoDao().getUserAndDisabledApps();
            for (AppInfo app : apps) {
                String packageName = app.packageName;
                Set<String> availableServiceNames = AppComponent.getServiceNames(packageName);
                Set<String> availableReceiverNames = AppComponent.getReceiverNames(packageName);
                Set<String> availableActivityNames = AppComponent.getActivityNames(packageName);
                for (String compName : compNames) {
                    boolean disable = false;
                    int permissionStatus = 0;
                    String componentType = "";

                    if (availableServiceNames.contains(compName)) {
                        disable = true;
                        permissionStatus = AppPermission.STATUS_SERVICE;
                        componentType = "service";
                    } else if (availableReceiverNames.contains(compName)) {
                        disable = true;
                        permissionStatus = AppPermission.STATUS_RECEIVER;
                        componentType = "receiver";
                    } else if (availableActivityNames.contains(compName)) {
                        disable = true;
                        permissionStatus = AppPermission.STATUS_ACTIVITY;
                        componentType = "activity";
                    }

                    if (disable) {
                        try {
                            boolean compState = AdhellFactory.getInstance().getComponentState(packageName, compName);
                            if (compState) {
                                ComponentName componentName = new ComponentName(packageName, compName);
                                ApplicationPolicy appPolicy = AdhellFactory.getInstance().getAppPolicy();
                                if (appPolicy != null) {
                                    boolean success = appPolicy.setApplicationComponentState(componentName, false);
                                    if (success) {
                                        count++;
                                        LogUtils.info(String.format(Locale.getDefault(), "Disabling %s '%s' for package '%s'", componentType, compName, packageName), handler);

                                        AppPermission appService = new AppPermission();
                                        appService.packageName = packageName;
                                        if (permissionStatus == AppPermission.STATUS_RECEIVER) {
                                            appService.permissionName = compName + "|Auto";
                                        } else {
                                            appService.permissionName = compName;
                                        }
                                        appService.permissionStatus = permissionStatus;
                                        appService.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                                        appDatabase.appPermissionDao().insert(appService);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            LogUtils.error("Unable to disable app components!", e, handler);
                        }
                    }
                }
            }
            if (count <= 0) {
                LogUtils.info("Nothing new to disable", handler);
            } else {
                LogUtils.info("Update for disabled app components completed.", handler);
            }
        } else {
            LogUtils.info("File is empty. Nothing to do.", handler);
        }
    }
}
