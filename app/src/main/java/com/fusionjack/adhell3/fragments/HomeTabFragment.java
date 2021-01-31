package com.fusionjack.adhell3.fragments;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
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
import com.fusionjack.adhell3.db.entity.AppPermission;
import com.fusionjack.adhell3.db.entity.ReportBlockedUrl;
import com.fusionjack.adhell3.db.entity.WhiteUrl;
import com.fusionjack.adhell3.dialogfragment.FirewallDialogFragment;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppDatabaseFactory;
import com.fusionjack.adhell3.utils.AppDiff;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.FileUtils;
import com.fusionjack.adhell3.utils.FirewallUtils;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.utils.SharedPreferenceBooleanLiveData;
import com.fusionjack.adhell3.utils.SharedPreferenceStringLiveData;
import com.fusionjack.adhell3.utils.rx.RxCompletableComputationBuilder;
import com.fusionjack.adhell3.utils.rx.RxCompletableIoBuilder;
import com.fusionjack.adhell3.utils.rx.RxSingleComputationBuilder;
import com.fusionjack.adhell3.utils.rx.RxSingleIoBuilder;
import com.fusionjack.adhell3.viewmodel.HomeViewModel;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.functions.Action;

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
        SwitchMaterial domainSwitch = view.findViewById(R.id.domainRulesSwitch);
        TextView domainStatusTextView = view.findViewById(R.id.domainStatusTextView);
        SwitchMaterial firewallSwitch = view.findViewById(R.id.firewallRulesSwitch);
        TextView firewallStatusTextView = view.findViewById(R.id.firewallStatusTextView);
        SwitchMaterial disablerSwitch = view.findViewById(R.id.appDisablerSwitch);
        TextView disablerStatusTextView = view.findViewById(R.id.disablerStatusTextView);
        SwitchMaterial appComponentSwitch = view.findViewById(R.id.appComponentSwitch);
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
        TextView firewallInfoTextView = view.findViewById(R.id.firewallInfoTextView);
        TextView disablerInfoTextView = view.findViewById(R.id.disablerInfoTextView);
        TextView appComponentInfoTextView = view.findViewById(R.id.appComponentInfoTextView);
        initInfoCount(domainInfoTextView, firewallInfoTextView, disablerInfoTextView, appComponentInfoTextView);

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

    void safeGuardLiveData(Runnable action) {
        if (getView() == null) {
            LogUtils.error("View is null");
            return;
        }
        action.run();
    }

    private void initTogglePreferences(TextView domainStatusTextView, SwitchMaterial domainSwitch,
                                       TextView firewallStatusTextView, SwitchMaterial firewallSwitch,
                                       TextView disablerStatusTextView, SwitchMaterial disablerSwitch,
                                       TextView appComponentStatusTextView, SwitchMaterial appComponentSwitch,
                                       TextView infoTextView, SwipeRefreshLayout swipeContainer) {

        Consumer<SharedPreferenceBooleanLiveData> domainRuleCallback = liveData -> {
            safeGuardLiveData(() -> {
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
            });
        };

        Consumer<SharedPreferenceBooleanLiveData> firewallRuleCallback = liveData -> {
            safeGuardLiveData(() -> {
                liveData.observe(getViewLifecycleOwner(), state -> {
                    if (contentBlocker == null || !state) {
                        firewallStatusTextView.setText(R.string.firewall_rules_disabled);
                        firewallSwitch.setChecked(false);
                    } else {
                        firewallStatusTextView.setText(R.string.firewall_rules_enabled);
                        firewallSwitch.setChecked(true);
                    }
                });
            });
        };

        Consumer<SharedPreferenceBooleanLiveData> disablerCallback = liveData -> {
            safeGuardLiveData(() -> {
                liveData.observe(getViewLifecycleOwner(), state -> {
                    if (state) {
                        disablerStatusTextView.setText(R.string.app_disabler_enabled);
                        disablerSwitch.setChecked(true);
                    } else {
                        disablerStatusTextView.setText(R.string.app_disabler_disabled);
                        disablerSwitch.setChecked(false);
                    }
                });
            });
        };

        Consumer<SharedPreferenceBooleanLiveData> appComponentCallback = liveData -> {
            safeGuardLiveData(() -> {
                liveData.observe(getViewLifecycleOwner(), state -> {
                    if (state) {
                        appComponentStatusTextView.setText(R.string.app_component_enabled);
                        appComponentSwitch.setChecked(true);
                    } else {
                        appComponentStatusTextView.setText(R.string.app_component_disabled);
                        appComponentSwitch.setChecked(false);
                    }
                });
            });
        };

        Single<SharedPreferenceBooleanLiveData> domainRuleObservable = AppPreferences.getInstance().getDomainRuleLiveData(contentBlocker);
        new RxSingleComputationBuilder().async(domainRuleObservable, domainRuleCallback);

        Single<SharedPreferenceBooleanLiveData> firewallRuleObservable = AppPreferences.getInstance().getFirewallRuleLiveData(contentBlocker);
        new RxSingleComputationBuilder().async(firewallRuleObservable, firewallRuleCallback);

        Single<SharedPreferenceBooleanLiveData> disablerObservable = AppPreferences.getInstance().getAppDisablerLiveData();
        new RxSingleIoBuilder().async(disablerObservable, disablerCallback);

        Single<SharedPreferenceBooleanLiveData> appComponentObservable = AppPreferences.getInstance().getAppComponentLiveData();
        new RxSingleIoBuilder().async(appComponentObservable, appComponentCallback);
    }

    private void initInfoCount(TextView domainInfoTextView, TextView firewallInfoTextView,
                               TextView disablerInfoTextView, TextView appComponentInfoTextView) {

        HomeViewModel viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        Consumer<SharedPreferenceStringLiveData> domainCountCallback = liveData -> {
            safeGuardLiveData(() -> {
                liveData.observe(getViewLifecycleOwner(), domainStatStr -> {
                    FirewallUtils.DomainStat stat = FirewallUtils.DomainStat.toStat(domainStatStr);
                    String domainInfo = resources.getString(R.string.domain_info_placeholder);
                    domainInfoTextView.setText(String.format(domainInfo, stat.blackListSize, stat.whiteListSize, stat.whiteAppsSize));
                });
            });
        };

        Consumer<SharedPreferenceStringLiveData> firewallCountCallback = liveData -> {
            safeGuardLiveData(() -> {
                liveData.observe(getViewLifecycleOwner(), firewallStatStr -> {
                    FirewallUtils.FirewallStat stat = FirewallUtils.FirewallStat.toStat(firewallStatStr);
                    String firewallInfo = resources.getString(R.string.firewall_rules_info_placeholder);
                    firewallInfoTextView.setText(String.format(firewallInfo, stat.mobileDataSize, stat.wifiDataSize, stat.allNetworkSize));
                });
            });
        };

        Consumer<LiveData<Integer>> disablerInfoCallback = liveData -> {
            safeGuardLiveData(() -> {
                liveData.observe(getViewLifecycleOwner(), disablerSize -> {
                    String disablerInfo = resources.getString(R.string.app_disabler_info_placeholder);
                    boolean enabled = AppPreferences.getInstance().isAppDisablerToggleEnabled();
                    disablerInfoTextView.setText(String.format(disablerInfo, enabled ? disablerSize : 0));
                });
            });
        };

        Consumer<LiveData<List<AppPermission>>> appComponentInfoCallback = liveData -> {
            safeGuardLiveData(() -> {
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
            });
        };

        new RxSingleComputationBuilder().async(AppPreferences.getInstance().getDomainCountLiveData(), domainCountCallback);
        new RxSingleComputationBuilder().async(AppPreferences.getInstance().getFirewallStatLiveData(), firewallCountCallback);
        new RxSingleIoBuilder().async(viewModel.getDisablerInfo(), disablerInfoCallback);
        new RxSingleIoBuilder().async(viewModel.getAppComponentInfo(), appComponentInfoCallback);
    }

    private void initBlockedDomainsView(ListView blockedDomainsListView, TextView infoTextView) {
        List<ReportBlockedUrl> blockedUrls = new ArrayList<>();
         ReportBlockedUrlAdapter blockedUrlAdapter = new ReportBlockedUrlAdapter(getContext(), blockedUrls);

         Consumer<LiveData<List<ReportBlockedUrl>>> callback = liveData -> {
             safeGuardLiveData(() -> {
                 liveData.observe(getViewLifecycleOwner(), list -> {
                     blockedUrls.clear();
                     blockedUrls.addAll(list);
                     blockedUrlAdapter.notifyDataSetChanged();
                     infoTextView.setText(String.format("%s%s", resources.getString(R.string.last_day_blocked), list.size()));
                 });
             });
         };

        HomeViewModel viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        new RxSingleIoBuilder().async(viewModel.getReportedBlockedDomains(), callback);

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
        Action addToWhiteList = () -> {
            WhiteUrl whiteUrl = new WhiteUrl(blockedUrl, new Date());
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            appDatabase.whiteUrlDao().insert(whiteUrl);
        };
        new RxCompletableIoBuilder().async(Completable.fromAction(addToWhiteList));
    }

    private void exportDomain() {
        Action exportDomain = () -> {
            Set<String> domains = FirewallUtils.getInstance().getReportBlockedUrl().stream()
                    .map(domain -> domain.url)
                    .collect(Collectors.toSet());

            File file = FileUtils.toFile("adhell_exported_domains.txt");
            try (FileWriter writer = new FileWriter(file)) {
                for (String domain : domains) {
                    writer.write(domain);
                    writer.write(System.lineSeparator());
                }
            }
        };

        Runnable callback = () ->
                Toast.makeText(getContext(), "Blocked domains have been exported!", Toast.LENGTH_LONG).show();

        new RxCompletableIoBuilder()
                .showErrorAlert(getContext())
                .async(Completable.fromAction(exportDomain), callback);
    }

    private void toggleAppDisabler() {
        Action toggleAppDisabler = () -> {
            boolean state = AppPreferences.getInstance().isAppDisablerToggleEnabled();
            AdhellFactory.getInstance().setAppDisablerToggle(!state); // toggle the switch
        };

        boolean enabled = AppPreferences.getInstance().isAppDisablerToggleEnabled();
        String dialogMessage = enabled ? "Enabling apps ..." : "Disabling apps ...";

        new RxCompletableComputationBuilder()
                .setShowDialog(dialogMessage, getContext())
                .async(Completable.fromAction(toggleAppDisabler));
    }

    private void toggleAppComponent() {
        Action toggleAppComponent = () -> {
            boolean state = AppPreferences.getInstance().isAppComponentToggleEnabled();
            AdhellFactory.getInstance().setAppComponentToggle(!state); // toggle the switch
        };

        boolean enabled = AppPreferences.getInstance().isAppComponentToggleEnabled();
        String dialogMessage = enabled ? "Enabling app component ..." : "Disabling app component ...";

        new RxCompletableComputationBuilder()
                .setShowDialog(dialogMessage, getContext())
                .async(Completable.fromAction(toggleAppComponent));
    }

    private void toggleFirewall(boolean isDomain) {
        Callable<String> getTitle = () -> getTitle(isDomain);
        Consumer<String> callback = title -> toggleFirewall(isDomain, title);
        new RxSingleComputationBuilder()
                .setShowErrorAlert(getContext())
                .async(Single.fromCallable(getTitle), callback);
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

        Action executeFirewall = () -> executeFirewall(isDomain, handler, updateProviders);
        Runnable onSubscribe = () -> {};
        Runnable onComplete = fragment::enableCloseButton;
        Runnable onError = fragment::enableCloseButton;

        new RxCompletableComputationBuilder()
                .showErrorAlert(getContext())
                .async(Completable.fromAction(executeFirewall), onSubscribe, onComplete, onError);
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
        SingleOnSubscribe<Boolean> isPackageDbEmptySource = emitter -> {
            AdhellAppIntegrity adhellAppIntegrity = AdhellAppIntegrity.getInstance();
            adhellAppIntegrity.checkDefaultPolicyExists();
            adhellAppIntegrity.checkAdhellStandardPackage();
            emitter.onSuccess(adhellAppIntegrity.isPackageDbEmpty());
        };

        Consumer<Boolean> callback = isPackageDbEmpty -> {
            if (isPackageDbEmpty) {
                resetInstalledApps();
            } else {
                detectNewOrDeletedApps();
            }
        };

        new RxSingleIoBuilder().async(Single.create(isPackageDbEmptySource), callback);
    }

    private void resetInstalledApps() {
        new RxCompletableIoBuilder()
                .setShowDialog("Processing installed apps ...", getContext())
                .async(AppDatabaseFactory.resetInstalledApps());
    }

    private void detectNewOrDeletedApps() {
        Consumer<AppDiff> callback = diff -> {
            if (!diff.isEmpty()) {
                int newAppSize = diff.getNewApps().size();
                int deletedAppSize = diff.getDeletedApps().size();
                String message = newAppSize + " new app(s) and " + deletedAppSize + " deleted app(s) have been detected.";
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        };
        new RxSingleComputationBuilder().async(AppDatabaseFactory.detectNewOrDeletedApps(), callback);
    }

    private void loadBlockedUrls(SwipeRefreshLayout swipeContainer) {
        Action action = () -> FirewallUtils.getInstance().getReportBlockedUrl();
        Runnable callback = () -> {
            if (swipeContainer != null) {
                swipeContainer.setRefreshing(false);
            }
        };
        new RxCompletableComputationBuilder().async(Completable.fromAction(action), callback);
    }

}
