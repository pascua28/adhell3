package com.fusionjack.adhell3.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.DnsPackage;
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.model.AppFlag;
import com.fusionjack.adhell3.tasks.ToggleAppInfoRxTask;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.LogUtils;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Observer;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class DnsFragment extends AppFragment {

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View view = inflateFragment(R.layout.fragment_dns, inflater, container, AppRepository.Type.DNS, AppFlag.createDnsFlag());

        FloatingActionsMenu dnsFloatMenu = view.findViewById(R.id.dns_actions);
        FloatingActionButton actionSetDns = view.findViewById(R.id.action_set_dns);
        actionSetDns.setIcon(R.drawable.ic_dns_white_24dp);
        actionSetDns.setOnClickListener(v -> {
            dnsFloatMenu.collapse();

            View dialogView = inflater.inflate(R.layout.dialog_set_dns, container, false);
            EditText primaryDnsEditText = dialogView.findViewById(R.id.primaryDnsEditText);
            EditText secondaryDnsEditText = dialogView.findViewById(R.id.secondaryDnsEditText);
            if (AppPreferences.getInstance().isDnsNotEmpty()) {
                primaryDnsEditText.setText(AppPreferences.getInstance().getDns1());
                secondaryDnsEditText.setText(AppPreferences.getInstance().getDns2());
            }
            primaryDnsEditText.requestFocus();

            new AlertDialog.Builder(context)
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        AppFlag appFlag = AppFlag.createDnsFlag();
                        ListView listView = view.findViewById(appFlag.getLayout());
                        setDns(primaryDnsEditText.getText().toString(), secondaryDnsEditText.getText().toString(), listView);
                    })
                    .setNegativeButton(android.R.string.no, null).show();
        });

        return view;
    }

    private void setDns(String primaryDns, String secondaryDns, ListView listView) {
        Observable.create((ObservableOnSubscribe<Integer>) emitter -> {
            AdhellFactory.getInstance().setDns(primaryDns, secondaryDns, emitter);
            emitter.onComplete();
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<Integer>() {
                    @Override
                    public void onSubscribe(@io.reactivex.annotations.NonNull Disposable d) {
                    }

                    @Override
                    public void onNext(@io.reactivex.annotations.NonNull Integer strId) {
                        Toast.makeText(context, getString(strId), Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onComplete() {
                        if (listView.getAdapter() instanceof AppInfoAdapter) {
                            ((AppInfoAdapter) listView.getAdapter()).notifyDataSetChanged();
                        }
                    }

                    @Override
                    public void onError(@io.reactivex.annotations.NonNull Throwable e) {
                        LogUtils.error(e.getMessage(), e);
                    }
                });
    }

    @Override
    protected void listOnItemClickListener(AdapterView<?> adView, View view2, int position, long id, AppFlag appFlag) {
        if (AppPreferences.getInstance().isDnsNotEmpty()) {
            AppInfoAdapter adapter = (AppInfoAdapter) adView.getAdapter();
            ToggleAppInfoRxTask.run(adapter.getItem(position), appFlag, adapter);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_enable_all) {
            toggleAllApps();
        }
        return super.onOptionsItemSelected(item);
    }

    private void toggleAllApps() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_question, (ViewGroup) getView(), false);
        TextView titlTextView = dialogView.findViewById(R.id.titleTextView);
        titlTextView.setText(R.string.dialog_toggle_title);
        TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
        questionTextView.setText(R.string.dialog_toggle_info);
        new AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton(android.R.string.yes, (dialog, whichButton) ->
                Completable.fromAction(() -> {
                    AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();

                    boolean isAllEnabled = AppPreferences.getInstance().isDnsAllAppsEnabled();
                    if (isAllEnabled) {
                        List<AppInfo> dnsApps = appDatabase.applicationInfoDao().getDnsApps();
                        for (AppInfo app : dnsApps) {
                            app.hasCustomDns = false;
                            appDatabase.applicationInfoDao().update(app);
                        }
                        appDatabase.dnsPackageDao().deleteAll();
                    } else {
                        appDatabase.dnsPackageDao().deleteAll();
                        List<AppInfo> userApps = appDatabase.applicationInfoDao().getUserApps();
                        for (AppInfo app : userApps) {
                            app.hasCustomDns = true;
                            appDatabase.applicationInfoDao().update(app);
                            DnsPackage dnsPackage = new DnsPackage();
                            dnsPackage.packageName = app.packageName;
                            dnsPackage.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
                            appDatabase.dnsPackageDao().insert(dnsPackage);
                        }
                    }

                    AppPreferences.getInstance().setDnsAllApps(!isAllEnabled);
                })
                        .subscribeOn(Schedulers.io())
                        .subscribe()
            )
            .setNegativeButton(android.R.string.no, null).show();
    }
}
