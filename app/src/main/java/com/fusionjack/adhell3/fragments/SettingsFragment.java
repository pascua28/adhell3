package com.fusionjack.adhell3.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.utils.dialog.DeviceAdminDialog;
import com.fusionjack.adhell3.utils.dialog.ShizukuDialog;
import com.fusionjack.adhell3.tasks.BackupDatabaseRxTask;
import com.fusionjack.adhell3.tasks.RestoreDatabaseRxTask;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.DeviceAdminInteractor;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.utils.dialog.AboutDialog;
import com.fusionjack.adhell3.utils.dialog.LicenseDialog;
import com.fusionjack.adhell3.utils.dialog.QuestionDialogBuilder;
import com.fusionjack.adhell3.utils.dialog.SetPasswordDialog;
import com.fusionjack.adhell3.utils.rx.RxSingleIoBuilder;

import java.util.function.Consumer;

import io.reactivex.rxjava3.core.Single;

public class SettingsFragment extends PreferenceFragmentCompat {
    private Context context;

    private static final String DELETE_PREFERENCE = "delete_preference";
    private static final String GRANT_DO_PREFERENCE = "grant_do_preference";
    private static final String CLEAR_DO_PREFERENCE = "clear_do_preference";
    private static final String DO_FIX_PREFERENCE = "do_fix_preference";
    private static final String BACKUP_PREFERENCE = "backup_preference";
    private static final String RESTORE_PREFERENCE = "restore_preference";
    public static final String UPDATE_PROVIDERS_PREFERENCE = "update_provider_preference";
    public static final String SET_PASSWORD_PREFERENCE = "set_password_preference";
    public static final String SET_NIGHT_MODE_PREFERENCE = "set_night_mode_preference";
    public static final String CREATE_LOGCAT_PREFERENCE = "create_logcat_preference";
    public static final String CHANGE_KEY_PREFERENCE = "change_key_preference";
    public static final String ABOUT_PREFERENCE = "about_preference";

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_preference, rootKey);
        this.context = getContext();
    }

    private void hidePreferences() {
        DeviceAdminInteractor dao = DeviceAdminInteractor.getInstance();
        PreferenceManager pm = getPreferenceManager();

        Preference delete = pm.findPreference("delete_preference");
        Preference grantDO = pm.findPreference("grant_do_preference");
        Preference clearDO = pm.findPreference("clear_do_preference");
        Preference doFix = pm.findPreference("do_fix_preference");
        if (delete != null && grantDO != null && clearDO != null) {
            if (dao.isDeviceOwner()) {
                delete.setVisible(false);
                grantDO.setVisible(false);
                clearDO.setVisible(true);
            } else {
                delete.setVisible(true);
                grantDO.setVisible(true);
                clearDO.setVisible(false);
            }
        }
        if (doFix != null) {
            if (!dao.isDeviceOwner()) {
                doFix.setVisible(false);
            } else {
                try {
                    doFix.setVisible(!dao.isBackupServiceEnabled(context));
                } catch (Exception e) {
                    doFix.setVisible(false);
                }
            }
        }
    }

    @NonNull
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        hidePreferences();
        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        LicenseDialog.destroy();
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        switch (preference.getKey()) {
            case DELETE_PREFERENCE: {
                DeviceAdminInteractor dai = DeviceAdminInteractor.getInstance();
                if (dai.isDeviceOwner()) {
                    Toast.makeText(context, "You need to clear Device Owner first", Toast.LENGTH_LONG).show();
                } else {
                    new QuestionDialogBuilder(getView())
                            .setTitle(R.string.delete_app_dialog_title)
                            .setQuestion(R.string.delete_app_dialog_text)
                            .show(() -> {
                                AdhellFactory.uninstall((Activity) context);
                            });
                }
                break;
            }
            case GRANT_DO_PREFERENCE: {
                DeviceAdminInteractor dai = DeviceAdminInteractor.getInstance();
                if (dai.isDeviceOwner()) {
                    Toast.makeText(context, "Device Owner already granted", Toast.LENGTH_LONG).show();
                } else {
                    new ShizukuDialog(getView(), this::hidePreferences).show();
                }
                break;
            }
            case CLEAR_DO_PREFERENCE: {
                DeviceAdminInteractor dai = DeviceAdminInteractor.getInstance();
                if (dai.isDeviceOwner()) {
                    new QuestionDialogBuilder(getView())
                            .setTitle(R.string.clear_do_dialog_title)
                            .setQuestion(R.string.clear_do_dialog_text)
                            .show(() -> {
                                boolean success = dai.disableDeviceOwner();
                                if (success) {
                                    Toast.makeText(context, "Successfully cleared Device Owner", Toast.LENGTH_LONG).show();
                                    hidePreferences();
                                    //After clearing DO admin is also revoked
                                    //This check prevents unexpected errors (crash after trying to uninstall app from adhell
                                    if (!dai.isAdminActive()) {
                                        LogUtils.info( "Admin is not active, showing activation dialog");
                                        Runnable requestDeviceAdminAction = () -> DeviceAdminInteractor.getInstance().forceEnableAdmin(MainActivity.getInstance());
                                        DeviceAdminDialog.getInstance(getView(), requestDeviceAdminAction).show();
                                    }
                                } else {
                                    Toast.makeText(context, "Error clearing Device Owner", Toast.LENGTH_LONG).show();
                                }
                            });
                } else {
                    Toast.makeText(context, "Device Owner is not granted", Toast.LENGTH_LONG).show();
                }
                break;
            }
            case DO_FIX_PREFERENCE: {
                DeviceAdminInteractor dai = DeviceAdminInteractor.getInstance();
                if (dai.isDeviceOwner()) {
                    new QuestionDialogBuilder(getView())
                            .setTitle(R.string.do_fix_dialog_title)
                            .setQuestion(R.string.do_fix_dialog_text)
                            .show(() -> {
                                if (dai.deviceOwnerFixes(context)) {
                                    Toast.makeText(context, "Successfully applied fixes", Toast.LENGTH_LONG).show();
                                    hidePreferences();
                                } else {
                                    Toast.makeText(context, "Requires Android 8 (API 26) or Up", Toast.LENGTH_LONG).show();
                                }
                            });
                } else {
                    Toast.makeText(context, "Device Owner is not granted", Toast.LENGTH_LONG).show();
                }
                break;
            }
            case BACKUP_PREFERENCE: {
                new QuestionDialogBuilder(getView())
                        .setTitle(R.string.backup_database_dialog_title)
                        .setQuestion(R.string.backup_database_dialog_text)
                        .show(() -> new BackupDatabaseRxTask(context).run());
                break;
            }
            case RESTORE_PREFERENCE: {
                new QuestionDialogBuilder(getView())
                        .setTitle(R.string.restore_database_dialog_title)
                        .setQuestion(R.string.restore_database_dialog_text)
                        .show(() -> new RestoreDatabaseRxTask(context).run());
                break;
            }
            case SET_PASSWORD_PREFERENCE: {
                PreferenceManager preferenceManager = getPreferenceManager();
                if (preferenceManager.getSharedPreferences().getBoolean(SET_PASSWORD_PREFERENCE, false)) {
                    SetPasswordDialog.show(getView(), () -> ((SwitchPreference) preference).setChecked(false));
                } else {
                    AppPreferences.getInstance().resetPassword();
                }
                break;
            }
            case SET_NIGHT_MODE_PREFERENCE: {
                PreferenceManager preferenceManager = getPreferenceManager();
                if (preferenceManager.getSharedPreferences().getBoolean(SET_NIGHT_MODE_PREFERENCE, false)) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    intent.putExtra("settingsFragment", SET_NIGHT_MODE_PREFERENCE);
                    startActivity(intent);
                    getActivity().finish();
                }
                else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    intent.putExtra("settingsFragment", SET_NIGHT_MODE_PREFERENCE);
                    startActivity(intent);
                    getActivity().finish();
                }
                break;
            }
            case CREATE_LOGCAT_PREFERENCE: {
                Consumer<String> callback = filename -> {
                    String message = getResources().getString(R.string.logcat_created);
                    Toast.makeText(context, String.format(message, filename), Toast.LENGTH_LONG).show();
                };
                new RxSingleIoBuilder()
                        .setShowErrorAlert(context)
                        .async(Single.fromCallable(LogUtils::createLogcat), callback);
                break;
            }
            case CHANGE_KEY_PREFERENCE: {
                SharedPreferences sharedPreferences = ((Activity) context).getPreferences(Context.MODE_PRIVATE);
                LicenseDialog.getChangeInstance(getView(), sharedPreferences).show();
                break;
            }

            case ABOUT_PREFERENCE: {
                AboutDialog.show(getView());
                break;
            }
        }
        return super.onPreferenceTreeClick(preference);
    }
}
