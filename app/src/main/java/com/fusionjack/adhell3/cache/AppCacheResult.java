package com.fusionjack.adhell3.cache;

import java.util.HashMap;
import java.util.Map;

public class AppCacheResult {

    private final Map<String, AppCacheInfo> map;

    public AppCacheResult() {
        this.map = new HashMap<>();
    }

    public void addAppCacheInfo(String packageName, AppCacheInfo appCacheInfo) {
        map.put(packageName, appCacheInfo);
    }

    public void merge(AppCacheResult chunkResult) {
        this.map.putAll(chunkResult.map);
    }

    AppCacheInfo getAppCacheInfo(String packageName) {
        return map.getOrDefault(packageName, AppCacheInfo.EMPTY);
    }

}
