package com.fusionjack.adhell3.fragments;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.AsyncTask;
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
import androidx.preference.PreferenceManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fusionjack.adhell3.BuildConfig;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.ReportBlockedUrlAdapter;
import com.fusionjack.adhell3.blocker.ContentBlocker;
import com.fusionjack.adhell3.blocker.ContentBlocker56;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.ReportBlockedUrl;
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
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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

    List<ReportBlockedUrl> blockedUrls;
    ReportBlockedUrlAdapter blockedUrlAdapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fragmentManager = getActivity().getSupportFragmentManager();
        parentActivity = (AppCompatActivity) getActivity();
        contentBlocker = ContentBlocker56.getInstance();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ActionBar actionBar = parentActivity.getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(false);
            actionBar.setHomeButtonEnabled(false);

            View view = inflater.inflate(R.layout.activity_actionbar, container, false);
            TextView subtitleTextView = view.findViewById(R.id.subtitleTextView);
            if (subtitleTextView != null) {
                String versionInfo = getContext().getResources().getString(R.string.version);
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
            LogUtils.info( "Domain switch button has been clicked");
            new SetFirewallAsyncTask(true, this, fragmentManager, getContext()).execute();
        });
        firewallSwitch.setOnClickListener(v -> {
            LogUtils.info( "Firewall switch button has been clicked");
            new SetFirewallAsyncTask(false, this, fragmentManager, getContext()).execute();
        });
        disablerSwitch.setOnClickListener(v -> {
            LogUtils.info( "App disabler switch button has been clicked");
            new AppDisablerAsyncTask(this, getActivity()).execute();
        });
        appComponentSwitch.setOnClickListener(v -> {
            LogUtils.info( "App component switch button has been clicked");
            new AppComponentAsyncTask(this, getActivity()).execute();
        });

        FloatingActionsMenu domainFloatMenu = view.findViewById(R.id.domain_actions);
        FloatingActionButton actionAddWhiteDomain = view.findViewById(R.id.action_export_domains);
        actionAddWhiteDomain.setIcon(R.drawable.ic_public_white_24dp);
        actionAddWhiteDomain.setOnClickListener(v -> {
            domainFloatMenu.collapse();
            new ExportDomainsAsyncTask(getContext()).execute();
        });

        ListView blockedDomainsListView = view.findViewById(R.id.blockedDomainsListView);
        initBlockedDomainsView(blockedDomainsListView);

        return view;
    }

    private void initBlockedDomainsView(ListView blockedDomainsListView) {
        Context context = getContext();

        this.blockedUrls = new ArrayList<>();
        this.blockedUrlAdapter = new ReportBlockedUrlAdapter(context, blockedUrls);

        blockedDomainsListView.setAdapter(blockedUrlAdapter);
        blockedDomainsListView.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_question, blockedDomainsListView, false);
            TextView titleTextView = dialogView.findViewById(R.id.titleTextView);
            titleTextView.setText(R.string.dialog_whitelist_domain_title);
            TextView questionTextView = dialogView.findViewById(R.id.questionTextView);
            questionTextView.setText(R.string.dialog_add_to_whitelist_question);
            new AlertDialog.Builder(context)
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

    @Override
    public void onResume() {
        super.onResume();
        checkDatabaseIntegrity();
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
                        updateUserInterface();
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
                        updateUserInterface();
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        LogUtils.error(e.getMessage(), e);
                    }
                });
    }

    private void updateUserInterface() {
        setInfoCount();
        setDomainInfo();
        setFirewallInfo();
        setAppDisablerInfo();
        setAppComponentInfo();
    }

    private void setInfoCount() {
        Single.create((SingleOnSubscribe<InfoCount>) emitter -> {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();

            int disablerSize = appDatabase.disabledPackageDao().getSize();
            int permissionSize = appDatabase.appPermissionDao().getPermissionSize();
            int serviceSize = appDatabase.appPermissionDao().getServiceSize();
            int receiverSize = appDatabase.appPermissionDao().getReceiverSize();

            FirewallUtils.DomainStat domainStat = FirewallUtils.getInstance().getDomainStatFromKnox();
            int blackListSize = domainStat.blackListSize;
            int whiteListSize = domainStat.whiteListSize;

            int whitelistAppSize = FirewallUtils.getInstance().getWhitelistAppCountFromKnox();

            // Dirty solution: Every deny firewall is created for IPv4 and IPv6.
            FirewallUtils.FirewallStat stat = FirewallUtils.getInstance().getFirewallStatFromKnox();
            int customSize = stat.allNetworkSize / 2;
            int mobileSize = stat.mobileDataSize / 2;
            int wifiSize = stat.wifiDataSize / 2;

            emitter.onSuccess(new InfoCount(mobileSize, wifiSize, customSize, blackListSize,
                    whiteListSize, whitelistAppSize, disablerSize, permissionSize, serviceSize, receiverSize));
        })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<InfoCount>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull InfoCount infoCount) {
                        int mobileSize = infoCount.getMobileSize();
                        int wifiSize = infoCount.getWifiSize();
                        int customSize = infoCount.getCustomSize();
                        int blackListSize = infoCount.getBlackListSize();
                        int whiteListSize = infoCount.getWhiteListSize();
                        int whitelistAppSize = infoCount.getWhitelistAppSize();
                        int disablerSize = infoCount.getDisablerSize();
                        int permissionSize = infoCount.getPermissionSize();
                        int serviceSize = infoCount.getServiceSize();
                        int receiverSize = infoCount.getReceiverSize();

                        Resources resources = getContext().getResources();
                        Activity activity = ((Activity) getContext());

                        TextView domainInfoTextView = activity.findViewById(R.id.domainInfoTextView);
                        if (domainInfoTextView != null) {
                            String domainInfo = resources.getString(R.string.domain_rules_info_placeholder);
                            domainInfoTextView.setText(String.format(domainInfo, blackListSize, whiteListSize, whitelistAppSize));
                        }
                        TextView firewallInfoTextView = activity.findViewById(R.id.firewallInfoTextView);
                        if (firewallInfoTextView != null) {
                            String firewallInfo = resources.getString(R.string.firewall_rules_info_placeholder);
                            firewallInfoTextView.setText(String.format(firewallInfo, mobileSize, wifiSize, customSize));
                        }
                        TextView disablerInfoTextView = activity.findViewById(R.id.disablerInfoTextView);
                        if (disablerInfoTextView != null) {
                            String disablerInfo = resources.getString(R.string.app_disabler_info_placeholder);
                            boolean enabled = AppPreferences.getInstance().isAppDisablerToggleEnabled();
                            disablerInfoTextView.setText(String.format(disablerInfo, enabled ? disablerSize : 0));
                        }
                        TextView appComponentInfoTextView = activity.findViewById(R.id.appComponentInfoTextView);
                        if (appComponentInfoTextView != null) {
                            String appComponentInfo = resources.getString(R.string.app_component_toggle_info_placeholder);
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

                    @Override
                    public void onError(@NonNull Throwable e) {
                        LogUtils.error(e.getMessage(), e);
                    }
                });
    }

    private static class InfoCount {
        private final int mobileSize;
        private final int wifiSize;
        private final int customSize;
        private final int blackListSize;
        private final int whiteListSize;
        private final int whitelistAppSize;
        private final int disablerSize;
        private final int permissionSize;
        private final int serviceSize;
        private final int receiverSize;

        public InfoCount(int mobileSize, int wifiSize, int customSize, int blackListSize,
                         int whiteListSize, int whitelistAppSize, int disablerSize,
                         int permissionSize, int serviceSize, int receiverSize) {
            this.mobileSize = mobileSize;
            this.wifiSize = wifiSize;
            this.customSize = customSize;
            this.blackListSize = blackListSize;
            this.whiteListSize = whiteListSize;
            this.whitelistAppSize = whitelistAppSize;
            this.disablerSize = disablerSize;
            this.permissionSize = permissionSize;
            this.serviceSize = serviceSize;
            this.receiverSize = receiverSize;
        }

        public int getMobileSize() {
            return mobileSize;
        }

        public int getWifiSize() {
            return wifiSize;
        }

        public int getCustomSize() {
            return customSize;
        }

        public int getBlackListSize() {
            return blackListSize;
        }

        public int getWhiteListSize() {
            return whiteListSize;
        }

        public int getWhitelistAppSize() {
            return whitelistAppSize;
        }

        public int getDisablerSize() {
            return disablerSize;
        }

        public int getPermissionSize() {
            return permissionSize;
        }

        public int getServiceSize() {
            return serviceSize;
        }

        public int getReceiverSize() {
            return receiverSize;
        }
    }

    private void setDomainInfo() {
        Single.create((SingleOnSubscribe<Boolean>) emitter -> {
            boolean isDomainRuleEmpty = contentBlocker.isDomainRuleEmpty();
            emitter.onSuccess(isDomainRuleEmpty);
        })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Boolean>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull Boolean isDomainRuleEmpty) {
                        if (contentBlocker == null || isDomainRuleEmpty) {
                            domainStatusTextView.setText(R.string.domain_rules_disabled);
                            domainSwitch.setChecked(false);
                        } else {
                            domainStatusTextView.setText(R.string.domain_rules_enabled);
                            domainSwitch.setChecked(true);
                        }

                        if (!isDomainRuleEmpty) {
                            infoTextView.setVisibility(View.VISIBLE);
                            swipeContainer.setVisibility(View.VISIBLE);
                            swipeContainer.setOnRefreshListener(() ->
                                    loadBlockedUrls()
                            );
                            loadBlockedUrls();
                        } else {
                            infoTextView.setVisibility(View.INVISIBLE);
                            swipeContainer.setVisibility(View.INVISIBLE);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        LogUtils.error(e.getMessage(), e);
                    }
                });
    }

    private void loadBlockedUrls() {
        Single.create((SingleOnSubscribe<List<ReportBlockedUrl>>) emitter -> {
            List<ReportBlockedUrl> blockedUrls = FirewallUtils.getInstance().getReportBlockedUrl();
            emitter.onSuccess(blockedUrls);
        })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<ReportBlockedUrl>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull List<ReportBlockedUrl> blockedUrls) {
                        updateBlockedUrls(blockedUrls);

                        Context context = getContext();
                        TextView infoTextView = ((Activity) context).findViewById(R.id.infoTextView);
                        if (infoTextView != null) {
                            infoTextView.setText(String.format("%s%s",
                                    context.getString(R.string.last_day_blocked), blockedUrls.size()));
                        }
                        SwipeRefreshLayout swipeContainer = ((Activity) context).findViewById(R.id.swipeContainer);
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

    private void updateBlockedUrls(List<ReportBlockedUrl> blockedUrls) {
        this.blockedUrls.clear();
        this.blockedUrls.addAll(blockedUrls);
        blockedUrlAdapter.notifyDataSetChanged();
    }

    private void setFirewallInfo() {
        Single.create((SingleOnSubscribe<Boolean>) emitter -> {
            boolean isFirewallRuleEmpty = contentBlocker.isFirewallRuleEmpty();
            emitter.onSuccess(isFirewallRuleEmpty);
        })
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Boolean>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull Boolean isFirewallRuleEmpty) {
                        if (contentBlocker == null || isFirewallRuleEmpty) {
                            firewallStatusTextView.setText(R.string.firewall_rules_disabled);
                            firewallSwitch.setChecked(false);
                        } else {
                            firewallStatusTextView.setText(R.string.firewall_rules_enabled);
                            firewallSwitch.setChecked(true);
                        }
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        LogUtils.error(e.getMessage(), e);
                    }
                });
    }

    private void setAppDisablerInfo() {
        boolean disablerEnabled = AppPreferences.getInstance().isAppDisablerToggleEnabled();
        if (disablerEnabled) {
            disablerStatusTextView.setText(R.string.app_disabler_enabled);
            disablerSwitch.setChecked(true);
        } else {
            disablerStatusTextView.setText(R.string.app_disabler_disabled);
            disablerSwitch.setChecked(false);
        }
    }

    private void setAppComponentInfo() {
        boolean appComponentEnabled = AppPreferences.getInstance().isAppComponentToggleEnabled();
        if (appComponentEnabled) {
            appComponentStatusTextView.setText(R.string.app_component_enabled);
            appComponentSwitch.setChecked(true);
        } else {
            appComponentStatusTextView.setText(R.string.app_component_disabled);
            appComponentSwitch.setChecked(false);
        }
    }

    private static class AppDisablerAsyncTask extends AsyncTask<Void, Void, Void> {
        private HomeTabFragment parentFragment;
        private ProgressDialog dialog;

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
        private HomeTabFragment parentFragment;
        private ProgressDialog dialog;

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
        private FragmentManager fragmentManager;
        private FirewallDialogFragment fragment;
        private HomeTabFragment parentFragment;
        private ContentBlocker contentBlocker;
        private Handler handler;
        private boolean isDomain;
        private boolean isDomainRuleEmpty;
        private boolean isFirewallRuleEmpty;
        private WeakReference<Context> contextReference;

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

    private static class ExportDomainsAsyncTask extends AsyncTask<Void, Void, Void> {

        private WeakReference<Context> contextReference;

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
