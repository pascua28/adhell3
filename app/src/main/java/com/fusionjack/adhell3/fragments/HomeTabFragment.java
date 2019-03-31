package com.fusionjack.adhell3.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.ReportBlockedUrlAdapter;
import com.fusionjack.adhell3.blocker.ContentBlocker;
import com.fusionjack.adhell3.blocker.ContentBlocker56;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppPermission;
import com.fusionjack.adhell3.db.entity.ReportBlockedUrl;
import com.fusionjack.adhell3.db.entity.WhiteUrl;
import com.fusionjack.adhell3.dialogfragment.FirewallDialogFragment;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.FirewallUtils;
import com.fusionjack.adhell3.utils.LogUtils;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class HomeTabFragment extends Fragment {

    private FragmentManager fragmentManager;
    private AppCompatActivity parentActivity;
    private TextView domainStatusTextView;
    private Switch domainSwitch;
    private TextView firewallStatusTextView;
    private Switch firewallSwitch;
    private TextView disablerStatusTextView;
    private Switch disablerSwitch;
    private TextView appComponentStatusTextView;
    private Switch appComponentSwitch;
    private TextView infoTextView;
    private SwipeRefreshLayout swipeContainer;
    private ContentBlocker contentBlocker;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentManager = Objects.requireNonNull(getActivity()).getSupportFragmentManager();
        parentActivity = (AppCompatActivity) getActivity();
        contentBlocker = ContentBlocker56.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ActionBar actionBar = parentActivity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);

            View view = inflater.inflate(R.layout.activity_actionbar, container, false);
            TextView subtitleTextView = view.findViewById(R.id.subtitleTextView);
            if (subtitleTextView != null) {
                String versionInfo = Objects.requireNonNull(getContext()).getResources().getString(R.string.version);
                subtitleTextView.setText(String.format(versionInfo, BuildConfig.VERSION_NAME));
            }
            actionBar.setCustomView(view);
            actionBar.setDisplayShowCustomEnabled(true);
        }

        View view = inflater.inflate(R.layout.fragment_blocker, container, false);
        domainSwitch = view.findViewById(R.id.domainRulesSwitch);
        domainStatusTextView = view.findViewById(R.id.domainStatusTextView);
        firewallSwitch = view.findViewById(R.id.firewallRulesSwitch);
        firewallStatusTextView = view.findViewById(R.id.firewallStatusTextView);
        disablerSwitch = view.findViewById(R.id.appDisablerSwitch);
        disablerStatusTextView = view.findViewById(R.id.disablerStatusTextView);
        appComponentSwitch = view.findViewById(R.id.appComponentSwitch);
        appComponentStatusTextView = view.findViewById(R.id.appComponentStatusTextView);
        swipeContainer = view.findViewById(R.id.swipeContainer);
        infoTextView = view.findViewById(R.id.infoTextView);

        infoTextView.setVisibility(View.INVISIBLE);
        swipeContainer.setVisibility(View.INVISIBLE);

        if (!BuildConfig.DISABLE_APPS) {
            view.findViewById(R.id.appDisablerLayout).setVisibility(View.GONE);
        }
        if (!BuildConfig.APP_COMPONENT) {
            view.findViewById(R.id.appComponentLayout).setVisibility(View.GONE);
        }

        domainSwitch.setOnClickListener(v -> {
            LogUtils.info("Domain switch button has been clicked");
            new SetFirewallAsyncTask(true, this, fragmentManager, getContext()).execute();
        });
        firewallSwitch.setOnClickListener(v -> {
            LogUtils.info("Firewall switch button has been clicked");
            new SetFirewallAsyncTask(false, this, fragmentManager, getContext()).execute();
        });
        disablerSwitch.setOnClickListener(v -> {
            LogUtils.info("App disabler switch button has been clicked");
            new AppDisablerAsyncTask(this, getActivity()).execute();
        });
        appComponentSwitch.setOnClickListener(v -> {
            LogUtils.info("App component switch button has been clicked");
            new AppComponentAsyncTask(this, getActivity()).execute();
        });

        FloatingActionsMenu domainFloatMenu = view.findViewById(R.id.domain_actions);
        FloatingActionButton actionAddWhiteDomain = view.findViewById(R.id.action_export_domains);
        actionAddWhiteDomain.setIcon(R.drawable.ic_public_white_24dp);
        actionAddWhiteDomain.setOnClickListener(v -> {
            domainFloatMenu.collapse();
            new ExportDomainsAsyncTask(getContext()).execute();
        });


        AsyncTask.execute(() -> {
            AdhellAppIntegrity adhellAppIntegrity = AdhellAppIntegrity.getInstance();
            adhellAppIntegrity.checkDefaultPolicyExists();
            adhellAppIntegrity.checkAdhellStandardPackage();
            adhellAppIntegrity.fillPackageDb();
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUserInterface();
    }

    private void updateUserInterface() {
        new SetInfoAsyncTask(getContext()).execute();

        boolean isDomainRuleEmpty = contentBlocker.isDomainRuleEmpty();
        boolean isFirewallRuleEmpty = contentBlocker.isFirewallRuleEmpty();

        if (contentBlocker == null || isDomainRuleEmpty) {
            domainStatusTextView.setText(R.string.domain_rules_disabled);
            domainSwitch.setChecked(false);
        } else {
            domainStatusTextView.setText(R.string.domain_rules_enabled);
            domainSwitch.setChecked(true);
        }

        if (contentBlocker == null || isFirewallRuleEmpty) {
            firewallStatusTextView.setText(R.string.firewall_rules_disabled);
            firewallSwitch.setChecked(false);
        } else {
            firewallStatusTextView.setText(R.string.firewall_rules_enabled);
            firewallSwitch.setChecked(true);
        }

        if (!isDomainRuleEmpty) {
            infoTextView.setVisibility(View.VISIBLE);
            swipeContainer.setVisibility(View.VISIBLE);
            swipeContainer.setOnRefreshListener(() ->
                    new RefreshAsyncTask(getContext()).execute()
            );
            new RefreshAsyncTask(getContext()).execute();
        } else {
            infoTextView.setVisibility(View.INVISIBLE);
            swipeContainer.setVisibility(View.INVISIBLE);
        }

        boolean disablerEnabled = AppPreferences.getInstance().isAppDisablerToggleEnabled();
        if (disablerEnabled) {
            disablerStatusTextView.setText(R.string.app_disabler_enabled);
            disablerSwitch.setChecked(true);
        } else {
            disablerStatusTextView.setText(R.string.app_disabler_disabled);
            disablerSwitch.setChecked(false);
        }

        boolean appComponentEnabled = AppPreferences.getInstance().isAppComponentToggleEnabled();
        if (appComponentEnabled) {
            appComponentStatusTextView.setText(R.string.app_component_enabled);
            appComponentSwitch.setChecked(true);
        } else {
            appComponentStatusTextView.setText(R.string.app_component_disabled);
            appComponentSwitch.setChecked(false);
        }
    }

    private static class SetInfoAsyncTask extends AsyncTask<Void, Void, Void> {
        private final WeakReference<Context> contextWeakReference;
        private int mobileSize;
        private int wifiSize;
        private int customSize;
        private int blackListSize;
        private int whiteListSize;
        private int whitelistAppSize;
        private int disablerSize;
        private int permissionSize;
        private int serviceSize;
        private int receiverSize;

        SetInfoAsyncTask(Context context) {
            this.contextWeakReference = new WeakReference<>(context);
        }

        @Override
        protected void onPreExecute() {
            Context context = contextWeakReference.get();
            if (context != null) {
                TextView domainInfoTextView = ((Activity) context).findViewById(R.id.domainInfoTextView);
                if (domainInfoTextView != null) {
                    String domainInfo = context.getResources().getString(R.string.domain_rules_info);
                    domainInfoTextView.setText(String.format(domainInfo, 0, 0, 0));
                }
                TextView firewallInfoTextView = ((Activity) context).findViewById(R.id.firewallInfoTextView);
                if (firewallInfoTextView != null) {
                    String firewallInfo = context.getResources().getString(R.string.firewall_rules_info);
                    firewallInfoTextView.setText(String.format(firewallInfo, 0, 0, 0));
                }
                TextView disablerInfoTextView = ((Activity) context).findViewById(R.id.disablerInfoTextView);
                if (disablerInfoTextView != null) {
                    String disablerInfo = context.getResources().getString(R.string.app_disabler_info);
                    disablerInfoTextView.setText(String.format(disablerInfo, 0));
                }
                TextView appComponentInfoTextView = ((Activity) context).findViewById(R.id.appComponentInfoTextView);
                if (appComponentInfoTextView != null) {
                    String appComponentInfo = context.getResources().getString(R.string.app_component_toggle_info);
                    appComponentInfoTextView.setText(String.format(appComponentInfo, 0, 0, 0));
                }
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            disablerSize = appDatabase.disabledPackageDao().getAll().size();

            List<AppPermission> appPermissions = appDatabase.appPermissionDao().getAll();
            for (AppPermission appPermission : appPermissions) {
                switch (appPermission.permissionStatus) {
                    case AppPermission.STATUS_PERMISSION:
                        permissionSize++;
                        break;
                    case AppPermission.STATUS_SERVICE:
                        serviceSize++;
                        break;
                    case AppPermission.STATUS_RECEIVER:
                        receiverSize++;
                        break;
                }
            }

            FirewallUtils.DomainStat domainStat = FirewallUtils.getInstance().getDomainStatFromKnox();
            blackListSize = domainStat.blackListSize;
            whiteListSize = domainStat.whiteListSize;

            whitelistAppSize = FirewallUtils.getInstance().getWhitelistAppCountFromKnox();

            // Dirty solution: Every deny firewall is created for IPv4 and IPv6.
            FirewallUtils.FirewallStat stat = FirewallUtils.getInstance().getFirewallStatFromKnox();
            customSize = stat.allNetworkSize / 2;
            mobileSize = stat.mobileDataSize / 2;
            wifiSize = stat.wifiDataSize / 2;

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Context context = contextWeakReference.get();
            if (context != null) {
                TextView domainInfoTextView = ((Activity) context).findViewById(R.id.domainInfoTextView);
                if (domainInfoTextView != null) {
                    String domainInfo = context.getResources().getString(R.string.domain_rules_info);
                    domainInfoTextView.setText(String.format(domainInfo, blackListSize, whiteListSize, whitelistAppSize));
                }
                TextView firewallInfoTextView = ((Activity) context).findViewById(R.id.firewallInfoTextView);
                if (firewallInfoTextView != null) {
                    String firewallInfo = context.getResources().getString(R.string.firewall_rules_info);
                    firewallInfoTextView.setText(String.format(firewallInfo, mobileSize, wifiSize, customSize));
                }
                TextView disablerInfoTextView = ((Activity) context).findViewById(R.id.disablerInfoTextView);
                if (disablerInfoTextView != null) {
                    String disablerInfo = context.getResources().getString(R.string.app_disabler_info);
                    boolean enabled = AppPreferences.getInstance().isAppDisablerToggleEnabled();
                    disablerInfoTextView.setText(String.format(disablerInfo, enabled ? disablerSize : 0));
                }
                TextView appComponentInfoTextView = ((Activity) context).findViewById(R.id.appComponentInfoTextView);
                if (appComponentInfoTextView != null) {
                    String appComponentInfo = context.getResources().getString(R.string.app_component_toggle_info);
                    boolean enabled = AppPreferences.getInstance().isAppComponentToggleEnabled();
                    String info;
                    if (enabled) {
                        info = String.format(appComponentInfo, permissionSize, serviceSize, receiverSize);
                    } else {
                        info = String.format(appComponentInfo, 0, 0, 0);
                    }
                    appComponentInfoTextView.setText(info);
                }
            }
        }
    }

    private static class AppDisablerAsyncTask extends AsyncTask<Void, Void, Void> {
        private final HomeTabFragment parentFragment;
        private final ProgressDialog dialog;

        AppDisablerAsyncTask(HomeTabFragment parentFragment, Activity activity) {
            this.parentFragment = parentFragment;
            this.dialog = new ProgressDialog(activity);
        }

        @Override
        protected void onPreExecute() {
            boolean enabled = AppPreferences.getInstance().isAppDisablerToggleEnabled();
            dialog.setMessage(enabled ? "Enabling apps..." : "Disabling apps...");
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            boolean enabled = AppPreferences.getInstance().isAppDisablerToggleEnabled();
            AdhellFactory.getInstance().setAppDisablerToggle(!enabled); // toggle the switch
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            parentFragment.updateUserInterface();
        }
    }

    private static class AppComponentAsyncTask extends AsyncTask<Void, Void, Void> {
        private final HomeTabFragment parentFragment;
        private final ProgressDialog dialog;

        AppComponentAsyncTask(HomeTabFragment parentFragment, Activity activity) {
            this.parentFragment = parentFragment;
            this.dialog = new ProgressDialog(activity);
        }

        @Override
        protected void onPreExecute() {
            boolean enabled = AppPreferences.getInstance().isAppComponentToggleEnabled();
            dialog.setMessage(enabled ? "Enabling app component..." : "Disabling app component...");
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            boolean toggleEnabled = AppPreferences.getInstance().isAppComponentToggleEnabled();
            AdhellFactory.getInstance().setAppComponentToggle(!toggleEnabled); // toggle the switch
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            parentFragment.updateUserInterface();
        }
    }

    private static class SetFirewallAsyncTask extends AsyncTask<Void, Void, Void> {
        private final FragmentManager fragmentManager;
        private FirewallDialogFragment fragment;
        private final HomeTabFragment parentFragment;
        private final ContentBlocker contentBlocker;
        private final Handler handler;
        private final boolean isDomain;
        private final boolean isDomainRuleEmpty;
        private final boolean isFirewallRuleEmpty;
        private final WeakReference<Context> contextReference;

        SetFirewallAsyncTask(boolean isDomain, HomeTabFragment parentFragment, FragmentManager fragmentManager, Context context) {
            this.isDomain = isDomain;
            this.parentFragment = parentFragment;
            this.fragmentManager = fragmentManager;
            this.contentBlocker = ContentBlocker56.getInstance();
            this.isDomainRuleEmpty = contentBlocker.isDomainRuleEmpty();
            this.isFirewallRuleEmpty = contentBlocker.isFirewallRuleEmpty();
            this.contextReference = new WeakReference<>(context);

            this.handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    fragment.appendText(msg.obj.toString());
                }
            };
        }

        @Override
        protected void onPreExecute() {
            if (isDomain) {
                fragment = FirewallDialogFragment.newInstance(
                        isDomainRuleEmpty ? "Enabling Domain Rules" : "Disabling Domain Rules");
            } else {
                fragment = FirewallDialogFragment.newInstance(
                        isFirewallRuleEmpty ? "Enabling Firewall Rules" : "Disabling Firewall Rules");
            }
            fragment.setCancelable(false);
            fragment.show(fragmentManager, "dialog_firewall");
        }

        @Override
        protected Void doInBackground(Void... args) {
            contentBlocker.setHandler(handler);
            if (isDomain) {
                if (isDomainRuleEmpty) {
                    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(contextReference.get());
                    boolean updateProviders = preferences.getBoolean(SettingsFragment.UPDATE_PROVIDERS_PREFERENCE, false);
                    if (!AdhellFactory.getInstance().hasInternetAccess(contextReference.get())) {
                        updateProviders = false;
                    }
                    contentBlocker.enableDomainRules(updateProviders);
                } else {
                    contentBlocker.disableDomainRules();
                }
            } else {
                if (isFirewallRuleEmpty) {
                    contentBlocker.enableFirewallRules();
                } else {
                    contentBlocker.disableFirewallRules();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            fragment.enableCloseButton();
            parentFragment.updateUserInterface();
        }
    }

    private static class RefreshAsyncTask extends AsyncTask<Void, Void, HashMap<String, List<ReportBlockedUrl>>> {
        private final WeakReference<Context> contextReference;

        RefreshAsyncTask(Context context) {
            this.contextReference = new WeakReference<>(context);
        }

        @Override
        protected HashMap<String, List<ReportBlockedUrl>> doInBackground(Void... voids) {
            HashMap<String, List<ReportBlockedUrl>> returnHashMap = new HashMap<>();
            List<ReportBlockedUrl> listReportBlockedUrl = FirewallUtils.getInstance().getReportBlockedUrl();
            for (ReportBlockedUrl reportBlockedUrl : listReportBlockedUrl) {
                List<ReportBlockedUrl> newList = returnHashMap.get(reportBlockedUrl.packageName);
                if (newList == null) {
                    newList = new ArrayList<>();
                    newList.add(reportBlockedUrl);
                    returnHashMap.put(reportBlockedUrl.packageName, newList);
                } else {
                    newList.add(reportBlockedUrl);
                }
            }

            // Sort HashMap by 'blockDate'
            LinkedList<HashMap.Entry<String, List<ReportBlockedUrl>>> linkedList = new LinkedList<>(returnHashMap.entrySet());
            LinkedHashMap<String, List<ReportBlockedUrl>> sortedHashMap = new LinkedHashMap<>();
            Collections.sort(linkedList, (list1, list2) ->
                    ((Comparable<Long>) list2.getValue().get(0).blockDate).compareTo(list1.getValue().get(0).blockDate));
            for (Map.Entry<String, List<ReportBlockedUrl>> entry : linkedList) {
                sortedHashMap.put(entry.getKey(), entry.getValue());
            }
            return sortedHashMap;
        }

        @Override
        protected void onPostExecute(HashMap<String, List<ReportBlockedUrl>> reportBlockedUrls) {
            Context context = contextReference.get();
            if (context != null) {
                ExpandableListView listView = ((Activity) context).findViewById(R.id.blockedDomainsListView);
                if (listView != null) {
                    Handler handler = new Handler(Looper.getMainLooper()) {
                        @Override
                        public void handleMessage(Message msg) {
                            ReportBlockedUrlAdapter adapter = (ReportBlockedUrlAdapter) listView.getExpandableListAdapter();
                            adapter.notifyDataSetChanged();
                        }
                    };

                    ReportBlockedUrlAdapter adapter = new ReportBlockedUrlAdapter(context, reportBlockedUrls, handler);
                    listView.setAdapter(adapter);
                    listView.setOnChildClickListener((ExpandableListView parent, View view, int groupPosition, int childPosition, long id) -> {
                        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_question, listView, false);
                        TextView titlTextView = dialogView.findViewById(R.id.titleTextView);
                        titlTextView.setText(R.string.dialog_whitelist_domain_title);
                        TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
                        questionTextView.setText(R.string.dialog_add_to_whitelist_question);
                        new AlertDialog.Builder(context)
                                .setView(dialogView)
                                .setPositiveButton(android.R.string.yes, (dialog, whichButton) ->
                                        AsyncTask.execute(() -> {
                                            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
                                            List<String> groupList = new ArrayList<>(reportBlockedUrls.keySet());
                                            final String blockedUrl = Objects.requireNonNull(reportBlockedUrls.get(groupList.get(groupPosition))).get(childPosition).url;
                                            WhiteUrl whiteUrl = new WhiteUrl(blockedUrl, new Date());
                                            appDatabase.whiteUrlDao().insert(whiteUrl);
                                        })
                                )
                                .setNegativeButton(android.R.string.no, null).show();
                        return false;
                    });
                }

                TextView infoTextView = ((Activity) context).findViewById(R.id.infoTextView);
                if (infoTextView != null) {
                    int totalCount = 0;
                    for (HashMap.Entry<String, List<ReportBlockedUrl>> entry : reportBlockedUrls.entrySet()) {
                        totalCount += entry.getValue().size();
                    }
                    infoTextView.setText(String.format("%s%s",
                            context.getString(R.string.last_day_blocked), String.valueOf(totalCount)));
                }

                SwipeRefreshLayout swipeContainer = ((Activity) context).findViewById(R.id.swipeContainer);
                if (swipeContainer != null) {
                    swipeContainer.setRefreshing(false);
                }
            }
        }
    }

    private static class ExportDomainsAsyncTask extends AsyncTask<Void, Void, Void> {

        private final WeakReference<Context> contextReference;

        ExportDomainsAsyncTask(Context context) {
            this.contextReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            List<ReportBlockedUrl> domains = FirewallUtils.getInstance().getReportBlockedUrl();
            try {
                Set<String> set = new HashSet<>();
                for (ReportBlockedUrl domain : domains) {
                    set.add(domain.url);
                }

                File file = new File(Environment.getExternalStorageDirectory(), "adhell_exported_domains.txt");
                FileWriter writer = new FileWriter(file);
                for (String domain : set) {
                    writer.write(domain);
                    writer.write(System.lineSeparator());
                }
                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Context context = contextReference.get();
            if (context != null) {
                Toast.makeText(context, "Blocked domains have been exported!", Toast.LENGTH_LONG).show();
            }
        }
    }
}
