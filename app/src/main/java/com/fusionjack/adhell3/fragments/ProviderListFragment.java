package com.fusionjack.adhell3.fragments;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.fusionjack.adhell3.db.entity.BlockUrlProvider;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.viewmodel.BlockUrlProvidersViewModel;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.tabs.TabLayout;
import com.leinardi.android.speeddial.SpeedDialActionItem;

import java.util.List;

import static com.fusionjack.adhell3.fragments.DomainTabPageFragment.PROVIDER_CONTENT_PAGE;

public class ProviderListFragment extends Fragment {
    private Context context;
    private FragmentActivity activity;
    private DialogAddProviderBinding dialogAddProviderBinding;

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

        BlockUrlProvidersViewModel providersViewModel = new ViewModelProvider(activity).get(BlockUrlProvidersViewModel.class);

        // Set URL limit
        String strFormat = getResources().getString(R.string.provider_info);
        binding.providerInfoTextView.setText(String.format(strFormat, AdhellAppIntegrity.BLOCK_URL_LIMIT));

        // Set observer for domain count
        providersViewModel.getDomainCountInfo(getContext()).observe(
                getViewLifecycleOwner(),
                binding.infoTextView::setText
        );

        // Set observer for loading bar
        providersViewModel.getLoadingBarVisibility().observe(
                getViewLifecycleOwner(),
                isVisible -> {
                    if (isVisible) {
                        if (!binding.providerSwipeContainer.isRefreshing() && binding.providerListView.getVisibility() == View.GONE) {
                            binding.loadingBarProvider.setVisibility(View.VISIBLE);
                        }

                        if (binding.providerListView.getVisibility() == View.VISIBLE) {
                            binding.providerListView.setVisibility(View.GONE);
                        }
                    } else {
                        binding.loadingBarProvider.setVisibility(View.GONE);
                        binding.providerSwipeContainer.setRefreshing(false);

                        if (binding.providerListView.getVisibility() == View.GONE) {
                            binding.providerListView.setVisibility(View.VISIBLE);
                        }
                    }
                }
        );

        // Provider list
        providersViewModel.getBlockUrlProviders().observe(getViewLifecycleOwner(), blockUrlProviders -> {
            ListAdapter adapter = binding.providerListView.getAdapter();
            if (adapter == null) {
                BlockUrlProviderAdapter arrayAdapter = new BlockUrlProviderAdapter(context, blockUrlProviders);
                binding.providerListView.setAdapter(arrayAdapter);

                for (int i = 0; i < arrayAdapter.getCount(); i++) {
                    BlockUrlProvider provider = arrayAdapter.getItem(i);
                    if (provider != null) {
                        BlockUrlProvider dbProvider = getProvider(provider.id, blockUrlProviders);
                        if (dbProvider != null) {
                            provider.count = dbProvider.count;
                            provider.lastUpdated = dbProvider.lastUpdated;
                        }
                    }
                }
                arrayAdapter.notifyDataSetChanged();
            } else if (binding.providerListView.getAdapter() != null && binding.providerListView.getAdapter() instanceof BlockUrlProviderAdapter) {
                BlockUrlProviderAdapter blockUrlProviderAdapter = ((BlockUrlProviderAdapter) binding.providerListView.getAdapter());
                blockUrlProviderAdapter.updatedBlockUrlProviderList(blockUrlProviders);
                blockUrlProviderAdapter.notifyDataSetChanged();
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

        binding.providerSwipeContainer.setOnRefreshListener(() -> providersViewModel.updateProvider(context, true));

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
                                Uri uri = Uri.parse(provider);
                                if (uri != null && uri.getScheme() != null && uri.getScheme().equals("content")) {
                                    try {
                                        context.grantUriPermission(context.getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
                                        context.getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
                                    } catch (Exception e) {
                                        if (getActivity() instanceof MainActivity) {
                                            MainActivity mainActivity = (MainActivity) getActivity();
                                            mainActivity.makeSnackbar("Unable to get read permission", Snackbar.LENGTH_LONG)
                                                    .show();
                                        }
                                    }
                                }
                                providersViewModel.addProvider(provider, getContext());
                            } else {
                                if (getActivity() instanceof MainActivity) {
                                    MainActivity mainActivity = (MainActivity) getActivity();
                                    mainActivity.makeSnackbar("Url is invalid", Snackbar.LENGTH_LONG)
                                            .show();
                                }
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

        providersViewModel.setDomainCount();

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        dialogAddProviderBinding = null;
    }

    private boolean isValidUri(String uri) {
        return URLUtil.isHttpUrl(uri) || URLUtil.isHttpsUrl(uri) || URLUtil.isContentUrl(uri) || URLUtil.isFileUrl(uri);
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
