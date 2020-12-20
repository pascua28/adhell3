package com.fusionjack.adhell3.fragments;

import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ExpandableListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.MenuCompat;

import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.AppInfoAdapter;
import com.fusionjack.adhell3.databinding.DialogQuestionBinding;
import com.fusionjack.adhell3.databinding.DialogSetDnsBinding;
import com.fusionjack.adhell3.databinding.FragmentDnsBinding;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.AppInfo;
import com.fusionjack.adhell3.db.entity.DnsPackage;
import com.fusionjack.adhell3.db.repository.AppRepository;
import com.fusionjack.adhell3.model.AppFlag;
import com.fusionjack.adhell3.tasks.SetAppAsyncTask;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.google.android.material.snackbar.Snackbar;
import com.leinardi.android.speeddial.SpeedDialActionItem;

import java.util.List;

public class DnsFragment extends AppFragment {

    private FragmentDnsBinding binding;

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
        DialogQuestionBinding dialogQuestionBinding = DialogQuestionBinding.inflate(LayoutInflater.from(getContext()));
        dialogQuestionBinding.titleTextView.setText(R.string.dialog_toggle_title);
        dialogQuestionBinding.questionTextView.setText(R.string.dialog_toggle_info);
        AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                .setView(dialogQuestionBinding.getRoot())
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

                            loadAppList(type);
                        })
                )
                .setNegativeButton(android.R.string.no, null)
                .create();

        alertDialog.show();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);

        binding = FragmentDnsBinding.inflate(inflater);
        appFlag = AppFlag.createDnsFlag();

        binding.dnsAppsList.setAdapter(adapter);

        if (AppPreferences.getInstance().isDnsNotEmpty()) {
            binding.dnsAppsList.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                AppInfoAdapter adapter = (AppInfoAdapter) adView.getAdapter();
                new SetAppAsyncTask(adapter.getItem(position), appFlag, context).execute();
            });
        }

        int themeColor = context.getResources().getColor(R.color.colorBottomNavUnselected, context.getTheme());

        binding.filterButton.setColorFilter(themeColor, PorterDuff.Mode.SRC_IN);
        binding.filterButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, binding.filterButton);
            popup.getMenuInflater().inflate(R.menu.filter_appinfo_menu, popup.getMenu());
            popup.getMenu().findItem(R.id.highlightRunningApps).setChecked(filterAppInfo.getHighlightRunningApps());
            popup.getMenu().findItem(R.id.filterSystemApps).setChecked(filterAppInfo.getSystemAppsFilter());
            popup.getMenu().findItem(R.id.filterUserApps).setChecked(filterAppInfo.getUserAppsFilter());
            popup.getMenu().findItem(R.id.filterRunningApps).setChecked(filterAppInfo.getRunningAppsFilter());
            popup.getMenu().findItem(R.id.filterStoppedApps).setChecked(filterAppInfo.getStoppedAppsFilter());
            MenuCompat.setGroupDividerEnabled(popup.getMenu(), true);
            popup.setOnMenuItemClickListener(item -> {
                item.setChecked(!item.isChecked());
                int id = item.getItemId();
                    if (id == R.id.highlightRunningApps) {
                        filterAppInfo.setHighlightRunningApps(item.isChecked());
                    } else if (id == R.id.filterSystemApps) {
                        filterAppInfo.setSystemAppsFilter(item.isChecked());
                        if (!item.isChecked()) {
                            if (!popup.getMenu().findItem(R.id.filterUserApps).isChecked()) {
                                popup.getMenu().findItem(R.id.filterUserApps).setChecked(true);
                                filterAppInfo.setUserAppsFilter(true);
                            }
                        }
                    } else if (id == R.id.filterUserApps) {
                        filterAppInfo.setUserAppsFilter(item.isChecked());
                        if (!item.isChecked()) {
                            if (!popup.getMenu().findItem(R.id.filterSystemApps).isChecked()) {
                                popup.getMenu().findItem(R.id.filterSystemApps).setChecked(true);
                                filterAppInfo.setSystemAppsFilter(true);
                            }
                        }
                    } else if (id == R.id.filterRunningApps) {
                        filterAppInfo.setRunningAppsFilter(item.isChecked());
                        if (!item.isChecked()) {
                            if (!popup.getMenu().findItem(R.id.filterStoppedApps).isChecked()) {
                                popup.getMenu().findItem(R.id.filterStoppedApps).setChecked(true);
                                filterAppInfo.setStoppedAppsFilter(true);
                            }
                        }
                    } else if (id == R.id.filterStoppedApps) {
                        filterAppInfo.setStoppedAppsFilter(item.isChecked());
                        if (!item.isChecked()) {
                            if (!popup.getMenu().findItem(R.id.filterRunningApps).isChecked()) {
                                popup.getMenu().findItem(R.id.filterRunningApps).setChecked(true);
                                filterAppInfo.setRunningAppsFilter(true);
                            }
                        }
                    }
                    if (!filterAppInfo.getHighlightRunningApps() &&
                        filterAppInfo.getSystemAppsFilter() &&
                        filterAppInfo.getUserAppsFilter() &&
                        filterAppInfo.getRunningAppsFilter() &&
                        filterAppInfo.getStoppedAppsFilter()
                ) {
                    binding.filterButton.setColorFilter(themeColor, PorterDuff.Mode.SRC_IN);
                } else {
                    int accentColor = context.getResources().getColor(R.color.colorAccent, context.getTheme());
                    binding.filterButton.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN);
                }

                MainActivity.setFilterAppInfo(filterAppInfo);
                resetSearchView();
                loadAppList(type);
                return false;
            });
            popup.show();
        });

        binding.dnsSwipeContainer.setOnRefreshListener(() -> {
            loadAppList(type);
            resetSearchView();
        });

        binding.dnsActions.addActionItem(new SpeedDialActionItem.Builder(R.id.action_set_dns, ResourcesCompat.getDrawable(getResources(), R.drawable.ic_dns_white_24dp, requireContext().getTheme()))
                .setLabel(getString(R.string.dialog_set_dns_title))
                .setFabBackgroundColor(getResources().getColor(R.color.colorFab, requireContext().getTheme()))
                .setLabelColor(getResources().getColor(R.color.colorText, requireContext().getTheme()))
                .setLabelBackgroundColor(getResources().getColor(R.color.colorBorder, requireContext().getTheme()))
                .setFabSize(com.google.android.material.floatingactionbutton.FloatingActionButton.SIZE_NORMAL)
                .setFabImageTintColor(Color.WHITE)
                .setLabelClickable(false)
                .create());

        binding.dnsActions.setOnActionSelectedListener(actionItem -> {
            binding.dnsActions.close();
            if (actionItem.getId() == R.id.action_set_dns) {
                DialogSetDnsBinding dialogSetDnsBinding = DialogSetDnsBinding.inflate(inflater);
                if (AppPreferences.getInstance().isDnsNotEmpty()) {
                    dialogSetDnsBinding.primaryDnsEditText.setText(AppPreferences.getInstance().getDns1());
                    dialogSetDnsBinding.secondaryDnsEditText.setText(AppPreferences.getInstance().getDns2());
                }
                dialogSetDnsBinding.primaryDnsEditText.requestFocus();

                new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                        .setView(dialogSetDnsBinding.getRoot())
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                            Handler handler = new Handler(Looper.getMainLooper()) {
                                @Override
                                public void handleMessage(@NonNull Message msg) {
                                    if (getActivity() instanceof MainActivity) {
                                        MainActivity mainActivity = (MainActivity) getActivity();
                                        mainActivity.makeSnackbar(getString(Integer.parseInt(msg.obj.toString())), Snackbar.LENGTH_LONG)
                                                .show();
                                    }
                                }
                            };

                            String primaryDns = dialogSetDnsBinding.primaryDnsEditText.getText().toString();
                            String secondaryDns = dialogSetDnsBinding.secondaryDnsEditText.getText().toString();
                            AdhellFactory.getInstance().setDns(primaryDns, secondaryDns, handler);

                            if (AppPreferences.getInstance().isDnsNotEmpty()) {
                                binding.dnsAppsList.setEnabled(true);
                                binding.dnsAppsList.setOnItemClickListener((AdapterView<?> adView, View view2, int position, long id) -> {
                                    AppInfoAdapter adapter = (AppInfoAdapter) adView.getAdapter();
                                    new SetAppAsyncTask(adapter.getItem(position), appFlag, context).execute();
                                });
                            } else {
                                binding.dnsAppsList.setEnabled(false);
                            }

                            if (binding.dnsAppsList.getAdapter() instanceof AppInfoAdapter) {
                                ((AppInfoAdapter) binding.dnsAppsList.getAdapter()).notifyDataSetChanged();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .show();

                return true;
            }
            return false;
        });

        final boolean[] noScroll = { false };
        final int[] previousDistanceFromFirstCellToTop = {0};
        binding.dnsAppsList.setOnScrollListener(new ExpandableListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL && noScroll[0]) {
                    if (binding.dnsActions.isShown()) binding.dnsActions.hide();
                    else binding.dnsActions.show();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem == 0 && visibleItemCount == totalItemCount) {
                    noScroll[0] = true;
                } else {
                    noScroll[0] = false;
                    View firstCell = binding.dnsAppsList.getChildAt(0);
                    if (firstCell == null) {
                        return;
                    }
                    int distanceFromFirstCellToTop = firstVisibleItem * firstCell.getHeight() - firstCell.getTop();
                    if (distanceFromFirstCellToTop < previousDistanceFromFirstCellToTop[0]) {
                        binding.dnsActions.show();
                    } else if (distanceFromFirstCellToTop > previousDistanceFromFirstCellToTop[0]) {
                        binding.dnsActions.hide();
                    }
                    previousDistanceFromFirstCellToTop[0] = distanceFromFirstCellToTop;
                }
            }
        });

        rootView = binding.getRoot();
        super.onCreateView(inflater, container, savedInstanceState);

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Set filter button color
        int themeColor = context.getResources().getColor(R.color.colorBottomNavUnselected, context.getTheme());
        if (!filterAppInfo.getHighlightRunningApps() &&
                filterAppInfo.getSystemAppsFilter() &&
                filterAppInfo.getUserAppsFilter() &&
                filterAppInfo.getRunningAppsFilter() &&
                filterAppInfo.getStoppedAppsFilter()
        ) {
            binding.filterButton.setColorFilter(themeColor, PorterDuff.Mode.SRC_IN);
        } else {
            int accentColor = context.getResources().getColor(R.color.colorAccent, context.getTheme());
            binding.filterButton.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN);
        }

        loadAppList(type);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
        rootView = null;
    }
}
