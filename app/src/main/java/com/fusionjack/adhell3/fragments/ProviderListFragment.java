package com.fusionjack.adhell3.fragments;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.BlockUrlProviderAdapter;
import com.fusionjack.adhell3.db.entity.BlockUrlProvider;
import com.fusionjack.adhell3.tasks.DomainRxTaskFactory;
import com.fusionjack.adhell3.utils.AdhellAppIntegrity;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.viewmodel.BlockUrlProvidersViewModel;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.CompletableObserver;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.annotations.NonNull;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;

import static com.fusionjack.adhell3.fragments.DomainTabPageFragment.PROVIDER_CONTENT_PAGE;

public class ProviderListFragment extends Fragment {
    private Context context;
    private FragmentActivity activity;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.context = getContext();
        this.activity = getActivity();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_provider, container, false);

        BlockUrlProvidersViewModel model = new ViewModelProvider(activity).get(BlockUrlProvidersViewModel.class);

        // Init URL limit
        TextView hintTextView = view.findViewById(R.id.providerInfoTextView);
        String strFormat = getResources().getString(R.string.provider_info);
        hintTextView.setText(String.format(strFormat, AdhellAppIntegrity.BLOCK_URL_LIMIT));

        // Init total domain count - LiveData
        TextView infoTextView = view.findViewById(R.id.infoTextView);
        initDomainCount(model, infoTextView);

        // Init provider list with an empty list - LiveData
        List<BlockUrlProvider> providerList = new ArrayList<>();
        BlockUrlProviderAdapter providerAdapter = new BlockUrlProviderAdapter(context, providerList);
        ListView providerListView = view.findViewById(R.id.providerListView);
        providerListView.setAdapter(providerAdapter);
        initProviderList(model, providerList, providerAdapter);

        providerListView.setOnItemClickListener((parent, view1, position, id) -> {
            BlockUrlProvider provider = (BlockUrlProvider) parent.getItemAtPosition(position);
            List<Fragment> fragments = getFragmentManager().getFragments();
            for (Fragment fragment : fragments) {
                if (fragment instanceof ProviderContentFragment) {
                    ((ProviderContentFragment) fragment).setProviderId(provider.id);
                }
            }
            TabLayout tabLayout = getParentFragment().getActivity().findViewById(R.id.domains_sliding_tabs);
            if (tabLayout != null) {
                TabLayout.Tab tab = tabLayout.getTabAt(PROVIDER_CONTENT_PAGE);
                if (tab != null) {
                    tab.select();
                }
            }
        });

        SwipeRefreshLayout dnsSwipeContainer = view.findViewById(R.id.providerSwipeContainer);
        dnsSwipeContainer.setOnRefreshListener(() ->
                updateAllProviders(dnsSwipeContainer)
        );

        FloatingActionsMenu providerFloatMenu = view.findViewById(R.id.provider_actions);
        FloatingActionButton actionAddProvider = view.findViewById(R.id.action_add_provider);
        actionAddProvider.setIcon(R.drawable.ic_event_note_white_24dp);
        actionAddProvider.setOnClickListener(v -> {
            providerFloatMenu.collapse();
            View dialogView = inflater.inflate(R.layout.dialog_add_provider, container, false);
            new AlertDialog.Builder(context)
                    .setView(dialogView)
                    .setPositiveButton(android.R.string.yes, (dialog, whichButton) -> {
                        EditText providerEditText = dialogView.findViewById(R.id.providerEditText);
                        String providerUrl = providerEditText.getText().toString();
                        if (URLUtil.isValidUrl(providerUrl)) {
                            addProvider(providerUrl);
                        } else {
                            Toast.makeText(getContext(), "Url is invalid", Toast.LENGTH_LONG).show();
                        }
                    })
                    .setNegativeButton(android.R.string.no, null).show();
        });

        return view;
    }

    private void initProviderList(BlockUrlProvidersViewModel model, List<BlockUrlProvider> providerList, BlockUrlProviderAdapter providerAdapter) {
        model.getBlockUrlProviders()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<LiveData<List<BlockUrlProvider>>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull LiveData<List<BlockUrlProvider>> liveData) {
                        liveData.observe(getViewLifecycleOwner(), _providerList -> {
                            providerList.clear();
                            providerList.addAll(_providerList);
                            providerAdapter.notifyDataSetChanged();
                        });
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {

                    }
                });
    }

    private void initDomainCount(BlockUrlProvidersViewModel model, TextView infoTextView) {
        model.getDomainCount()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<LiveData<Integer>>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        String strFormat = getResources().getString(R.string.total_unique_domains);
                        infoTextView.setText(String.format(strFormat, 0));
                    }

                    @Override
                    public void onSuccess(@NonNull LiveData<Integer> liveData) {
                        liveData.observe(getViewLifecycleOwner(), count -> {
                            String strFormat = context.getResources().getString(R.string.total_unique_domains);
                            infoTextView.setText(String.format(strFormat, count));
                        });
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        LogUtils.error(e.getMessage(), e);
                    }
                });
    }

    private void addProvider(String providerUrl) {
        DomainRxTaskFactory.addProvider(providerUrl)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<BlockUrlProvider>() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                    }

                    @Override
                    public void onSuccess(@NonNull BlockUrlProvider provider) {
                        ProgressDialog dialog = new ProgressDialog(context);
                        DomainRxTaskFactory.loadProvider(provider)
                                .subscribeOn(Schedulers.computation())
                                .subscribe(new CompletableObserver() {
                                    @Override
                                    public void onSubscribe(@NonNull Disposable d) {
                                        dialog.setMessage("Loading provider ...");
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
                                    }
                                });
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        LogUtils.error(e.getMessage(), e);
                    }
                });
    }

    private void updateAllProviders(SwipeRefreshLayout swipeContainer) {
        ProgressDialog dialog = new ProgressDialog(context);
        boolean hasInternetAccess = AdhellFactory.getInstance().hasInternetAccess(context);
        DomainRxTaskFactory.updateAllProviders(hasInternetAccess)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new CompletableObserver() {
                    @Override
                    public void onSubscribe(@NonNull Disposable d) {
                        swipeContainer.setRefreshing(false);
                        dialog.setMessage("Updating providers ...");
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
                    }
                });
    }

}
