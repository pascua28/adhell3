package com.fusionjack.adhell3.dialogfragment;


import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.databinding.DialogFragmentActivationBinding;
import com.fusionjack.adhell3.databinding.DialogQuestionBinding;
import com.fusionjack.adhell3.fragments.HomeTabFragment;
import com.fusionjack.adhell3.tasks.BackupDatabaseRxTask;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.DeviceAdminInteractor;
import com.fusionjack.adhell3.utils.LogUtils;
import com.google.android.material.snackbar.Snackbar;
import com.samsung.android.knox.license.EnterpriseLicenseManager;
import com.samsung.android.knox.license.KnoxEnterpriseLicenseManager;

import java.util.Locale;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.CompletableObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class ActivationDialogFragment extends DialogFragment {
    public static final String DIALOG_TAG = "activation_dialog";
    private final DeviceAdminInteractor deviceAdminInteractor;
    private final Completable knoxKeyObservable;
    private final CompletableObserver knoxKeyObserver;
    private final BroadcastReceiver receiver;
    private SharedPreferences sharedPreferences;
    private DialogFragmentActivationBinding binding;

    public ActivationDialogFragment() {
        deviceAdminInteractor = DeviceAdminInteractor.getInstance();

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if (KnoxEnterpriseLicenseManager.ACTION_LICENSE_STATUS.equals(action)) {
                    int errorCode = intent.getIntExtra(KnoxEnterpriseLicenseManager.EXTRA_LICENSE_ERROR_CODE, -1);
                    if (errorCode == KnoxEnterpriseLicenseManager.ERROR_NONE) {
                        handleResult(intent, context);
                    } else {
                        handleError(intent, errorCode);
                    }
                }

                if (EnterpriseLicenseManager.ACTION_LICENSE_STATUS.equals(action)) {
                    int errorCode = intent.getIntExtra(EnterpriseLicenseManager.EXTRA_LICENSE_ERROR_CODE, -1);
                    if (errorCode == EnterpriseLicenseManager.ERROR_NONE) {
                        handleResult(intent, context);
                    } else  {
                        handleError(intent, errorCode);
                    }
                }
            }
        };

        knoxKeyObservable = Completable.create(emitter -> {
            try {
                emitter.onComplete();
            } catch (Throwable e) {
                emitter.onError(e);
            }
        });

        knoxKeyObserver = new CompletableObserver() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
                IntentFilter filter = new IntentFilter();
                filter.addAction(KnoxEnterpriseLicenseManager.ACTION_LICENSE_STATUS);
                filter.addAction(EnterpriseLicenseManager.ACTION_LICENSE_STATUS);
                if (getActivity() != null) {
                    getActivity().registerReceiver(receiver, filter);
                }
            }

            @Override
            public void onComplete() {
                boolean knoxEnabled = deviceAdminInteractor.isKnoxEnabled(getContext());
                if (knoxEnabled) {
                    try {
                        deviceAdminInteractor.deactivateKnoxKey(sharedPreferences, getContext());
                    } catch (Exception ex) {
                        if (ex.getMessage() != null && getActivity() != null && getActivity().findViewById(R.id.bottomBar) != null) {
                            if (getActivity() instanceof MainActivity) {
                                MainActivity mainActivity = (MainActivity) getActivity();
                                mainActivity.makeSnackbar(ex.getMessage(), Snackbar.LENGTH_LONG)
                                        .show();
                            }
                        }
                        setLicenseState(true);
                    }
                } else {
                    deviceAdminInteractor.activateKnoxKey(sharedPreferences, getContext());
                }
            }

            @Override
            public void onError(@NonNull Throwable e) {
                if (getActivity() != null) {
                    getActivity().unregisterReceiver(receiver);
                }
                setLicenseState(false);
            }
        };
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog dialog = getDialog();
        if (dialog != null) {
            int width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
            if (dialog.getWindow() != null) {
                dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        }
    }

    @androidx.annotation.NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getActivity() != null) {
            sharedPreferences = getActivity().getPreferences(Context.MODE_PRIVATE);
            deviceAdminInteractor.setKnoxKey(sharedPreferences, BuildConfig.SKL_KEY);
        }
        return super.onCreateDialog(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@androidx.annotation.NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        binding = DialogFragmentActivationBinding.inflate(inflater);

        String knoxKey = deviceAdminInteractor.getKnoxKey(sharedPreferences);
        binding.knoxKeyEditText.setText(knoxKey);

        binding.turnOnAdminButton.setOnClickListener(v -> {
            FragmentActivity fragmentActivity = getActivity();
            if (fragmentActivity instanceof MainActivity) {
                deviceAdminInteractor.forceEnableAdmin((MainActivity) fragmentActivity);
            }
        });

        binding.activateKnoxButton.setOnClickListener(v -> {
            deviceAdminInteractor.setKnoxKey(sharedPreferences, binding.knoxKeyEditText.getText().toString());

            disableActiveButton();
            boolean knoxEnabled = deviceAdminInteractor.isKnoxEnabled(getContext());
            if (knoxEnabled) {
                binding.activateKnoxButton.setText(R.string.deactivating_knox_license);
            } else {
                binding.activateKnoxButton.setText(R.string.activating_knox_license);
            }

            knoxKeyObservable
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(knoxKeyObserver);
        });

        binding.backupButton.setOnClickListener(v -> {
            DialogQuestionBinding dialogQuestionBinding = DialogQuestionBinding.inflate(LayoutInflater.from(getContext()));
            dialogQuestionBinding.titleTextView.setText(R.string.backup_database_dialog_title);
            dialogQuestionBinding.questionTextView.setText(R.string.backup_database_dialog_text);

            AlertDialog alertDialog = new AlertDialog.Builder(requireContext(), R.style.AlertDialogStyle)
                    .setView(dialogQuestionBinding.getRoot())
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) ->
                            new BackupDatabaseRxTask(getActivity()).run()
                    )
                    .setNegativeButton(android.R.string.no, null)
                    .create();

            alertDialog.show();
        });

        binding.deleteButton.setOnClickListener(v -> {
            DialogQuestionBinding dialogQuestionBinding = DialogQuestionBinding.inflate(LayoutInflater.from(getContext()));
            dialogQuestionBinding.titleTextView.setText(R.string.delete_app_dialog_title);
            dialogQuestionBinding.questionTextView.setText(R.string.delete_app_dialog_text);

            Context context = getContext();
            if (context != null) {
                AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                        .setView(dialogQuestionBinding.getRoot())
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) ->
                                AdhellFactory.uninstall(context, this))
                        .setNegativeButton(android.R.string.no, null)
                        .create();

                alertDialog.show();
            }
        });

        if (getDialog() != null && getDialog().getWindow() != null) {
            getDialog().getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getDialog().getWindow().getAttributes().windowAnimations = R.style.FragmentDialogAnimation;
        }

        return binding.getRoot();
    }

    @Override
    public void onDestroy() {
        // Clean resource to prevent memory leak
        this.sharedPreferences = null;

        super.onDestroy();
    }

    private void handleResult(Intent intent, Context context) {
        if (getActivity() != null) {
            getActivity().unregisterReceiver(receiver);
        }

        int result_type = intent.getIntExtra(KnoxEnterpriseLicenseManager.EXTRA_LICENSE_RESULT_TYPE, -1);
        if (result_type != -1) {
            if (result_type == KnoxEnterpriseLicenseManager.LICENSE_RESULT_TYPE_ACTIVATION) {
                setLicenseState(true);
                LogUtils.info("License activated");
                if (getActivity() != null && getActivity().findViewById(R.id.bottomBar) != null) {
                    if (getActivity() instanceof MainActivity) {
                        MainActivity mainActivity = (MainActivity) getActivity();
                        mainActivity.makeSnackbar("License activated", Snackbar.LENGTH_LONG)
                                .show();
                    }
                }
                FragmentManager fragmentManager = getParentFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.setCustomAnimations(R.anim.dialog_pop_in, R.anim.dialog_pop_out, R.anim.dialog_pop_in, R.anim.dialog_pop_out);
                fragmentTransaction
                        .replace(R.id.fragmentContainer, new HomeTabFragment(), HomeTabFragment.class.getCanonicalName())
                        .commit();
                dismiss();
            } else if (result_type == KnoxEnterpriseLicenseManager.LICENSE_RESULT_TYPE_DEACTIVATION) {
                setLicenseState(false);
                LogUtils.info("License deactivated");
                if (getActivity() != null && getActivity().findViewById(R.id.bottomBar) != null) {
                    if (getActivity() instanceof MainActivity) {
                        MainActivity mainActivity = (MainActivity) getActivity();
                        mainActivity.makeSnackbar("License deactivated", Snackbar.LENGTH_LONG)
                                .show();
                    }
                }
            }
        }

        result_type = intent.getIntExtra(EnterpriseLicenseManager.EXTRA_LICENSE_RESULT_TYPE, -1);
        if (result_type != -1) {
            if (result_type == EnterpriseLicenseManager.LICENSE_RESULT_TYPE_ACTIVATION) {
                setLicenseState(true);
                LogUtils.info("License activated");
                if (getActivity() != null && getActivity().findViewById(R.id.bottomBar) != null) {
                    if (getActivity() instanceof MainActivity) {
                        MainActivity mainActivity = (MainActivity) getActivity();
                        mainActivity.makeSnackbar("License activated", Snackbar.LENGTH_LONG)
                                .show();
                    }
                }
                dismiss();
                FragmentManager fragmentManager = getParentFragmentManager();
                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction
                        .replace(R.id.fragmentContainer, new HomeTabFragment(), HomeTabFragment.class.getCanonicalName())
                        .commit();
            }
        }

        if (context != null) {
            if (context instanceof MainActivity) {
                ((MainActivity) context).finishOnResume();
            }
        }
    }

    private void handleError(Intent intent, int errorCode) {
        if (getActivity() != null) {
            getActivity().unregisterReceiver(receiver);
        }

        if (intent != null) {
            String status = intent.getStringExtra(KnoxEnterpriseLicenseManager.EXTRA_LICENSE_STATUS);
            if (status == null || status.isEmpty()) {
                status = intent.getStringExtra(EnterpriseLicenseManager.EXTRA_LICENSE_STATUS);
            }
            LogUtils.error("Status: " + status + ". Error code: " + errorCode);
            if (getActivity() != null && getActivity().findViewById(R.id.bottomBar) != null) {
                if (getActivity() instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    mainActivity.makeSnackbar(
                            String.format(Locale.getDefault(), "Status: %s. Error code: %d", status, errorCode),
                            Snackbar.LENGTH_LONG)
                            .show();
                }
            }
        }

        // Allow the user to try again
        setLicenseState(false);
        LogUtils.error( "License activation failed");
    }

    @Override
    public void onResume() {
        super.onResume();
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        this.dismiss();
        binding = null;
    }

    private void setAdminState(boolean enabled) {
        if (enabled) {
            binding.turnOnAdminButton.setText(R.string.admin_enabled);
        } else {
            binding.turnOnAdminButton.setText(R.string.enable_admin);
        }
        binding.turnOnAdminButton.setClickable(!enabled);
        binding.turnOnAdminButton.setEnabled(!enabled);
    }

    private void setLicenseState(boolean isActivated) {
        if (isActivated) {
            binding.activateKnoxButton.setText(R.string.deactivate_license);
        } else {
            binding.activateKnoxButton.setText(R.string.activate_license);
        }
        binding.activateKnoxButton.setEnabled(true);
        binding.activateKnoxButton.setClickable(true);
        binding.knoxKeyEditText.setEnabled(!isActivated);
    }

    private void disableActiveButton() {
        binding.activateKnoxButton.setEnabled(false);
        binding.activateKnoxButton.setClickable(false);
        binding.knoxKeyEditText.setEnabled(false);
    }
}
