package com.fusionjack.adhell3.utils;

import android.os.Handler;
import android.os.Message;

import androidx.documentfile.provider.DocumentFile;

import com.fusionjack.adhell3.App;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public final class LogUtils {

    private static final String STORAGE_FOLDERS = "Adhell3/Logs";
    private static final String ERROR_LOG_BASE_FILENAME = "adhell_logcat_%s.txt";
    private static final String AUTO_UPDATE_LOG_FILENAME = "adhell_auto_update_log.txt";

    public static String createLogcat() {
        String filename = String.format(ERROR_LOG_BASE_FILENAME, System.currentTimeMillis());
        DocumentFile logFile = FileUtils.getDocumentFile(STORAGE_FOLDERS, filename, FileUtils.FileCreationType.IF_NOT_EXIST);
        try {
            Process logcatProcess = Runtime.getRuntime().exec( "logcat -d");
            InputStream in = logcatProcess.getInputStream();
            OutputStream out = App.getAppContext().getContentResolver().openOutputStream(logFile.getUri());
            if (out != null) {
                int n;
                byte[] buffer = new byte[16384];
                while((n = in.read(buffer)) > -1) {
                    out.write(buffer, 0, n);
                }
                out.close();
            }
        } catch (Exception e) {
            error(e.getMessage(), e);
            return "";
        }
        return logFile.getName();
    }

    public static void appendLogFile(String newLine, DocumentFile logFile) {
        try {
            OutputStream out = App.getAppContext().getContentResolver().openOutputStream(logFile.getUri(), "wa");
            if (out != null) {
                out.write(String.format("%s%s", newLine, System.lineSeparator()).getBytes());
                out.close();
            }
        } catch (IOException e) {
            error(e.getMessage(), e);
        }
    }

    public static DocumentFile getAutoUpdateLogFile() {
        DocumentFile logFile = FileUtils.getDocumentFile(STORAGE_FOLDERS, AUTO_UPDATE_LOG_FILENAME, FileUtils.FileCreationType.IF_NOT_EXIST);
        try {
            shrinkLogFile(logFile, 1572864, 1000);
        } catch (IOException e) {
            error(e.getMessage(), e);
        }
        return logFile;
    }

    public static void info(String text) {
        Timber.i(text);
    }

    public static void info(String text, Handler handler) {
        if (handler != null) {
            Message message = handler.obtainMessage(0, text);
            message.sendToTarget();
        }
        Timber.i(text);
    }

    public static void warning(String text, Throwable e) {
        Timber.w(e, text);
    }

    public static void error(String text) {
        Timber.e(text);
    }

    public static void error(String text, Throwable e) {
        Timber.e(e, text);
    }

    public static void error(String text, Throwable e, Handler handler) {
        if (handler != null) {
            Message message = handler.obtainMessage(0, text);
            message.sendToTarget();
        }
        Timber.e(e, text);
    }

    private static void shrinkLogFile(DocumentFile logFile, @SuppressWarnings("SameParameterValue") int maxFileSizeInBytes, @SuppressWarnings("SameParameterValue") int nbLinesToKeep) throws IOException {
        if(logFile.length() > maxFileSizeInBytes) {
            InputStream input = App.getAppContext().getContentResolver().openInputStream(logFile.getUri());
            if (input != null) {
                BufferedReader br = new BufferedReader(new InputStreamReader(input));
                List<String> tmp = new ArrayList<>();
                String line;
                do {
                    line = br.readLine();
                    if (line != null) {
                        if (tmp.size() >= nbLinesToKeep) {
                            tmp.remove(0);
                        }
                        tmp.add(line);
                    }
                } while (line != null);
                input.close();

                LogUtils.info("log - tmp size: "+tmp.size());

                OutputStream out = App.getAppContext().getContentResolver().openOutputStream(logFile.getUri(), "wrt");
                if (out != null) {
                    for (String tmpLine : tmp) {
                        out.write(String.format("%s%s", tmpLine, System.lineSeparator()).getBytes());
                    }
                    out.flush();
                    out.close();
                }
            }
        }
    }
}

