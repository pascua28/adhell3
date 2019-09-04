package com.fusionjack.adhell3.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import androidx.annotation.NonNull;
import android.webkit.URLUtil;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.BlockUrl;
import com.fusionjack.adhell3.db.entity.BlockUrlProvider;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

public class BlockUrlUtils {

    // Pattern to detect lines that do not start with a word or wildcard
    private static final Pattern linePattern = Pattern.compile("(?im)^(?![a-z0-9*]|\\|{2}).+$");

    // Pattern to detect 'deadzone' - We only want the domain
    private static final Pattern deadZonePattern = Pattern.compile("(?im)^(?:0|127)\\.0\\.0\\.[0-1]\\s+");

    // Pattern to detect comments
    private static final Pattern commentPattern = Pattern.compile("(?im)(?:^|[^\\S\\n]+)#.*$");

    // Pattern to detect empty lines
    private static final Pattern emptyLinePattern = Pattern.compile("(?im)^\\s*");

    @NonNull
    public static List<BlockUrl> loadBlockUrls(BlockUrlProvider blockUrlProvider) throws IOException, URISyntaxException {
        Context context = App.getAppContext();
        Date start = new Date();

        // Read the host source and convert it to string
        String hostFileStr = "";
        if (URLUtil.isHttpUrl(blockUrlProvider.url) || URLUtil.isHttpsUrl(blockUrlProvider.url)) {
            URL urlProviderUrl = new URL(blockUrlProvider.url);
            URLConnection connection = urlProviderUrl.openConnection();
            try (final Reader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8)) {
                BufferedReader r = new BufferedReader(reader);
                StringBuilder hostFileStringBuilder = new StringBuilder();
                for (String line; (line = r.readLine()) != null; ) {
                    hostFileStringBuilder.append(line).append('\n');
                }
                hostFileStr = hostFileStringBuilder.toString();
            }
        } else if (URLUtil.isContentUrl(blockUrlProvider.url)) {
            Uri contentUri = Uri.parse(blockUrlProvider.url);
            context.getContentResolver().takePersistableUriPermission(contentUri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            InputStream inputStream = context.getContentResolver().openInputStream(contentUri);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            StringBuilder contentBuilder = new StringBuilder();
            try {
                BufferedReader in = new BufferedReader(inputStreamReader);
                String str;
                while ((str = in.readLine()) != null) {
                    contentBuilder.append(str).append('\n');
                }
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            hostFileStr = contentBuilder.toString();
        } else if (URLUtil.isFileUrl(blockUrlProvider.url)) {
            File file = new File(new URI(blockUrlProvider.url));
            FileInputStream fileInputStream = new FileInputStream(file);
            StringBuilder contentBuilder = new StringBuilder();
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(fileInputStream));
                String str;
                while ((str = in.readLine()) != null) {
                    contentBuilder.append(str).append('\n');
                }
                in.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            hostFileStr = contentBuilder.toString();
        }

        // If we received any host file data
        if (!hostFileStr.isEmpty()) {
            // Clean up the host string
            hostFileStr = linePattern.matcher(hostFileStr).replaceAll("");
            hostFileStr = deadZonePattern.matcher(hostFileStr).replaceAll("");
            hostFileStr = commentPattern.matcher(hostFileStr).replaceAll("");
            hostFileStr = emptyLinePattern.matcher(hostFileStr).replaceAll("");
            hostFileStr = hostFileStr.toLowerCase();

            // Fetch valid domains
            List<BlockUrl> blockUrls = BlockUrlPatternsMatch.validHostFileDomains(hostFileStr, blockUrlProvider.id);

            Date end = new Date();
            LogUtils.info("Domain processing duration: " + (end.getTime() - start.getTime()) + " ms");

            return blockUrls;
        }

        return new ArrayList<>();
    }

    public static List<String> getUserBlockedUrls(AppDatabase appDatabase, boolean enableLog, Handler handler) {
        List<String> list = new ArrayList<>();
        int userBlockUrlCount = 0;
        List<String> urls = appDatabase.userBlockUrlDao().getAll3();
        for (String url : urls) {
            if (url.indexOf('|') == -1) {
                list.add(url);
                if (enableLog) {
                    LogUtils.info("Domain: " + url, handler);
                }
                userBlockUrlCount++;
            }
        }
        if (enableLog) {
            LogUtils.info("Size: " + userBlockUrlCount, handler);
        }
        return list;
    }

    public static int getAllBlockedUrlsCount(AppDatabase appDatabase) {
        return appDatabase.blockUrlProviderDao().getUniqueBlockedUrlsCount();
    }

    public static List<String> getAllBlockedUrls(AppDatabase appDatabase) {
        return appDatabase.blockUrlProviderDao().getUniqueBlockedUrls();
    }

    public static List<String> getBlockedUrls(long providerId, AppDatabase appDatabase) {
        return appDatabase.blockUrlDao().getUrlsByProviderId(providerId);
    }

    public static List<String> getFilteredBlockedUrls(String filterText, AppDatabase appDatabase) {
        List<String> result = new ArrayList<>();
        List<BlockUrlProvider> blockUrlProviders = appDatabase.blockUrlProviderDao().getBlockUrlProviderBySelectedFlag(1);
        for (BlockUrlProvider blockUrlProvider : blockUrlProviders) {
            List<BlockUrl> blockUrls = appDatabase.blockUrlDao().getByUrl(blockUrlProvider.id, filterText);
            for (BlockUrl blockUrl : blockUrls) {
                result.add(blockUrl.url);
            }
        }
        return result;
    }

    public static List<String> getFilteredBlockedUrls(String filterText, long providerId, AppDatabase appDatabase) {
        List<String> result = new ArrayList<>();
        List<BlockUrl> blockUrls = appDatabase.blockUrlDao().getByUrl(providerId, filterText);
        for (BlockUrl blockUrl : blockUrls) {
            result.add(blockUrl.url);
        }
        return result;
    }

    public static boolean isDomainLimitAboveDefault() {
        int defaultDomainLimit = 15000;
        int domainLimit = AdhellAppIntegrity.BLOCK_URL_LIMIT;
        return domainLimit > defaultDomainLimit;
    }

}