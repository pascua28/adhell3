package com.fusionjack.adhell3.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.MainActivity;
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
import com.fusionjack.adhell3.utils.BlockUrlPatternsMatch;
import com.fusionjack.adhell3.utils.DialogUtils;
import com.fusionjack.adhell3.utils.FileUtils;
import com.fusionjack.adhell3.utils.FirewallUtils;
import com.fusionjack.adhell3.utils.LogUtils;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.leinardi.android.speeddial.SpeedDialActionItem;
import com.leinardi.android.speeddial.SpeedDialView;

import java.io.IOException;
import java.io.OutputStream;
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
import java.util.StringTokenizer;

public class HomeTabFragment extends Fragment {

    private static final String STORAGE_FOLDERS = "Adhell3/Exports";
    private static final String EXPORTED_DOMAINS_FILENAME = "adhell_exported_domains.txt";

    private FragmentManager fragmentManager;
    private AppCompatActivity parentActivity;
    private TextView domainStatusTextView;
    private SwitchMaterial domainSwitch;
    private TextView firewallStatusTextView;
    private SwitchMaterial firewallSwitch;
    private TextView disablerStatusTextView;
    private SwitchMaterial disablerSwitch;
    private TextView appComponentStatusTextView;
    private SwitchMaterial appComponentSwitch;
    private TextView infoTextView;
    private SwipeRefreshLayout swipeContainer;
    private ContentBlocker contentBlocker;
    private ProgressBar loadingBar;
    private ImageView refreshButton;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentManager = requireActivity().getSupportFragmentManager();
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
                String versionInfo = requireContext().getResources().getString(R.string.version);
                subtitleTextView.setText(String.format(versionInfo, BuildConfig.VERSION_NAME));
            }
            this.refreshButton = view.findViewById(R.id.refreshButton);
            int themeColor = getResources().getColor(R.color.colorBottomNavUnselected, requireContext().getTheme());
            refreshButton.setColorFilter(themeColor, PorterDuff.Mode.SRC_IN);
            refreshButton.setOnClickListener(v -> new SetFirewallAsyncTask(true, this, fragmentManager, getContext(), true).execute());
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
        loadingBar = view.findViewById(R.id.loadingBar);

        infoTextView.setVisibility(View.INVISIBLE);
        swipeContainer.setVisibility(View.INVISIBLE);
        loadingBar.setVisibility(View.INVISIBLE);

        if (!BuildConfig.DISABLE_APPS) {
            view.findViewById(R.id.appDisablerLayout).setVisibility(View.GONE);
        }
        if (!BuildConfig.APP_COMPONENT) {
            view.findViewById(R.id.appComponentLayout).setVisibility(View.GONE);
        }

        domainSwitch.setOnClickListener(v -> {
            LogUtils.info("Domain switch button has been clicked");
            new SetFirewallAsyncTask(true, this, fragmentManager, getContext(), false).execute();
        });
        firewallSwitch.setOnClickListener(v -> {
            LogUtils.info("Firewall switch button has been clicked");
            new SetFirewallAsyncTask(false, this, fragmentManager, getContext(), false).execute();
        });
        disablerSwitch.setOnClickListener(v -> {
            LogUtils.info("App disabler switch button has been clicked");
            new AppDisablerAsyncTask(this, getContext()).execute();
        });
        appComponentSwitch.setOnClickListener(v -> {
            LogUtils.info("App component switch button has been clicked");
            new AppComponentAsyncTask(this, getContext()).execute();
        });

        SpeedDialView speedDialView = view.findViewById(R.id.domain_actions);
        speedDialView.addActionItem(new SpeedDialActionItem.Builder(R.id.action_export_domains, ResourcesCompat.getDrawable(getResources(), R.drawable.ic_export, requireContext().getTheme()))
                .setLabel(getString(R.string.export_domains_title))
                .setFabBackgroundColor(getResources().getColor(R.color.colorFab, requireContext().getTheme()))
                .setLabelColor(getResources().getColor(R.color.colorText, requireContext().getTheme()))
                .setLabelBackgroundColor(getResources().getColor(R.color.colorBorder, requireContext().getTheme()))
                .setFabSize(FloatingActionButton.SIZE_NORMAL)
                .setFabImageTintColor(Color.WHITE)
                .setLabelClickable(false)
                .create());

        speedDialView.setOnActionSelectedListener(actionItem -> {
            if (actionItem.getId() == R.id.action_export_domains) {
                speedDialView.close();
                new ExportDomainsAsyncTask(getContext()).execute();
                return true;
            }
            return false;
        });

        final boolean[] noScroll = { false };
        final int[] previousDistanceFromFirstCellToTop = {0};
        final ExpandableListView expandableListView = view.findViewById(R.id.blockedDomainsListView);
        expandableListView.setOnScrollListener(new ExpandableListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL && noScroll[0]) {
                    if (speedDialView.isShown()) speedDialView.hide();
                    else speedDialView.show();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem == 0 && visibleItemCount == totalItemCount) {
                    noScroll[0] = true;
                } else {
                    noScroll[0] = false;
                    View firstCell = expandableListView.getChildAt(0);
                    if (firstCell == null) {
                        return;
                    }
                    int distanceFromFirstCellToTop = firstVisibleItem * firstCell.getHeight() - firstCell.getTop();
                    if (distanceFromFirstCellToTop < previousDistanceFromFirstCellToTop[0]) {
                        speedDialView.show();
                    } else if (distanceFromFirstCellToTop > previousDistanceFromFirstCellToTop[0]) {
                        speedDialView.hide();
                    }
                    previousDistanceFromFirstCellToTop[0] = distanceFromFirstCellToTop;
                }
            }
        });

        AsyncTask.execute(() -> {
            AdhellAppIntegrity adhellAppIntegrity = AdhellAppIntegrity.getInstance();
            adhellAppIntegrity.checkDefaultPolicyExists();
            adhellAppIntegrity.checkAdhellStandardPackage();
            adhellAppIntegrity.fillPackageDb();
        });

        View.OnClickListener appComponentDisabledOnClickListener = v -> {
            if (getActivity() != null) {
                    AdhellFactory.getInstance().showAppComponentDisabledFragment(getActivity().getSupportFragmentManager());
            }};
        appComponentStatusTextView.setOnClickListener(appComponentDisabledOnClickListener);
        TextView appComponentInfoTextView = view.findViewById(R.id.appComponentInfoTextView);
        appComponentInfoTextView.setOnClickListener(appComponentDisabledOnClickListener);

        if (!contentBlocker.isDomainRuleEmpty()) {
            infoTextView.setVisibility(View.VISIBLE);
            swipeContainer.setVisibility(View.VISIBLE);
            swipeContainer.setOnRefreshListener(() -> {
                if (loadingBar != null) {
                    loadingBar.setVisibility(View.VISIBLE);
                }
                new RefreshAsyncTask(getContext()).execute();
            });

            if (loadingBar != null) {
                loadingBar.setVisibility(View.VISIBLE);
            }
            new RefreshAsyncTask(getContext()).execute();
        } else {
            infoTextView.setVisibility(View.INVISIBLE);
            swipeContainer.setVisibility(View.INVISIBLE);
        }


        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUserInterface();
    }

    private void updateUserInterface() {
        new SetInfoAsyncTask(getContext(), refreshButton).execute();

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
            swipeContainer.setOnRefreshListener(() -> {
                if (loadingBar != null) {
                    loadingBar.setVisibility(View.VISIBLE);
                }
                new RefreshAsyncTask(getContext()).execute();
            });

            if (loadingBar != null) {
                loadingBar.setVisibility(View.VISIBLE);
            }
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

        if (((domainSwitch.isChecked() || firewallSwitch.isChecked())))  {
            refreshButton.setVisibility(View.VISIBLE);
        } else {
            refreshButton.setVisibility(View.GONE);
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
        private int activitySize;
        private boolean isCurrentDomainLimitAboveDefault;
        private final WeakReference<ImageView> refreshButton;

        SetInfoAsyncTask(Context context, ImageView refreshButton) {
            this.contextWeakReference = new WeakReference<>(context);
            this.refreshButton = new WeakReference<>(refreshButton);
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
                    appComponentInfoTextView.setText(String.format(appComponentInfo, 0, 0, 0, 0));
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
                    case AppPermission.STATUS_ACTIVITY:
                        activitySize++;
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

            isCurrentDomainLimitAboveDefault = FirewallUtils.getInstance().isCurrentDomainLimitAboveDefault();

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
                        info = String.format(appComponentInfo, permissionSize, serviceSize, receiverSize, activitySize);
                    } else {
                        info = String.format(appComponentInfo, 0, 0, 0, 0);
                    }
                    appComponentInfoTextView.setText(info);
                }

                SwitchMaterial domainRulesSwitch = ((Activity) context).findViewById(R.id.domainRulesSwitch);
                SwitchMaterial firewallRulesSwitch = ((Activity) context).findViewById(R.id.firewallRulesSwitch);
                if (domainRulesSwitch != null && firewallRulesSwitch != null) {
                    if (!isCurrentDomainLimitAboveDefault && ((domainRulesSwitch.isChecked() || firewallRulesSwitch.isChecked()))) {
                        refreshButton.get().setVisibility(View.VISIBLE);
                    } else {
                        refreshButton.get().setVisibility(View.GONE);
                    }
                }
            }
        }
    }

    private static class AppDisablerAsyncTask extends AsyncTask<Void, Void, Void> {
        private final HomeTabFragment parentFragment;
        private final AlertDialog dialog;

        AppDisablerAsyncTask(HomeTabFragment parentFragment, Context context) {
            this.parentFragment = parentFragment;

            boolean enabled = AppPreferences.getInstance().isAppDisablerToggleEnabled();
            String message = enabled ? "Enabling apps..." : "Disabling apps...";
            this.dialog = DialogUtils.getProgressDialog(message, context);
        }

        @Override
        protected void onPreExecute() {
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
        private final AlertDialog dialog;

        AppComponentAsyncTask(HomeTabFragment parentFragment, Context context) {
            this.parentFragment = parentFragment;

            boolean enabled = AppPreferences.getInstance().isAppComponentToggleEnabled();
            String message = enabled ? "Enabling app component..." : "Disabling app component...";
            this.dialog = DialogUtils.getProgressDialog(message, context);
        }

        @Override
        protected void onPreExecute() {
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
        private final boolean doRefresh;

        SetFirewallAsyncTask(boolean isDomain, HomeTabFragment parentFragment, FragmentManager fragmentManager, Context context, boolean doRefresh) {
            this.isDomain = isDomain;
            this.parentFragment = parentFragment;
            this.fragmentManager = fragmentManager;
            this.contentBlocker = ContentBlocker56.getInstance();
            this.isDomainRuleEmpty = contentBlocker.isDomainRuleEmpty();
            this.isFirewallRuleEmpty = contentBlocker.isFirewallRuleEmpty();
            this.contextReference = new WeakReference<>(context);
            this.doRefresh = doRefresh;

            this.handler = new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(@NonNull Message msg) {
                    fragment.appendText(msg.obj.toString());
                }
            };
        }

        @Override
        protected void onPreExecute() {
            if (doRefresh) {
                fragment = FirewallDialogFragment.newInstance("Updating all Rules");
            } else {
                if (isDomain) {
                    fragment = FirewallDialogFragment.newInstance(
                            (isDomainRuleEmpty) ? "Enabling Domain Rules" : "Disabling Domain Rules");
                } else {
                    fragment = FirewallDialogFragment.newInstance(
                            isFirewallRuleEmpty ? "Enabling Firewall Rules" : "Disabling Firewall Rules");
                }
            }
            fragment.setCancelable(false);
            fragment.show(fragmentManager, "dialog_firewall");
        }

        @Override
        protected Void doInBackground(Void... args) {
            contentBlocker.setHandler(handler);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(contextReference.get());
            boolean updateProviders = preferences.getBoolean(SettingsFragment.UPDATE_PROVIDERS_PREFERENCE, false);
            if (!AdhellFactory.getInstance().hasInternetAccess(contextReference.get())) {
                updateProviders = false;
            }

            boolean isCurrentDomainLimitAboveDefault = FirewallUtils.getInstance().isCurrentDomainLimitAboveDefault();

            if (doRefresh && !isCurrentDomainLimitAboveDefault)
                contentBlocker.updateAllRules(updateProviders, parentFragment);
            else {
                if (isDomain) {
                    if (isDomainRuleEmpty) {
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

                    ReportBlockedUrlAdapter adapter = new ReportBlockedUrlAdapter(context, reportBlockedUrls, null);
                    listView.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                    listView.setOnChildClickListener((ExpandableListView parent, View view, int groupPosition, int childPosition, long id) -> {
                        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_whitelist_domain, listView, false);
                        EditText domainEditText = dialogView.findViewById(R.id.domainEditText);
                        List<String> groupList = new ArrayList<>(reportBlockedUrls.keySet());
                        String blockedPackageName = Objects.requireNonNull(reportBlockedUrls.get(groupList.get(groupPosition))).get(childPosition).packageName;
                        String blockedUrl = Objects.requireNonNull(reportBlockedUrls.get(groupList.get(groupPosition))).get(childPosition).url;
                        domainEditText.setText(String.format("%s|%s", blockedPackageName, blockedUrl));
                        AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                                .setView(dialogView)
                                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                                    String domainToAdd = domainEditText.getText().toString().trim();
                                    if (domainToAdd.indexOf('|') == -1) {
                                        if (!BlockUrlPatternsMatch.isUrlValid(domainToAdd)) {
                                            MainActivity.makeSnackbar("Url not valid. Please check", Snackbar.LENGTH_SHORT)
                                                    .show();
                                            return;
                                        }
                                    } else {
                                        // packageName|url
                                        StringTokenizer tokens = new StringTokenizer(domainToAdd, "|");
                                        if (tokens.countTokens() != 2) {
                                            MainActivity.makeSnackbar("Rule not valid. Please check", Snackbar.LENGTH_SHORT)
                                                    .show();
                                            return;
                                        }
                                    }
                                    WhiteUrl whiteUrl = new WhiteUrl(domainToAdd, new Date());
                                    AsyncTask.execute(() -> AdhellFactory.getInstance().getAppDatabase().whiteUrlDao().insert(whiteUrl));
                                    MainActivity.makeSnackbar("Domain whitelist has been added", Snackbar.LENGTH_SHORT)
                                            .show();
                                })
                                .setNegativeButton(android.R.string.no, null)
                                .create();

                        alertDialog.show();

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
                            context.getString(R.string.last_day_blocked), totalCount));
                }

                SwipeRefreshLayout swipeContainer = ((Activity) context).findViewById(R.id.swipeContainer);
                if (swipeContainer != null) {
                    swipeContainer.setRefreshing(false);
                }

                ProgressBar loadingBar = ((Activity) context).findViewById(R.id.loadingBar);
                if (loadingBar != null) {
                    loadingBar.setVisibility(View.GONE);
                }
                if (listView != null && listView.getVisibility() == View.GONE) {
                    AlphaAnimation animation = new AlphaAnimation(0f, 1f);
                    animation.setDuration(500);
                    animation.setStartOffset(50);
                    animation.setFillAfter(true);

                    listView.setVisibility(View.VISIBLE);
                    listView.startAnimation(animation);
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

                DocumentFile file = FileUtils.getDocumentFile(STORAGE_FOLDERS, EXPORTED_DOMAINS_FILENAME, FileUtils.FileCreationType.ALWAYS);
                if (file != null) {
                    OutputStream out = contextReference.get().getContentResolver().openOutputStream(file.getUri());
                    if (out != null) {
                        for (String domain : set) {
                            try {
                                out.write(String.format("%s%s", domain, System.lineSeparator()).getBytes());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        out.flush();
                        out.close();
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Context context = contextReference.get();
            if (context != null) {
                    MainActivity.makeSnackbar("Blocked domains have been exported!", Snackbar.LENGTH_LONG)
                        .show();
            }
        }
    }

    public boolean getDomainSwitchState() {
        return domainSwitch.isChecked();
    }

    public boolean getFirewallSwitchState() {
        return firewallSwitch.isChecked();
    }
}
