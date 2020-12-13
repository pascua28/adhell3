package com.fusionjack.adhell3.fragments;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.fragment.app.Fragment;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.databinding.FragmentShowBlockedUrlsBinding;
import com.fusionjack.adhell3.db.AppDatabase;
import com.fusionjack.adhell3.utils.AdhellFactory;
import com.fusionjack.adhell3.utils.BlockUrlUtils;

import java.lang.ref.WeakReference;
import java.util.List;

public class ProviderContentFragment extends Fragment {

    private String searchText;
    private Long providerId;
    private FragmentShowBlockedUrlsBinding binding;

    void setProviderId(Long providerId) {
        this.providerId = providerId;
        new LoadBlockedUrlAsyncTask(getContext(), providerId, binding).execute();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.searchText = "";
        this.providerId = null;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        binding = FragmentShowBlockedUrlsBinding.inflate(inflater);
        binding.loadingBarContent.setVisibility(View.VISIBLE);
        new LoadBlockedUrlAsyncTask(getContext(), providerId, binding).execute();

        binding.providerListSwipeContainer.setOnRefreshListener(() -> {
            providerId = null;
            binding.loadingBarContent.setVisibility(View.VISIBLE);
            new LoadBlockedUrlAsyncTask(getContext(), null, binding).execute();
        });

        return binding.getRoot();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.search_menu, menu);

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
                new FilterUrlAsyncTask(text, providerId, getContext(), binding).execute();
                return false;
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private static class LoadBlockedUrlAsyncTask extends AsyncTask<Void, Void, List<String>> {
        private final WeakReference<Context> contextReference;
        private final Long providerId;
        private AppDatabase appDatabase;
        private FragmentShowBlockedUrlsBinding binding;

        LoadBlockedUrlAsyncTask(Context context, Long providerId, FragmentShowBlockedUrlsBinding binding) {
            this.contextReference = new WeakReference<>(context);
            this.appDatabase = AdhellFactory.getInstance().getAppDatabase();
            this.providerId = providerId;
            this.binding = binding;
        }

        @Override
        protected List<String> doInBackground(Void... o) {
            return providerId == null ?
                    BlockUrlUtils.getAllBlockedUrls(appDatabase) :
                    BlockUrlUtils.getBlockedUrls(providerId, appDatabase);
        }

        @Override
        protected void onPostExecute(List<String> blockedUrls) {
            Context context = contextReference.get();
            if (context != null) {
                ArrayAdapter<String> itemsAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, blockedUrls);
                binding.blockedUrlList.setAdapter(itemsAdapter);

                binding.providerListSwipeContainer.setRefreshing(false);

                binding.totalBlockedUrls.setText(String.format("%s%s", context.getString(R.string.total_domains), blockedUrls.size()));

                binding.loadingBarContent.setVisibility(View.GONE);

                if (binding.blockedUrlList.getVisibility() == View.GONE) {
                    AlphaAnimation animation = new AlphaAnimation(0f, 1f);
                    animation.setDuration(500);
                    animation.setStartOffset(50);
                    animation.setFillAfter(true);

                    binding.blockedUrlList.setVisibility(View.VISIBLE);
                    binding.blockedUrlList.startAnimation(animation);
                }
            }
            // Clean resource to prevent memory leak
            this.appDatabase = null;
            this.binding = null;
        }
    }

    private static class FilterUrlAsyncTask extends AsyncTask<Void, Void, List<String>> {
        private final WeakReference<Context> contextReference;
        private final String text;
        private final Long providerId;
        private AppDatabase appDatabase;
        private FragmentShowBlockedUrlsBinding binding;

        FilterUrlAsyncTask(String text, Long providerId, Context context, FragmentShowBlockedUrlsBinding binding) {
            this.text = text;
            this.providerId = providerId;
            this.contextReference = new WeakReference<>(context);
            this.appDatabase = AdhellFactory.getInstance().getAppDatabase();
            this.binding = binding;
        }

        @Override
        protected List<String> doInBackground(Void... o) {
            final String filterText = '%' + text + '%';
            if (providerId == null) {
                return text.isEmpty() ? BlockUrlUtils.getAllBlockedUrls(appDatabase) :
                        BlockUrlUtils.getFilteredBlockedUrls(filterText, appDatabase);
            }
            return text.isEmpty() ? BlockUrlUtils.getBlockedUrls(providerId, appDatabase) :
                    BlockUrlUtils.getFilteredBlockedUrls(filterText, providerId, appDatabase);
        }

        @Override
        protected void onPostExecute(List<String> list) {
            Context context = contextReference.get();
            if (context != null) {
                ArrayAdapter<String> itemsAdapter = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, list);
                binding.blockedUrlList.setAdapter(itemsAdapter);
                itemsAdapter.notifyDataSetChanged();
            }
            // Clean resource to prevent memory leak
            this.appDatabase = null;
            this.binding = null;
        }
    }
}
