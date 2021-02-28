package com.fusionjack.adhell3.utils;

import android.content.UriPermission;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import com.fusionjack.adhell3.App;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

public final class DocumentFileUtils {

    private DocumentFileUtils() {
    }

    public static String readFileUri(String uriStr) throws IOException {
        StringBuilder sb = new StringBuilder();
        Uri uri = Uri.parse(uriStr);
        if (hasAccess(uri)) {
            DocumentFile file = DocumentFile.fromSingleUri(App.get().getApplicationContext(), uri);
            if (file != null) {
                Optional<InputStream> in = getInputStreamFrom(file);
                if (in.isPresent()) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(in.get()))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            sb.append(line);
                            sb.append(System.lineSeparator());
                        }
                    }
                }
            }
        }
        return sb.toString();
    }

    public static void copyFile(String inputFileName, String outputFileName) throws Exception {
        DocumentFile file = findFile(inputFileName);
        if (file == null) {
            throw new IOException("Cannot find file '" + inputFileName + "'");
        }
        Optional<InputStream> in = getInputStreamFrom(file);
        if (in.isPresent()) {
            try (DocumentFileWriter writer = DocumentFileWriter.overrideMode(outputFileName);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in.get()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    writer.write(line);
                }
            }
        }
    }

    public static void dumpToFile(InputStream in, String fileName) throws Exception {
        if (in != null) {
            try (DocumentFileWriter writer = DocumentFileWriter.overrideMode(fileName);
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    writer.write(line);
                }
            }
        }
    }

    public static boolean hasAccess(Uri uri) {
        List<UriPermission> list = App.get().getApplicationContext().getContentResolver().getPersistedUriPermissions();
        Predicate<UriPermission> exactMatch = uriPermission -> uriPermission.getUri().equals(uri);
        Predicate<UriPermission> startWith = uriPermission -> {
            int index = uri.getPath().indexOf("tree");
            return index != -1 && uri.getPath().startsWith(uriPermission.getUri().getPath());
        };
        boolean exist = list.stream().anyMatch(exactMatch.or(startWith));
        if (!exist) {
            LogUtils.info("'" + uri.toString() + "' has no access");
        }
        return exist;
    }

    private static DocumentFile getBasedDir() {
        String adhell3Folder = AppPreferences.getInstance().getAdhell3FolderUri();
        if (adhell3Folder != null) {
            Uri uri = Uri.parse(adhell3Folder);
            if (adhell3Folder != null && hasAccess(uri)) {
                return DocumentFile.fromTreeUri(App.get().getApplicationContext(), uri);
            }
        }
        return null;
    }

    private static void validate(DocumentFile basedDir, String fileName) throws IOException {
        if (basedDir == null) {
            AppPreferences.getInstance().setAdhell3FolderUri(null);
            throw new IOException("Directory access might have been revoked. Go to 'Home' tab and set it again.");
        }
        if (fileName == null) {
            throw new IOException("File name cannot be empty!");
        }
    }

    public static DocumentFile findFile(String fileName) throws IOException {
        DocumentFile basedDir = DocumentFileUtils.getBasedDir();
        DocumentFileUtils.validate(basedDir, fileName);
        return basedDir.findFile(fileName);
    }

    public static DocumentFile createFile(String fileName) throws IOException {
        DocumentFile basedDir = DocumentFileUtils.getBasedDir();
        DocumentFileUtils.validate(basedDir, fileName);
        return basedDir.createFile("text/plain", fileName);
    }

    public static Optional<InputStream> getInputStreamFrom(DocumentFile file) throws IOException {
        InputStream in = null;
        if (file != null && hasAccess(file.getUri())) {
            in = App.get().getApplicationContext().getContentResolver().openInputStream(file.getUri());
        }
        return Optional.ofNullable(in);
    }

    public static Optional<OutputStream> getOutputStreamFrom(DocumentFile file, boolean append) throws IOException {
        OutputStream out = null;
        if (file != null && hasAccess(file.getUri())) {
            out = App.get().getApplicationContext().getContentResolver().openOutputStream(file.getUri(), append ? "wa" : "w");
        }
        return Optional.ofNullable(out);
    }

}
