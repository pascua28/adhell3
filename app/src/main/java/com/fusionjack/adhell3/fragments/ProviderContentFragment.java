package com.fusionjack.adhell3.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.utils.AppPreferences;
import com.fusionjack.adhell3.utils.LogUtils;
import com.fusionjack.adhell3.utils.UiUtils;
import com.fusionjack.adhell3.utils.rx.RxSingleComputationBuilder;
import com.fusionjack.adhell3.utils.rx.RxSingleIoBuilder;
import com.fusionjack.adhell3.viewmodel.ProviderViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.core.SingleOnSubscribe;

public class ProviderContentFragment extends Fragment {

    private String searchText;

    private TextView totalUrlsTextView;
    private SwipeRefreshLayout swipeContainer;

    private List<String> initialList;
    private List<String> adapterList;
    private ArrayAdapter<String> adapter;

    private boolean isInProgress;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        this.searchText = "";
        this.initialList = Collections.emptyList();
        this.adapterList = new ArrayList<>();
        this.adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, adapterList);

        this.isInProgress = false;
        AppPreferences.getInstance().resetCurrentProviderId();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_show_blocked_urls, container, false);

        ListView listView = view.findViewById(R.id.blocked_url_list);
        listView.setAdapter(adapter);

        this.totalUrlsTextView = view.findViewById(R.id.total_blocked_urls);
        setTotalCount();

        this.swipeContainer = view.findViewById(R.id.providerListSwipeContainer);
        swipeContainer.setOnRefreshListener(() -> loadList(ProviderViewModel.ALL_PROVIDERS));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!isInProgress) {
            long providerId = AppPreferences.getInstance().getCurrentProviderId();
            loadList(providerId);
        }
    }

    private void loadList(long providerId) {
        LogUtils.info("Loading list with provider id: " + providerId);
        Consumer<List<String>> urlCallback = list -> {
            LogUtils.info("List size:" + list.size());
            safeGuard(() -> {
                isInProgress = false;
                initialList = list;
                updateList(initialList);
            });
        };
        Runnable onSubscribedCallBack = () -> {
            isInProgress = true;
            if (swipeContainer.isRefreshing()) swipeContainer.setRefreshing(false);
            AppPreferences.getInstance().setCurrentProviderId(providerId);
        };

        ProviderViewModel viewModel = new ViewModelProvider(this).get(ProviderViewModel.class);
        new RxSingleComputationBuilder().async(viewModel.getUrls(providerId), onSubscribedCallBack, urlCallback, () -> {});
    }

    void safeGuard(Runnable action) {
        if (getView() == null) {
            LogUtils.error("View is null");
            return;
        }
        action.run();
    }

    private void updateList(List<String> newList) {
        adapterList.clear();
        adapterList.addAll(newList);
        adapter.notifyDataSetChanged();
        setTotalCount();
    }

    private void setTotalCount() {
        String totalUrlsPlaceholder = getResources().getString(R.string.total_domains_placeholder);
        totalUrlsTextView.setText(String.format(totalUrlsPlaceholder, adapterList.size()));
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.search_menu, menu);
        UiUtils.setMenuIconColor(menu, getContext());

        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setMaxWidth(Integer.MAX_VALUE);
        if (!searchText.isEmpty()) {
            searchView.setQuery(searchText, false);
            searchView.setIconified(false);
            searchView.requestFocus();
        }
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String text) {
                searchText = text;
                if (text.isEmpty()) {
                    updateList(initialList);
                } else {
                    SingleOnSubscribe<List<String>> source = emitter -> {
                        List<String> filteredList = adapterList.stream()
                                .filter(url -> url.toLowerCase().contains(text.toLowerCase()))
                                .collect(Collectors.toList());
                        emitter.onSuccess(filteredList);
                    };
                    new RxSingleIoBuilder().async(Single.create(source), list -> updateList(list));
                }
                return false;
            }
        });
        UiUtils.setSearchIconColor(searchView, getContext());
    }
}
