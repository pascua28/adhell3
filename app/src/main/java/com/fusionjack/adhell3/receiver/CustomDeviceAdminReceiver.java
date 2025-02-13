package com.fusionjack.adhell3.receiver;

import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PersistableBundle;

import com.fusionjack.adhell3.utils.LogUtils;

public class CustomDeviceAdminReceiver extends DeviceAdminReceiver {

    @Override
    public void onEnabled(Context context, Intent intent) {
        LogUtils.info("Admin is activated");
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        LogUtils.info("Admin is not activated");
    }

    @Override
    public void onTransferOwnershipComplete (Context context, PersistableBundle bundle) {
        LogUtils.info("Transfer ownership complete");
    }
}
