package com.fusionjack.adhell3.tasks;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.WorkerParameters;
import androidx.work.rxjava3.RxWorker;

import com.fusionjack.adhell3.dialogfragment.AutoUpdateDialogFragment;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AppDatabaseFactory;
import com.fusionjack.adhell3.utils.AppDiff;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.LogUtils;

import java.text.DateFormat;
import java.util.Locale;

import io.reactivex.rxjava3.core.Single;

public class CheckDBIntegrityWorker extends RxWorker {
    private int retryCount;
    private Handler handler = null;

    public CheckDBIntegrityWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
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
    }

    @NonNull
    @Override
    public Single<Result> createWork() {
        return Single.create(emitter -> {
            retryCount = this.getRunAttemptCount();
            if (retryCount > AutoUpdateDialogFragment.MAX_RETRY) {
                AutoUpdateDialogFragment.enqueueNextAutoUpdateWork();
                emitter.onError(new Exception("Retry exceeded"));
                return;
            }

            if (AppPreferences.getInstance().getCheckDBAutoUpdate()) {
                LogUtils.info("------Start DB integrity check------", handler);
                try {
                    LogUtils.info("Launching DB integrity check...", handler);
                    AdhellAppIntegrity adhellAppIntegrity = AdhellAppIntegrity.getInstance();
                    adhellAppIntegrity.checkDefaultPolicyExists();
                    adhellAppIntegrity.checkAdhellStandardPackage();
                    boolean isPackageDbEmpty = adhellAppIntegrity.isPackageDbEmpty();
                    if (isPackageDbEmpty) {
                        LogUtils.info("Package DB is empty, need to reset installed apps...", handler);
                        AppDatabaseFactory.resetInstalledApps().blockingSubscribe();
                    } else {
                        LogUtils.info("Package DB is not empty, start detection of new or deleted apps...", handler);
                        AppDiff appDiff = AppDatabaseFactory.detectNewOrDeletedApps().blockingGet();
                        if (!appDiff.isEmpty()) {
                            int newAppSize = appDiff.getNewApps().size();
                            int deletedAppSize = appDiff.getDeletedApps().size();
                            LogUtils.info(newAppSize + " new app(s) and " + deletedAppSize + " deleted app(s) have been detected.", handler);
                        } else {
                            LogUtils.info("No new app(s) or deleted app(s) detected. Nothing to do.", handler);
                        }
                    }
                    LogUtils.info("DB integrity check performed with success!", handler);
                    emitter.onSuccess(Result.success());
                } catch (Exception e) {
                    emitter.onError(e);
                }
            } else {
                LogUtils.info("------DB integrity check is disable------", handler);
            }
        });
    }
}
