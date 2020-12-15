package com.fusionjack.adhell3.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.databinding.DialogQuestionBinding;
import com.fusionjack.adhell3.databinding.FragmentAppComponentBinding;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.model.AppFlag;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppComponentFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.DialogUtils;
import com.google.android.material.snackbar.Snackbar;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.SingleObserver;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class AppComponentFragment extends AppFragment {

    private FragmentAppComponentBinding binding;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        type = AppRepository.Type.COMPONENT;

        initAppModel(type);

        if (BuildConfig.SHOW_SYSTEM_APP_COMPONENT && !AppPreferences.getInstance().getWarningDialogAppComponentDontShow()) {
            DialogQuestionBinding dialogQuestionBinding = DialogQuestionBinding.inflate(LayoutInflater.from(getContext()));
            dialogQuestionBinding.questionDontShow.setVisibility(View.VISIBLE);
            dialogQuestionBinding.titleTextView.setText(R.string.dialog_system_app_components_title);
            dialogQuestionBinding.questionTextView.setText(R.string.dialog_system_app_components_info);
            AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                    .setView(dialogQuestionBinding.getRoot())
                    .setPositiveButton(android.R.string.yes, ((dialog, which) -> {
                        if (dialogQuestionBinding.questionDontShow.isChecked()) {
                            AppPreferences.getInstance().setWarningDialogAppComponentDontShow(true);
                        }
                    }))
                    .create();

            alertDialog.show();
        }
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.appcomponent_tab_menu, menu);

        initSearchView(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_enable_all) {
            enableAllAppComponents();
        } else if (id == R.id.action_batch) {
            batchOperation();
        } else if (id == R.id.action_show_disabled) {
            if (getActivity() != null) {
                AdhellFactory.getInstance().showAppComponentDisabledFragment(getActivity().getSupportFragmentManager());
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        binding = FragmentAppComponentBinding.inflate(inflater);
        appFlag = AppFlag.createComponentFlag();

        binding.componentAppsList.setAdapter(adapter);

        binding.componentAppsList.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
            AppInfoAdapter adapter = (AppInfoAdapter) adView.getAdapter();

            if (getActivity() != null) {
                FragmentManager fragmentManager = getActivity().getSupportFragmentManager();

                Bundle bundle = new Bundle();
                AppInfo appInfo = adapter.getItem(position);
                bundle.putString("packageName", appInfo.packageName);
                bundle.putString("appName", appInfo.appName);
                ComponentTabFragment fragment = new ComponentTabFragment();
                fragment.setArguments(bundle);

                FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
                fragmentTransaction.setCustomAnimations(R.anim.fragment_fade_in, R.anim.fragment_fade_out, R.anim.fragment_fade_in, R.anim.fragment_fade_out);
                fragmentTransaction.replace(R.id.fragmentContainer, fragment);
                fragmentTransaction.addToBackStack("appComponents");
                fragmentTransaction.commit();
            }
        });

        binding.componentSwipeContainer.setOnRefreshListener(() -> {
            loadAppList(type);
            resetSearchView();
        });

        rootView = binding.getRoot();
        super.onCreateView(inflater, container, savedInstanceState);

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAppList(type);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void batchOperation() {
        DialogQuestionBinding dialogQuestionBinding = DialogQuestionBinding.inflate(LayoutInflater.from(getContext()));
        dialogQuestionBinding.titleTextView.setText(R.string.dialog_appcomponent_batch_title);
        dialogQuestionBinding.questionTextView.setText(R.string.dialog_appcomponent_batch_summary);

        AlertDialog progressDialog = DialogUtils.getProgressDialog("", context);
        progressDialog.setCancelable(false);

        SingleObserver<String> observer = new SingleObserver<String>() {
            @Override
            public void onSubscribe(@NonNull Disposable d) {
            }

            @Override
            public void onSuccess(@NonNull String s) {
                progressDialog.dismiss();
                if (getActivity() instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) getActivity();
                    mainActivity.makeSnackbar(s, Snackbar.LENGTH_LONG)
                            .show();
                }
            }

            @Override
            public void onError(Throwable e) {
                progressDialog.dismiss();
                if (e.getMessage() != null) {
                    if (getActivity() instanceof MainActivity) {
                        MainActivity mainActivity = (MainActivity) getActivity();
                        mainActivity.makeSnackbar(e.getMessage(), Snackbar.LENGTH_LONG)
                                .show();
                    }
                }
            }
        };

        AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                .setView(dialogQuestionBinding.getRoot())
                .setPositiveButton(R.string.button_enable, (dialog, whichButton) -> {
                    progressDialog.show();
                    DialogUtils.setProgressDialogMessage(progressDialog, getString(R.string.dialog_appcomponent_enable_summary));
                    AppComponentFactory.getInstance().processAppComponentInBatch(true)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(observer);
                })
                .setNegativeButton(R.string.button_disable, (dialog, whichButton) -> {
                    progressDialog.show();
                    DialogUtils.setProgressDialogMessage(progressDialog, getString(R.string.dialog_appcomponent_disable_summary));
                    AppComponentFactory.getInstance().processAppComponentInBatch(false)
                            .subscribeOn(Schedulers.io())
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribe(observer);
                })
                .setNeutralButton(android.R.string.no, null)
                .create();

        alertDialog.show();

        if (getView() != null) {
            AppComponentFactory.getInstance().checkMigrateOldBatchFiles(getContext());
        }
    }

    private void enableAllAppComponents() {
        DialogQuestionBinding dialogQuestionBinding = DialogQuestionBinding.inflate(LayoutInflater.from(getContext()));
        dialogQuestionBinding.titleTextView.setText(R.string.dialog_enable_components_title);
        dialogQuestionBinding.questionTextView.setText(R.string.dialog_enable_components_info);
        AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                .setView(dialogQuestionBinding.getRoot())
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) ->
                        AsyncTask.execute(() -> {
                            AdhellFactory.getInstance().setAppComponentState(true);
                            AdhellFactory.getInstance().getAppDatabase().appPermissionDao().deleteAll();
                        })
                )
                .setNegativeButton(android.R.string.no, null)
                .create();

        alertDialog.show();
    }
}
