package com.fusionjack.adhell3.utils;

import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.BuildConfig;
import com.samsung.android.knox.EnterpriseDeviceManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by fusionjack on 15/03/2018.
 */

public final class LogUtils {

    private static final String TAG = "adhell3";
    private static final SimpleDateFormat FORMATTER = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US);

    private LogUtils() {
    }

    public static String createLogcat() throws Exception {
        info("Build version: " + BuildConfig.VERSION_NAME);
        info("Knox API: " + EnterpriseDeviceManager.getAPILevel());
        info("Android API: " + Build.VERSION.SDK_INT);
        String filename = String.format("adhell_logcat_%s.txt", now());
        String ownPackageName = App.get().getApplicationContext().getPackageName();
        Process logcatProcess = Runtime.getRuntime().exec( "logcat -d | grep " + ownPackageName);
        DocumentFileUtils.dumpToFile(logcatProcess.getInputStream(), filename);
        return filename;
    }

    public static void info(String text) {
        Log.i(TAG, buildMessage(text));
    }

    public static void info(String text, Handler handler) {
        if (handler != null) {
            Message message = handler.obtainMessage(0, text);
            message.sendToTarget();
        }
        Log.i(TAG, buildMessage(text));
    }

    public static void error(String text) {
        Log.e(TAG, buildMessage(text));
    }

    public static void error(String text, Throwable e) {
        Log.e(TAG, buildMessage(text), e);
    }

    public static void error(String text, Throwable e, Handler handler) {
        if (handler != null) {
            Message message = handler.obtainMessage(0, text);
            message.sendToTarget();
        }
        Log.e(TAG, buildMessage(text), e);
    }
    
    private static String now() {
        return FORMATTER.format(new Date());
    }

    private static String buildMessage(String text) {
        return getCallerInfo() + " " + text;
    }

    private static String getCallerInfo() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        if (stackTraceElements.length > 5) {
            StackTraceElement element = stackTraceElements[5];
            return element.getClassName() + "(" + element.getMethodName() + ")";
        }
        return "Empty class name";
    }
    
}
