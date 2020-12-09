package com.fusionjack.adhell3.fragments;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.webkit.URLUtil;
import android.widget.AbsListView;
import android.widget.ExpandableListView;
import android.widget.ListAdapter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;

import com.fusionjack.adhell3.MainActivity;
import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.BlockUrlProviderAdapter;
import com.fusionjack.adhell3.databinding.DialogAddProviderBinding;
import com.fusionjack.adhell3.databinding.FragmentProviderBinding;
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

import java.lang.ref.WeakReference;
import java.util.Date;
import java.util.List;

import static com.fusionjack.adhell3.fragments.DomainTabPageFragment.PROVIDER_CONTENT_PAGE;

public class ProviderListFragment extends Fragment {
    private Context context;
    private FragmentActivity activity;
    private DialogAddProviderBinding dialogAddProviderBinding;
    private static String strTotalUniqueDomains;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = getContext();
        this.activity = getActivity();
    }

    private final ActivityResultLauncher<String[]> openDocumentLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), result -> {
        if (result != null) {
            dialogAddProviderBinding.providerEditText.setText(result.toString());
        }
    });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        FragmentProviderBinding binding = FragmentProviderBinding.inflate(inflater);

        strTotalUniqueDomains = context.getResources().getString(R.string.total_unique_domains);

        // Set URL limit
        String strFormat = getResources().getString(R.string.provider_info);
        binding.providerInfoTextView.setText(String.format(strFormat, AdhellAppIntegrity.BLOCK_URL_LIMIT));

        // Set domain count to 0
        strFormat = getResources().getString(R.string.total_unique_domains);
        binding.infoTextView.setText(String.format(strFormat, 0));

        // Provider list
        if (binding.providerListView.getVisibility() == View.VISIBLE) binding.loadingBarProvider.setVisibility(View.VISIBLE);
        BlockUrlProvidersViewModel providersViewModel = new ViewModelProvider(activity).get(BlockUrlProvidersViewModel.class);
        providersViewModel.getBlockUrlProviders().observe(getViewLifecycleOwner(), blockUrlProviders -> {
            ListAdapter adapter = binding.providerListView.getAdapter();
            if (adapter == null) {
                BlockUrlProviderAdapter arrayAdapter = new BlockUrlProviderAdapter(context, blockUrlProviders);
                binding.providerListView.setAdapter(arrayAdapter);
            }
        });

        binding.providerListView.setOnItemClickListener((parent, view1, position, id) -> {
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

        binding.providerSwipeContainer.setOnRefreshListener(() -> {
            binding.loadingBarProvider.setVisibility(View.VISIBLE);
            new UpdateProviderAsyncTask(context, true, binding).execute();
        });

        binding.providerActions.addActionItem(new SpeedDialActionItem.Builder(R.id.action_add_provider, ResourcesCompat.getDrawable(getResources(), R.drawable.ic_event_note_white_24dp, requireContext().getTheme()))
                .setLabel(getString(R.string.dialog_add_provider_title))
                .setFabBackgroundColor(getResources().getColor(R.color.colorFab, requireContext().getTheme()))
                .setLabelColor(getResources().getColor(R.color.colorText, requireContext().getTheme()))
                .setLabelBackgroundColor(getResources().getColor(R.color.colorBorder, requireContext().getTheme()))
                .setFabSize(com.google.android.material.floatingactionbutton.FloatingActionButton.SIZE_NORMAL)
                .setFabImageTintColor(Color.WHITE)
                .setLabelClickable(false)
                .create());

        binding.providerActions.setOnActionSelectedListener(actionItem -> {
            binding.providerActions.close();
            if (actionItem.getId() == R.id.action_add_provider) {
                dialogAddProviderBinding = DialogAddProviderBinding.inflate(inflater);
                AlertDialog alertDialog = new AlertDialog.Builder(context, R.style.AlertDialogStyle)
                        .setView(dialogAddProviderBinding.getRoot())
                        .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                            String provider = dialogAddProviderBinding.providerEditText.getText().toString();
                            if (isValidUri(provider)) {
                                new AddProviderAsyncTask(provider, binding).execute();
                            } else {
                                MainActivity.makeSnackbar("Url is invalid", Snackbar.LENGTH_LONG)
                                        .show();
                            }
                        })
                        .setNegativeButton(android.R.string.no, null)
                        .create();

                alertDialog.show();

                dialogAddProviderBinding.filePicker.setOnClickListener(v1 -> {
                    MainActivity.setSelectFileActivityLaunched(true);
                    String[] types = { "*/*" };
                    openDocumentLauncher.launch(types);
                });

                dialogAddProviderBinding.providerTypeRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                    if (checkedId == R.id.providerTypeRemote) {
                        dialogAddProviderBinding.filePicker.setVisibility(View.GONE);
                        dialogAddProviderBinding.providerEditText.setEnabled(true);
                        dialogAddProviderBinding.providerEditText.setHint(R.string.dialog_add_provider_hint);
                        dialogAddProviderBinding.providerEditText.setText("");
                    } else if (checkedId == R.id.providerTypeLocal) {
                        dialogAddProviderBinding.filePicker.setVisibility(View.VISIBLE);
                        dialogAddProviderBinding.providerEditText.setEnabled(false);
                        dialogAddProviderBinding.providerEditText.setHint("");
                        dialogAddProviderBinding.providerEditText.setText("");
                    }
                });
                return true;
            }
            return false;
        });

        final boolean[] noScroll = { false };
        final int[] previousDistanceFromFirstCellToTop = {0};
        binding.providerListView.setOnScrollListener(new ExpandableListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL && noScroll[0]) {
                    if (binding.providerActions.isShown()) binding.providerActions.hide();
                    else binding.providerActions.show();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem == 0 && visibleItemCount == totalItemCount) {
                    noScroll[0] = true;
                } else {
                    noScroll[0] = false;
                    View firstCell = binding.providerListView.getChildAt(0);
                    if (firstCell == null) {
                        return;
                    }
                    int distanceFromFirstCellToTop = firstVisibleItem * firstCell.getHeight() - firstCell.getTop();
                    if (distanceFromFirstCellToTop < previousDistanceFromFirstCellToTop[0]) {
                        binding.providerActions.show();
                    } else if (distanceFromFirstCellToTop > previousDistanceFromFirstCellToTop[0]) {
                        binding.providerActions.hide();
                    }
                    previousDistanceFromFirstCellToTop[0] = distanceFromFirstCellToTop;
                }
            }
        });

        if (binding.providerListView.getVisibility() == View.GONE) {
            AlphaAnimation animation = new AlphaAnimation(0f, 1f);
            animation.setDuration(500);
            animation.setStartOffset(50);
            animation.setFillAfter(true);

            binding.providerListView.setVisibility(View.VISIBLE);
            binding.providerListView.startAnimation(animation);
        } else {
            binding.loadingBarProvider.setVisibility(View.GONE);
        }

        new SetDomainCountAsyncTask(100, binding).execute();

        return binding.getRoot();
    }

    private boolean isValidUri(String uri) {
        return URLUtil.isHttpUrl(uri) || URLUtil.isHttpsUrl(uri) || URLUtil.isContentUrl(uri) || URLUtil.isFileUrl(uri);
    }

    private static class AddProviderAsyncTask extends AsyncTask<Void, Void, Void> {
        private final String provider;
        private BlockUrlProvider blockUrlProvider;
        private final FragmentProviderBinding binding;

        AddProviderAsyncTask(String provider, FragmentProviderBinding binding) {
            this.provider = provider;
            this.binding = binding;
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
            if (binding.providerListView.getAdapter() instanceof BlockUrlProviderAdapter) {
                BlockUrlProviderAdapter adapter = (BlockUrlProviderAdapter) binding.providerListView.getAdapter();
                adapter.add(blockUrlProvider);
                adapter.notifyDataSetChanged();

                new LoadProviderAsyncTask(blockUrlProvider, binding).execute();
            }
            binding.loadingBarProvider.setVisibility(View.GONE);
        }
    }

    private static class LoadProviderAsyncTask extends AsyncTask<Void, Void, Void> {
        private final BlockUrlProvider provider;
        private final FragmentProviderBinding binding;

        LoadProviderAsyncTask(BlockUrlProvider provider, FragmentProviderBinding binding) {
            this.provider = provider;
            this.binding = binding;
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
            if (binding.providerListView.getAdapter() instanceof BlockUrlProviderAdapter) {
                BlockUrlProviderAdapter adapter = (BlockUrlProviderAdapter) binding.providerListView.getAdapter();
                adapter.notifyDataSetChanged();
                AlphaAnimation animation = new AlphaAnimation(0f, 1f);
                animation.setDuration(500);
                animation.setStartOffset(50);
                animation.setFillAfter(true);
                binding.providerListView.setVisibility(View.VISIBLE);
                binding.providerListView.startAnimation(animation);
            }
            binding.loadingBarProvider.setVisibility(View.GONE);
        }
    }

    private static class UpdateProviderAsyncTask extends AsyncTask<Void, Void, Void> {
        private final WeakReference<Context> contextWeakReference;
        private final boolean updateProviders;
        private final FragmentProviderBinding binding;

        UpdateProviderAsyncTask(Context context, boolean updateProviders, FragmentProviderBinding binding) {
            this.contextWeakReference = new WeakReference<>(context);
            this.updateProviders = updateProviders;
            this.binding = binding;
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
            binding.loadingBarProvider.setVisibility(View.GONE);

            new SetProviderAsyncTask(binding).execute();

            binding.providerSwipeContainer.setRefreshing(false);

            new SetDomainCountAsyncTask(0, binding).execute();
        }
    }

    private static class SetProviderAsyncTask extends AsyncTask<Void, Void, List<BlockUrlProvider>> {
        private final FragmentProviderBinding binding;

        SetProviderAsyncTask(FragmentProviderBinding binding) {
            this.binding = binding;
        }

        @Override
        protected List<BlockUrlProvider> doInBackground(Void... voids) {
            AppDatabase appDatabase = AdhellFactory.getInstance().getAppDatabase();
            return appDatabase.blockUrlProviderDao().getAll2();
        }

        @Override
        protected void onPostExecute(List<BlockUrlProvider> providers) {
            if (binding.providerListView.getAdapter() instanceof BlockUrlProviderAdapter) {
                BlockUrlProviderAdapter adapter = (BlockUrlProviderAdapter) binding.providerListView.getAdapter();
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

                binding.loadingBarProvider.setVisibility(View.GONE);

                if (binding.providerListView.getVisibility() == View.GONE) {
                    AlphaAnimation animation = new AlphaAnimation(0f, 1f);
                    animation.setDuration(500);
                    animation.setStartOffset(50);
                    animation.setFillAfter(true);

                    binding.providerListView.setVisibility(View.VISIBLE);
                    binding.providerListView.startAnimation(animation);
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
        private final int delay;
        private final FragmentProviderBinding binding;

        SetDomainCountAsyncTask(int delay, FragmentProviderBinding binding) {
            this.delay = delay;
            this.binding = binding;
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
            binding.infoTextView.setText(String.format(strTotalUniqueDomains, count));
            binding.loadingBarProvider.setVisibility(View.GONE);
        }
    }
}
