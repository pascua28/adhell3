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
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.ViewModelProvider;
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
import com.fusionjack.adhell3.db.entity.ReportBlockedUrl;
import com.fusionjack.adhell3.db.entity.WhiteUrl;
import com.fusionjack.adhell3.dialogfragment.FirewallDialogFragment;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppCache;
import com.fusionjack.adhell3.utils.AppCacheChangeListener;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.BlockUrlPatternsMatch;
import com.fusionjack.adhell3.utils.DialogUtils;
import com.fusionjack.adhell3.utils.FileUtils;
import com.fusionjack.adhell3.utils.FirewallUtils;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.viewmodel.HomeTabViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.leinardi.android.speeddial.SpeedDialActionItem;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.StringTokenizer;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class HomeTabFragment extends Fragment implements DefaultLifecycleObserver, AppCacheChangeListener {
    private static final String STORAGE_FOLDERS = "Adhell3/Exports";
    private static final String EXPORTED_DOMAINS_FILENAME = "adhell_exported_domains.txt";
    private HomeTabViewModel homeTabViewModel;
    private FragmentManager fragmentManager;
    private FragmentBlockerBinding binding;

    private boolean refreshRunning = false;

    private boolean domainRulesEnabled;
    private boolean firewallRulesEnabled;
    private boolean disablerEnabled;
    private boolean appComponentEnabled;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentManager = getParentFragmentManager();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        requireActivity().setTitle(R.string.app_name);
        setHasOptionsMenu(true);

        homeTabViewModel = new ViewModelProvider(getActivity() != null ? getActivity() : this).get(HomeTabViewModel.class);
        FragmentActivity activity = getActivity();
        if (activity != null) {
            AppCompatActivity parentActivity = (AppCompatActivity) activity;
            if (parentActivity.getSupportActionBar() != null) {
                parentActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
                String versionInfo = getResources().getString(R.string.version);
                parentActivity.getSupportActionBar().setSubtitle(String.format(versionInfo, BuildConfig.VERSION_NAME));
            }
        }

        binding = FragmentBlockerBinding.inflate(inflater);

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        homeTabViewModel.getBlockedDomainInfo(getContext()).observe(
                getViewLifecycleOwner(),
                blockedDomainInfo -> {
                    if (blockedDomainInfo.isEmpty()) {
                        binding.infoTextView.setVisibility(View.INVISIBLE);
                        binding.swipeContainer.setVisibility(View.INVISIBLE);
                        binding.domainActions.setVisibility(View.INVISIBLE);
                    } else {
                        binding.infoTextView.setText(blockedDomainInfo);
                        binding.infoTextView.setVisibility(View.VISIBLE);
                        binding.swipeContainer.setVisibility(View.VISIBLE);
                        binding.domainActions.setVisibility(View.VISIBLE);
                    }
                }
        );
        binding.swipeContainer.setOnRefreshListener(() -> homeTabViewModel.refreshBlockedUrls());

        homeTabViewModel.getLoadingBarVisibility().observe(
                getViewLifecycleOwner(),
                isVisible -> {
                    if (isVisible) {
                        boolean swipeContainerIsVisible = binding.swipeContainer.getVisibility() == View.VISIBLE;
                        if (!binding.swipeContainer.isRefreshing() && swipeContainerIsVisible)  {
                            binding.loadingBar.setVisibility(View.VISIBLE);
                        } else if (!swipeContainerIsVisible) {
                            binding.loadingBar.setVisibility(View.GONE);
                        }

                        if (binding.blockedDomainsListView.getVisibility() == View.VISIBLE) {
                            binding.blockedDomainsListView.setVisibility(View.GONE);
                        }
                    } else {
                        binding.loadingBar.setVisibility(View.GONE);
                        binding.swipeContainer.setRefreshing(false);

                        if (binding.blockedDomainsListView.getVisibility() == View.GONE) {
                            binding.blockedDomainsListView.setVisibility(View.VISIBLE);
                        }
                    }
                }
        );

        homeTabViewModel.getReportBlockedUrls().observe(getViewLifecycleOwner(), reportBlockedUrls -> {
            if (AppCache.getAppsIsCached()) {
                ExpandableListAdapter adapter = binding.blockedDomainsListView.getExpandableListAdapter();
                Context context = getContext();
                if (adapter == null && context != null) {
                    ReportBlockedUrlAdapter arrayAdapter = new ReportBlockedUrlAdapter(context, reportBlockedUrls);
                    binding.blockedDomainsListView.setAdapter(arrayAdapter);
                    arrayAdapter.notifyDataSetChanged();
                } else if (binding.blockedDomainsListView.getExpandableListAdapter() != null && binding.blockedDomainsListView.getExpandableListAdapter() instanceof ReportBlockedUrlAdapter) {
                    ReportBlockedUrlAdapter reportBlockedUrlAdapter = ((ReportBlockedUrlAdapter) binding.blockedDomainsListView.getExpandableListAdapter());
                    reportBlockedUrlAdapter.updateReportBlockedUrlMap(reportBlockedUrls);
                    reportBlockedUrlAdapter.notifyDataSetChanged();
                }
                binding.blockedDomainsListView.setOnChildClickListener((ExpandableListView parent, View childView, int groupPosition, int childPosition, long id) -> {
                    DialogWhitelistDomainBinding dialogWhitelistDomainBinding = DialogWhitelistDomainBinding.inflate(LayoutInflater.from(context));
                    List<String> groupList = new ArrayList<>(reportBlockedUrls.keySet());
                    String blockedPackageName = Objects.requireNonNull(reportBlockedUrls.get(groupList.get(groupPosition))).get(childPosition).packageName;
                    String blockedUrl = Objects.requireNonNull(reportBlockedUrls.get(groupList.get(groupPosition))).get(childPosition).url;
                    dialogWhitelistDomainBinding.domainEditText.setText(String.format("%s|%s", blockedPackageName, blockedUrl));
                    if (context != null && dialogWhitelistDomainBinding.domainEditText.getText() != null) {
                        AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                                .setView(dialogWhitelistDomainBinding.getRoot())
                                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                                    String domainToAdd = dialogWhitelistDomainBinding.domainEditText.getText().toString().trim();
                                    if (domainToAdd.indexOf('|') == -1) {
                                        if (!BlockUrlPatternsMatch.isUrlValid(domainToAdd)) {
                                            if (context instanceof MainActivity) {
                                                MainActivity mainActivity = (MainActivity) context;
                                                mainActivity.makeSnackbar("Url not valid. Please check", Snackbar.LENGTH_SHORT)
                                                        .show();
                                            }
                                            return;
                                        }
                                    } else {
                                        // packageName|url
                                        StringTokenizer tokens = new StringTokenizer(domainToAdd, "|");
                                        if (tokens.countTokens() != 2) {
                                            if (context instanceof MainActivity) {
                                                MainActivity mainActivity = (MainActivity) context;
                                                mainActivity.makeSnackbar("Rule not valid. Please check", Snackbar.LENGTH_SHORT)
                                                        .show();
                                            }
                                            return;
                                        }
                                    }
                                    addBlockedUrlToWhitelist(domainToAdd);
                                    if (context instanceof MainActivity) {
                                        MainActivity mainActivity = (MainActivity) context;
                                        mainActivity.makeSnackbar("Domain whitelist has been added", Snackbar.LENGTH_SHORT)
                                                .show();
                                    }
                                })
                                .setNegativeButton(android.R.string.no, null)
                                .create();
                        alertDialog.show();
                    }
                    return false;
                });

                homeTabViewModel.updateBlockedDomainInfo();
            }
        });

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
            domainRulesEnabled = isChecked;
            if (isChecked) {
                binding.domainInfoTextView.setVisibility(View.VISIBLE);
            } else {
                binding.domainInfoTextView.setVisibility(View.GONE);
            }
            invalidateOptionsMenu();
        });
        homeTabViewModel.getDomainInfo(getResources().getString(R.string.domain_rules_info)).observe(
                getViewLifecycleOwner(),
                binding.domainInfoTextView::setText
        );

        binding.firewallRulesSwitch.setOnClickListener(v -> {
            LogUtils.info("Firewall switch button has been clicked");
            new SetFirewallAsyncTask(false, this, fragmentManager, getContext(), false).execute();
        });
        binding.firewallRulesSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            firewallRulesEnabled = isChecked;
            if (isChecked) {
                binding.firewallInfoTextView.setVisibility(View.VISIBLE);
            } else {
                binding.firewallInfoTextView.setVisibility(View.GONE);
            }
            invalidateOptionsMenu();
        });
        homeTabViewModel.getFirewallInfo(getResources().getString(R.string.firewall_rules_info)).observe(
                getViewLifecycleOwner(),
                binding.firewallInfoTextView::setText
        );

        binding.appDisablerSwitch.setOnClickListener(v -> {
            LogUtils.info("App disabler switch button has been clicked");
            new AppDisablerAsyncTask(disablerEnabled, this, getContext()).execute();
        });
        binding.appDisablerSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            disablerEnabled = isChecked;
            if (isChecked) {
                binding.disablerInfoTextView.setVisibility(View.VISIBLE);
            } else {
                binding.disablerInfoTextView.setVisibility(View.GONE);
            }
        });
        homeTabViewModel.getDisablerInfo(getResources().getString(R.string.app_disabler_info)).observe(
                getViewLifecycleOwner(),
                binding.disablerInfoTextView::setText
        );

        binding.appComponentSwitch.setOnClickListener(v -> {
            LogUtils.info("App component switch button has been clicked");
            new AppComponentAsyncTask(appComponentEnabled, this, getContext()).execute();
        });
        binding.appComponentSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            appComponentEnabled = isChecked;
            if (isChecked) {
                binding.appComponentInfoTextView.setVisibility(View.VISIBLE);
            } else {
                binding.appComponentInfoTextView.setVisibility(View.GONE);
            }
        });
        homeTabViewModel.getAppComponentInfo(getResources().getString(R.string.app_component_toggle_info)).observe(
                getViewLifecycleOwner(),
                binding.appComponentInfoTextView::setText
        );

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

        View.OnClickListener appComponentDisabledOnClickListener = v -> AdhellFactory.getInstance().showAppComponentDisabledFragment(getParentFragmentManager());
        binding.appComponentStatusTextView.setOnClickListener(appComponentDisabledOnClickListener);
        binding.appComponentInfoTextView.setOnClickListener(appComponentDisabledOnClickListener);
    }

    private void addBlockedUrlToWhitelist(String blockedUrl) {
        Completable.fromAction(() -> {
            WhiteUrl whiteUrl = new WhiteUrl(blockedUrl, new Date());
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            appDatabase.whiteUrlDao().insert(whiteUrl);
        })
                .subscribeOn(Schedulers.io())
                .subscribe();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateUserInterface();
    }


    @Override
    public void onPause() {
        super.onPause();
        if (!MainActivity.finishActivity.compareAndSet(true, false)) {
            homeTabViewModel.updateLoadingBarVisibility(true);
        }
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
        if (item.getItemId() == R.id.refresh && !refreshRunning) {
            refreshRunning = true;
            new SetFirewallAsyncTask(true, this, fragmentManager, getContext(), true).execute();
            refreshRunning = false;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void updateUserInterface() {
        domainRulesEnabled = AppPreferences.getInstance().isDomainRulesToggleEnabled();
        firewallRulesEnabled = AppPreferences.getInstance().isFirewallRulesToggleEnabled();
        disablerEnabled = AppPreferences.getInstance().isAppDisablerToggleEnabled();
        appComponentEnabled = AppPreferences.getInstance().isAppComponentToggleEnabled();

        binding.domainRulesSwitch.setChecked(domainRulesEnabled);
        binding.firewallRulesSwitch.setChecked(firewallRulesEnabled);
        binding.appDisablerSwitch.setChecked(disablerEnabled);
        binding.appComponentSwitch.setChecked(appComponentEnabled);

        homeTabViewModel.setInfoCount();
        homeTabViewModel.refreshBlockedUrls();
    }

    private void invalidateOptionsMenu() {
        FragmentActivity parentActivity = getActivity();
        if (parentActivity != null) {
            parentActivity.invalidateOptionsMenu();
        }
    }

    private static class AppDisablerAsyncTask extends AsyncTask<Void, Void, Void> {
        private HomeTabFragment parentFragment;
        private AlertDialog dialog;
        private final boolean enabled;

        AppDisablerAsyncTask(boolean enabled, HomeTabFragment parentFragment, Context context) {
            LogUtils.info("AppDisabler enabled: "+enabled);
            this.parentFragment = parentFragment;

            this.enabled = enabled;
            String message = this.enabled ? "Disabling apps..." : "Enabling apps...";
            this.dialog = DialogUtils.getProgressDialog(message, context);
            this.dialog.setCancelable(false);
        }

        @Override
        protected void onPreExecute() {
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            AdhellFactory.getInstance().setAppDisablerToggle(enabled); // toggle the switch
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            parentFragment.updateUserInterface();

            // Clean resources to prevent memory leak
            this.parentFragment = null;
            this.dialog = null;
        }
    }

    private static class AppComponentAsyncTask extends AsyncTask<Void, Void, Void> {
        private HomeTabFragment parentFragment;
        private AlertDialog dialog;
        private final boolean enabled;

        AppComponentAsyncTask(boolean enabled, HomeTabFragment parentFragment, Context context) {
            this.parentFragment = parentFragment;

            this.enabled = enabled;
            String message = this.enabled ? "Disabling app component..." : "Enabling app component...";
            this.dialog = DialogUtils.getProgressDialog(message, context);
            this.dialog.setCancelable(false);
        }

        @Override
        protected void onPreExecute() {
            dialog.show();
        }

        @Override
        protected Void doInBackground(Void... voids) {
            AdhellFactory.getInstance().setAppComponentToggle(enabled); // toggle the switch
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            parentFragment.updateUserInterface();

            // Clean resources to prevent memory leak
            this.parentFragment = null;
            this.dialog = null;
        }
    }

    private static class SetFirewallAsyncTask extends AsyncTask<Void, Void, Void> {
        private FragmentManager fragmentManager;
        private FirewallDialogFragment fragment;
        private HomeTabFragment parentFragment;
        private ContentBlocker contentBlocker;
        private Handler handler;
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
                    if (fragment != null) {
                        fragment.appendText(msg.obj.toString());
                    }
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

            // Clean resources to prevent memory leak
            this.fragmentManager = null;
            this.contentBlocker = null;
            this.handler.removeCallbacksAndMessages(null);
            this.handler = null;
            this.fragment = null;
            this.parentFragment = null;

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
                if (context instanceof MainActivity) {
                    MainActivity mainActivity = (MainActivity) context;
                    mainActivity.makeSnackbar("Blocked domains have been exported!", Snackbar.LENGTH_LONG)
                            .show();
                }
            }
        }
    }

    public boolean getDomainSwitchState() {
        return binding.domainRulesSwitch.isChecked();
    }

    public boolean getFirewallSwitchState() {
        return binding.firewallRulesSwitch.isChecked();
    }

    @Override
    public void onAppCacheChange() {
        ExpandableListAdapter expandableListAdapter = binding.blockedDomainsListView.getExpandableListAdapter();
        if (expandableListAdapter instanceof BaseExpandableListAdapter) {
            ((BaseExpandableListAdapter) expandableListAdapter).notifyDataSetChanged();
        }
        if (homeTabViewModel != null) {
            homeTabViewModel.setInfoCount();
        }
    }
}
