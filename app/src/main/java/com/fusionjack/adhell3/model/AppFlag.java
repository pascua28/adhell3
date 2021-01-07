package com.fusionjack.adhell3.model;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.db.repository.AppRepository;

public class AppFlag {

    private final AppRepository.Type type;
    private final int layout;

    private AppFlag(AppRepository.Type type, int layout) {
        this.type = type;
        this.layout = layout;
    }

    public static AppFlag createDisablerFlag() {
        int loadLayout = R.id.installed_apps_list;
        return new AppFlag(AppRepository.Type.DISABLER, loadLayout);
    }

    public static AppFlag createMobileRestrictedFlag() {
        int loadLayout = R.id.mobile_apps_list;
        return new AppFlag(AppRepository.Type.MOBILE_RESTRICTED, loadLayout);
    }

    public static AppFlag createWifiRestrictedFlag() {
        int loadLayout = R.id.wifi_apps_list;
        return new AppFlag(AppRepository.Type.WIFI_RESTRICTED, loadLayout);
    }

    public static AppFlag createWhitelistedFlag() {
        int loadLayout = R.id.whitelisted_apps_list;
        return new AppFlag(AppRepository.Type.WHITELISTED, loadLayout);
    }

    public static AppFlag createComponentFlag() {
        int loadLayout = R.id.component_apps_list;
        return new AppFlag(AppRepository.Type.COMPONENT, loadLayout);
    }

    public static AppFlag createDnsFlag() {
        int loadLayout = R.id.dns_apps_list;
        return new AppFlag(AppRepository.Type.DNS, loadLayout);
    }

    public AppRepository.Type getType() {
        return type;
    }

    public int getLayout() {
        return layout;
    }
}
