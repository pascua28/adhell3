package com.fusionjack.adhell3.utils;

import android.os.Environment;

import java.io.File;

public final class FileUtils {
    private static final String ADHELL3_FOLDER = "adhell3";

    private FileUtils() {
    }

    public static File toFile(String fileName) {
        File folder = new File(Environment.getExternalStorageDirectory(), ADHELL3_FOLDER);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return new File(folder, fileName);
    }

}
