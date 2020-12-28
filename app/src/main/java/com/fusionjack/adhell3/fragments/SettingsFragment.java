package com.fusionjack.adhell3.fragments;

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
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.blocker.ContentBlocker;
import com.fusionjack.adhell3.blocker.ContentBlocker56;
import com.fusionjack.adhell3.databinding.DialogQuestionBinding;
import com.fusionjack.adhell3.databinding.DialogSetPasswordBinding;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.DatabaseFactory;
import com.fusionjack.adhell3.dialogfragment.ActivationDialogFragment;
import com.fusionjack.adhell3.dialogfragment.AutoUpdateDialogFragment;
import com.fusionjack.adhell3.dialogfragment.FirewallDialogFragment;
import com.fusionjack.adhell3.model.CustomSwitchPreference;
import com.fusionjack.adhell3.tasks.BackupDatabaseAsyncTask;
import com.fusionjack.adhell3.tasks.CleanDBUpdateWorker;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppCache;
import com.fusionjack.adhell3.utils.AppDatabaseFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.DialogUtils;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.utils.PasswordStorage;
import com.google.android.material.snackbar.Snackbar;

import java.lang.ref.WeakReference;
import java.util.Locale;

public class SettingsFragment extends PreferenceFragmentCompat {
    public static final String ALLOW_QSTILES_LOCKSCREEN = "allow_qstiles_lockscreen";
    public static final String SET_NIGHT_MODE_PREFERENCE = "set_night_mode_preference";
    public static final String UPDATE_PROVIDERS_PREFERENCE = "update_provider_preference";
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

        CustomSwitchPreference switchPreference = findPreference("auto_update_preference");
        if (switchPreference != null) {
            switchPreference.setOnPreferenceClickListener(switchPref -> {
                AutoUpdateDialogFragment autoUpdateDialogFragment = new AutoUpdateDialogFragment(switchPref);
                autoUpdateDialogFragment.setCancelable(true);
                autoUpdateDialogFragment.show(getChildFragmentManager(), "dialog_auto_update");
                return false;
            });
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        switch (preference.getKey()) {
            case DELETE_PREFERENCE: {
                DialogQuestionBinding dialogQuestionBinding = DialogQuestionBinding.inflate(LayoutInflater.from(getContext()));
                dialogQuestionBinding.titleTextView.setText(R.string.delete_app_dialog_title);
                dialogQuestionBinding.questionTextView.setText(R.string.delete_app_dialog_text);

                AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                        .setView(dialogQuestionBinding.getRoot())
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) ->
                                AdhellFactory.uninstall(context, this))
                        .setNegativeButton(android.R.string.no, null)
                        .create();

                alertDialog.show();
                break;
            }
            case BACKUP_PREFERENCE: {
                DialogQuestionBinding dialogQuestionBinding = DialogQuestionBinding.inflate(LayoutInflater.from(getContext()));
                dialogQuestionBinding.titleTextView.setText(R.string.backup_database_dialog_title);
                dialogQuestionBinding.questionTextView.setText(R.string.backup_database_dialog_text);

                AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                        .setView(dialogQuestionBinding.getRoot())
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) ->
                                new BackupDatabaseAsyncTask(context).execute()
                        )
                        .setNegativeButton(android.R.string.no, null)
                        .create();

                alertDialog.show();
                break;
            }
            case RESTORE_PREFERENCE: {
                DialogQuestionBinding dialogQuestionBinding = DialogQuestionBinding.inflate(LayoutInflater.from(getContext()));
                dialogQuestionBinding.titleTextView.setText(R.string.restore_database_dialog_title);
                dialogQuestionBinding.questionTextView.setText(R.string.restore_database_dialog_text);

                AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                        .setView(dialogQuestionBinding.getRoot())
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) ->
                                new RestoreDatabaseAsyncTask(context).execute()
                        )
                        .setNegativeButton(android.R.string.no, null)
                        .create();

                alertDialog.show();
                break;
            }
            case CLEAN_PREFERENCE: {
                DialogQuestionBinding dialogQuestionBinding = DialogQuestionBinding.inflate(LayoutInflater.from(getContext()));
                dialogQuestionBinding.titleTextView.setText(R.string.clean_database_dialog_title);
                dialogQuestionBinding.questionTextView.setText(R.string.clean_database_dialog_text);

                AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                        .setView(dialogQuestionBinding.getRoot())
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) ->
                                new CleanDatabaseAsyncTask(context, getChildFragmentManager()).execute()
                        )
                        .setNegativeButton(android.R.string.no, null)
                        .create();

                alertDialog.show();
                break;
            }
            case SET_PASSWORD_PREFERENCE: {
                PreferenceManager preferenceManager = getPreferenceManager();
                int themeColor = this.getResources().getColor(R.color.colorBottomNavUnselected, this.requireActivity().getTheme());
                if (preferenceManager.getSharedPreferences().getBoolean(SET_PASSWORD_PREFERENCE, false)) {
                    DialogSetPasswordBinding dialogSetPasswordBinding = DialogSetPasswordBinding.inflate(LayoutInflater.from(getContext()));
                    AlertDialog passwordDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                            .setView(dialogSetPasswordBinding.getRoot())
                            .setPositiveButton(android.R.string.yes, null)
                            .setNegativeButton(android.R.string.no, null)
                            .create();

                    dialogSetPasswordBinding.passwordIcon.setColorFilter(themeColor, PorterDuff.Mode.SRC_IN);
                    passwordDialog.setOnShowListener(dialogInterface -> {
                        Button positiveButton = passwordDialog.getButton(AlertDialog.BUTTON_POSITIVE);
                        positiveButton.setOnClickListener(view -> {
                            String password = (dialogSetPasswordBinding.passwordEditText.getText() != null) ? dialogSetPasswordBinding.passwordEditText.getText().toString() : "";
                            String passwordConfirm = (dialogSetPasswordBinding.passwordConfirmEditText.getText() != null) ? dialogSetPasswordBinding.passwordConfirmEditText.getText().toString() : "";
                            if (!password.isEmpty()) {
                                if (password.equals(passwordConfirm)) {
                                    try {
                                        AppPreferences.getInstance().setPassword(password);
                                        passwordDialog.dismiss();
                                    } catch (PasswordStorage.CannotPerformOperationException e) {
                                        e.printStackTrace();
                                    }
                                } else {
                                    dialogSetPasswordBinding.infoTextView.setText(R.string.dialog_mismatch_password);
                                }
                            } else {
                                dialogSetPasswordBinding.infoTextView.setText(R.string.dialog_empty_password);
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
                if (getActivity() instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    mainActivity.makeSnackbar(getString(R.string.restore_warning_success), Snackbar.LENGTH_LONG)
                            .show();
                }
                break;
            }
            case REVOKE_STORAGE_PERMISSION: {
                DialogQuestionBinding dialogQuestionBinding = DialogQuestionBinding.inflate(LayoutInflater.from(getContext()));
                dialogQuestionBinding.titleTextView.setText(R.string.revoke_storage_permission_title);
                dialogQuestionBinding.questionTextView.setText(R.string.revoke_storage_permission_question);

                AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                        .setView(dialogQuestionBinding.getRoot())
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                            int intentFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION;
                            Uri treePath = Uri.parse(AppPreferences.getInstance().getStorageTreePath());
                            context.getContentResolver().releasePersistableUriPermission(treePath, intentFlags);
                            context.revokeUriPermission(treePath, intentFlags);
                            AppPreferences.getInstance().setStorageTreePath("");
                            if (getActivity() instanceof MainActivity) {
                                MainActivity mainActivity = (MainActivity) getActivity();
                                mainActivity.makeSnackbar(getString(R.string.revoke_storage_permission_success), Snackbar.LENGTH_LONG)
                                        .show();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .create();

                alertDialog.show();
                break;
            }
            case SET_NIGHT_MODE_PREFERENCE: {
                PreferenceManager preferenceManager = getPreferenceManager();
                int nightMode = (preferenceManager.getSharedPreferences().getBoolean(SET_NIGHT_MODE_PREFERENCE, false)) ?
                        AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO;

                MainActivity.themeChanged = 2;
                AppCompatDelegate.setDefaultNightMode(nightMode);
                break;
            }
            case CREATE_LOGCAT_PREFERENCE: {
                String filename = LogUtils.createLogcat();
                if (filename.isEmpty()) {
                    if (getActivity() instanceof MainActivity) {
                        MainActivity mainActivity = (MainActivity) getActivity();
                        mainActivity.makeSnackbar(getString(R.string.logcat_not_created), Snackbar.LENGTH_LONG)
                                .show();
                    }
                } else {
                    if (getActivity() instanceof MainActivity) {
                        MainActivity mainActivity = (MainActivity) getActivity();
                        mainActivity.makeSnackbar(String.format(Locale.getDefault(), getString(R.string.logcat_created), filename), Snackbar.LENGTH_LONG)
                                .show();
                    }
                }
                break;
            }
            case CHANGE_KEY_PREFERENCE: {
                FragmentManager fragmentManager = getChildFragmentManager();
                Fragment activationDialog = fragmentManager.findFragmentByTag(ActivationDialogFragment.DIALOG_TAG);
                if (activationDialog == null) {
                    ActivationDialogFragment fragment = new ActivationDialogFragment();
                    fragment.show(fragmentManager, ActivationDialogFragment.DIALOG_TAG);
                }
                break;
            }

            case ABOUT_PREFERENCE: {
                DialogQuestionBinding dialogQuestionBinding = DialogQuestionBinding.inflate(LayoutInflater.from(getContext()));
                dialogQuestionBinding.titleTextView.setText(R.string.about_title);
                dialogQuestionBinding.questionTextView.setText(R.string.about_content);
                dialogQuestionBinding.questionTextView.setMovementMethod(LinkMovementMethod.getInstance());
                AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                        .setView(dialogQuestionBinding.getRoot())
                        .setPositiveButton(android.R.string.yes, null)
                        .create();

                alertDialog.show();
                break;
            }
        }
        return super.onPreferenceTreeClick(preference);
    }

    private static class RestoreDatabaseAsyncTask extends AsyncTask<Void, String, String> {
        private final WeakReference<Context> contextWeakReference;
        private final AlertDialog dialog;
        private AlertDialog.Builder builder;

        RestoreDatabaseAsyncTask(Context context) {
            this.builder = new AlertDialog.Builder(context, R.style.AlertDialogStyle);
            this.dialog = DialogUtils.getProgressDialog("Restore database is running...", context);
            this.dialog.setCancelable(false);
            this.contextWeakReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            dialog.show();
        }

        @Override
        protected String doInBackground(Void... args) {
            try {
                ContentBlocker contentBlocker = ContentBlocker56.getInstance();
                contentBlocker.disableDomainRules();
                contentBlocker.disableFirewallRules();
                AdhellFactory.getInstance().setAppDisablerToggle(false);
                AdhellFactory.getInstance().setAppComponentToggle(false);
                AppDatabaseFactory.resetInstalledApps().subscribe();
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
                builder.setMessage("Restore database is finished. Turn on Adhell.\n\nIf needed, re-add local host sources file since permissions cannot be restored.");
                builder.setTitle("Info");
            } else {
                builder.setMessage(message);
                builder.setTitle("Error");
            }
            AlertDialog dialog = builder.create();

            dialog.show();

            // Clean resource to prevent memory leak
            this.builder = null;
        }
    }

    private static class CleanDatabaseAsyncTask extends AsyncTask<Void, Void, Void> {
        private final WeakReference<Context> contextReference;
        private final FragmentManager fragmentManager;
        private Handler handler;
        private FirewallDialogFragment fragment;
        private AppCache appCache;
        private AppDatabase appDatabase;

        CleanDatabaseAsyncTask(Context context, FragmentManager fragmentManager) {
            this.fragmentManager = fragmentManager;
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
            appCache = AppCache.getInstance(null);
            appDatabase = AppDatabase.getAppDatabase(contextReference.get());
        }

        @Override
        protected Void doInBackground(Void... args) {
            CleanDBUpdateWorker.cleanDatabase(appDatabase, appCache, handler);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            fragment.enableCloseButton();

            // Clean resource to prevent memory leak
            this.handler = null;
            this.fragment = null;
            this.appCache = null;
            this.appDatabase = null;
        }
    }
}
