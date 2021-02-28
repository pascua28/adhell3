package com.fusionjack.adhell3.fragments;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import com.fusionjack.adhell3.cache.AppCache;
import com.fusionjack.adhell3.db.entity.AppPermission;
import com.fusionjack.adhell3.db.entity.ReportBlockedUrl;
import com.fusionjack.adhell3.dialogfragment.FirewallDialogFragment;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppDatabaseFactory;
import com.fusionjack.adhell3.utils.AppDiff;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.DocumentFileUtils;
import com.fusionjack.adhell3.utils.DocumentFileWriter;
import com.fusionjack.adhell3.utils.FirewallUtils;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.utils.SharedPreferenceBooleanLiveData;
import com.fusionjack.adhell3.utils.SharedPreferenceStringLiveData;
import com.fusionjack.adhell3.utils.rx.RxCompletableComputationBuilder;
import com.fusionjack.adhell3.utils.rx.RxCompletableIoBuilder;
import com.fusionjack.adhell3.utils.rx.RxSingleComputationBuilder;
import com.fusionjack.adhell3.utils.rx.RxSingleIoBuilder;
import com.fusionjack.adhell3.viewmodel.HomeViewModel;
import com.fusionjack.adhell3.viewmodel.UserListViewModel;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.reactivex.Completable;
import io.reactivex.Single;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.functions.Action;
import toan.android.floatingactionmenu.FloatingActionButton;
import toan.android.floatingactionmenu.FloatingActionsMenu;
import toan.android.floatingactionmenu.ScrollDirectionListener;

import static com.fusionjack.adhell3.db.entity.AppPermission.STATUS_ACTIVITY;
import static com.fusionjack.adhell3.db.entity.AppPermission.STATUS_PERMISSION;
import static com.fusionjack.adhell3.db.entity.AppPermission.STATUS_PROVIDER;
import static com.fusionjack.adhell3.db.entity.AppPermission.STATUS_RECEIVER;
import static com.fusionjack.adhell3.db.entity.AppPermission.STATUS_SERVICE;

public class HomeTabFragment extends Fragment {

    private FragmentManager fragmentManager;
    private AppCompatActivity parentActivity;
    private ReportBlockedUrlAdapter blockedUrlAdapter;

    private ContentBlocker contentBlocker;
    private Resources resources;

    private final ActivityResultLauncher<Uri> openDocumentTreeLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocumentTree(), result -> {
        LogUtils.info("Picked folder: " + result);
        if (result != null) {
            getContext().grantUriPermission(getContext().getPackageName(), result, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            getContext().getContentResolver().takePersistableUriPermission(result, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            AppPreferences.getInstance().setAdhell3FolderUri(result.toString());
        }
    });

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
        TextView appComponentInfo2TextView = view.findViewById(R.id.appComponentInfo2TextView);
        initInfoCount(domainInfoTextView, firewallInfoTextView, disablerInfoTextView, appComponentInfoTextView, appComponentInfo2TextView);

        // Init reported blocked domains
        ListView blockedDomainsListView = view.findViewById(R.id.blockedDomainsListView);
        initBlockedDomainsView(blockedDomainsListView, blockedDomainInfoTextView, blockedDomainSwipeContainer);

        FloatingActionsMenu domainFloatMenu = view.findViewById(R.id.domain_actions);
        domainFloatMenu.attachToListView(blockedDomainsListView, new ScrollDirectionListener() {
            @Override
            public void onScrollDown() {
                domainFloatMenu.setVisibleWithAnimation(true);
            }
            @Override
            public void onScrollUp() {
                domainFloatMenu.setVisibleWithAnimation(false);
            }
        });

        FloatingActionButton actionExportDomain = view.findViewById(R.id.action_export_domains);
        actionExportDomain.setOnClickListener(v -> {
            domainFloatMenu.collapse();
            exportDomains();
        });

        FloatingActionButton actionDumpDomain = view.findViewById(R.id.action_dump_domains);
        actionDumpDomain.setOnClickListener(v -> {
            domainFloatMenu.collapse();
            dumpDomains();
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        String uri = AppPreferences.getInstance().getAdhell3FolderUri();
        if (uri == null || !DocumentFileUtils.hasAccess(Uri.parse(uri))) {
            showStoragePermissionDialog();
        } else {
            cacheApps();
        }
    }

    private void showStoragePermissionDialog() {
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_question, (ViewGroup) getView(), false);
        TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
        titleTextView.setText(R.string.dialog_storage_permission_title);
        TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
        questionTextView.setText(R.string.dialog_storage_permission_summary);
        new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                    openDocumentTreeLauncher.launch(Uri.fromFile(Environment.getExternalStorageDirectory()));
                })
                .setNegativeButton(android.R.string.no, (dialog, which) -> cacheApps())
                .show();
    }

    private void cacheApps() {
        Runnable callback = () -> {
            triggerReportedBlockedUrls(() -> blockedUrlAdapter.notifyDataSetChanged());
            checkDatabaseIntegrity();
        };
        AppCache.getInstance().cacheApps(getContext(), callback);
    }

    private void safeGuardLiveData(Runnable action) {
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
                                       TextView blockedDomainInfoTextView, SwipeRefreshLayout blockedDomainSwipeContainer) {

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
                        blockedDomainInfoTextView.setVisibility(View.VISIBLE);
                        blockedDomainSwipeContainer.setVisibility(View.VISIBLE);
                    } else {
                        blockedDomainInfoTextView.setVisibility(View.INVISIBLE);
                        blockedDomainSwipeContainer.setVisibility(View.INVISIBLE);
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

    private void initInfoCount(TextView domainInfoTextView, TextView firewallInfoTextView, TextView disablerInfoTextView,
                               TextView appComponentInfoTextView, TextView appComponentInfo2TextView) {

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
                    long permissionSize = list.stream().filter(info -> info.permissionStatus == STATUS_PERMISSION).count();
                    long activitySize = list.stream().filter(info -> info.permissionStatus == STATUS_ACTIVITY).count();
                    long serviceSize = list.stream().filter(info -> info.permissionStatus == STATUS_SERVICE).count();
                    long receiverSize = list.stream().filter(info -> info.permissionStatus == STATUS_RECEIVER).count();
                    long providerSize = list.stream().filter(info -> info.permissionStatus == STATUS_PROVIDER).count();

                    boolean enabled = AppPreferences.getInstance().isAppComponentToggleEnabled();

                    String info;
                    String appComponentInfo = resources.getString(R.string.app_component_toggle_info_placeholder);
                    if (enabled) {
                        info = String.format(appComponentInfo, permissionSize, serviceSize, receiverSize);
                    } else {
                        info = String.format(appComponentInfo, 0, 0, 0);
                    }
                    appComponentInfoTextView.setText(info);

                    String info2;
                    String appComponentInfo2 = resources.getString(R.string.app_component_toggle_info_placeholder2);
                    if (enabled) {
                        info2 = String.format(appComponentInfo2, activitySize, providerSize);
                    } else {
                        info2 = String.format(appComponentInfo2, 0, 0);
                    }
                    appComponentInfo2TextView.setText(info2);
                });
            });
        };

        new RxSingleComputationBuilder().async(AppPreferences.getInstance().getDomainCountLiveData(), domainCountCallback);
        new RxSingleComputationBuilder().async(AppPreferences.getInstance().getFirewallStatLiveData(), firewallCountCallback);
        new RxSingleIoBuilder().async(viewModel.getDisablerInfo(), disablerInfoCallback);
        new RxSingleIoBuilder().async(viewModel.getAppComponentInfo(), appComponentInfoCallback);
    }

    private void initBlockedDomainsView(ListView blockedDomainsListView, TextView infoTextView, SwipeRefreshLayout swipeContainer) {
        Runnable swipeCallback = () -> swipeContainer.setRefreshing(false);
        swipeContainer.setOnRefreshListener(() ->
                triggerReportedBlockedUrls(swipeCallback)
        );

        List<ReportBlockedUrl> blockedUrls = new ArrayList<>();
        this.blockedUrlAdapter = new ReportBlockedUrlAdapter(getContext(), blockedUrls);

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
                        ReportBlockedUrl blockedUrl = blockedUrls.get(position);
                        addBlockedUrlToWhitelist(blockedUrl);
                    })
                    .setNegativeButton(android.R.string.no, null).show();
        });

        Consumer<LiveData<List<ReportBlockedUrl>>> callback = liveData -> {
            safeGuardLiveData(() -> {
                liveData.observe(getViewLifecycleOwner(), list -> {
                    blockedUrls.clear();
                    blockedUrls.addAll(list);
                    blockedUrlAdapter.notifyDataSetChanged();
                    String blockedDomainInfo = resources.getString(R.string.last_day_blocked);
                    infoTextView.setText(String.format(blockedDomainInfo, BuildConfig.BLOCKED_DOMAIN_DURATION_UI, list.size()));
                });
            });
        };

        HomeViewModel viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        new RxSingleIoBuilder().async(viewModel.getReportedBlockedDomains(), callback);
    }

    private void triggerReportedBlockedUrls(Runnable callback) {
        Action action = () -> FirewallUtils.getInstance().fetchReportBlockedUrlLastXHours();
        new RxCompletableComputationBuilder().async(Completable.fromAction(action), callback);
    }

    private void addBlockedUrlToWhitelist(ReportBlockedUrl blockedUrl) {
        UserListViewModel viewModel = new ViewModelProvider(this, new UserListViewModel.WhiteListFactory()).get(UserListViewModel.class);
        String domainToAdd = blockedUrl.packageName + "|" + blockedUrl.url;
        Consumer<String> callback = str -> Toast.makeText(getContext(), "Selected domain added to the whitelist", Toast.LENGTH_LONG).show();
        new RxSingleIoBuilder().async(viewModel.addItemObservable(domainToAdd), callback);
    }

    private void dumpDomains() {
        Action dumpDomains = () -> {
            SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.ENGLISH);
            List<ReportBlockedUrl> reports = FirewallUtils.getInstance().getReportBlockedUrls();
            try (DocumentFileWriter writer = DocumentFileWriter.overrideMode("adhell3_dumped_domains.txt")) {
                for (ReportBlockedUrl report : reports) {
                    String line = String.format("%-75s %-100s %s", report.packageName, report.url, dateFormatter.format(report.blockDate));
                    writer.write(line);
                }
            }
        };

        Runnable callback = () ->
                Toast.makeText(getContext(), "Blocked domains have been dumped!", Toast.LENGTH_LONG).show();

        new RxCompletableComputationBuilder()
                .showErrorAlert(getContext())
                .async(Completable.fromAction(dumpDomains), callback);
    }

    private void exportDomains() {
        Action exportDomains = () -> {
            Set<String> domains = FirewallUtils.getInstance().getReportBlockedUrlLastXHours().stream()
                    .map(domain -> domain.url)
                    .collect(Collectors.toSet());

            try (DocumentFileWriter writer = DocumentFileWriter.overrideMode("adhell3_exported_domains.txt")) {
                for (String domain : domains) {
                    writer.write(domain);
                }
            }
        };

        Runnable callback = () ->
                Toast.makeText(getContext(), "Blocked domains have been exported!", Toast.LENGTH_LONG).show();

        new RxCompletableIoBuilder()
                .showErrorAlert(getContext())
                .async(Completable.fromAction(exportDomains), callback);
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

}
