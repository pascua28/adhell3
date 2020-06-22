package com.fusionjack.adhell3.tasks;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import androidx.annotation.NonNull;
import androidx.documentfile.provider.DocumentFile;
import androidx.work.BackoffPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.dialogfragment.AutoUpdateDialogFragment;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.LogUtils;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class ReScheduleUpdateWorker extends Worker {
    private int retryCount;
    private Handler handler = null;

    public ReScheduleUpdateWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
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
            return Result.failure();
        }

        LogUtils.info("------Start scheduling next job------", handler);
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        LogUtils.info(String.format(Locale.getDefault(), "Setting the start time of the next job to %s...", dateFormat.format(getNexStartDateTime().getTime())), handler);
        try{
            readjustPeriodicWork();
            LogUtils.info("  Done.", handler);
        } catch (Exception e) {
            LogUtils.error("Failure to setting the start time of the next job. Will be retried.", e, handler);
            LogUtils.info("------Failed scheduling next job------", handler);
            return Result.retry();
        }
        LogUtils.info("------Successful scheduling next job------", handler);

        return Result.success();
    }

    private void readjustPeriodicWork() {
        WorkManager workManager = WorkManager.getInstance(App.getAppContext());
        long nextJobMillis = getNexStartDateTime().getTimeInMillis();
        long nowMillis = Calendar.getInstance().getTimeInMillis();
        long initialDelay = nextJobMillis - nowMillis;

        OneTimeWorkRequest rulesWorkRequest = new OneTimeWorkRequest.Builder(RulesUpdateWorker.class)
                .setConstraints(AutoUpdateDialogFragment.getAutoUpdateConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .setInitialDelay(initialDelay, TimeUnit.MILLISECONDS)
                .build();

        OneTimeWorkRequest appComponentsWorkRequest = new OneTimeWorkRequest.Builder(AppComponentsUpdateWorker.class)
                .setConstraints(AutoUpdateDialogFragment.getAutoUpdateConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build();

        OneTimeWorkRequest reScheduleWorkRequest = new OneTimeWorkRequest.Builder(ReScheduleUpdateWorker.class)
                .setConstraints(AutoUpdateDialogFragment.getAutoUpdateConstraints())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build();

        // Cancel previous job
        workManager.cancelAllWork();

        // Enqueue new job with readjusting initial delay
        workManager.beginWith(rulesWorkRequest)
                .then(appComponentsWorkRequest)
                .then(reScheduleWorkRequest)
                .enqueue();
    }

    private Calendar getNexStartDateTime() {
        Calendar calendar = Calendar.getInstance();
        int repeatInterval = AutoUpdateDialogFragment.intervalArray[AppPreferences.getInstance().getAutoUpdateInterval()];

        calendar.set(Calendar.HOUR_OF_DAY, AppPreferences.getInstance().getStartHourAutoUpdate());
        calendar.set(Calendar.MINUTE, AppPreferences.getInstance().getStartMinuteAutoUpdate());
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        calendar.add(Calendar.DAY_OF_MONTH, repeatInterval);

        return calendar;
    }
}