package com.fusionjack.adhell3.utils;

import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import com.fusionjack.adhell3.App;

public class FileUtils {

    public static DocumentFile getDocumentFile(String filePath, String fileName, FileCreationType fileCreationType) {
        String[] storageFolders = filePath.split("/");
        DocumentFile parentDirectory = DocumentFile.fromTreeUri(App.getAppContext(), Uri.parse(AppPreferences.getInstance().getStorageTreePath()));
        String documentFileMimeType = "text/plain";
        DocumentFile file = null;

        for(String folder : storageFolders)
        {
            DocumentFile newDir;
            if (parentDirectory != null) {
                newDir = parentDirectory.findFile(folder);

                if (newDir == null || !newDir.exists()) {
                    newDir = parentDirectory.createDirectory(folder);
                }
                parentDirectory = newDir;
            }
        }
        if (parentDirectory != null) {
            file = parentDirectory.findFile(fileName);

            if (file == null || !file.exists() || fileCreationType == FileUtils.FileCreationType.ALWAYS) {
                if (fileCreationType != FileUtils.FileCreationType.NEVER) file = parentDirectory.createFile(documentFileMimeType, fileName);
            }
        }

        return file;
    }

    public enum FileCreationType {
        NEVER,
        IF_NOT_EXIST,
        ALWAYS
    }
}
