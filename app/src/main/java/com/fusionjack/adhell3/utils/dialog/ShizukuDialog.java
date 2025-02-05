package com.fusionjack.adhell3.utils.dialog;

import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.IBinder;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.IShizukuService;
import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.receiver.CustomDeviceAdminReceiver;
import com.fusionjack.adhell3.service.ShizukuService;
import com.fusionjack.adhell3.utils.DeviceAdminInteractor;
import com.fusionjack.adhell3.utils.LogUtils;

import rikka.shizuku.Shizuku;


public class ShizukuDialog {
    protected View view;

    private AlertDialog dialog;

    private IShizukuService service = null;

    public void show() {
        if (dialog != null && !dialog.isShowing()) {
            dialog.show();
        }
    }

    public ShizukuDialog(View view, Runnable onSuccessListener) {
        if (view == null || view.getContext() == null) {
            return;
        }

        this.view = view;
        Context context = view.getContext();

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_shizuku, (ViewGroup) view, false);

        dialog = new AlertDialog.Builder(context, R.style.DialogStyle)
                .setView(dialogView)
                .setPositiveButton(R.string.grant, null)
                .setCancelable(true)
                .create();

        dialog.setOnDismissListener(d -> {
            Shizuku.removeRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);
            try {
                // Requires Shizuku Permission
                Shizuku.unbindUserService(serviceArgs, connection, true);
            } catch (Exception e) {
                LogUtils.error("Failed to unbind ShizukuService", e);
            }


            DeviceAdminInteractor dai = DeviceAdminInteractor.getInstance();
            if (dai.isDeviceOwner()) {
                onSuccessListener.run();
            }

            //When Device Owner granting fails admin is revoked
            //Looking for information I found this android source code commit that should fix the issue: https://cs.android.com/android/_/android/platform/frameworks/base/+/1165c2d1b2d1ded57021fa2dc729895ddbd96db3
            //The commit should be included in android 14 however I can still reproduce the issue, which might indicate that something else is going on
            //This check prevents unexpected errors
            if (!dai.isAdminActive()) {
                LogUtils.info( "Admin is not active, showing activation dialog");
                Runnable requestDeviceAdminAction = () -> DeviceAdminInteractor.getInstance().forceEnableAdmin(new MainActivity());
                DeviceAdminDialog.getInstance(view, requestDeviceAdminAction).show();
            }
        });

        dialog.setOnShowListener(d -> {
            TextView summaryTextView = dialog.findViewById(R.id.infoTextView);
            summaryTextView.setMovementMethod(LinkMovementMethod.getInstance());

            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setEnabled(true);

            positiveButton.setOnClickListener(v -> {
                setText("");
                positiveButton.setText(R.string.granting);
                positiveButton.setEnabled(false);
                positiveButton.setTextColor(Color.GRAY);

                boolean granted;
                try {
                    granted = checkPermission();
                } catch (Exception e) {
                    LogUtils.error("Shizuku checkPermission Exception", e);
                    setText(e.toString());
                    Toast.makeText(view.getContext(), "Error during Device Owner granting", Toast.LENGTH_LONG).show();
                    return;
                }

                if (!granted) {
                    LogUtils.info("Requesting for Shizuku Permission...");
                    Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);
                    Shizuku.requestPermission(1);
                } else {
                    LogUtils.info("Shizuku Permission already granted");
                    onRequestPermissionsResult(1, PackageManager.PERMISSION_GRANTED);
                }
            });
        });
    }

    private boolean checkPermission() throws Exception {
        if (Shizuku.isPreV11() || Shizuku.getVersion() < 11) {
            throw new Exception("Requires Shizuku v11 or newer");
        } else {
            return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void onRequestPermissionsResult(int requestCode, int grantResult) {
        boolean granted = grantResult == PackageManager.PERMISSION_GRANTED;
        if (granted) {
            LogUtils.info("Shizuku Permission granted");
            if (service == null) {
                LogUtils.info("Binding ShizukuService...");
                Shizuku.bindUserService(serviceArgs, connection);
            } else {
                LogUtils.info("ShizukuService already bound, activating...");
                activate();
            }
        } else {
            LogUtils.info("Shizuku Permission denied");
            setText("Shizuku Permission denied");
            if (view != null) {
                Toast.makeText(view.getContext(), "Error during Device Owner granting", Toast.LENGTH_LONG).show();
            }
        }
    }
    private final Shizuku.OnRequestPermissionResultListener REQUEST_PERMISSION_RESULT_LISTENER = this::onRequestPermissionsResult;

    private final Shizuku.UserServiceArgs serviceArgs = new Shizuku.UserServiceArgs(
            new ComponentName(BuildConfig.APPLICATION_ID, ShizukuService.class.getName()))
            .processNameSuffix("shizuku_service")
            .debuggable(BuildConfig.DEBUG)
            .daemon(false)
            .version(1);

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            LogUtils.info("ShizukuService received ServiceConnection");
            if (binder != null && binder.pingBinder()) {
                service = IShizukuService.Stub.asInterface(binder);
                activate();
            } else {
                LogUtils.error("ShizukuService received invalid binder for " + componentName);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            LogUtils.info("ShizukuService received ServiceDisconnect");
            service = null;
        }
    };

    private void activate() {
        String command = "dpm set-device-owner " + BuildConfig.APPLICATION_ID + "/" + CustomDeviceAdminReceiver.class.getCanonicalName();
        String output;
        try {
            output = service.execute(command);
        } catch (Exception e) {
            LogUtils.error("ShizukuService Exception", e);
            output = e.toString();
        }

        if (output.startsWith("Error")) {
            LogUtils.error("ShizukuService Error: " + output);
        } else {
            LogUtils.info("ShizukuService Output: " + output);
        }

        if (view != null & dialog != null) {
            setText(output);

            DeviceAdminInteractor dai = DeviceAdminInteractor.getInstance();
            if (dai.isDeviceOwner()) {
                Toast.makeText(view.getContext(), "Successfully granted Device Owner", Toast.LENGTH_LONG).show();
                dialog.dismiss();
            } else {
                Toast.makeText(view.getContext(), "Error during Device Owner granting", Toast.LENGTH_LONG).show();
            }
        } else {
            LogUtils.error("Shizuku dialog or view is null. Command output:\n" + output);
        }
    }

    private void setText(String text) {
        if (dialog != null) {
            TextView summaryTextView = dialog.findViewById(R.id.infoTextView);
            if (summaryTextView != null) {
                summaryTextView.setText(text);
            } else {
                LogUtils.error("ShizukuDialog tried to set text when summaryTextView is null");
            }
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setText(R.string.grant);
            positiveButton.setEnabled(true);
            positiveButton.setTextColor(ContextCompat.getColor(dialog.getContext(), R.color.colorAccent));
        } else {
            LogUtils.error("ShizukuDialog tried to set text when dialog is null");
        }
    }
}
