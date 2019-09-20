package com.fusionjack.adhell3.utils;

import androidx.annotation.NonNull;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static CrashHandler instance;

    private final Thread.UncaughtExceptionHandler defaultHandler;

    private CrashHandler() {
        this.defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
    }

    public static CrashHandler getInstance() {
        if (instance == null) {
            instance = new CrashHandler();
        }
        return instance;
    }

    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable throwable) {
        LogUtils.createLogcat();
        defaultHandler.uncaughtException(thread, throwable);
    }
}
