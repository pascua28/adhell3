package com.fusionjack.adhell3.utils;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;

import androidx.documentfile.provider.DocumentFile;

import com.fusionjack.adhell3.App;

import java.io.File;

public final class PathUtils {

    private PathUtils() {
    }

    public static String getPath(Uri uri) {
        if (!DocumentFileUtils.hasAccess(uri)) {
            return "No access - " + uri.getPath();
        }
        if (DocumentsContract.isDocumentUri(App.get().getApplicationContext(), uri)) {
            if (isExternalStorageDocument(uri)) {
                return getPathFromFileProvider(uri);
            } else if (isDownloadsDocument(uri)) {
                return getPathFromDownloadProvider(uri);
            } else {
                return getPathFromUnknownProvider(uri);
            }
        }
        return uri.getPath();
    }

    private static String getPathFromUnknownProvider(Uri uri) {
        String fileName = DocumentFile.fromSingleUri(App.get().getApplicationContext(), uri).getName();
        String cloudProvider = uri.getAuthority();
        return cloudProvider + "/" + fileName;
    }

    private static String getPathFromDownloadProvider(Uri uri) {
        Context context = App.get().getApplicationContext();
        try (Cursor cursor = context.getContentResolver().query(uri, new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                String fileName = cursor.getString(0);
                String path = Environment.getExternalStorageDirectory().toString() + "/Download/" + fileName;
                if (!TextUtils.isEmpty(path)) {
                    return path;
                }
            }
        }
        String id = DocumentsContract.getDocumentId(uri);
        if (!TextUtils.isEmpty(id)) {
            if (id.startsWith("raw:")) {
                return id.replaceFirst("raw:", "");
            }
            String[] contentUriPrefixesToTry = new String[]{
                    "content://downloads/public_downloads",
                    "content://downloads/my_downloads"
            };
            for (String contentUriPrefix : contentUriPrefixesToTry) {
                try {
                    Uri contentUri = ContentUris.withAppendedId(Uri.parse(contentUriPrefix), Long.parseLong(id));
                    return getDataColumn(context, contentUri, null, null);
                } catch (NumberFormatException e) {
                    //In Android 8 and Android P the id is not a number
                    return uri.getPath().replaceFirst("^/document/raw:", "").replaceFirst("^raw:", "");
                }
            }
        }
        return null;
    }

    private static String getPathFromFileProvider(Uri uri) {
        String docId = DocumentsContract.getDocumentId(uri);
        String[] pathData = docId.split(":");
        String type = pathData[0];
        String relativePath = "/" + pathData[1];
        String fullPath;

        if ("primary".equalsIgnoreCase(type)) {
            fullPath = Environment.getExternalStorageDirectory() + relativePath;
            if (fileExists(fullPath)) {
                return fullPath;
            }
        }

        fullPath = System.getenv("SECONDARY_STORAGE") + relativePath;
        if (fileExists(fullPath)) {
            return fullPath;
        }

        fullPath = System.getenv("EXTERNAL_STORAGE") + relativePath;
        if (fileExists(fullPath)) {
            return fullPath;
        }

        return fullPath;
    }

    private static boolean fileExists(String filePath) {
        File file = new File(filePath);
        return file.exists();
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    private static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
        String column = "_data";
        String[] projection = {column};
        try (Cursor cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        }
        return null;
    }

}
