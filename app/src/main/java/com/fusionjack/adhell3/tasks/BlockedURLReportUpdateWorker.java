package com.fusionjack.adhell3.tasks;


import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.fusionjack.adhell3.dialogfragment.AutoUpdateDialogFragment;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.FirewallUtils;
import com.fusionjack.adhell3.utils.LogUtils;

import java.text.DateFormat;
import java.util.Locale;

public class BlockedURLReportUpdateWorker extends Worker {
    private int retryCount;
    private Handler handler = null;

    public BlockedURLReportUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
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
    public Result doWork() {
        retryCount = this.getRunAttemptCount();
        if (retryCount > AutoUpdateDialogFragment.MAX_RETRY) {
            AutoUpdateDialogFragment.enqueueNextAutoUpdateWork();
            return Result.failure();
        }

        if (AppPreferences.getInstance().getBlockedUrlReportAutoUpdate()) {
            LogUtils.info("------Start auto update blocked url report------", handler);
            try {
                LogUtils.info("Updating blocked url report...", handler);
                FirewallUtils.getInstance().getReportBlockedUrl();
                LogUtils.info("Done.", handler);
            } catch (Exception e) {
                LogUtils.error("Failed auto update blocked url report! Will be retried.", e, handler);
                LogUtils.info("------Failed auto update blocked url report------", handler);
                return Result.retry();
            }
            LogUtils.info("------Successful auto update blocked url report------", handler);
        } else {
            LogUtils.info("------Auto update blocked url report is disable------", handler);
        }

        return Result.success();
    }
}
