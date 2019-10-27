package com.fusionjack.adhell3.utils;

import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.fusionjack.adhell3.BuildConfig;
import com.samsung.android.knox.EnterpriseDeviceManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

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

    public static void appendLogFile(String newLine, File logFile) {
        try {
            FileOutputStream fos = new FileOutputStream(logFile, true);
            OutputStreamWriter osw = new OutputStreamWriter(fos);
            osw.write(String.format("%s%s", newLine, System.lineSeparator()));
            osw.flush();
            osw.close();
            fos.close();
        } catch (IOException e) {
            error(e.getMessage(), e);
        }
    }

    public static File getAutoUpdateLogFile() {
        File folder = new File(Environment.getExternalStorageDirectory() + STORAGE_FOLDER);
        if (!folder.exists()) if (!folder.mkdirs()) error("Unable to create folder");
        String filename = "adhell_auto_update_log.txt";
        File logFile = new File(folder, filename);
        try {
            if (!logFile.exists()) {
                if (!logFile.createNewFile()) error("Unable to create auto update log file");
            }
            shrinkLogFile(logFile, 2097152, 1000);
        } catch (IOException e) {
            error(e.getMessage(), e);
        }
        return logFile;
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

    private static void shrinkLogFile(File logFile, int maxFileSizeInBytes, int nbLinesToKeep) throws IOException {
        if(logFile.length() > maxFileSizeInBytes){
            String line;
            BufferedReader br = new BufferedReader(new FileReader(logFile));
            List<String> tmp = new ArrayList<>();
            do {
                line = br.readLine();
                tmp.add(line);
            } while (line != null);

            if (tmp.get(tmp.size()-1) == null) tmp.remove(tmp.size()-1);

            RandomAccessFile raf = new RandomAccessFile(logFile, "rw");
            raf.setLength(0);
            FileOutputStream fos = new FileOutputStream(logFile);
            for(int i=((tmp.size()-1)-nbLinesToKeep);i<=tmp.size()-1;i++) {
                fos.write(String.format("%s%s", tmp.get(i), System.lineSeparator()).getBytes());
            }
            fos.flush();
            fos.close();
        }
    }
}