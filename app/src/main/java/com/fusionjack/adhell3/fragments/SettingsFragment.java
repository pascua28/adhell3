package com.fusionjack.adhell3.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.DatabaseFactory;
import com.fusionjack.adhell3.db.entity.AppPermission;
import com.fusionjack.adhell3.db.entity.DisabledPackage;
import com.fusionjack.adhell3.db.entity.DnsPackage;
import com.fusionjack.adhell3.db.entity.FirewallWhitelistedPackage;
import com.fusionjack.adhell3.db.entity.RestrictedPackage;
import com.fusionjack.adhell3.dialogfragment.ActivationDialogFragment;
import com.fusionjack.adhell3.dialogfragment.AutoUpdateDialogFragment;
import com.fusionjack.adhell3.dialogfragment.FirewallDialogFragment;
import com.fusionjack.adhell3.model.CustomSwitchPreference;
import com.fusionjack.adhell3.tasks.BackupDatabaseAsyncTask;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppCache;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.utils.PasswordStorage;
import com.google.android.material.textfield.TextInputEditText;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Objects;

public class SettingsFragment extends PreferenceFragmentCompat {
    public static final String ALLOW_QSTILES_LOCKSCREEN = "allow_qstiles_lockscreen";
    public static final String SET_NIGHT_MODE_PREFERENCE = "set_night_mode_preference";
    static final String UPDATE_PROVIDERS_PREFERENCE = "update_provider_preference";
    private static final String SET_PASSWORD_PREFERENCE = "set_password_preference";
    private static final String CREATE_LOGCAT_PREFERENCE = "create_logcat_preference";
    private static final String CHANGE_KEY_PREFERENCE = "change_key_preference";
    private static final String ABOUT_PREFERENCE = "about_preference";
    private static final String DELETE_PREFERENCE = "delete_preference";
    private static final String BACKUP_PREFERENCE = "backup_preference";
    private static final String RESTORE_PREFERENCE = "restore_preference";
    private static final String RESTORE_WARNING_PREFERENCE = "restore_warning_dialog";
    private static final String CLEAN_PREFERENCE = "clean_preference";
    private static final String REVOKE_STORAGE_PERMISSION = "revoke_storage_permission";
    private Context context;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_preference, rootKey);
        this.context = getContext();

        CustomSwitchPreference switchPreference = (CustomSwitchPreference) findPreference("auto_update_preference");
        switchPreference.setOnPreferenceClickListener(switchPref -> {
            AutoUpdateDialogFragment autoUpdateDialogFragment = new AutoUpdateDialogFragment(switchPref);
            autoUpdateDialogFragment.setCancelable(true);
            if (getActivity() != null)
                autoUpdateDialogFragment.show(getActivity().getSupportFragmentManager(), "dialog_auto_update");
            return false;
        });
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        switch (preference.getKey()) {
            case DELETE_PREFERENCE: {
                View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_question, (ViewGroup) getView(), false);
                TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
                titleTextView.setText(R.string.delete_app_dialog_title);
                TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
                questionTextView.setText(R.string.delete_app_dialog_text);

                new AlertDialog.Builder(context, R.style.ThemeOverlay_AlertDialog)
                        .setView(dialogView)
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) ->
                                AdhellFactory.uninstall(context, this))
                        .setNegativeButton(android.R.string.no, null).show();
                break;
            }
            case BACKUP_PREFERENCE: {
                View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_question, (ViewGroup) getView(), false);
                TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
                titleTextView.setText(R.string.backup_database_dialog_title);
                TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
                questionTextView.setText(R.string.backup_database_dialog_text);

                new AlertDialog.Builder(context, R.style.ThemeOverlay_AlertDialog)
                        .setView(dialogView)
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) ->
                                new BackupDatabaseAsyncTask(getActivity()).execute()
                        )
                        .setNegativeButton(android.R.string.no, null).show();
                break;
            }
            case RESTORE_PREFERENCE: {
                View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_question, (ViewGroup) getView(), false);
                TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
                titleTextView.setText(R.string.restore_database_dialog_title);
                TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
                questionTextView.setText(R.string.restore_database_dialog_text);

                new AlertDialog.Builder(context, R.style.ThemeOverlay_AlertDialog)
                        .setView(dialogView)
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) ->
                                new RestoreDatabaseAsyncTask(getActivity(), getContext()).execute()
                        )
                        .setNegativeButton(android.R.string.no, null)
                        .show();
                break;
            }
            case CLEAN_PREFERENCE: {
                View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_question, (ViewGroup) getView(), false);
                TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
                titleTextView.setText(R.string.clean_database_dialog_title);
                TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
                questionTextView.setText(R.string.clean_database_dialog_text);

                new AlertDialog.Builder(context, R.style.ThemeOverlay_AlertDialog)
                        .setView(dialogView)
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) ->
                                new CleanDatabaseAsyncTask(getActivity(), getContext()).execute()
                        )
                        .setNegativeButton(android.R.string.no, null)
                        .show();
                break;
            }
            case SET_PASSWORD_PREFERENCE: {
                PreferenceManager preferenceManager = getPreferenceManager();
                int themeColor = this.getResources().getColor(R.color.colorBottomNavUnselected, Objects.requireNonNull(this.getActivity()).getTheme());
                if (preferenceManager.getSharedPreferences().getBoolean(SET_PASSWORD_PREFERENCE, false)) {
                    View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_set_password, (ViewGroup) getView(), false);
                    AlertDialog passwordDialog = new AlertDialog.Builder(context, R.style.ThemeOverlay_AlertDialog)
                            .setView(dialogView)
                            .setPositiveButton(android.R.string.yes, null)
                            .setNegativeButton(android.R.string.no, null)
                            .create();

                    ImageView icon = dialogView.findViewById(R.id.passwordIcon);
                    icon.setColorFilter(themeColor, PorterDuff.Mode.SRC_IN);
                    passwordDialog.setOnShowListener(dialogInterface -> {
                        Button positiveButton = passwordDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                        positiveButton.setOnClickListener(view -> {
                            TextView infoTextView = dialogView.findViewById(R.id.infoTextView);
                            TextInputEditText passwordEditText = dialogView.findViewById(R.id.passwordEditText);
                            TextInputEditText passwordConfirmEditText = dialogView.findViewById(R.id.passwordConfirmEditText);
                            String password = (passwordEditText.getText() != null) ? passwordEditText.getText().toString() : "";
                            String passwordConfirm = (passwordConfirmEditText.getText() != null) ? passwordConfirmEditText.getText().toString() : "";
                            if (!password.isEmpty()) {
                                if (password.equals(passwordConfirm)) {
                                    try {
                                        AppPreferences.getInstance().setPassword(password);
                                        passwordDialog.dismiss();
                                    } catch (PasswordStorage.CannotPerformOperationException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    infoTextView.setText(R.string.dialog_mismatch_password);
                                }
                            } else {
                                infoTextView.setText(R.string.dialog_empty_password);
                            }
                        });

                        Button negativeButton = passwordDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
                        negativeButton.setOnClickListener(view -> {
                            ((SwitchPreference) preference).setChecked(false);
                            passwordDialog.dismiss();
                        });
                    });
                    passwordDialog.setCancelable(false);
                    passwordDialog.show();
                } else {
                    AppPreferences.getInstance().resetPassword();
                }
                break;
            }
            case RESTORE_WARNING_PREFERENCE: {
                AppPreferences.getInstance().setWarningDialogAppComponentDontShow(false);
                Toast.makeText(context, getString(R.string.restore_warning_success), Toast.LENGTH_LONG).show();
                break;
            }
            case REVOKE_STORAGE_PERMISSION: {
                View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_question, (ViewGroup) getView(), false);
                TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
                titleTextView.setText(R.string.revoke_storage_permission_title);
                TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
                questionTextView.setText(R.string.revoke_storage_permission_question);

                new AlertDialog.Builder(context, R.style.ThemeOverlay_AlertDialog)
                        .setView(dialogView)
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                            int intentFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                            Uri treePath = Uri.parse(AppPreferences.getInstance().getStorageTreePath());
                            context.getContentResolver().releasePersistableUriPermission(treePath, intentFlags);
                            context.revokeUriPermission(treePath, intentFlags);
                            AppPreferences.getInstance().setStorageTreePath("");
                            Toast.makeText(context, getString(R.string.revoke_storage_permission_success), Toast.LENGTH_LONG).show();
                        })
                        .setNegativeButton(android.R.string.no, null).show();
                break;
            }
            case SET_NIGHT_MODE_PREFERENCE: {
                PreferenceManager preferenceManager = getPreferenceManager();
                int nightMode = (preferenceManager.getSharedPreferences().getBoolean(SET_NIGHT_MODE_PREFERENCE, false)) ?
                        AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;

                AppCompatDelegate.setDefaultNightMode(nightMode);
                Intent intent = new Intent(getActivity(), MainActivity.class);
                intent.putExtra("settingsFragment", SET_NIGHT_MODE_PREFERENCE);
                startActivity(intent);
                Objects.requireNonNull(getActivity()).finish();
                break;
            }
            case CREATE_LOGCAT_PREFERENCE: {
                String filename = LogUtils.createLogcat();
                if (filename.isEmpty()) {
                    Toast.makeText(context, R.string.logcat_not_created, Toast.LENGTH_LONG).show();
                } else {
                    String message = context.getResources().getString(R.string.logcat_created);
                    Toast.makeText(context, String.format(message, filename), Toast.LENGTH_LONG).show();
                }
                break;
            }
            case CHANGE_KEY_PREFERENCE: {
                FragmentManager fragmentManager = Objects.requireNonNull(getActivity()).getSupportFragmentManager();
                Fragment activationDialog = fragmentManager.findFragmentByTag(ActivationDialogFragment.DIALOG_TAG);
                if (activationDialog == null) {
                    ActivationDialogFragment fragment = new ActivationDialogFragment();
                    fragment.show(fragmentManager, ActivationDialogFragment.DIALOG_TAG);
                }
                break;
            }

            case ABOUT_PREFERENCE: {
                View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_question, (ViewGroup) getView(), false);
                TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
                titleTextView.setText(R.string.about_title);
                TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
                questionTextView.setText(R.string.about_content);
                questionTextView.setMovementMethod(LinkMovementMethod.getInstance());
                new AlertDialog.Builder(context, R.style.ThemeOverlay_AlertDialog)
                        .setView(dialogView)
                        .setPositiveButton(android.R.string.yes, null).show();
                break;
            }
        }
        return super.onPreferenceTreeClick(preference);
    }

    private static class RestoreDatabaseAsyncTask extends AsyncTask<Void, String, String> {
        private final AlertDialog dialog;
        private final AlertDialog.Builder builder;
        private final WeakReference<Context> contextWeakReference;

        RestoreDatabaseAsyncTask(Activity activity, Context context) {
            this.builder = new AlertDialog.Builder(activity, R.style.ThemeOverlay_AlertDialog);
            this.dialog = LogUtils.getProgressDialog("Restore database is running...", context);
            this.contextWeakReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            dialog.show();
        }

        @Override
        protected String doInBackground(Void... args) {
            try {
                DatabaseFactory.getInstance().restoreDatabase();

                Context context = contextWeakReference.get();
                if (context != null) {
                    if (AdhellFactory.getInstance().hasInternetAccess(context)) {
                        publishProgress("Updating all providers...");
                        AdhellFactory.getInstance().updateAllProviders();
                    }
                }

                return null;
            } catch (Exception e) {
                return e.getMessage();
            }
        }

        @Override
        protected void onProgressUpdate(String... values) {
            dialog.setMessage(values[0]);
        }

        @Override
        protected void onPostExecute(String message) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }

            if (message == null) {
                builder.setMessage("Restore database is finished. Turn on Adhell.");
                builder.setTitle("Info");
            } else {
                builder.setMessage(message);
                builder.setTitle("Error");
            }
            builder.create().show();
        }
    }

    private static class CleanDatabaseAsyncTask extends AsyncTask<Void, Void, Void> {
        private final FragmentManager fragmentManager;
        private final Handler handler;
        private final WeakReference<Context> contextReference;
        private FirewallDialogFragment fragment;
        private AppCache appCache;
        private AppDatabase appDatabase;

        CleanDatabaseAsyncTask(Activity activity, Context context) {
            FragmentActivity fragmentActivity = (FragmentActivity) activity;
            this.fragmentManager = fragmentActivity.getSupportFragmentManager();
            this.contextReference = new WeakReference<>(context);
            this.handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    fragment.appendText(msg.obj.toString());
                }
            };
        }

        @Override
        protected void onPreExecute() {
            fragment = FirewallDialogFragment.newInstance("Cleaning Database");
            fragment.setCancelable(false);
            fragment.show(fragmentManager, "dialog_clean_db");
            appCache = AppCache.getInstance(contextReference.get(), handler);
            appDatabase = AppDatabase.getAppDatabase(contextReference.get());
        }

        @Override
        protected Void doInBackground(Void... args) {
            int count = 0;

            // Disabled packages rules
            LogUtils.info("Cleaning disabled packages rules...", handler);
            List<DisabledPackage> disabledPackages = appDatabase.disabledPackageDao().getAll();
            for (DisabledPackage disabledPackage: disabledPackages) {
                if (!appCache.getNames().containsKey(disabledPackage.packageName)) {
                    try {
                        LogUtils.info(String.format("    Deleting rule for package: %s.", disabledPackage.packageName), handler);
                        appDatabase.disabledPackageDao().deleteByPackageName(disabledPackage.packageName);
                        LogUtils.info("    Done.", handler);
                    } catch (Exception e) {
                        LogUtils.error("    Error deleting rule.", e, handler);
                    }
                    count++;
                }
            }
            if (count > 0)
                LogUtils.info("  Done.", handler);
            else
                LogUtils.info("  Nothing to clean up.", handler);

            // Restricted packages rules
            count = 0;
            LogUtils.info("Cleaning restricted packages rules...", handler);
            List<RestrictedPackage> restrictedPackages = appDatabase.restrictedPackageDao().getAll();
            for (RestrictedPackage restrictedPackage: restrictedPackages) {
                if (!appCache.getNames().containsKey(restrictedPackage.packageName)) {
                    try {
                        LogUtils.info(String.format("    Deleting rule for package: %s.", restrictedPackage.packageName), handler);
                        appDatabase.restrictedPackageDao().deleteByPackageName(restrictedPackage.packageName, restrictedPackage.type);
                        LogUtils.info("    Done.", handler);
                    } catch (Exception e) {
                        LogUtils.error("    Error deleting rule.", e, handler);
                    }
                    count++;
                }
            }
            if (count > 0)
                LogUtils.info("  Done.", handler);
            else
                LogUtils.info("  Nothing to clean up.", handler);

            // Firewall whitelisted packages rules
            count = 0;
            LogUtils.info("Cleaning firewall whitelisted packages rules...", handler);
            List<FirewallWhitelistedPackage> whitelistedPackages = appDatabase.firewallWhitelistedPackageDao().getAll();
            for (FirewallWhitelistedPackage whitelistedPackage: whitelistedPackages) {
                if (!appCache.getNames().containsKey(whitelistedPackage.packageName)) {
                    try {
                        LogUtils.info(String.format("    Deleting rule for package: %s.", whitelistedPackage.packageName), handler);
                        appDatabase.firewallWhitelistedPackageDao().deleteByPackageName(whitelistedPackage.packageName);
                        LogUtils.info("    Done.", handler);
                    } catch (Exception e) {
                        LogUtils.error("    Error deleting rule.", e, handler);
                    }
                    count++;
                }
            }
            if (count > 0)
                LogUtils.info("  Done.", handler);
            else
                LogUtils.info("  Nothing to clean up.", handler);

            // DNS packages rules
            count = 0;
            LogUtils.info("Cleaning DNS packages rules...", handler);
            List<DnsPackage> dnsPackages = appDatabase.dnsPackageDao().getAll();
            for (DnsPackage dnsPackage: dnsPackages) {
                if (!appCache.getNames().containsKey(dnsPackage.packageName)) {
                    try {
                        LogUtils.info(String.format("    Deleting rule for package: %s.", dnsPackage.packageName), handler);
                        appDatabase.dnsPackageDao().deleteByPackageName(dnsPackage.packageName);
                        LogUtils.info("    Done.", handler);
                    } catch (Exception e) {
                        LogUtils.error("    Error deleting rule.", e, handler);
                    }
                    count++;
                }
            }
            if (count > 0)
                LogUtils.info("  Done.", handler);
            else
                LogUtils.info("  Nothing to clean up.", handler);

            // App component restriction packages rules
            count = 0;
            LogUtils.info("Cleaning app component restriction packages rules...", handler);
            List<AppPermission> appPermissionsPackages = appDatabase.appPermissionDao().getAll();
            for (AppPermission appPermissionsPackage : appPermissionsPackages) {
                if (!appCache.getNames().containsKey(appPermissionsPackage.packageName)) {
                    try {
                        LogUtils.info(String.format("    Deleting rules for package: %s.", appPermissionsPackage.packageName), handler);
                        appDatabase.appPermissionDao().deletePermissions(appPermissionsPackage.packageName);
                        appDatabase.appPermissionDao().deleteServices(appPermissionsPackage.packageName);
                        appDatabase.appPermissionDao().deleteReceivers(appPermissionsPackage.packageName);
                        LogUtils.info("    Done.", handler);
                    } catch (Exception e) {
                        LogUtils.error("    Error deleting rule.", e, handler);
                    }
                    count++;
                }
            }
            if (count > 0)
                LogUtils.info("  Done.", handler);
            else
                LogUtils.info("  Nothing to clean up.", handler);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            fragment.enableCloseButton();
        }
    }
}
