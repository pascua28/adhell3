package com.fusionjack.adhell3.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.AbsListView;
import android.widget.ExpandableListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.preference.PreferenceManager;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.ReportBlockedUrlAdapter;
import com.fusionjack.adhell3.blocker.ContentBlocker;
import com.fusionjack.adhell3.blocker.ContentBlocker56;
import com.fusionjack.adhell3.databinding.DialogWhitelistDomainBinding;
import com.fusionjack.adhell3.databinding.FragmentBlockerBinding;
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
import com.leinardi.android.speeddial.SpeedDialActionItem;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
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
    private ContentBlocker contentBlocker;
    private FragmentBlockerBinding binding;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentManager = requireActivity().getSupportFragmentManager();
        contentBlocker = ContentBlocker56.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        requireActivity().setTitle(R.string.app_name);
        setHasOptionsMenu(true);

        binding = FragmentBlockerBinding.inflate(inflater);

        binding.infoTextView.setVisibility(View.INVISIBLE);
        binding.swipeContainer.setVisibility(View.INVISIBLE);
        binding.loadingBar.setVisibility(View.INVISIBLE);

        if (!BuildConfig.DISABLE_APPS) {
            binding.appDisablerSwitch.setEnabled(false);
        }
        if (!BuildConfig.APP_COMPONENT) {
            binding.appComponentSwitch.setEnabled(false);
        }

        binding.domainRulesSwitch.setOnClickListener(v -> {
            LogUtils.info("Domain switch button has been clicked");
            new SetFirewallAsyncTask(true, this, fragmentManager, getContext(), false).execute();
        });
        binding.domainRulesSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.domainInfoTextView.setVisibility(View.VISIBLE);
            } else {
                binding.domainInfoTextView.setVisibility(View.GONE);
            }
        });
        binding.firewallRulesSwitch.setOnClickListener(v -> {
            LogUtils.info("Firewall switch button has been clicked");
            new SetFirewallAsyncTask(false, this, fragmentManager, getContext(), false).execute();
        });
        binding.firewallRulesSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.firewallInfoTextView.setVisibility(View.VISIBLE);
            } else {
                binding.firewallInfoTextView.setVisibility(View.GONE);
            }
        });
        binding.appDisablerSwitch.setOnClickListener(v -> {
            LogUtils.info("App disabler switch button has been clicked");
            new AppDisablerAsyncTask(this, getContext()).execute();
        });
        binding.appDisablerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.disablerInfoTextView.setVisibility(View.VISIBLE);
            } else {
                binding.disablerInfoTextView.setVisibility(View.GONE);
            }
        });
        binding.appComponentSwitch.setOnClickListener(v -> {
            LogUtils.info("App component switch button has been clicked");
            new AppComponentAsyncTask(this, getContext()).execute();
        });
        binding.appComponentSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.appComponentInfoTextView.setVisibility(View.VISIBLE);
            } else {
                binding.appComponentInfoTextView.setVisibility(View.GONE);
            }
        });

        binding.domainActions.addActionItem(new SpeedDialActionItem.Builder(R.id.action_export_domains, ResourcesCompat.getDrawable(getResources(), R.drawable.ic_export, requireContext().getTheme()))
                .setLabel(getString(R.string.export_domains_title))
                .setFabBackgroundColor(getResources().getColor(R.color.colorFab, requireContext().getTheme()))
                .setLabelColor(getResources().getColor(R.color.colorText, requireContext().getTheme()))
                .setLabelBackgroundColor(getResources().getColor(R.color.colorBorder, requireContext().getTheme()))
                .setFabSize(FloatingActionButton.SIZE_NORMAL)
                .setFabImageTintColor(Color.WHITE)
                .setLabelClickable(false)
                .create());

        binding.domainActions.setOnActionSelectedListener(actionItem -> {
            if (actionItem.getId() == R.id.action_export_domains) {
                binding.domainActions.close();
                new ExportDomainsAsyncTask(getContext()).execute();
                return true;
            }
            return false;
        });

        final boolean[] noScroll = { false };
        final int[] previousDistanceFromFirstCellToTop = {0};
        binding.blockedDomainsListView.setOnScrollListener(new ExpandableListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL && noScroll[0]) {
                    if (binding.domainActions.isShown()) binding.domainActions.hide();
                    else binding.domainActions.show();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem == 0 && visibleItemCount == totalItemCount) {
                    noScroll[0] = true;
                } else {
                    noScroll[0] = false;
                    View firstCell = binding.blockedDomainsListView.getChildAt(0);
                    if (firstCell == null) {
                        return;
                    }
                    int distanceFromFirstCellToTop = firstVisibleItem * firstCell.getHeight() - firstCell.getTop();
                    if (distanceFromFirstCellToTop < previousDistanceFromFirstCellToTop[0]) {
                        binding.domainActions.show();
                    } else if (distanceFromFirstCellToTop > previousDistanceFromFirstCellToTop[0]) {
                        binding.domainActions.hide();
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
        binding.appComponentStatusTextView.setOnClickListener(appComponentDisabledOnClickListener);
        binding.appComponentInfoTextView.setOnClickListener(appComponentDisabledOnClickListener);

        if (!contentBlocker.isDomainRuleEmpty()) {
            binding.infoTextView.setVisibility(View.VISIBLE);
            binding.swipeContainer.setVisibility(View.VISIBLE);
            binding.swipeContainer.setOnRefreshListener(() -> {
                binding.loadingBar.setVisibility(View.VISIBLE);
                new RefreshAsyncTask(getContext(), binding).execute();
            });

            binding.loadingBar.setVisibility(View.VISIBLE);
            new RefreshAsyncTask(getContext(), binding).execute();
        } else {
            binding.infoTextView.setVisibility(View.INVISIBLE);
            binding.swipeContainer.setVisibility(View.INVISIBLE);
        }
        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUserInterface();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        if (binding.domainRulesSwitch.isChecked() || binding.firewallRulesSwitch.isChecked()) {
            inflater.inflate(R.menu.home_menu, menu);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.refresh) {
            new SetFirewallAsyncTask(true, this, fragmentManager, getContext(), true).execute();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void updateUserInterface() {
        boolean isDomainRuleEmpty = contentBlocker.isDomainRuleEmpty();
        boolean isFirewallRuleEmpty = contentBlocker.isFirewallRuleEmpty();

        binding.domainRulesSwitch.setChecked(contentBlocker != null && !isDomainRuleEmpty);

        binding.firewallRulesSwitch.setChecked(contentBlocker != null && !isFirewallRuleEmpty);

        if (!isDomainRuleEmpty) {
            binding.infoTextView.setVisibility(View.VISIBLE);
            binding.swipeContainer.setVisibility(View.VISIBLE);
            binding.swipeContainer.setOnRefreshListener(() -> {
                binding.loadingBar.setVisibility(View.VISIBLE);
                new RefreshAsyncTask(getContext(), binding).execute();
            });

            binding.loadingBar.setVisibility(View.VISIBLE);
            new RefreshAsyncTask(getContext(), binding).execute();
        } else {
            binding.infoTextView.setVisibility(View.INVISIBLE);
            binding.swipeContainer.setVisibility(View.INVISIBLE);
        }

        boolean disablerEnabled = AppPreferences.getInstance().isAppDisablerToggleEnabled();
        binding.appDisablerSwitch.setChecked(disablerEnabled);

        boolean appComponentEnabled = AppPreferences.getInstance().isAppComponentToggleEnabled();
        binding.appComponentSwitch.setChecked(appComponentEnabled);

        FragmentActivity parentActivity = getActivity();
        if (parentActivity != null) {
            parentActivity.invalidateOptionsMenu();
        }

        new SetInfoAsyncTask(getContext(), binding).execute();
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
        private final FragmentBlockerBinding binding;
        private final boolean appDisablerEnabled = AppPreferences.getInstance().isAppDisablerToggleEnabled();
        private final boolean appComponentEnabled = AppPreferences.getInstance().isAppComponentToggleEnabled();

        SetInfoAsyncTask(Context context, FragmentBlockerBinding binding) {
            this.contextWeakReference = new WeakReference<>(context);
            this.binding = binding;
        }

        @Override
        protected void onPreExecute() {
            Context context = contextWeakReference.get();
            if (context != null) {
                String domainInfo = context.getResources().getString(R.string.domain_rules_info);
                binding.domainInfoTextView.setText(String.format(domainInfo, 0, 0, 0));

                String firewallInfo = context.getResources().getString(R.string.firewall_rules_info);
                binding.firewallInfoTextView.setText(String.format(firewallInfo, 0, 0, 0));

                if (appDisablerEnabled) {
                    String disablerInfo = context.getResources().getString(R.string.app_disabler_info);
                    binding.disablerInfoTextView.setText(String.format(disablerInfo, 0));
                }

                if (appComponentEnabled) {
                    String appComponentInfo = context.getResources().getString(R.string.app_component_toggle_info);
                    binding.appComponentInfoTextView.setText(String.format(appComponentInfo, 0, 0, 0, 0));
                }
            }
        }

        @Override
        protected Void doInBackground(Void... voids) {
            if (appDisablerEnabled) {
                AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
                disablerSize = appDatabase.disabledPackageDao().getAll().size();
            }

            if (appComponentEnabled) {
                AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
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
                String domainInfo = context.getResources().getString(R.string.domain_rules_info);
                binding.domainInfoTextView.setText(String.format(domainInfo, blackListSize, whiteListSize, whitelistAppSize));

                String firewallInfo = context.getResources().getString(R.string.firewall_rules_info);
                binding.firewallInfoTextView.setText(String.format(firewallInfo, mobileSize, wifiSize, customSize));

                if (appDisablerEnabled) {
                    String disablerInfo = context.getResources().getString(R.string.app_disabler_info);
                    binding.disablerInfoTextView.setText(String.format(disablerInfo, disablerSize));
                }

                if (appComponentEnabled) {
                    String appComponentInfo = context.getResources().getString(R.string.app_component_toggle_info);
                    binding.appComponentInfoTextView.setText(String.format(appComponentInfo, permissionSize, serviceSize, receiverSize, activitySize));
                }
            }
        }
    }

    private static class AppDisablerAsyncTask extends AsyncTask<Void, Void, Void> {
        private final HomeTabFragment parentFragment;
        private final AlertDialog dialog;
        private final boolean enabled;

        AppDisablerAsyncTask(HomeTabFragment parentFragment, Context context) {
            this.parentFragment = parentFragment;

            this.enabled = AppPreferences.getInstance().isAppDisablerToggleEnabled();
            String message = enabled ? "Enabling apps..." : "Disabling apps...";
            this.dialog = DialogUtils.getProgressDialog(message, context);
        }

        @Override
        protected void onPreExecute() {
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
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

            if (doRefresh)
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
        private final FragmentBlockerBinding binding;

        RefreshAsyncTask(Context context, FragmentBlockerBinding binding) {
            this.contextReference = new WeakReference<>(context);
            this.binding = binding;
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
            linkedList.sort((list1, list2) ->
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
                    ReportBlockedUrlAdapter adapter = new ReportBlockedUrlAdapter(context, reportBlockedUrls, null);
                    binding.blockedDomainsListView.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                    binding.blockedDomainsListView.setOnChildClickListener((ExpandableListView parent, View view, int groupPosition, int childPosition, long id) -> {
                        DialogWhitelistDomainBinding dialogWhitelistDomainBinding = DialogWhitelistDomainBinding.inflate(LayoutInflater.from(context));
                        List<String> groupList = new ArrayList<>(reportBlockedUrls.keySet());
                        String blockedPackageName = Objects.requireNonNull(reportBlockedUrls.get(groupList.get(groupPosition))).get(childPosition).packageName;
                        String blockedUrl = Objects.requireNonNull(reportBlockedUrls.get(groupList.get(groupPosition))).get(childPosition).url;
                        dialogWhitelistDomainBinding.domainEditText.setText(String.format("%s|%s", blockedPackageName, blockedUrl));
                        AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                                .setView(dialogWhitelistDomainBinding.getRoot())
                                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                                    String domainToAdd = dialogWhitelistDomainBinding.domainEditText.getText().toString().trim();
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

                    int totalCount = 0;
                    for (HashMap.Entry<String, List<ReportBlockedUrl>> entry : reportBlockedUrls.entrySet()) {
                        totalCount += entry.getValue().size();
                    }
                    binding.infoTextView.setText(String.format("%s%s",
                            context.getString(R.string.last_day_blocked), totalCount));

            }
                binding.swipeContainer.setRefreshing(false);
                binding.loadingBar.setVisibility(View.GONE);

                if (binding.blockedDomainsListView.getVisibility() == View.GONE) {
                    AlphaAnimation animation = new AlphaAnimation(0f, 1f);
                    animation.setDuration(500);
                    animation.setStartOffset(50);
                    animation.setFillAfter(true);

                    binding.blockedDomainsListView.setVisibility(View.VISIBLE);
                    binding.blockedDomainsListView.startAnimation(animation);
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
        return binding.domainRulesSwitch.isChecked();
    }

    public boolean getFirewallSwitchState() {
        return binding.firewallRulesSwitch.isChecked();
    }
}
