package com.fusionjack.adhell3.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.appcompat.app.AppCompatActivity;

import com.fusionjack.adhell3.R;
import com.fusionjack.adhell3.adapter.DomainPagerAdapter;
import com.google.android.material.tabs.TabLayout;

import static com.fusionjack.adhell3.fragments.DomainTabPageFragment.PROVIDER_LIST_PAGE;

public class DomainTabFragment extends TabFragment {

    private final int[] imageResId = {
            R.drawable.ic_blacklist,
            R.drawable.ic_whitelist,
            R.drawable.ic_providerlist,
            R.drawable.ic_searchlist
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        getActivity().setTitle("Domains Management");
        AppCompatActivity parentActivity = (AppCompatActivity) getActivity();
        if (parentActivity.getSupportActionBar() != null) {
            parentActivity.getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            parentActivity.getSupportActionBar().setHomeButtonEnabled(false);
            parentActivity.getSupportActionBar().setDisplayShowCustomEnabled(false);
        }
        setHasOptionsMenu(true);

        DomainPagerAdapter adapter = new DomainPagerAdapter(getChildFragmentManager(), getContext());
        View view = inflateFragment(R.layout.fragment_domains, inflater, container, adapter, 1, imageResId, 0);

        // Select provider list tab as default
        TabLayout tabLayout = view.findViewById(R.id.sliding_tabs);
        TabLayout.Tab tab = tabLayout.getTabAt(PROVIDER_LIST_PAGE);
        if (tab != null) {
            tab.select();
        }

        return view;
    }




}
