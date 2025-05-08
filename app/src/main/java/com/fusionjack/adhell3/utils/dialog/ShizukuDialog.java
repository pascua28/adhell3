package com.fusionjack.adhell3.utils.dialog;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.IBinder;
import android.provider.Settings;
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rikka.shizuku.Shizuku;


public class ShizukuDialog {
    protected View view;

    private AlertDialog dialog;
    private Button neutralButton;
    private boolean areRequirementsMet = false;

    private Runnable ShizukuAction = null;

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
                .setNeutralButton(R.string.check_do_requirements, null)
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
                Runnable requestDeviceAdminAction = () -> DeviceAdminInteractor.getInstance().forceEnableAdmin(MainActivity.getInstance());
                DeviceAdminDialog.getInstance(view, requestDeviceAdminAction).show();
            }
        });

        dialog.setOnShowListener(d -> {
            TextView summaryTextView = dialog.findViewById(R.id.infoTextView);
            summaryTextView.setMovementMethod(LinkMovementMethod.getInstance());

            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setEnabled(true);

            positiveButton.setOnClickListener(v -> {
                Runnable onPositiveButton = () -> {
                    setText("");
                    setAccountButtonVisibility(false);
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

                    ShizukuAction = ShizukuDialog.this::activate;
                    if (!granted) {
                        LogUtils.info("Requesting for Shizuku Permission...");
                        Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);
                        Shizuku.requestPermission(1);
                    } else {
                        LogUtils.info("Shizuku Permission already granted");
                        onRequestPermissionsResult(1, PackageManager.PERMISSION_GRANTED);
                    }
                };

                if (!areRequirementsMet) {
                    new QuestionDialogBuilder(view)
                            .setTitle(R.string.do_requirements_dialog_title)
                            .setQuestion(R.string.do_requirements_dialog_summary)
                            .show(onPositiveButton);
                } else {
                    onPositiveButton.run();
                }
            });

            neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            neutralButton.setEnabled(true);
            neutralButton.setOnClickListener(v -> {
                setText("");
                neutralButton.setText(R.string.checking_do_requirements);
                neutralButton.setEnabled(false);
                neutralButton.setTextColor(Color.GRAY);

                boolean granted;
                try {
                    granted = checkPermission();
                } catch (Exception e) {
                    LogUtils.error("Shizuku checkPermission Exception", e);
                    setText(e.toString());
                    Toast.makeText(view.getContext(), "Error during Device Owner granting", Toast.LENGTH_LONG).show();
                    return;
                }

                ShizukuAction = this::checkRequirements;
                if (!granted) {
                    LogUtils.info("Requesting for Shizuku Permission...");
                    Shizuku.addRequestPermissionResultListener(REQUEST_PERMISSION_RESULT_LISTENER);
                    Shizuku.requestPermission(1);
                } else {
                    LogUtils.info("Shizuku Permission already granted");
                    onRequestPermissionsResult(1, PackageManager.PERMISSION_GRANTED);
                }
            });

            Button accountButton = dialog.findViewById(R.id.openAccountsButton);
            accountButton.setOnClickListener(v -> {
                Intent intent = new Intent(Settings.ACTION_SYNC_SETTINGS);
                view.getContext().startActivity(intent);
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
                LogUtils.info("ShizukuService already bound");
                if (ShizukuAction != null) {
                    ShizukuAction.run();
                    ShizukuAction = null;
                } else {
                    LogUtils.error("ShizukuAction is null");
                }
            }
        } else {
            LogUtils.info("Shizuku Permission denied");
            setText("Shizuku Permission denied");
            if (view != null) {
                Toast.makeText(view.getContext(), "Error: Shizuku Permission denied", Toast.LENGTH_LONG).show();
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
                if (ShizukuAction != null) {
                    ShizukuAction.run();
                    ShizukuAction = null;
                } else {
                    LogUtils.error("ShizukuAction is null");
                }
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

        Pattern pattern = Pattern.compile("(Error \\(([0-9]{0,3})\\) \\n)((.+?(?=:):)?java\\.lang\\.IllegalStateException: )(.+?(?=\\t))(\\tat .*)*");
        Matcher matcher = pattern.matcher(output);

        if (matcher.matches()) {
            output = matcher.group(5);
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

    private void checkRequirements() {
        LogUtils.info("Checking Device Owner requirements...");
        areRequirementsMet = true;

        String commandAccounts = "dumpsys account";
        StringBuilder accounts = new StringBuilder();
        try {
            String accountsRaw = service.execute(commandAccounts);
            Pattern pattern = Pattern.compile("(User UserInfo\\{(.+?(?=\\}))\\}: {2}Accounts: [0-9]+(( {4}Account \\{name=(.+?(?=,)), type=(.+?(?=\\}))\\})*)(.*)??)+");
            Matcher matcher = pattern.matcher(accountsRaw);
            int accountCount = 0;
            while (matcher.find()) {
                Pattern accountPattern = Pattern.compile("Account \\{name=(.+?(?=,)), type=(.+?(?=\\}))\\}");
                Matcher accountMatcher = accountPattern.matcher(matcher.group());
                while (accountMatcher.find()) {
                    accountCount++;
                    String accountName = accountMatcher.group(1);
                    String accountType = accountMatcher.group(2);
                    accounts.append("Name: ").append(accountName).append(" App: ").append(accountType).append("\n");
                }
            }

            if (accountCount == 0) {
                accounts.append("No accounts found. (Check passed)\n");
            } else {
                accounts.append("Accounts found! (Check failed)\n");
                areRequirementsMet = false;
            }
        } catch (Exception e) {
            LogUtils.error("ShizukuService Exception", e);
            accounts = new StringBuilder(e.toString());
            areRequirementsMet = false;
        }

        LogUtils.info( "Accounts:\n" + accounts);

        String commandUsers = "pm list users";
        StringBuilder users = new StringBuilder();
        try {
            String usersRaw = service.execute(commandUsers);

            Pattern userPattern = Pattern.compile("UserInfo\\{(.+?(?=\\}))\\}");
            Matcher userMatcher = userPattern.matcher(usersRaw);
            int userCount = 0;
            while (userMatcher.find()) {
                userCount++;
                users.append("User ").append(userCount).append(": ").append(userMatcher.group(1)).append("\n");
            }

            if (userCount == 1) {
                users.append("Only main profile found. (Check passed)\n");
            } else {
                users.append("Multiple profiles found! (Check failed)\n");
                areRequirementsMet = false;
            }
        } catch (Exception e) {
            LogUtils.error("ShizukuService Exception", e);
            users = new StringBuilder(e.toString());
            areRequirementsMet = false;
        }
        LogUtils.info("Profiles:\n" + users);

        String commandDeviceOwner = "dpm list-owners";
        String deviceOwner;
        try {
            deviceOwner = service.execute(commandDeviceOwner);
            if (deviceOwner.equals("no owners")) {
                deviceOwner += " (Check passed)";
            } else {
                deviceOwner += "\nOwners found! (Check failed)";
                areRequirementsMet = false;
            }
        } catch (Exception e) {
            LogUtils.error("ShizukuService Exception", e);
            deviceOwner = e.toString();
            areRequirementsMet = false;
        }
        LogUtils.info("Owners:\n" + deviceOwner);

        String output = "Accounts:\n" + accounts + "\nProfiles:\n" + users + "\nOwners:\n" + deviceOwner;

        setText(output);
        setAccountButtonVisibility(!areRequirementsMet);
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
            Button neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);
            neutralButton.setText(R.string.check_do_requirements);
            neutralButton.setEnabled(true);
            neutralButton.setTextColor(ContextCompat.getColor(dialog.getContext(), R.color.colorAccent));
        } else {
            LogUtils.error("ShizukuDialog tried to set text when dialog is null");
        }
    }

    private void setAccountButtonVisibility(boolean visibility) {
        if (dialog != null) {
            Button accountButton = dialog.findViewById(R.id.openAccountsButton);
            if (accountButton != null) {
                accountButton.setVisibility(visibility ? View.VISIBLE : View.GONE);
            } else {
                LogUtils.error("ShizukuDialog tried to set button visibility when accountButton is null");
            }
        } else {
            LogUtils.error("ShizukuDialog tried to set button visibility when dialog is null");
        }
    }
}
