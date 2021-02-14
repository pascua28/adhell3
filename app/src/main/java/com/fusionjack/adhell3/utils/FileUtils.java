package com.fusionjack.adhell3.utils;

import android.os.Environment;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class FileUtils {
    private static final String ADHELL3_FOLDER = "adhell3";

    private FileUtils() {
    }

    public static List<File> getHostsFiles() {
        File folder = getAdhell3Folder();
        return Optional.ofNullable(folder.listFiles())
                .map(arr -> Arrays.stream(arr)
                        .filter(file -> file.getName().startsWith("hosts"))
                        .sorted()
                        .limit(15)
                        .collect(Collectors.toList()))
                .orElse(Collections.emptyList());
    }

    public static File toFile(String fileName) {
        File folder = getAdhell3Folder();
        return new File(folder, fileName);
    }

    private static File getAdhell3Folder() {
        File folder = new File(Environment.getExternalStorageDirectory(), ADHELL3_FOLDER);
        if (!folder.exists()) {
            folder.mkdirs();
        }
        return folder;
    }

}
