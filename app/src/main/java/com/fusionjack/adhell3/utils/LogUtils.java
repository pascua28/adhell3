package com.fusionjack.adhell3.utils;

import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.fusionjack.adhell3.BuildConfig;
import com.samsung.android.knox.EnterpriseDeviceManager;

import java.io.File;
import java.io.IOException;


public final class LogUtils {

    private static final String STORAGE_FOLDER = "/Adhell3/Logs/";

    private LogUtils() {
    }

    public static String createLogcat() {
        info("Build version: " + BuildConfig.VERSION_NAME);
        info("Knox API: " + EnterpriseDeviceManager.getAPILevel());
        info("Android API: " + Build.VERSION.SDK_INT);
        File folder = new File(Environment.getExternalStorageDirectory() + STORAGE_FOLDER);
        if (!folder.exists()) if (!folder.mkdirs()) error("Unable to create folder");
        String filename = String.format("adhell_logcat_%s.txt", System.currentTimeMillis());
        File logFile = new File(folder, filename);
        try {
            Runtime.getRuntime().exec( "logcat -f " + logFile + " | grep com.fusionjack.adhell3");
        } catch (IOException e) {
            error(e.getMessage(), e);
            return "";
        }
        return filename;
    }

    public static void info(String text) {
        Log.i(getCallerInfo(), text);
    }

    public static void info(String text, Handler handler) {
        if (handler != null) {
            Message message = handler.obtainMessage(0, text);
            message.sendToTarget();
        }
        Log.i(getCallerInfo(), text);
    }

    public static void error(String text) {
        Log.e(getCallerInfo(), text);
    }

    public static void error(String text, Throwable e) {
        Log.e(getCallerInfo(), text, e);
    }

    public static void error(String text, Throwable e, Handler handler) {
        if (handler != null) {
            Message message = handler.obtainMessage(0, text);
            message.sendToTarget();
        }
        Log.e(getCallerInfo(), text, e);
    }

    private static String getCallerInfo() {
        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();
        if (stackTraceElements.length > 4) {
            return stackTraceElements[4].getClassName() + "(" + stackTraceElements[4].getMethodName() + ")";
        }
        return "Empty class name";
    }
}