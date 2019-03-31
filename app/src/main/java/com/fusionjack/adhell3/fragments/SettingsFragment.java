package com.fusionjack.adhell3.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.PorterDuff;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.TextInputEditText;
import android.support.v14.preference.SwitchPreference;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.blocker.ContentBlocker;
import com.fusionjack.adhell3.blocker.ContentBlocker56;
import com.fusionjack.adhell3.db.DatabaseFactory;
import com.fusionjack.adhell3.dialogfragment.ActivationDialogFragment;
import com.fusionjack.adhell3.receiver.CustomDeviceAdminReceiver;
import com.fusionjack.adhell3.tasks.BackupDatabaseAsyncTask;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.utils.PasswordStorage;

import java.lang.ref.WeakReference;
import java.util.Objects;

public class SettingsFragment extends PreferenceFragmentCompat {
    public static final String UPDATE_PROVIDERS_PREFERENCE = "update_provider_preference";
    private static final String SET_PASSWORD_PREFERENCE = "set_password_preference";
    public static final String SET_NIGHT_MODE_PREFERENCE = "set_night_mode_preference";
    private static final String CREATE_LOGCAT_PREFERENCE = "create_logcat_preference";
    private static final String CHANGE_KEY_PREFERENCE = "change_key_preference";
    private static final String ABOUT_PREFERENCE = "about_preference";
    private static final String DELETE_PREFERENCE = "delete_preference";
    private static final String BACKUP_PREFERENCE = "backup_preference";
    private static final String RESTORE_PREFERENCE = "restore_preference";
    private Context context;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings_preference, rootKey);
        this.context = getContext();
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

                new AlertDialog.Builder(context)
                        .setView(dialogView)
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                            ContentBlocker contentBlocker = ContentBlocker56.getInstance();
                            contentBlocker.disableDomainRules();
                            contentBlocker.disableFirewallRules();
                            ComponentName devAdminReceiver = new ComponentName(context, CustomDeviceAdminReceiver.class);
                            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
                            dpm.removeActiveAdmin(devAdminReceiver);
                            Intent intent = new Intent(Intent.ACTION_DELETE);
                            String packageName = "package:" + BuildConfig.APPLICATION_ID;
                            intent.setData(Uri.parse(packageName));
                            startActivity(intent);
                        })
                        .setNegativeButton(android.R.string.no, null).show();
                break;
            }
            case BACKUP_PREFERENCE: {
                View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_question, (ViewGroup) getView(), false);
                TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
                titleTextView.setText(R.string.backup_database_dialog_title);
                TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
                questionTextView.setText(R.string.backup_database_dialog_text);

                new AlertDialog.Builder(context)
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

                new AlertDialog.Builder(context)
                        .setView(dialogView)
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) ->
                                new RestoreDatabaseAsyncTask(getActivity(), getContext()).execute()
                        )
                        .setNegativeButton(android.R.string.no, null).show();
                break;
            }
            case SET_PASSWORD_PREFERENCE: {
                PreferenceManager preferenceManager = getPreferenceManager();
                int themeColor = this.getResources().getColor(R.color.colorBottomNavUnselected, Objects.requireNonNull(this.getActivity()).getTheme());
                if (preferenceManager.getSharedPreferences().getBoolean(SET_PASSWORD_PREFERENCE, false)) {
                    View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_set_password, (ViewGroup) getView(), false);
                    AlertDialog passwordDialog = new AlertDialog.Builder(context)
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
                            String password = passwordEditText.getText().toString();
                            String passwordConfirm = passwordConfirmEditText.getText().toString();
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
            case SET_NIGHT_MODE_PREFERENCE: {
                PreferenceManager preferenceManager = getPreferenceManager();
                if (preferenceManager.getSharedPreferences().getBoolean(SET_NIGHT_MODE_PREFERENCE, false)) {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    intent.putExtra("settingsFragment", SET_NIGHT_MODE_PREFERENCE);
                    startActivity(intent);
                    Objects.requireNonNull(getActivity()).finish();
                } else {
                    AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    intent.putExtra("settingsFragment", SET_NIGHT_MODE_PREFERENCE);
                    startActivity(intent);
                    Objects.requireNonNull(getActivity()).finish();
                }
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
                new AlertDialog.Builder(context)
                        .setView(dialogView)
                        .setPositiveButton(android.R.string.yes, null).show();
                break;
            }
        }
        return super.onPreferenceTreeClick(preference);
    }

    private static class RestoreDatabaseAsyncTask extends AsyncTask<Void, String, String> {
        private final ProgressDialog dialog;
        private final AlertDialog.Builder builder;
        private final WeakReference<Context> contextWeakReference;

        RestoreDatabaseAsyncTask(Activity activity, Context context) {
            this.builder = new AlertDialog.Builder(activity);
            this.dialog = new ProgressDialog(activity);
            this.contextWeakReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            dialog.setMessage("Restore database is running...");
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
}
