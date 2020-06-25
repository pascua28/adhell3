package com.fusionjack.adhell3.fragments;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.webkit.URLUtil;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.BlockUrlProviderAdapter;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.BlockUrl;
import com.fusionjack.adhell3.db.entity.BlockUrlProvider;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.BlockUrlUtils;
import com.fusionjack.adhell3.viewmodel.BlockUrlProvidersViewModel;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.leinardi.android.speeddial.SpeedDialActionItem;
import com.leinardi.android.speeddial.SpeedDialView;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static com.fusionjack.adhell3.fragments.DomainTabPageFragment.PROVIDER_CONTENT_PAGE;

public class ProviderListFragment extends Fragment {
    private final int SELECT_FILE_REQUEST_CODE = 42;
    private Context context;
    private FragmentActivity activity;
    private EditText providerEditText;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = getContext();
        this.activity = getActivity();
        App.setAppContext(context);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_provider, container, false);
        ProgressBar loadingBar = view.findViewById(R.id.loadingBarProvider);

        // Set URL limit
        TextView hintTextView = view.findViewById(R.id.providerInfoTextView);
        String strFormat = getResources().getString(R.string.provider_info);
        hintTextView.setText(String.format(strFormat, AdhellAppIntegrity.BLOCK_URL_LIMIT));

        // Set domain count to 0
        TextView infoTextView = view.findViewById(R.id.infoTextView);
        strFormat = getResources().getString(R.string.total_unique_domains);
        infoTextView.setText(String.format(strFormat, 0));

        // Provider list
        ListView providerListView = view.findViewById(R.id.providerListView);
        if (providerListView.getVisibility() == View.VISIBLE) loadingBar.setVisibility(View.VISIBLE);
        BlockUrlProvidersViewModel providersViewModel = new ViewModelProvider(activity).get(BlockUrlProvidersViewModel.class);
        providersViewModel.getBlockUrlProviders().observe(getViewLifecycleOwner(), blockUrlProviders -> {
            ListAdapter adapter = providerListView.getAdapter();
            if (adapter == null) {
                BlockUrlProviderAdapter arrayAdapter = new BlockUrlProviderAdapter(context, blockUrlProviders);
                providerListView.setAdapter(arrayAdapter);
            }
        });

        providerListView.setOnItemClickListener((parent, view1, position, id) -> {
            BlockUrlProvider provider = (BlockUrlProvider) parent.getItemAtPosition(position);
            List<Fragment> fragments = getParentFragmentManager().getFragments();
            for (Fragment fragment : fragments) {
                if (fragment instanceof ProviderContentFragment) {
                    ((ProviderContentFragment) fragment).setProviderId(provider.id);
                }
            }
            TabLayout tabLayout = null;
            if (getParentFragment() != null) {
                tabLayout = getParentFragment().requireActivity().findViewById(R.id.domains_sliding_tabs);
            }
            if (tabLayout != null) {
                TabLayout.Tab tab = tabLayout.getTabAt(PROVIDER_CONTENT_PAGE);
                if (tab != null) {
                    tab.select();
                }
            }
        });

        SwipeRefreshLayout providerSwipeContainer = view.findViewById(R.id.providerSwipeContainer);
        providerSwipeContainer.setOnRefreshListener(() -> {
            if (loadingBar != null) {
                loadingBar.setVisibility(View.VISIBLE);
            }
            new UpdateProviderAsyncTask(context, true).execute();
        });

        SpeedDialView speedDialView = view.findViewById(R.id.provider_actions);
        speedDialView.addActionItem(new SpeedDialActionItem.Builder(R.id.action_add_provider, getResources().getDrawable(R.drawable.ic_event_note_white_24dp, requireContext().getTheme()))
                .setLabel(getString(R.string.dialog_add_provider_title))
                .setFabBackgroundColor(getResources().getColor(R.color.colorFab, requireContext().getTheme()))
                .setLabelColor(getResources().getColor(R.color.colorText, requireContext().getTheme()))
                .setLabelBackgroundColor(getResources().getColor(R.color.colorBorder, requireContext().getTheme()))
                .setFabSize(com.google.android.material.floatingactionbutton.FloatingActionButton.SIZE_NORMAL)
                .setFabImageTintColor(Color.WHITE)
                .setLabelClickable(false)
                .create());

        speedDialView.setOnActionSelectedListener(actionItem -> {
            speedDialView.close();
            if (actionItem.getId() == R.id.action_add_provider) {
                View dialogView = inflater.inflate(R.layout.dialog_add_provider, container, false);
                providerEditText = dialogView.findViewById(R.id.providerEditText);
                AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                        .setView(dialogView)
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                            String provider = providerEditText.getText().toString();
                            if (isValidUri(provider)) {
                                new AddProviderAsyncTask(provider, context).execute();
                            } else {
                                MainActivity.makeSnackbar("Url is invalid", Snackbar.LENGTH_LONG)
                                        .show();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .create();

                alertDialog.show();

                Button filePicker = dialogView.findViewById(R.id.filePicker);
                filePicker.setOnClickListener(v1 -> {
                    MainActivity.setSelectFileActivityLaunched(true);
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                    intent.setType("*/*");

                    startActivityForResult(intent, SELECT_FILE_REQUEST_CODE);
                });

                RadioGroup providerTypeRadioGroup = dialogView.findViewById(R.id.providerTypeRadioGroup);
                providerTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                    switch (checkedId) {
                        case R.id.providerTypeRemote:
                            filePicker.setVisibility(View.GONE);
                            providerEditText.setEnabled(true);
                            providerEditText.setHint(R.string.dialog_add_provider_hint);
                            providerEditText.setText("");
                            break;
                        case R.id.providerTypeLocal:
                            filePicker.setVisibility(View.VISIBLE);
                            providerEditText.setEnabled(false);
                            providerEditText.setHint("");
                            providerEditText.setText("");
                            break;
                    }
                });
                return true;
            }
            return false;
        });

        final boolean[] noScroll = { false };
        final int[] previousDistanceFromFirstCellToTop = {0};
        providerListView.setOnScrollListener(new ExpandableListView.OnScrollListener() {
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
                    View firstCell = providerListView.getChildAt(0);
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

        if (providerListView.getVisibility() == View.GONE) {
            AlphaAnimation animation = new AlphaAnimation(0f, 1f);
            animation.setDuration(500);
            animation.setStartOffset(50);
            animation.setFillAfter(true);

            providerListView.setVisibility(View.VISIBLE);
            providerListView.startAnimation(animation);
        } else {
            loadingBar.setVisibility(View.GONE);
        }

        new SetDomainCountAsyncTask(context, 100).execute();

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == SELECT_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            Uri uri;
            if (resultData != null) {
                uri = resultData.getData();
                providerEditText.setText(uri != null ? uri.toString() : null);
            }
        }
    }

    private boolean isValidUri(String uri) {
        return URLUtil.isHttpUrl(uri) || URLUtil.isHttpsUrl(uri) || URLUtil.isContentUrl(uri) || URLUtil.isFileUrl(uri);
    }

    private static class AddProviderAsyncTask extends AsyncTask<Void, Void, Void> {
        private final String provider;
        private final WeakReference<Context> contextWeakReference;
        private BlockUrlProvider blockUrlProvider;

        AddProviderAsyncTask(String provider, Context context) {
            this.provider = provider;
            this.contextWeakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();

            blockUrlProvider = new BlockUrlProvider();
            blockUrlProvider.url = provider;
            blockUrlProvider.count = 0;
            blockUrlProvider.deletable = true;
            blockUrlProvider.lastUpdated = new Date();
            blockUrlProvider.selected = false;
            blockUrlProvider.id = appDatabase.blockUrlProviderDao().insertAll(blockUrlProvider)[0];
            blockUrlProvider.policyPackageId = AdhellAppIntegrity.DEFAULT_POLICY_ID;
            appDatabase.blockUrlProviderDao().updateBlockUrlProviders(blockUrlProvider);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Context context = contextWeakReference.get();

            if (context != null) {
                ListView listView = ((Activity) context).findViewById(R.id.providerListView);
                if (listView != null) {
                    if (listView.getAdapter() instanceof BlockUrlProviderAdapter) {
                        BlockUrlProviderAdapter adapter = (BlockUrlProviderAdapter) listView.getAdapter();
                        adapter.add(blockUrlProvider);
                        adapter.notifyDataSetChanged();

                        new LoadProviderAsyncTask(blockUrlProvider, context).execute();
                    }
                }

                ProgressBar loadingBar = ((Activity) context).findViewById(R.id.loadingBarProvider);
                if (loadingBar != null) {
                    loadingBar.setVisibility(View.GONE);
                }
            }
        }
    }

    private static class LoadProviderAsyncTask extends AsyncTask<Void, Void, Void> {
        private final BlockUrlProvider provider;
        private final WeakReference<Context> contextWeakReference;

        LoadProviderAsyncTask(BlockUrlProvider provider, Context context) {
            this.provider = provider;
            this.contextWeakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            try {
                List<BlockUrl> blockUrls = BlockUrlUtils.loadBlockUrls(provider);
                provider.count = blockUrls.size();
                provider.lastUpdated = new Date();
                appDatabase.blockUrlProviderDao().updateBlockUrlProviders(provider);
                appDatabase.blockUrlDao().insertAll(blockUrls);
            } catch (Exception e) {
                appDatabase.blockUrlProviderDao().delete(provider);
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Context context = contextWeakReference.get();
            if (context != null) {
                ListView listView = ((Activity) context).findViewById(R.id.providerListView);
                if (listView != null) {
                    if (listView.getAdapter() instanceof BlockUrlProviderAdapter) {
                        BlockUrlProviderAdapter adapter = (BlockUrlProviderAdapter) listView.getAdapter();
                        adapter.notifyDataSetChanged();
                        AlphaAnimation animation = new AlphaAnimation(0f, 1f);
                        animation.setDuration(500);
                        animation.setStartOffset(50);
                        animation.setFillAfter(true);
                        listView.setVisibility(View.VISIBLE);
                        listView.startAnimation(animation);
                    }
                }
                ProgressBar loadingBar = ((Activity) context).findViewById(R.id.loadingBarProvider);
                if (loadingBar != null) {
                    loadingBar.setVisibility(View.GONE);
                }
            }
        }
    }

    private static class UpdateProviderAsyncTask extends AsyncTask<Void, Void, Void> {
        private final WeakReference<Context> contextWeakReference;
        private final boolean updateProviders;

        UpdateProviderAsyncTask(Context context, boolean updateProviders) {
            this.contextWeakReference = new WeakReference<>(context);
            this.updateProviders = updateProviders;
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Context context = contextWeakReference.get();
            if (context != null) {
                if (AdhellFactory.getInstance().hasInternetAccess(context) && updateProviders) {
                    AdhellFactory.getInstance().updateAllProviders();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            Context context = contextWeakReference.get();
            if (context != null) {
                ProgressBar loadingBar = ((Activity) context).findViewById(R.id.loadingBarProvider);
                if (loadingBar != null) {
                    loadingBar.setVisibility(View.GONE);
                }

                new SetProviderAsyncTask(context).execute();

                SwipeRefreshLayout swipeContainer = ((Activity) context).findViewById(R.id.providerSwipeContainer);
                if (swipeContainer != null) {
                    swipeContainer.setRefreshing(false);
                }

                new SetDomainCountAsyncTask(context, 0).execute();
            }
        }
    }

    private static class SetProviderAsyncTask extends AsyncTask<Void, Void, List<BlockUrlProvider>> {
        private final WeakReference<Context> contextWeakReference;

        SetProviderAsyncTask(Context context) {
            this.contextWeakReference = new WeakReference<>(context);
        }

        @Override
        protected List<BlockUrlProvider> doInBackground(Void... voids) {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            return appDatabase.blockUrlProviderDao().getAll2();
        }

        @Override
        protected void onPostExecute(List<BlockUrlProvider> providers) {
            Context context = contextWeakReference.get();
            if (context != null) {
                ListView listView = ((Activity) context).findViewById(R.id.providerListView);
                if (listView != null) {
                    if (listView.getAdapter() instanceof BlockUrlProviderAdapter) {
                        BlockUrlProviderAdapter adapter = (BlockUrlProviderAdapter) listView.getAdapter();
                        for (int i = 0; i < adapter.getCount(); i++) {
                            BlockUrlProvider provider = adapter.getItem(i);
                            if (provider != null) {
                                BlockUrlProvider dbProvider = getProvider(provider.id, providers);
                                if (dbProvider != null) {
                                    provider.count = dbProvider.count;
                                    provider.lastUpdated = dbProvider.lastUpdated;
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();

                        ProgressBar loadingBar = ((Activity) context).findViewById(R.id.loadingBarProvider);
                        if (loadingBar != null) {
                            loadingBar.setVisibility(View.GONE);
                        }
                        if (listView.getVisibility() == View.GONE) {
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
        }

        private BlockUrlProvider getProvider(long id, List<BlockUrlProvider> providers) {
            for (BlockUrlProvider provider : providers) {
                if (provider.id == id) {
                    return provider;
                }
            }
            return null;
        }
    }

    private static class SetDomainCountAsyncTask extends AsyncTask<Void, Integer, Integer> {
        private final WeakReference<Context> contextWeakReference;
        private final int delay;

        SetDomainCountAsyncTask(Context context, int delay) {
            this.contextWeakReference = new WeakReference<>(context);
            this.delay = delay;
        }

        @Override
        protected Integer doInBackground(Void... voids) {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            try {
                Thread.sleep(delay);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return BlockUrlUtils.getAllBlockedUrlsCount(appDatabase);
        }

        @Override
        protected void onPostExecute(Integer count) {
            Context context = contextWeakReference.get();
            if (context != null) {
                TextView infoTextView = ((Activity) context).findViewById(R.id.infoTextView);
                if (infoTextView != null) {
                    String strFormat = context.getResources().getString(R.string.total_unique_domains);
                    infoTextView.setText(String.format(strFormat, count));
                }

                ProgressBar loadingBar = ((Activity) context).findViewById(R.id.loadingBarProvider);
                if (loadingBar != null) {
                    loadingBar.setVisibility(View.GONE);
                }
            }
        }
    }
}
