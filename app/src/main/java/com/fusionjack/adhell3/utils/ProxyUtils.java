package com.fusionjack.adhell3.utils;

import android.os.Handler;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import com.fusionjack.adhell3.db.entity.StaticProxy;
import com.fusionjack.adhell3.db.repository.StaticProxyRepository;
import com.samsung.android.knox.net.AuthConfig;
import com.samsung.android.knox.net.GlobalProxy;
import com.samsung.android.knox.net.ProxyProperties;

import java.util.Arrays;
import java.util.List;

public final class ProxyUtils {
    private static ProxyUtils instance;

    private final GlobalProxy globalProxy;
    private ProxyUtils() {
        globalProxy = AdhellFactory.getInstance().getGlobalProxy();
    }

    public static ProxyUtils getInstance() {
        if (instance == null) {
            instance = new ProxyUtils();
        }
        return instance;
    }

    public void setStaticProxy(StaticProxy staticProxy, Handler handler) throws Exception {
        if (globalProxy == null) {
            throw new Exception("Knox GlobalProxy is not initialized");
        }

        ProxyProperties properties = staticProxyToProperties(staticProxy);
        int result = globalProxy.setGlobalProxy(properties);

        if (result == 1) {
            LogUtils.info("Result: Success", handler);
        } else {
            Exception ex = new Exception("Failed to apply proxy");
            LogUtils.error(ex.getMessage(), ex);
            throw ex;
        }
    }

    public void clearProxy(Handler handler) throws Exception {
        if (globalProxy == null) {
            throw new Exception("Knox GlobalProxy is not initialized");
        }

        int result = globalProxy.setGlobalProxy(null);

        if (result == 1) {
            LogUtils.info("Result: Success", handler);
        } else {
            Exception ex = new Exception("Failed to clear proxy");
            LogUtils.error(ex.getMessage(), ex);
            throw ex;
        }
    }

    private ProxyProperties staticProxyToProperties(StaticProxy proxy) {

        List<String> exclusionList = Arrays.asList(proxy.exclusionList.split("\\r?\\n"));
        ProxyProperties staticProxy = new ProxyProperties();
        staticProxy.setHostname(proxy.hostname);
        staticProxy.setPortNumber(proxy.port);
        staticProxy.setExclusionList(exclusionList);

        if (!(proxy.user.isEmpty()) || !(proxy.password.isEmpty())) {
            staticProxy.setAuthConfigList(Arrays.asList(new AuthConfig[] {
                    new AuthConfig(proxy.user, proxy.password)
            }));
        }
        return staticProxy;
    }

    @Nullable
    public LiveData<StaticProxy> getCurrentStaticProxyFromDB() throws Exception {
        if (globalProxy == null) {
            throw new Exception("Knox GlobalProxy is not initialized");
        }

        ProxyProperties current = globalProxy.getGlobalProxy();

        if (current == null) {
            return null;
        }

        String hostname = current.getHostname();
        int port = current.getPortNumber();
        String exclusionList = String.join("\\n", current.getExclusionList());
        String user = "";
        String password = "";
        for (AuthConfig auth: current.getAuthConfigList()) {
            user = auth.getUsername();
            password = auth.getPassword();
        }

        return new StaticProxyRepository().getByProperties(hostname, port, exclusionList, user, password);
    }
}
