package com.fusionjack.adhell3.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.utils.rx.RxCompletableIoBuilder;
import com.samsung.android.knox.license.KnoxEnterpriseLicenseManager;

import java.util.function.Consumer;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.functions.Action;

public class LicenseHandler {

    private static LicenseHandler instance;

    private final BroadcastReceiver receiver;

    private Runnable onSuccessActivation;
    private Runnable onSuccessDeactivation;
    private Consumer<String> onError;

    private LicenseHandler() {
        this.receiver = createReceiver();
    }

    public synchronized static LicenseHandler getInstance() {
        if (instance == null) {
            instance = new LicenseHandler();
        }
        return instance;
    }

    public void activeOrDeactivateLicense(String key, Runnable onSuccessActivation, Runnable onSuccessDeactivation, Consumer<String> onError) {
        this.onSuccessActivation = onSuccessActivation;
        this.onSuccessDeactivation = onSuccessDeactivation;
        this.onError = onError;

        Context context = App.get().getApplicationContext();
        boolean knoxEnabled = DeviceAdminInteractor.getInstance().isKnoxEnabled(context);
        LogUtils.info("Knox is " + (knoxEnabled ? "enabled" : "disabled"));

        Action action = () -> {
            if (knoxEnabled) {
                DeviceAdminInteractor.getInstance().deactivateKnoxKey(key, context);
            } else {
                DeviceAdminInteractor.getInstance().activateKnoxKey(key, context);
            }
        };

        Runnable onSubscribe = () -> {
            LogUtils.info("Registering receiver ...");
            IntentFilter filter = new IntentFilter();
            filter.addAction(KnoxEnterpriseLicenseManager.ACTION_LICENSE_STATUS);
            App.get().getApplicationContext().registerReceiver(receiver, filter);
        };

        new RxCompletableIoBuilder().async(Completable.fromAction(action), onSubscribe, () -> {}, () -> {});
    }

    private BroadcastReceiver createReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                LogUtils.info("BroadcastReceiver - Intent action name: " + action);

                if (KnoxEnterpriseLicenseManager.ACTION_LICENSE_STATUS.equals(action)) {
                    int errorCode = intent.getIntExtra(KnoxEnterpriseLicenseManager.EXTRA_LICENSE_ERROR_CODE, -1);
                    LogUtils.info("KnoxEnterpriseLicenseManager - Error code: " + errorCode);
                    if (errorCode == KnoxEnterpriseLicenseManager.ERROR_NONE) {
                        handleResult(intent);
                    } else {
                        handleError(intent, errorCode);
                    }
                }

                try {
                    LogUtils.info("Unregistering receiver ...");
                    App.get().getApplicationContext().unregisterReceiver(receiver);
                } catch (Exception ignored) {
                }
            }
        };
    }

    private void handleResult(Intent intent) {
        int result_type = intent.getIntExtra(KnoxEnterpriseLicenseManager.EXTRA_LICENSE_RESULT_TYPE, -1);
        if (result_type != -1) {
            if (result_type == KnoxEnterpriseLicenseManager.LICENSE_RESULT_TYPE_ACTIVATION) {
                LogUtils.info("License activated");
                onSuccessActivation.run();
            } else if (result_type == KnoxEnterpriseLicenseManager.LICENSE_RESULT_TYPE_DEACTIVATION) {
                LogUtils.info("License deactivated");
                onSuccessDeactivation.run();
            }
        }
    }

    private void handleError(Intent intent, int errorCode) {
        if (intent != null) {
            String status = intent.getStringExtra(KnoxEnterpriseLicenseManager.EXTRA_LICENSE_STATUS);
            String message = "Status: " + status + ". Error code: " + errorCode;
            LogUtils.error(message);
            onError.accept(message);
        }
    }

}
