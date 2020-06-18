package com.fusionjack.adhell3.fragments;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.DnsPackage;
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.model.AppFlag;
import com.fusionjack.adhell3.tasks.SetAppAsyncTask;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.leinardi.android.speeddial.SpeedDialActionItem;
import com.leinardi.android.speeddial.SpeedDialView;

import java.util.List;

public class DnsFragment extends AppFragment {

    private ProgressBar loadingBar;
    private ListView listView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        type = AppRepository.Type.DNS;

        initAppModel(type);
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
        TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
        titleTextView.setText(R.string.dialog_toggle_title);
        TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
        questionTextView.setText(R.string.dialog_toggle_info);
        AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                .setView(dialogView)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) ->
                        AsyncTask.execute(() -> {
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

                            loadAppList(type, loadingBar, listView);
                        })
                )
                .setNegativeButton(android.R.string.no, null)
                .create();

        alertDialog.show();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        View view = inflater.inflate(R.layout.fragment_dns, container, false);

        loadingBar = view.findViewById(R.id.loadingBar);
        AppFlag appFlag = AppFlag.createDnsFlag();
        listView = view.findViewById(R.id.dns_apps_list);
        listView.setAdapter(adapter);
        if (AppPreferences.getInstance().isDnsNotEmpty()) {
            listView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                AppInfoAdapter adapter = (AppInfoAdapter) adView.getAdapter();
                new SetAppAsyncTask(adapter.getItem(position), appFlag, context).execute();
            });
        }

        SwipeRefreshLayout dnsSwipeContainer = view.findViewById(R.id.dnsSwipeContainer);
        dnsSwipeContainer.setOnRefreshListener(() -> {
            loadAppList(type, loadingBar, listView);
            dnsSwipeContainer.setRefreshing(false);
            resetSearchView();
        });

        SpeedDialView speedDialView = view.findViewById(R.id.dns_actions);
        speedDialView.addActionItem(new SpeedDialActionItem.Builder(R.id.action_set_dns, getResources().getDrawable(R.drawable.ic_dns_white_24dp, requireContext().getTheme()))
                .setLabel(getString(R.string.dialog_set_dns_title))
                .setFabBackgroundColor(getResources().getColor(R.color.colorFab, requireContext().getTheme()))
                .setLabelColor(getResources().getColor(R.color.colorText, requireContext().getTheme()))
                .setLabelBackgroundColor(getResources().getColor(R.color.colorBorder, requireContext().getTheme()))
                .setFabSize(com.google.android.material.floatingactionbutton.FloatingActionButton.SIZE_NORMAL)
                .setLabelClickable(false)
                .create());

        speedDialView.setOnActionSelectedListener(actionItem -> {
            if (actionItem.getId() == R.id.action_set_dns) {
                View dialogView = inflater.inflate(R.layout.dialog_set_dns, container, false);
                EditText primaryDnsEditText = dialogView.findViewById(R.id.primaryDnsEditText);
                EditText secondaryDnsEditText = dialogView.findViewById(R.id.secondaryDnsEditText);
                if (AppPreferences.getInstance().isDnsNotEmpty()) {
                    primaryDnsEditText.setText(AppPreferences.getInstance().getDns1());
                    secondaryDnsEditText.setText(AppPreferences.getInstance().getDns2());
                }
                primaryDnsEditText.requestFocus();

                new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                        .setView(dialogView)
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                            Handler handler = new Handler(Looper.getMainLooper()) {
                                @Override
                                public void handleMessage(Message msg) {
                                    Toast.makeText(context, getString(Integer.parseInt(msg.obj.toString())), Toast.LENGTH_LONG).show();
                                }
                            };

                            String primaryDns = primaryDnsEditText.getText().toString();
                            String secondaryDns = secondaryDnsEditText.getText().toString();
                            AdhellFactory.getInstance().setDns(primaryDns, secondaryDns, handler);

                            if (AppPreferences.getInstance().isDnsNotEmpty()) {
                                listView.setEnabled(true);
                                listView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                                    AppInfoAdapter adapter = (AppInfoAdapter) adView.getAdapter();
                                    new SetAppAsyncTask(adapter.getItem(position), appFlag, context).execute();
                                });
                            } else {
                                listView.setEnabled(false);
                            }

                            if (listView.getAdapter() instanceof AppInfoAdapter) {
                                ((AppInfoAdapter) listView.getAdapter()).notifyDataSetChanged();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .show();

                speedDialView.close();
                return true;
            }
            return false;
        });

        loadAppList(type, loadingBar, listView);
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAppList(type, loadingBar, listView);
    }
}
