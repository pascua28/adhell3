package com.fusionjack.adhell3.utils;

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static CrashHandler instance;

    private Thread.UncaughtExceptionHandler defaultHandler;

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
    public void uncaughtException(Thread thread, Throwable throwable) {
        try {
            LogUtils.createLogcat();
        } catch (Exception ignored) {
        }
        defaultHandler.uncaughtException(thread, throwable);
    }
}
