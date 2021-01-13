package com.fusionjack.adhell3.fragments;

import android.app.ProgressDialog;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.ReportBlockedUrlAdapter;
import com.fusionjack.adhell3.blocker.ContentBlocker;
import com.fusionjack.adhell3.blocker.ContentBlocker56;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.DatabaseFactory;
import com.fusionjack.adhell3.db.entity.AppPermission;
import com.fusionjack.adhell3.db.entity.ReportBlockedUrl;
import com.fusionjack.adhell3.db.entity.RestrictedPackage;
import com.fusionjack.adhell3.db.entity.WhiteUrl;
import com.fusionjack.adhell3.dialog.AppCacheDialog;
import com.fusionjack.adhell3.dialogfragment.FirewallDialogFragment;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppDatabaseFactory;
import com.fusionjack.adhell3.utils.AppDiff;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.FirewallUtils;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.utils.SharedPreferenceBooleanLiveData;
import com.fusionjack.adhell3.utils.SharedPreferenceStringLiveData;
import com.fusionjack.adhell3.viewmodel.HomeViewModel;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.reactivex.Completable;
import io.reactivex.CompletableObserver;
import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

public class HomeTabFragment extends Fragment {

    private FragmentManager fragmentManager;
    private AppCompatActivity parentActivity;

    private ContentBlocker contentBlocker;

    private Resources resources;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentManager = getActivity().getSupportFragmentManager();
        parentActivity = (AppCompatActivity) getActivity();
        contentBlocker = ContentBlocker56.getInstance();
    }

    @Override
    public View onCreateView(@androidx.annotation.NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.resources = getContext().getResources();

        ActionBar actionBar = parentActivity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);

            View view = inflater.inflate(R.layout.activity_actionbar, container, false);
            TextView subtitleTextView = view.findViewById(R.id.subtitleTextView);
            if (subtitleTextView != null) {
                String versionInfo = resources.getString(R.string.version);
                subtitleTextView.setText(String.format(versionInfo, BuildConfig.VERSION_NAME));
            }
            actionBar.setCustomView(view);
            actionBar.setDisplayShowCustomEnabled(true);
        }

        View view = inflater.inflate(R.layout.fragment_blocker, container, false);

        if (!BuildConfig.DISABLE_APPS) {
            view.findViewById(R.id.appDisablerLayout).setVisibility(View.GONE);
        }
        if (!BuildConfig.APP_COMPONENT) {
            view.findViewById(R.id.appComponentLayout).setVisibility(View.GONE);
        }

        FloatingActionsMenu domainFloatMenu = view.findViewById(R.id.domain_actions);
        FloatingActionButton actionAddWhiteDomain = view.findViewById(R.id.action_export_domains);
        actionAddWhiteDomain.setIcon(R.drawable.ic_export_domain);
        actionAddWhiteDomain.setOnClickListener(v -> {
            domainFloatMenu.collapse();
            exportDomain();
        });

        // Init main toggles
        Switch domainSwitch = view.findViewById(R.id.domainRulesSwitch);
        TextView domainStatusTextView = view.findViewById(R.id.domainStatusTextView);
        Switch firewallSwitch = view.findViewById(R.id.firewallRulesSwitch);
        TextView firewallStatusTextView = view.findViewById(R.id.firewallStatusTextView);
        Switch disablerSwitch = view.findViewById(R.id.appDisablerSwitch);
        TextView disablerStatusTextView = view.findViewById(R.id.disablerStatusTextView);
        Switch appComponentSwitch = view.findViewById(R.id.appComponentSwitch);
        TextView appComponentStatusTextView = view.findViewById(R.id.appComponentStatusTextView);
        SwipeRefreshLayout blockedDomainSwipeContainer = view.findViewById(R.id.swipeContainer);
        TextView blockedDomainInfoTextView = view.findViewById(R.id.infoTextView);

        blockedDomainInfoTextView.setVisibility(View.INVISIBLE);
        blockedDomainSwipeContainer.setVisibility(View.INVISIBLE);

        domainSwitch.setOnClickListener(v -> {
            LogUtils.info( "Domain switch button has been clicked");
            toggleFirewall(true);
        });
        firewallSwitch.setOnClickListener(v -> {
            LogUtils.info( "Firewall switch button has been clicked");
            toggleFirewall(false);
        });
        disablerSwitch.setOnClickListener(v -> {
            LogUtils.info( "App disabler switch button has been clicked");
            toggleAppDisabler();
        });
        appComponentSwitch.setOnClickListener(v -> {
            LogUtils.info( "App component switch button has been clicked");
            toggleAppComponent();
        });

        initTogglePreferences(domainStatusTextView, domainSwitch, firewallStatusTextView, firewallSwitch,
                disablerStatusTextView, disablerSwitch, appComponentStatusTextView, appComponentSwitch, blockedDomainInfoTextView, blockedDomainSwipeContainer);

        // Init info count
        TextView domainInfoTextView = view.findViewById(R.id.domainInfoTextView);
        TextView whitelistAppInfoTextView = view.findViewById(R.id.whiteListAppInfoTextView);
        TextView firewallInfoTextView = view.findViewById(R.id.firewallInfoTextView);
        TextView disablerInfoTextView = view.findViewById(R.id.disablerInfoTextView);
        TextView appComponentInfoTextView = view.findViewById(R.id.appComponentInfoTextView);
        initInfoCount(domainInfoTextView, whitelistAppInfoTextView, firewallInfoTextView, disablerInfoTextView, appComponentInfoTextView);

        // Init reported blocked domains
        ListView blockedDomainsListView = view.findViewById(R.id.blockedDomainsListView);
        initBlockedDomainsView(blockedDomainsListView, blockedDomainInfoTextView);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        checkDatabaseIntegrity();
        loadBlockedUrls(null);
    }

    private void initTogglePreferences(TextView domainStatusTextView, Switch domainSwitch,
                                       TextView firewallStatusTextView, Switch firewallSwitch,
                                       TextView disablerStatusTextView, Switch disablerSwitch,
                                       TextView appComponentStatusTextView, Switch appComponentSwitch,
                                       TextView infoTextView, SwipeRefreshLayout swipeContainer) {

        AppPreferences.getInstance().getDomainRuleLiveData(contentBlocker)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<SharedPreferenceBooleanLiveData>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull SharedPreferenceBooleanLiveData liveData) {
                        liveData.observe(getViewLifecycleOwner(), state -> {
                            if (contentBlocker == null || !state) {
                                domainStatusTextView.setText(R.string.domain_rules_disabled);
                                domainSwitch.setChecked(false);
                            } else {
                                domainStatusTextView.setText(R.string.domain_rules_enabled);
                                domainSwitch.setChecked(true);
                            }

                            if (state) {
                                infoTextView.setVisibility(View.VISIBLE);
                                swipeContainer.setVisibility(View.VISIBLE);
                                swipeContainer.setOnRefreshListener(() ->
                                        loadBlockedUrls(swipeContainer)
                                );
                            } else {
                                infoTextView.setVisibility(View.INVISIBLE);
                                swipeContainer.setVisibility(View.INVISIBLE);
                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        LogUtils.error(e.getMessage(), e);
                    }
                });

        AppPreferences.getInstance().getFirewallRuleLiveData(contentBlocker)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<SharedPreferenceBooleanLiveData>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull SharedPreferenceBooleanLiveData liveData) {
                        liveData.observe(getViewLifecycleOwner(), state -> {
                            if (contentBlocker == null || !state) {
                                firewallStatusTextView.setText(R.string.firewall_rules_disabled);
                                firewallSwitch.setChecked(false);
                            } else {
                                firewallStatusTextView.setText(R.string.firewall_rules_enabled);
                                firewallSwitch.setChecked(true);
                            }
                        });
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        LogUtils.error(e.getMessage(), e);
                    }
                });

        AppPreferences.getInstance().getAppDisablerLiveData().observe(getViewLifecycleOwner(), state -> {
            if (state) {
                disablerStatusTextView.setText(R.string.app_disabler_enabled);
                disablerSwitch.setChecked(true);
            } else {
                disablerStatusTextView.setText(R.string.app_disabler_disabled);
                disablerSwitch.setChecked(false);
            }
        });

        AppPreferences.getInstance().getAppComponentLiveData().observe(getViewLifecycleOwner(), state -> {
            if (state) {
                appComponentStatusTextView.setText(R.string.app_component_enabled);
                appComponentSwitch.setChecked(true);
            } else {
                appComponentStatusTextView.setText(R.string.app_component_disabled);
                appComponentSwitch.setChecked(false);
            }
        });
    }

    private void initInfoCount(TextView domainInfoTextView, TextView whitelistAppInfoTextView,
                               TextView firewallInfoTextView, TextView disablerInfoTextView,
                               TextView appComponentInfoTextView) {

        HomeViewModel viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        AppPreferences.getInstance().getDomainCountLiveData()
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<SharedPreferenceStringLiveData>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull SharedPreferenceStringLiveData liveData) {
                        liveData.observe(getViewLifecycleOwner(), countStr -> {
                            int blackListSize = AppPreferences.getInstance().getBlockedDomainCount(countStr);
                            int whiteListSize = AppPreferences.getInstance().getWhitelistedDomainCount(countStr);
                            String domainInfo = resources.getString(R.string.domain_info_placeholder);
                            domainInfoTextView.setText(String.format(domainInfo, blackListSize, whiteListSize));
                        });
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        LogUtils.error(e.getMessage(), e);
                    }
                });

        viewModel.getWhiteListAppInfo()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<LiveData<Integer>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull LiveData<Integer> liveData) {
                        liveData.observe(getViewLifecycleOwner(), size -> {
                            String whitelistInfo = resources.getString(R.string.whitelist_app_info_placeholder);
                            whitelistAppInfoTextView.setText(String.format(whitelistInfo, size));
                        });
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        LogUtils.error(e.getMessage(), e);
                    }
                });

        viewModel.getRestrictedInfo()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<LiveData<List<RestrictedPackage>>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull LiveData<List<RestrictedPackage>> liveData) {
                        liveData.observe(getViewLifecycleOwner(), list -> {
                            long mobileSize = list.stream().filter(info -> info.type.equalsIgnoreCase(DatabaseFactory.MOBILE_RESTRICTED_TYPE)).count();
                            long wifiSize = list.stream().filter(info -> info.type.equalsIgnoreCase(DatabaseFactory.WIFI_RESTRICTED_TYPE)).count();

                            String firewallInfo = resources.getString(R.string.firewall_rules_info_placeholder);
                            firewallInfoTextView.setText(String.format(firewallInfo, mobileSize, wifiSize, 0));
                        });
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        LogUtils.error(e.getMessage(), e);
                    }
                });

        viewModel.getDisablerInfo()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<LiveData<Integer>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull LiveData<Integer> liveData) {
                        liveData.observe(getViewLifecycleOwner(), disablerSize -> {
                            String disablerInfo = resources.getString(R.string.app_disabler_info_placeholder);
                            boolean enabled = AppPreferences.getInstance().isAppDisablerToggleEnabled();
                            disablerInfoTextView.setText(String.format(disablerInfo, enabled ? disablerSize : 0));
                        });
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        LogUtils.error(e.getMessage(), e);
                    }
                });

        viewModel.getAppComponentInfo()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<LiveData<List<AppPermission>>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull LiveData<List<AppPermission>> liveData) {
                        liveData.observe(getViewLifecycleOwner(), list -> {
                            long permissionSize = list.stream().filter(info -> info.permissionStatus == -1).count();
                            long serviceSize = list.stream().filter(info -> info.permissionStatus == 2).count();
                            long receiverSize = list.stream().filter(info -> info.permissionStatus == 5).count();

                            String appComponentInfo = resources.getString(R.string.app_component_toggle_info_placeholder);
                            boolean enabled = AppPreferences.getInstance().isAppComponentToggleEnabled();
                            String info;
                            if (enabled) {
                                info = String.format(appComponentInfo, permissionSize, serviceSize, receiverSize);
                            } else {
                                info = String.format(appComponentInfo, 0, 0, 0);
                            }
                            appComponentInfoTextView.setText(info);
                        });
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        LogUtils.error(e.getMessage(), e);
                    }
                });
    }

    private void initBlockedDomainsView(ListView blockedDomainsListView, TextView infoTextView) {
        List<ReportBlockedUrl> blockedUrls = new ArrayList<>();
         ReportBlockedUrlAdapter blockedUrlAdapter = new ReportBlockedUrlAdapter(getContext(), blockedUrls);

        HomeViewModel viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        viewModel.getReportedBlockedDomains()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<LiveData<List<ReportBlockedUrl>>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull LiveData<List<ReportBlockedUrl>> liveData) {
                        liveData.observe(getViewLifecycleOwner(), list -> {
                            blockedUrls.clear();
                            blockedUrls.addAll(list);
                            blockedUrlAdapter.notifyDataSetChanged();
                            infoTextView.setText(String.format("%s%s", resources.getString(R.string.last_day_blocked), list.size()));
                        });
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        LogUtils.error(e.getMessage(), e);
                    }
                });

        blockedDomainsListView.setAdapter(blockedUrlAdapter);
        blockedDomainsListView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
            View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_question, blockedDomainsListView, false);
            TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
            titleTextView.setText(R.string.dialog_whitelist_domain_title);
            TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
            questionTextView.setText(R.string.dialog_add_to_whitelist_question);
            new AlertDialog.Builder(getContext())
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        String blockedUrl = blockedUrls.get(position).url;
                        addBlockedUrlToWhitelist(blockedUrl);
                    })
                    .setNegativeButton(android.R.string.no, null).show();
        });
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

    private void exportDomain() {
        Completable.fromAction(() -> {
            Set<String> domains = FirewallUtils.getInstance().getReportBlockedUrl().stream()
                    .map(domain -> domain.url)
                    .collect(Collectors.toSet());

            File file = new File(Environment.getExternalStorageDirectory(), "adhell_exported_domains.txt");
            try (FileWriter writer = new FileWriter(file)) {
                for (String domain : domains) {
                    writer.write(domain);
                    writer.write(System.lineSeparator());
                }
            }
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onComplete() {
                        Toast.makeText(getContext(), "Blocked domains have been exported!", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        LogUtils.error(e.getMessage(), e);
                        new AlertDialog.Builder(getContext())
                                .setTitle("Error")
                                .setMessage(e.getMessage())
                                .show();
                    }
                });
    }

    private void toggleAppDisabler() {
        ProgressDialog dialog = new ProgressDialog(getContext());
        Completable.fromAction(() -> {
            boolean state = AppPreferences.getInstance().isAppDisablerToggleEnabled();
            AdhellFactory.getInstance().setAppDisablerToggle(!state); // toggle the switch
        })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        boolean enabled = AppPreferences.getInstance().isAppDisablerToggleEnabled();
                        dialog.setMessage(enabled ? "Enabling apps ..." : "Disabling apps ...");
                        dialog.show();
                    }

                    @Override
                    public void onComplete() {
                        dialog.dismiss();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        dialog.dismiss();
                        LogUtils.error(e.getMessage(), e);
                        new AlertDialog.Builder(getContext())
                                .setTitle("Error")
                                .setMessage(e.getMessage())
                                .show();
                    }
                });
    }

    private void toggleAppComponent() {
        ProgressDialog dialog = new ProgressDialog(getContext());
        Completable.fromAction(() -> {
            boolean state = AppPreferences.getInstance().isAppComponentToggleEnabled();
            AdhellFactory.getInstance().setAppComponentToggle(!state); // toggle the switch
        })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        boolean enabled = AppPreferences.getInstance().isAppComponentToggleEnabled();
                        dialog.setMessage(enabled ? "Enabling app component ..." : "Disabling app component ...");
                        dialog.show();
                    }

                    @Override
                    public void onComplete() {
                        dialog.dismiss();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        dialog.dismiss();
                        LogUtils.error(e.getMessage(), e);
                        new AlertDialog.Builder(getContext())
                                .setTitle("Error")
                                .setMessage(e.getMessage())
                                .show();
                    }
                });
    }

    private void toggleFirewall(boolean isDomain) {
        Single.fromCallable(() -> getTitle(isDomain))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<String>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull String title) {
                        toggleFirewall(isDomain, title);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        LogUtils.error(e.getMessage(), e);
                        new AlertDialog.Builder(getContext())
                                .setTitle("Error")
                                .setMessage(e.getMessage())
                                .show();
                    }
                });
    }

    private String getTitle(boolean isDomain) {
        String title;
        if (isDomain) {
            boolean isDomainRuleEnabled = AppPreferences.getInstance().isDomainRuleToggleEnabled(contentBlocker);
            title = isDomainRuleEnabled ? "Disabling Domain Rules" : "Enabling Domain Rules";
        } else {
            boolean isFirewallRuleEnabled = AppPreferences.getInstance().isFirewallRuleToggleEnabled(contentBlocker);
            title = isFirewallRuleEnabled ? "Disabling Firewall Rules" : "Enabling Firewall Rules";
        }
        return title;
    }

    private void toggleFirewall(boolean isDomain, String title) {
        FirewallDialogFragment fragment = FirewallDialogFragment.newInstance(title);
        fragment.setCancelable(false);
        fragment.show(fragmentManager, "dialog_firewall");

        Handler handler = new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                fragment.appendText(msg.obj.toString());
            }
        };

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        boolean hasInternetAccess = AdhellFactory.getInstance().hasInternetAccess(getContext());
        boolean updateProviders = preferences.getBoolean(SettingsFragment.UPDATE_PROVIDERS_PREFERENCE, false) && hasInternetAccess;

        Completable.fromAction(() -> executeFirewall(isDomain, handler, updateProviders))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onComplete() {
                        fragment.enableCloseButton();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        fragment.enableCloseButton();
                        LogUtils.error(e.getMessage(), e);
                        new AlertDialog.Builder(getContext())
                                .setTitle("Error")
                                .setMessage(e.getMessage())
                                .show();
                    }
                });
    }

    private void executeFirewall(boolean isDomain, Handler handler, boolean updateProviders) {
        contentBlocker.setHandler(handler);
        if (isDomain) {
            boolean isDomainRuleEnabled = AppPreferences.getInstance().isDomainRuleToggleEnabled(contentBlocker);
            if (isDomainRuleEnabled) {
                contentBlocker.disableDomainRules();
            } else {
                contentBlocker.enableDomainRules(updateProviders);
            }
            AppPreferences.getInstance().setDomainRuleToggle(!isDomainRuleEnabled);
        } else {
            boolean isFirewallRuleEnabled = AppPreferences.getInstance().isFirewallRuleToggleEnabled(contentBlocker);
            if (isFirewallRuleEnabled) {
                contentBlocker.disableFirewallRules();
            } else {
                contentBlocker.enableFirewallRules();
            }
            AppPreferences.getInstance().setFirewallRuleToggle(!isFirewallRuleEnabled);
        }
    }

    private void checkDatabaseIntegrity() {
        Single.create((SingleOnSubscribe<Boolean>) emitter -> {
            AdhellAppIntegrity adhellAppIntegrity = AdhellAppIntegrity.getInstance();
            adhellAppIntegrity.checkDefaultPolicyExists();
            adhellAppIntegrity.checkAdhellStandardPackage();
            Boolean isPackageDbEmpty = adhellAppIntegrity.isPackageDbEmpty();
            emitter.onSuccess(isPackageDbEmpty);
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Boolean>() {
                        @Override
                        public void onSubscribe(@NonNull Disposable d) {
                        }

                        @Override
                        public void onSuccess(@NonNull Boolean isPackageDbEmpty) {
                            if (isPackageDbEmpty) {
                                resetInstalledApps();
                            } else {
                                detectNewOrDeletedApps();
                            }
                        }

                        @Override
                        public void onError(@NonNull Throwable e) {
                            LogUtils.error(e.getMessage(), e);
                        }
        });
    }

    private void resetInstalledApps() {
        final AppCacheDialog dialog = new AppCacheDialog(getContext());
        AppDatabaseFactory.resetInstalledApps()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        dialog.showDialog("Processing installed apps ...");
                    }

                    @Override
                    public void onComplete() {
                        dialog.dismissDialog();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        dialog.dismissDialog();
                        LogUtils.error(e.getMessage(), e);
                    }
                });
    }

    private void detectNewOrDeletedApps() {
        AppDatabaseFactory.detectNewOrDeletedApps()
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<AppDiff>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull AppDiff diff) {
                        if (!diff.isEmpty()) {
                            int newAppSize = diff.getNewApps().size();
                            int deletedAppSize = diff.getDeletedApps().size();
                            String message = newAppSize + " new app(s) and " + deletedAppSize + " deleted app(s) have been detected.";
                            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        LogUtils.error(e.getMessage(), e);
                    }
                });
    }

    private void loadBlockedUrls(SwipeRefreshLayout swipeContainer) {
        Completable.fromAction(() -> FirewallUtils.getInstance().getReportBlockedUrl())
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onComplete() {
                        if (swipeContainer != null) {
                            swipeContainer.setRefreshing(false);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        LogUtils.error(e.getMessage(), e);
                    }
                });
    }

}
