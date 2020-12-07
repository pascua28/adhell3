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
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.MenuCompat;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fusionjack.adhell3.MainActivity;
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
import com.google.android.material.snackbar.Snackbar;
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

        int themeColor = context.getResources().getColor(R.color.colorBottomNavUnselected, context.getTheme());

        ImageView filterButton = view.findViewById(R.id.filterButton);
        filterButton.setColorFilter(themeColor, PorterDuff.Mode.SRC_IN);
        filterButton.setOnClickListener(v -> {
            PopupMenu popup = new PopupMenu(context, filterButton);
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
                    } else if (id == R.id.filterUserApps) {
                        filterAppInfo.setUserAppsFilter(item.isChecked());
                    } else if (id == R.id.filterRunningApps) {
                        filterAppInfo.setRunningAppsFilter(item.isChecked());
                    } else if (id == R.id.filterStoppedApps) {
                        filterAppInfo.setStoppedAppsFilter(item.isChecked());
                    }
                    if (!filterAppInfo.getHighlightRunningApps() &&
                        filterAppInfo.getSystemAppsFilter() &&
                        filterAppInfo.getUserAppsFilter() &&
                        filterAppInfo.getRunningAppsFilter() &&
                        filterAppInfo.getStoppedAppsFilter()
                ) {
                    filterButton.setColorFilter(themeColor, PorterDuff.Mode.SRC_IN);
                } else {
                    int accentColor = context.getResources().getColor(R.color.colorAccent, context.getTheme());
                    filterButton.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN);
                }

                MainActivity.setFilterAppInfo(filterAppInfo);
                resetSearchView();
                loadAppList(type, loadingBar, listView);
                return false;
            });
            popup.show();
        });

        SwipeRefreshLayout dnsSwipeContainer = view.findViewById(R.id.dnsSwipeContainer);
        dnsSwipeContainer.setOnRefreshListener(() -> {
            loadAppList(type, loadingBar, listView);
            dnsSwipeContainer.setRefreshing(false);
            resetSearchView();
        });

        SpeedDialView speedDialView = view.findViewById(R.id.dns_actions);
        speedDialView.addActionItem(new SpeedDialActionItem.Builder(R.id.action_set_dns, ResourcesCompat.getDrawable(getResources(), R.drawable.ic_dns_white_24dp, requireContext().getTheme()))
                .setLabel(getString(R.string.dialog_set_dns_title))
                .setFabBackgroundColor(getResources().getColor(R.color.colorFab, requireContext().getTheme()))
                .setLabelColor(getResources().getColor(R.color.colorText, requireContext().getTheme()))
                .setLabelBackgroundColor(getResources().getColor(R.color.colorBorder, requireContext().getTheme()))
                .setFabSize(com.google.android.material.floatingactionbutton.FloatingActionButton.SIZE_NORMAL)
                .setFabImageTintColor(Color.WHITE)
                .setLabelClickable(false)
                .create());

        speedDialView.setOnActionSelectedListener(actionItem -> {
            speedDialView.close();
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
                                    MainActivity.makeSnackbar(getString(Integer.parseInt(msg.obj.toString())), Snackbar.LENGTH_LONG)
                                            .show();
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

                return true;
            }
            return false;
        });

        final boolean[] noScroll = { false };
        final int[] previousDistanceFromFirstCellToTop = {0};
        listView.setOnScrollListener(new ExpandableListView.OnScrollListener() {
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
                    View firstCell = listView.getChildAt(0);
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

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Set filter button color
        int themeColor = context.getResources().getColor(R.color.colorBottomNavUnselected, context.getTheme());
        ImageView filterButton = requireView().findViewById(R.id.filterButton);
        if (!filterAppInfo.getHighlightRunningApps() &&
                filterAppInfo.getSystemAppsFilter() &&
                filterAppInfo.getUserAppsFilter() &&
                filterAppInfo.getRunningAppsFilter() &&
                filterAppInfo.getStoppedAppsFilter()
        ) {
            filterButton.setColorFilter(themeColor, PorterDuff.Mode.SRC_IN);
        } else {
            int accentColor = context.getResources().getColor(R.color.colorAccent, context.getTheme());
            filterButton.setColorFilter(accentColor, PorterDuff.Mode.SRC_IN);
        }

        loadAppList(type, loadingBar, listView);
    }
}
