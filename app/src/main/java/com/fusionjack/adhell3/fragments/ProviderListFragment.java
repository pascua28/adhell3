package com.fusionjack.adhell3.fragments;

import android.app.Activity;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.webkit.URLUtil;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.fusionjack.adhell3.App;
import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.BlockUrlProviderAdapter;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.db.entity.BlockUrl;
import com.fusionjack.adhell3.db.entity.BlockUrlProvider;
import com.fusionjack.adhell3.tasks.SetDomainCountAsyncTask;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.BlockUrlUtils;
import com.fusionjack.adhell3.viewmodel.BlockUrlProvidersViewModel;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import static android.app.Activity.RESULT_OK;
import static com.fusionjack.adhell3.fragments.DomainTabPageFragment.PROVIDER_CONTENT_PAGE;

public class ProviderListFragment extends Fragment {
    private int SELECT_FILE_REQUEST_CODE = 42;
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

        new SetDomainCountAsyncTask(context).execute();

        // Provider list
        ListView providerListView = view.findViewById(R.id.providerListView);
        if (providerListView.getVisibility() == View.VISIBLE) loadingBar.setVisibility(View.VISIBLE);
        BlockUrlProvidersViewModel providersViewModel = ViewModelProviders.of(activity).get(BlockUrlProvidersViewModel.class);
        providersViewModel.getBlockUrlProviders().observe(this, blockUrlProviders -> {
            ListAdapter adapter = providerListView.getAdapter();
            if (adapter == null) {
                BlockUrlProviderAdapter arrayAdapter = new BlockUrlProviderAdapter(context, blockUrlProviders);
                providerListView.setAdapter(arrayAdapter);
            }
        });

        providerListView.setOnItemClickListener((parent, view1, position, id) -> {
            BlockUrlProvider provider = (BlockUrlProvider) parent.getItemAtPosition(position);
            List<Fragment> fragments = Objects.requireNonNull(getFragmentManager()).getFragments();
            for (Fragment fragment : fragments) {
                if (fragment instanceof ProviderContentFragment) {
                    ((ProviderContentFragment) fragment).setProviderId(provider.id);
                }
            }
            TabLayout tabLayout = null;
            if (getParentFragment() != null) {
                tabLayout = Objects.requireNonNull(getParentFragment().getActivity()).findViewById(R.id.domains_sliding_tabs);
            }
            if (tabLayout != null) {
                TabLayout.Tab tab = tabLayout.getTabAt(PROVIDER_CONTENT_PAGE);
                if (tab != null) {
                    tab.select();
                }
            }
        });

        SwipeRefreshLayout dnsSwipeContainer = view.findViewById(R.id.providerSwipeContainer);
        dnsSwipeContainer.setOnRefreshListener(() -> {
            if (loadingBar != null) {
                loadingBar.setVisibility(View.VISIBLE);
            }
            new UpdateProviderAsyncTask(context).execute();
        });

        FloatingActionsMenu providerFloatMenu = view.findViewById(R.id.provider_actions);
        FloatingActionButton actionAddProvider = view.findViewById(R.id.action_add_provider);
        actionAddProvider.setIcon(R.drawable.ic_event_note_white_24dp);
        actionAddProvider.setOnClickListener(v -> {
            providerFloatMenu.collapse();
            View dialogView = inflater.inflate(R.layout.dialog_add_provider, container, false);
            providerEditText = dialogView.findViewById(R.id.providerEditText);
            new AlertDialog.Builder(context)
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        String provider = providerEditText.getText().toString();
                        if (isValidUri(provider)) {
                            new AddProviderAsyncTask(provider, context).execute();
                        } else {
                            Toast.makeText(getContext(), "Url is invalid", Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton(android.R.string.no, null).show();

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

        UpdateProviderAsyncTask(Context context) {
            this.contextWeakReference = new WeakReference<>(context);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            Context context = contextWeakReference.get();
            if (context != null) {
                if (AdhellFactory.getInstance().hasInternetAccess(context)) {
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

                new SetDomainCountAsyncTask(context).execute();
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
}
