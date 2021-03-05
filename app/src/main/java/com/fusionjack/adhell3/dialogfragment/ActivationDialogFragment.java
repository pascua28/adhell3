package com.fusionjack.adhell3.dialogfragment;


import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.fragments.HomeTabFragment;
import com.fusionjack.adhell3.tasks.BackupDatabaseRxTask;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.DeviceAdminInteractor;
import com.fusionjack.adhell3.utils.dialog.QuestionDialogBuilder;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.utils.rx.RxCompletableIoBuilder;
import com.samsung.android.knox.license.KnoxEnterpriseLicenseManager;

import io.reactivex.Completable;
import io.reactivex.functions.Action;


public class ActivationDialogFragment extends DialogFragment {

    public static final String DIALOG_TAG = "activation_dialog";

    private Button turnOnAdminButton;
    private Button activateKnoxButton;
    private SharedPreferences sharedPreferences;
    private EditText knoxKeyEditText;

    private final DeviceAdminInteractor deviceAdminInteractor;
    private final BroadcastReceiver receiver;

    public ActivationDialogFragment() {
        this.deviceAdminInteractor = DeviceAdminInteractor.getInstance();
        this.receiver = createReceiver();
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = (int)(getResources().getDisplayMetrics().widthPixels * 0.9);
            dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        boolean adminActive = deviceAdminInteractor.isAdminActive();
        if (adminActive) {
            setAdminState(true);
            boolean knoxEnabled = deviceAdminInteractor.isKnoxEnabled(getContext());
            setLicenseState(knoxEnabled);
        } else {
            setAdminState(false);
            disableActiveButton();
        }
    }

    @androidx.annotation.NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
        deviceAdminInteractor.setKnoxKey(sharedPreferences, BuildConfig.SKL_KEY);
        return super.onCreateDialog(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@androidx.annotation.NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_fragment_activation, container);

        turnOnAdminButton = view.findViewById(R.id.turnOnAdminButton);
        activateKnoxButton = view.findViewById(R.id.activateKnoxButton);
        knoxKeyEditText = view.findViewById(R.id.knoxKeyEditText);

        String knoxKey = deviceAdminInteractor.getKnoxKey(sharedPreferences);
        knoxKeyEditText.setText(knoxKey);

        turnOnAdminButton.setOnClickListener(v -> deviceAdminInteractor.forceEnableAdmin(getContext()));
        activateKnoxButton.setOnClickListener(v -> activeOrDeactivateLicense());

        Button backupButton = view.findViewById(R.id.backupButton);
        backupButton.setOnClickListener(v -> backupDatabase());

        Button deleteButton = view.findViewById(R.id.deleteButton);
        deleteButton.setOnClickListener(v -> deleteAdhell3());

        return view;
    }

    private void activeOrDeactivateLicense() {
        disableActiveButton();
        deviceAdminInteractor.setKnoxKey(sharedPreferences, knoxKeyEditText.getText().toString());

        boolean knoxEnabled = deviceAdminInteractor.isKnoxEnabled(getContext());
        LogUtils.info("Knox is " + (knoxEnabled ? "enabled" : "disabled"));
        activateKnoxButton.setText(knoxEnabled ? R.string.deactivating_knox_license : R.string.activating_knox_license);

        Action action = () -> {
            if (knoxEnabled) {
                try {
                    deviceAdminInteractor.deactivateKnoxKey(sharedPreferences, getContext());
                } catch (Exception ex) {
                    setLicenseState(true);
                    LogUtils.error(ex.getMessage(), ex);
                }
            } else {
                deviceAdminInteractor.activateKnoxKey(sharedPreferences, getContext());
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
                        handleResult(intent, context);
                    } else {
                        handleError(intent, context, errorCode);
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

    private void handleResult(Intent intent, Context context) {
        int result_type = intent.getIntExtra(KnoxEnterpriseLicenseManager.EXTRA_LICENSE_RESULT_TYPE, -1);
        if (result_type != -1) {
            if (result_type == KnoxEnterpriseLicenseManager.LICENSE_RESULT_TYPE_ACTIVATION) {
                setLicenseState(true);
                LogUtils.info("License activated");
                Toast.makeText(context, "License activated", Toast.LENGTH_LONG).show();
                dismiss();
                showHomeTab();
            } else if (result_type == KnoxEnterpriseLicenseManager.LICENSE_RESULT_TYPE_DEACTIVATION) {
                setLicenseState(false);
                LogUtils.info("License deactivated");
                Toast.makeText(context, "License deactivated", Toast.LENGTH_LONG).show();
                setCancelable(false);
            }
        }
    }

    private void showHomeTab() {
        FragmentActivity activity = getActivity();
        if (activity != null) {
            FragmentManager fragmentManager = activity.getSupportFragmentManager();
            fragmentManager
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, new HomeTabFragment(), HomeTabFragment.class.getCanonicalName())
                    .commit();
        }
    }

    private void handleError(Intent intent, Context context, int errorCode) {
        if (intent != null) {
            String status = intent.getStringExtra(KnoxEnterpriseLicenseManager.EXTRA_LICENSE_STATUS);
            LogUtils.error("Status: " + status + ". Error code: " + errorCode);
            Toast.makeText(context, "Status: " + status + ". Error code: " + errorCode, Toast.LENGTH_LONG).show();
        }

        // Allow the user to try again
        setLicenseState(false);
        LogUtils.error( "License activation failed");
    }

    private void backupDatabase() {
        new QuestionDialogBuilder(getView())
                .setTitle(R.string.backup_database_dialog_title)
                .setQuestion(R.string.backup_database_dialog_text)
                .show(() -> new BackupDatabaseRxTask(getContext()).run());
    }

    private void deleteAdhell3() {
        new QuestionDialogBuilder(getView())
                .setTitle(R.string.delete_app_dialog_title)
                .setQuestion(R.string.delete_app_dialog_text)
                .show(() -> AdhellFactory.uninstall(getContext(), this));
    }

    private void setAdminState(boolean enabled) {
        if (enabled) {
            turnOnAdminButton.setText(R.string.admin_enabled);
        } else {
            turnOnAdminButton.setText(R.string.enable_admin);
        }
        turnOnAdminButton.setClickable(!enabled);
        turnOnAdminButton.setEnabled(!enabled);
    }

    private void setLicenseState(boolean isActivated) {
        if (isActivated) {
            activateKnoxButton.setText(R.string.deactivate_license);
        } else {
            activateKnoxButton.setText(R.string.activate_license);
        }
        activateKnoxButton.setEnabled(true);
        activateKnoxButton.setClickable(true);
        knoxKeyEditText.setEnabled(!isActivated);
    }

    private void disableActiveButton() {
        activateKnoxButton.setEnabled(false);
        activateKnoxButton.setClickable(false);
        knoxKeyEditText.setEnabled(false);
    }
}